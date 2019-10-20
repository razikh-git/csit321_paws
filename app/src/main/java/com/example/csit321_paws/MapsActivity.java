package com.example.csit321_paws;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.os.ResultReceiver;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.android.gms.maps.model.UrlTileProvider;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.Places;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapsActivity
        extends BottomNavBarActivity
        implements OnMapReadyCallback
{

    SharedPreferences mSharedPref;
    SharedPreferences.Editor mSharedEditor;

    private static final String TAG = "snowpaws_maps";

    private static final String BUNDLE_KEY = "MapViewBundleKey";
    private static final String CAMERA_KEY = "MapCameraPositionKey";
    private static final String LOCATION_KEY = "MapLocationKey";

    private static final int DEFAULT_ZOOM = 5;
    private static final int DASH_WIDTH = 30;
    private static final int GAP_WIDTH = 20;
    private static final int MAP_TILE_WIDTH = 256;
    private static final int BTN_STROKE_WIDTH = 5;
    private static final int POLY_STROKE_WIDTH = 10;
    private static final int POLY_SELECT_RANGE = 250000;

    private AddressResultReceiver mResultReeceiver;
    private ArrayList<Address> mAddressResult;
    protected Location mLastLocation;

    // The entry points to the Places API.
    private GeoDataClient mGeoDataClient;
    private PlaceDetectionClient mPlaceDetectionClient;

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationClient;

    private MapView mMapView;
    private GoogleMap mMap;
    private CameraPosition mCameraPosition;

    private boolean mIsPolyDrawing;
    private Polyline mPolyLine;
    private List<Polygon> mPolyList = new ArrayList<>();

    private String mTileOverlayURL;

    private Map<String, TileOverlay> mTileOverlayMap;
    private Map<String, TileProvider> mTileProviderMap = new HashMap<>();
    private Map<String, TileOverlayOptions> mTileOverlayOptionsMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        // Load global preferences.
        mSharedPref = this.getSharedPreferences(
                getResources().getString(R.string.app_global_preferences), Context.MODE_PRIVATE);

        // Load saved state.
        Bundle bundle = null;
        if (savedInstanceState != null) {
            bundle = savedInstanceState.getBundle(BUNDLE_KEY);
            mCameraPosition = savedInstanceState.getParcelable(CAMERA_KEY);
            mLastLocation = savedInstanceState.getParcelable(LOCATION_KEY);
        }

        // Load the activity layout.
        setContentView(R.layout.activity_maps);

        // Construct a GeoDataClient.
        mGeoDataClient = Places.getGeoDataClient(this, null);

        // Construct a PlaceDetectionClient.
        mPlaceDetectionClient = Places.getPlaceDetectionClient(this, null);

        // Construct a FusedLocationProviderClient.
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialise bottom sheet fields.
        mResultReeceiver = new AddressResultReceiver(new Handler());
        initSheet();

        // Bottom navigation bar functionality.
        BottomNavigationView nav = (BottomNavigationView)findViewById(R.id.bottomNavigation);
        nav.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // Button functionality.
        findViewById(R.id.laySheetHeader).setOnClickListener((view) -> {onSheetHeaderClick(view);});
        //findViewById(R.id.cardSearch).setOnClickListener((view) -> {onSearchClick(view);});
        findViewById(R.id.btnMapPolyDraw).setOnClickListener((view) -> {onMapPolyDrawClick(view);});
        findViewById(R.id.btnMapPolyErase).setOnClickListener((view) -> {onMapPolyEraseClick(view);});
        findViewById(R.id.btnMapWeatherRedirect).setOnClickListener((view) -> {onMapWeatherRedirectClick(view);});
        findViewById(R.id.btnMapTypePopout).setOnClickListener((view) -> {onMapTypePopoutClick(view);});
        findViewById(R.id.btnMapTypeDefault).setOnClickListener((view) -> {onMapTypeButtonClick(view);});
        findViewById(R.id.btnMapTypeSatellite).setOnClickListener((view) -> {onMapTypeButtonClick(view);});
        findViewById(R.id.btnMapTypeTerrain).setOnClickListener((view) -> {onMapTypeButtonClick(view);});
        findViewById(R.id.btnMapOverlayWind).setOnClickListener((view) -> {onMapOverlayButtonClick(view);});
        findViewById(R.id.btnMapOverlayPrecip).setOnClickListener((view) -> {onMapOverlayButtonClick(view);});
        findViewById(R.id.btnMapOverlayRisk).setOnClickListener((view) -> {onMapOverlayButtonClick(view);});

        // Prepare the map.
        mTileOverlayURL = getString(R.string.app_url_owm_map_root)
                +"%s/%s/%d/%d.png?appid=%s";
        mMapView = findViewById(R.id.mapView);
        mMapView.onCreate(bundle);
        mMapView.getMapAsync(this);
    }

    private void onMapWeatherRedirectClick(View view) {
        // Redirect to weather screen with data from the current marker.
        Intent intent = new Intent(this, WeatherActivity.class);
        if (mLastLocation != null) {
            intent.putExtra(RequestCode.EXTRA_LATLNG,
                    new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
        }
        startActivityForResult(intent, RequestCode.REQUEST_WEATHER_BY_LOCATION);
    }

    private void onMapPolyDrawClick(View view) {

        // TODO change colour scheme of buttons

        if (mMap != null) {
            if (mIsPolyDrawing) {
                // Clear polylines in progress.
                mPolyLine.setPoints(new ArrayList<>());
                // Reset interface layout.
                findViewById(R.id.btnMapPolyDraw).setBackgroundColor(
                        ContextCompat.getColor(this, R.color.color_on_primary));
                findViewById(R.id.btnMapPolyDraw).setBackgroundDrawable(
                        getDrawable(R.drawable.ic_draw_selected));
                findViewById(R.id.btnMapPolyErase).setVisibility(View.GONE);
                findViewById(R.id.btnMapWeatherRedirect).setVisibility(View.VISIBLE);
                // Reset click event listeners.
                mMap.setOnMapClickListener((latLng) -> { onMapDefaultClick(latLng);});
                mMap.setOnMapLongClickListener((latLng) -> { onMapDefaultLongClick(latLng);});
            } else {
                // Show contextual interface.
                findViewById(R.id.btnMapPolyDraw).setBackgroundColor(
                        ContextCompat.getColor(this, R.color.color_primary_alt));
                findViewById(R.id.btnMapPolyDraw).setBackgroundDrawable(
                        getDrawable(R.drawable.ic_draw));
                findViewById(R.id.btnMapPolyErase).setVisibility(View.VISIBLE);
                findViewById(R.id.btnMapWeatherRedirect).setVisibility(View.GONE);
                // Use click event listeners for live drawing.
                mMap.setOnMapClickListener((latLng) -> {
                    onMapDrawingClick(latLng);});
                mMap.setOnMapLongClickListener((latLng) -> {
                    onMapDrawingLongClick(latLng);});
            }
        }

        mIsPolyDrawing = !mIsPolyDrawing;
    }

    private void onMapPolyEraseClick(View view) {

        // TODO : selective removal of polygons

        // Clear all screen polygons and polylines
        mPolyLine.setPoints(new ArrayList<>());
        for (Polygon polygon : mPolyList) {
            polygon.remove();
        }
        mPolyList.clear();
    }

    private void onMapTypePopoutClick(View view) {
        togglePopoutButton();
    }

    private void togglePopoutButton() {
        if (findViewById(R.id.cardMapType).getVisibility() == View.VISIBLE) {
            // Change button style.
            ((FloatingActionButton)findViewById(R.id.btnMapTypePopout)).setBackgroundColor(
                    ContextCompat.getColor(this, R.color.color_primary_alt));
            ((FloatingActionButton)findViewById(R.id.btnMapTypePopout)).setImageDrawable(
                    getDrawable(R.drawable.ic_eye_settings));

            // Reveal other FABs and hide the map type picker popout.
            findViewById(R.id.btnMapPolyDraw).setVisibility(View.VISIBLE);
            findViewById(R.id.btnMapWeatherRedirect).setVisibility(View.VISIBLE);
            findViewById(R.id.cardMapType).setVisibility(View.GONE);
        } else {
            // Change button style.
            ((FloatingActionButton)findViewById(R.id.btnMapTypePopout)).setBackgroundColor(
                    ContextCompat.getColor(this, R.color.color_on_primary));
            ((FloatingActionButton)findViewById(R.id.btnMapTypePopout)).setImageDrawable(
                    getDrawable(R.drawable.ic_eye_settings_outline));

            // Hide other FABs and show the map type picker popout.
            mIsPolyDrawing = true;
            onMapPolyDrawClick(null);
            findViewById(R.id.btnMapPolyDraw).setVisibility(View.GONE);
            findViewById(R.id.btnMapWeatherRedirect).setVisibility(View.GONE);
            findViewById(R.id.cardMapType).setVisibility(View.VISIBLE);
        }
    }

    private void onMapTypeButtonClick(View view) {
        int pad = BTN_STROKE_WIDTH;
        int type;
        int idBtn;
        int idTxt;
        switch (view.getId()) {
            case R.id.btnMapTypeDefault: {
                idBtn = R.id.btnMapTypeDefault;
                idTxt = R.id.txtMapTypeDefault;
                type = GoogleMap.MAP_TYPE_NORMAL;
                break;
            }
            case R.id.btnMapTypeSatellite: {
                idBtn = R.id.btnMapTypeSatellite;
                idTxt = R.id.txtMapTypeSatellite;
                type = GoogleMap.MAP_TYPE_SATELLITE;
                break;
            }
            case R.id.btnMapTypeTerrain: {
                idBtn = R.id.btnMapTypeTerrain;
                idTxt = R.id.txtMapTypeTerrain;
                type = GoogleMap.MAP_TYPE_TERRAIN;
                break;
            }
            default:
                return;
        }

        if (mMap.getMapType() == type)
            return;

        // Reset element styles.
        findViewById(R.id.btnMapTypeDefault).setPadding(0, 0, 0, 0);
        findViewById(R.id.btnMapTypeSatellite).setPadding(0, 0, 0, 0);
        findViewById(R.id.btnMapTypeTerrain).setPadding(0, 0, 0, 0);
        ((TextView)findViewById(R.id.txtMapTypeDefault)).setTextColor(
                ContextCompat.getColor(this, R.color.color_grey));
        ((TextView)findViewById(R.id.txtMapTypeSatellite)).setTextColor(
                ContextCompat.getColor(this, R.color.color_grey));
        ((TextView)findViewById(R.id.txtMapTypeTerrain)).setTextColor(
                ContextCompat.getColor(this, R.color.color_grey));

        // Highlight elements for the selected map type.
        findViewById(idBtn).setPadding(pad, pad, pad, pad);
        ((TextView)findViewById(idTxt)).setTextColor(
                ContextCompat.getColor(this, R.color.color_primary_alt));

        // Set the map type.
        mMap.setMapType(type);
    }

    private void onMapOverlayButtonClick(View view) {
        String str = null;

        // Toggle the visibility of the additional map overlays.
        switch (view.getId()) {
            // Only one OpenWeatherMaps overlay may be visible at a time.
            case R.id.btnMapOverlayWind: {
                str = getString(R.string.app_url_map_layer_wind);
                break;
            }
            case R.id.btnMapOverlayPrecip: {
                str = getString(R.string.app_url_map_layer_precip);
                break;
            }
            case R.id.btnMapOverlayRisk: {
                // Display risk assessment.

                // TODO risk assessment and display

                break;
            }
        }
        // Toggle the selected overlay.
        if (str != null) {
            mTileOverlayMap.get(str).setVisible(!mTileOverlayMap.get(str).isVisible());
        }
    }

    private void onMapClick(LatLng latLng) {
        // Manage interface elements.

        // Toggle map type popout menu visibility.
        if (findViewById(R.id.cardMapType).getVisibility() == View.VISIBLE) {
            // Hide the map type picker popout and show the popout button.
            togglePopoutButton();
        }
        else
        {
            // Toggle searchbar visibility.
            if (findViewById(R.id.cardSearch).getVisibility() != View.VISIBLE)
                findViewById(R.id.cardSearch).setVisibility(View.VISIBLE);
            else
                findViewById(R.id.cardSearch).setVisibility(View.GONE);

            // Toggle bottomsheet visibility.
            BottomSheetBehavior sheetBehavior = BottomSheetBehavior.from(findViewById(R.id.sheetView));
            if (sheetBehavior.getState() != BottomSheetBehavior.STATE_COLLAPSED) {
                // Show the bottom sheet
                sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                // Adjust the map to fit
                mMapView.setPadding(0, 0, 0,
                        (int)getResources().getDimension(R.dimen.height_sheets));
            }
            else
            {
                // Hide the bottom sheet
                // TODO : get hidden functionality working for fullscreen map view
                //sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                // Expand the map to fill
                mMapView.setPadding(0, 0, 0, 0);
            }
        }
    }

    private void onMapDefaultClick(LatLng latLng) {
        onMapClick(latLng);
    }

    private void onMapDefaultLongClick(LatLng latLng) {
        // Remove old markers.
        mMap.clear();

        // Place a new marker on the held location and move the camera.
        mMap.addMarker(new MarkerOptions().position(latLng));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        // Reinitialise the location information.
        mLastLocation = new Location(LocationManager.GPS_PROVIDER);
        mLastLocation.setLongitude(latLng.longitude);
        mLastLocation.setLatitude(latLng.latitude);
        startIntentService();
    }

    private void onMapDrawingClick(LatLng latLng) {
        //onMapClick(latLng);

        List<LatLng> polyPoints = mPolyLine.getPoints();
        for (LatLng point : polyPoints) {

            // Identify whether the user closed the polygon.
            float[] results = new float[1];
            Location.distanceBetween(latLng.latitude, latLng.longitude,
                    point.latitude, point.longitude,
                    results);

            double range = POLY_SELECT_RANGE / mCameraPosition.zoom;
            if (results[0] < range) {
                // Generate a new polygon from the points.
                PolygonOptions polyOptions = new PolygonOptions();
                polyOptions.strokeWidth(POLY_STROKE_WIDTH);

                // TODO : add fill+stroke colour algorithmically by depth

                for (LatLng p : polyPoints) {
                    polyOptions.add(p);
                }
                mPolyList.add(mMap.addPolygon(polyOptions));

                // Clear the polyline from the map.
                mPolyLine.setPoints(new ArrayList<>());
                return;
            }
        }

        // Add another coordinate point to the polyline.
        polyPoints.add(latLng);
        mPolyLine.setPoints(polyPoints);
    }

    private void onMapDrawingLongClick(LatLng latLng) {
        List<LatLng> polyPoints = mPolyLine.getPoints();
        if (polyPoints.size() > 0) {
            polyPoints.remove(polyPoints.size() - 1);
            mPolyLine.setPoints(polyPoints);
        }
    }

    private void onSearchClick(View view) {

    }

    private void onSheetHeaderClick(View view) {
        BottomSheetBehavior sheetBehavior = BottomSheetBehavior.from(findViewById(R.id.sheetView));
        if (sheetBehavior.getState() != BottomSheetBehavior.STATE_COLLAPSED)
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        else
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }
    private void onCameraMove() {
        mCameraPosition = mMap.getCameraPosition();
    }

    // TODO : map/camera reset function

    private void initSheet() {
        Log.d(TAG, "initSheet");
        // Prepare the initial location information.
        mFusedLocationClient.getLastLocation().addOnSuccessListener(
            (location -> {
                mLastLocation = location;
                if (mLastLocation == null) {
                    return;
                }

                // Push an error message on geocoder failure.
                if (!Geocoder.isPresent()) {
                    Toast.makeText(this,
                            R.string.sv_fa_geocoder_unavailable,
                            Toast.LENGTH_LONG).show();
                    return;
                }

                Log.d(TAG, "Location success: "
                        + "Lat " + location.getLatitude()
                        + " Lng " + location.getLongitude());

                // Request an address from the current location.
                startIntentService();
            })
        );
    }

    private void updateLocationDisplay() {
        Log.d(TAG, "updateLocationDisplay");
        String str = null;

        // Debug print the full address.
        for (Address address : mAddressResult) {
            for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                Log.d(TAG, address.getAddressLine(i));
            }
        }

        // Reposition the camera, and zoom in to a reasonably broad scope.
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), DEFAULT_ZOOM));

        // Place a marker.
        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude())));

        // Set the location title
        // eg. 95 Iris St, Beacon Hill NSW 2100, Australia
        // ==> Beacon Hill NSW 2100
        ((TextView)findViewById(R.id.txtSheetTitle)).setText("");
        ((TextView)findViewById(R.id.txtSheetCoordinates)).setText("");
        if (mAddressResult != null) {
            str = mAddressResult.get(0).getAddressLine(0).split(", ", 3)[1];
            ((TextView)findViewById(R.id.txtSheetTitle)).setText(str);

            // Set the coordinates display.
            str = new DecimalFormat("#.##").format(mAddressResult.get(0).getLongitude());
            str += " " + new DecimalFormat("#.##").format(mAddressResult.get(0).getLatitude());
            ((TextView)findViewById(R.id.txtSheetCoordinates)).setText(str);

        }
        // Set the coordinates display.
        double lng = mAddressResult.get(0).getLongitude();
        double lat = mAddressResult.get(0).getLatitude();
        char bearingLng = lng > 0 ? 'E' : 'W';
        char bearingLat = lat > 0 ? 'N' : 'S';
        str = new DecimalFormat("#.##").format(Math.abs(lng)) + " " + bearingLng
                + "  " + new DecimalFormat("#.##").format(Math.abs(lat)) + " " + bearingLat;
        ((TextView)findViewById(R.id.txtSheetCoordinates)).setText(str);
    }

    protected void startIntentService() {
        Log.d(TAG, "startIntentService");
        FetchAddressIntentService.startActionFetchAddress(this,
                mResultReeceiver, mLastLocation);
    }

    private void initTileOverlayMaps(String key) {
        mTileProviderMap.put(
                key, new UrlTileProvider(MAP_TILE_WIDTH, MAP_TILE_WIDTH) {
                    @Override
                    public URL getTileUrl(int x, int y, int zoom) {
                        String s = String.format(Locale.US, mTileOverlayURL,
                                key,
                                zoom, x, y, getString(R.string.open_weather_maps_key));
                        try {
                            return new URL(s);
                        } catch (MalformedURLException e) {
                            throw new AssertionError(e);
                        }
                    }
                }
        );
        mTileOverlayOptionsMap.put(
                key, new TileOverlayOptions().visible(false).fadeIn(true)
                        .tileProvider(mTileProviderMap.get(key)));
        mTileOverlayMap.put(
                key, mMap.addTileOverlay(mTileOverlayOptionsMap.get(key)));
    }

    private void initTileOverlays() {
        mTileOverlayMap = new HashMap<>();
        String str = "";
        try {
            // Wind layer
            str = getString(R.string.app_url_map_layer_wind);
            initTileOverlayMaps(str);
            // Precipitation layer
            str = getString(R.string.app_url_map_layer_precip);
            initTileOverlayMaps(str);

        } catch (NullPointerException e) {
            Log.println(Log.DEBUG, TAG, "No tile overlay was returned by provider for " + str + ".");
            e.printStackTrace();
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady");

        mMap = googleMap;

        // Camera event listener.
        mCameraPosition = mMap.getCameraPosition();
        googleMap.setOnCameraMoveListener(() -> {onCameraMove();});

        // Initialise click event listeners.
        googleMap.setOnMapClickListener((latLng) -> {
            onMapDefaultClick(latLng);});
        googleMap.setOnMapLongClickListener((latLng) -> {
            onMapDefaultLongClick(latLng);});

        // Highlight elements for the default map type.
        int pad = BTN_STROKE_WIDTH;
        findViewById(R.id.btnMapTypeDefault).setPadding(pad, pad, pad, pad);
        ((TextView)findViewById(R.id.txtMapTypeDefault)).setTextColor(
                ContextCompat.getColor(this, R.color.color_primary_alt));

        // Set the map type.
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Setup map polyline graphics.
        PolylineOptions polyOptions = new PolylineOptions();
        polyOptions.color(ContextCompat.getColor(this, R.color.color_error));
        polyOptions.pattern(Arrays.asList(
                new Dash(DASH_WIDTH), new Gap(GAP_WIDTH)
        ));
        polyOptions.startCap(new RoundCap());
        polyOptions.endCap(new RoundCap());
        mPolyLine = mMap.addPolyline(polyOptions);

        // Setup map overlays.
        initTileOverlays();
    }

     class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
         protected void onReceiveResult(int resultCode, Bundle resultData) {
            Log.d(TAG, "onReceiveResult");
            if (resultData == null) {
                return;
            }
            String error = resultData.getString(FetchAddressCode.RESULT_DATA_KEY);
            if (error.equals("")) {
                mAddressResult = resultData.getParcelableArrayList(FetchAddressCode.RESULT_ADDRESSLIST_KEY);
            } else {
                // TODO Display an error message.
            }

            // Update the interface with the new location.
            updateLocationDisplay();
        }
     }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle bundle = outState.getBundle(BUNDLE_KEY);
        if (bundle != null) {
            bundle = new Bundle();
            outState.putBundle(BUNDLE_KEY, bundle);
            outState.putParcelable(LOCATION_KEY, mLastLocation);
            outState.putParcelable(CAMERA_KEY, mMap.getCameraPosition());
        }
        mMapView.onSaveInstanceState(bundle);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mCameraPosition = savedInstanceState.getParcelable(CAMERA_KEY);
        mLastLocation = savedInstanceState.getParcelable(LOCATION_KEY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }
}
