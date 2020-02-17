package com.amw188.csit321_paws;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.android.gms.maps.model.UrlTileProvider;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.maps.android.PolyUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity
        extends
                LocationActivity
        implements
                OnMapReadyCallback,
        TrackingService.LocationResultListener,
                GoogleMap.OnPoiClickListener,
                GoogleMap.OnMarkerClickListener
{
    SharedPreferences mSharedPref;

    private static final String TAG = "snowpaws_ma";

    private static final String BUNDLE_KEY = "MapViewBundleKey";
    private static final String CAMERA_KEY = "MapCameraPositionKey";
    private static final String LOCATION_KEY = "MapLocationKey";
    private static final String ISTRACKING_KEY = "MapIsTrackingKey";

    private static final String TIMER_KEY = "PAWSCooldown";
    private static final long NOTIFICATION_COOLDOWN_INTERVAL = 30000;

    private static final int DEFAULT_ZOOM = 5;
    private static final int DASH_WIDTH = 30;
    private static final int GAP_WIDTH = 20;
    private static final int MAP_TILE_WIDTH = 256;
    private static final int BTN_STROKE_WIDTH = 5;
    private static final int POLY_STROKE_WIDTH = 10;
    private static final int POLY_SELECT_RANGE = 250000;

    private static final int LOCATION_REQUEST_DEFAULT_INTERVAL = 5000;

    // Google Maps
    private Bundle mBundle;
    private MapView mMapView;
    private GoogleMap mMap;
    private CameraPosition mCameraPosition;

    // Map Elements
    private Marker mMarker;
    private boolean mIsPolyDrawing;
    private Polyline mPolyLine;
    private List<Polygon> mPolyList = new ArrayList<>();

    // OpenWeatherMaps
    private String mTileOverlayURL;
    private Map<String, TileOverlay> mTileOverlayMap;
    private Map<String, TileProvider> mTileProviderMap = new HashMap<>();
    private Map<String, TileOverlayOptions> mTileOverlayOptionsMap = new HashMap<>();

    // PAWS Location Service
    private TrackingService mTrackingService;
    private TrackingReceiver mTrackingReceiver;
    private boolean mBound;
    private boolean mIsTrackingLocation;
    private boolean mIsCooldown;
    private Timer mCooldownTimer;

    private class TrackingReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location = intent.getParcelableExtra(TrackingService.EXTRA_LOCATION);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        // Load global preferences.
        mSharedPref = this.getSharedPreferences(
                getResources().getString(R.string.app_global_preferences), Context.MODE_PRIVATE);

        // Load saved state.
        if (savedInstanceState != null) {
            mBundle = savedInstanceState.getBundle(BUNDLE_KEY);
            mCameraPosition = savedInstanceState.getParcelable(CAMERA_KEY);
            mLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            mIsTrackingLocation = savedInstanceState.getBoolean(ISTRACKING_KEY);
        }

        // Load the activity layout.
        setContentView(R.layout.activity_maps);

        // Bottom navigation bar functionality.
        BottomNavigationView nav = (BottomNavigationView)findViewById(R.id.bottomNavigation);
        nav.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // Beg for permissions. Block all further functionality without them.
        if (checkHasPermissions(RequestCode.PERMISSION_MULTIPLE,
                RequestCode.REQUEST_PERMISSIONS_LOCATION)) {
            if (checkHasPermissions(RequestCode.PERMISSION_MULTIPLE,
                    RequestCode.REQUEST_PERMISSIONS_NETWORK)) {
                // Continue with the activity.
                initActivity();
            }
        }

    }

    private void initActivity() {
        // Initialise buttons.
        initButtons();

        // Prepare PAWS location tracking.
        mTrackingReceiver = new TrackingReceiver();
        mCooldownTimer = new Timer(TIMER_KEY);

        // Prepare the map.
        mTileOverlayURL = getString(R.string.app_url_owm_map_root)
                +"%s/%s/%d/%d.png?appid=%s";
        mMapView = findViewById(R.id.mapView);
        mMapView.onCreate(mBundle);
        mMapView.getMapAsync(this);
    }

    private void onMapWeatherRedirectClick(View view) {
        // Redirect to weather screen with data from the current marker.
        Intent intent = new Intent(this, WeatherActivity.class);
        if (mLocation != null) {
            intent.putExtra(RequestCode.EXTRA_LATLNG,
                    new LatLng(mLocation.getLatitude(), mLocation.getLongitude()));
        }
        startActivityForResult(intent, RequestCode.REQUEST_WEATHER_BY_LOCATION);
    }

    private void onMapPolyDrawClick(View view) {

        // TODO Change button styles (this solution is deprecated).

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

        // TODO : Selective removal of polygons.

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
                ContextCompat.getColor(this, R.color.color_midtone));
        ((TextView)findViewById(R.id.txtMapTypeSatellite)).setTextColor(
                ContextCompat.getColor(this, R.color.color_midtone));
        ((TextView)findViewById(R.id.txtMapTypeTerrain)).setTextColor(
                ContextCompat.getColor(this, R.color.color_midtone));

        // Highlight elements for the selected map type.
        findViewById(idBtn).setPadding(pad, pad, pad, pad);
        ((TextView)findViewById(idTxt)).setTextColor(
                ContextCompat.getColor(this, R.color.color_accent_alt));

        // Set the map type.
        mMap.setMapType(type);
    }

    private void onMapOverlayButtonClick(View view) {
        String str = null;

        // Toggle visibility of the additional map tile overlays.
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
                // Toggle visibility of the polydraw overlay.
                boolean isVis = true;
                if (!mPolyList.isEmpty())
                    if (mPolyList.get(0).isVisible())
                        isVis = false;
                for (Polygon poly : mPolyList)
                    poly.setVisible(isVis);
                break;
            }
        }
        // Toggle tile overlay.
        if (str != null && mTileOverlayMap.containsKey(str)) {
            mTileOverlayMap.get(str).setVisible(!mTileOverlayMap.get(str).isVisible());
        }
    }

    private void onMapDefaultClick(LatLng latLng) {
        // Toggle map type popout menu visibility.
        if (findViewById(R.id.cardMapType).getVisibility() == View.VISIBLE) {
            // Hide the map type picker popout and show the popout button.
            togglePopoutButton();
        }
        else
        {
            // Toggle searchbar visibility.
            /*
            if (findViewById(R.id.cardSearch).getVisibility() != View.VISIBLE)
                findViewById(R.id.cardSearch).setVisibility(View.VISIBLE);
            else
                findViewById(R.id.cardSearch).setVisibility(View.GONE);
             */

            // Toggle bottomsheet visibility.
            BottomSheetBehavior sheetBehavior = BottomSheetBehavior.from(findViewById(R.id.sheetView));
            if (sheetBehavior.getState() != BottomSheetBehavior.STATE_COLLAPSED) {
                // Show the bottom sheet
                sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        }
    }

    private void onMapDefaultLongClick(LatLng latLng) {
        // Remove old markers.
        if (mMarker != null)
            mMarker.remove();

        // Place a new marker on the held location and move the camera.
        mMarker = mMap.addMarker(new MarkerOptions().position(latLng));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        // Reinitialise the location information.
        mLocation = new Location(LocationManager.GPS_PROVIDER);
        mLocation.setLongitude(latLng.longitude);
        mLocation.setLatitude(latLng.latitude);
        fetchAddress();
    }

    private void onMapDrawingClick(LatLng latLng) {
        List<LatLng> polyPoints = mPolyLine.getPoints();

        if (polyPoints.size() > 0) {
            // Identify whether the user closed the polygon.
            LatLng startPoint = polyPoints.get(0);
            float[] distance = new float[1];
            Location.distanceBetween(latLng.latitude, latLng.longitude,
                    startPoint.latitude, startPoint.longitude,
                    distance);

            double pow = 1;
            if (mCameraPosition.zoom >= 15)
                pow = 3.5;
            else if (mCameraPosition.zoom >= 13.5)
                pow = 3;
            else if (mCameraPosition.zoom >= 11)
                pow = 2.5;
            else if (mCameraPosition.zoom >= 8.5)
                pow = 2;
            double scale = Math.pow(mCameraPosition.zoom, pow);
            double range = POLY_SELECT_RANGE / scale;

            Log.d(TAG, "onMapDrawingClick() : "
                    + "\nTap   : "
                    + new DecimalFormat("#.######").format(latLng.latitude) + " "
                    + new DecimalFormat("#.######").format(latLng.longitude)
                    + "\nStart : "
                    + new DecimalFormat("#.######").format(startPoint.latitude) + " "
                    + new DecimalFormat("#.######").format(startPoint.longitude)
                    + "\nDist  : "
                    + distance[0]
                    + "\nMPP   : "
                    + new DecimalFormat("#.##").format(scale)
                    + " at zoom " + mCameraPosition.zoom
                    + " pow " + pow
                    + "\nRange : "
                    + new DecimalFormat("#.##").format(range)
                    + "\n"
                    + distance[0] + (distance[0] < range ? " < " : " > ")
                    + new DecimalFormat("#.##").format(range)
                    + "\nd     : " + (distance[0] - range)
            );

            if (distance[0] < range) {
                if (polyPoints.size() > 2) {
                    // Generate a new polygon from the points.
                    PolygonOptions polyOptions = new PolygonOptions();
                    polyOptions.strokeWidth(POLY_STROKE_WIDTH);
                    int strokeColor = ContextCompat.getColor(this, R.color.color_risk_low);
                    int fillColor = ContextCompat.getColor(this, R.color.color_risk_low_fill);

                    // Check for bounding box overlaps.
                    for (LatLng polyPoint : polyPoints) {
                        for (Polygon polygon : mPolyList) {
                            if (PolyUtil.containsLocation(polyPoint, polygon.getPoints(), false)) {
                                // Change polygon style.
                                if (polygon.getStrokeColor() == ContextCompat.getColor(
                                        this, R.color.color_risk_low)) {
                                    strokeColor = ContextCompat.getColor(
                                            this, R.color.color_risk_med);
                                    fillColor = ContextCompat.getColor(
                                            this, R.color.color_risk_med_fill);
                                } else {
                                    strokeColor = ContextCompat.getColor(
                                            this, R.color.color_risk_high);
                                    fillColor = ContextCompat.getColor(
                                            this, R.color.color_risk_high_fill);
                                    break;
                                }
                            }
                        }
                    }
                    polyOptions.strokeColor(strokeColor);
                    polyOptions.fillColor(fillColor);

                    for (LatLng p : polyPoints) {
                        polyOptions.add(p);
                    }
                    mPolyList.add(mMap.addPolygon(polyOptions));

                    // Clear the polyline from the map.
                    mPolyLine.setPoints(new ArrayList<>());
                    return;
                }
            }
        }

        // Add another coordinate point to the polyline.
        polyPoints.add(latLng);
        mPolyLine.setPoints(polyPoints);
    }

    private void onMapDrawingLongClick(LatLng latLng) {
        List<LatLng> polyPoints = mPolyLine.getPoints();
        if (polyPoints.size() > 0) {
            // Pop the last point on the polyline.
            polyPoints.remove(polyPoints.size() - 1);
            // Clear the list if there are no lines visible.
            if (polyPoints.size() == 1)
                polyPoints.remove(0);
            mPolyLine.setPoints(polyPoints);
        }
    }

    private void onSheetHeaderClick(View view) {
        BottomSheetBehavior sheetBehavior = BottomSheetBehavior.from(findViewById(R.id.sheetView));
        if (sheetBehavior.getState() != BottomSheetBehavior.STATE_COLLAPSED)
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        else
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void onMapTrackingButtonClick(View view) {
        // Start or end the location tracking service.
        if (mIsTrackingLocation) {
            // Change element styling.
            findViewById(R.id.viewFABPadding).setVisibility(View.GONE);
            findViewById(R.id.laySheetHeader).setBackgroundColor(
                    ContextCompat.getColor(this, R.color.color_primary_alt));
            ((MaterialButton)findViewById(R.id.btnMapTracking)).setText(
                    getString(R.string.ma_tracking_enable));
            findViewById(R.id.btnMapTracking).setBackgroundColor(
                    ContextCompat.getColor(this, R.color.color_primary_alt));

            // End the location tracking service.
            mIsTrackingLocation = false;
            stopLocationRequests();

        } else {
            // Attempt to start the location tracking service.
            checkToStartLocationRequests();
        }
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

    private boolean initButtons() {
        // Button functionality.
        try {
            //findViewById(R.id.cardSearch).setOnClickListener((view) -> {onSearchClick(view);});
            findViewById(R.id.laySheetHeader).setOnClickListener((view) -> { onSheetHeaderClick(view); });
            findViewById(R.id.btnMapPolyDraw).setOnClickListener((view) -> { onMapPolyDrawClick(view); });
            findViewById(R.id.btnMapPolyErase).setOnClickListener((view) -> { onMapPolyEraseClick(view); });
            findViewById(R.id.btnMapWeatherRedirect).setOnClickListener((view) -> { onMapWeatherRedirectClick(view); });
            findViewById(R.id.btnMapTypePopout).setOnClickListener((view) -> { onMapTypePopoutClick(view); });
            findViewById(R.id.btnMapTypeDefault).setOnClickListener((view) -> { onMapTypeButtonClick(view); });
            findViewById(R.id.btnMapTypeSatellite).setOnClickListener((view) -> { onMapTypeButtonClick(view); });
            findViewById(R.id.btnMapTypeTerrain).setOnClickListener((view) -> { onMapTypeButtonClick(view); });
            findViewById(R.id.btnMapOverlayWind).setOnClickListener((view) -> { onMapOverlayButtonClick(view); });
            findViewById(R.id.btnMapOverlayPrecip).setOnClickListener((view) -> { onMapOverlayButtonClick(view); });
            findViewById(R.id.btnMapOverlayRisk).setOnClickListener((view) -> { onMapOverlayButtonClick(view); });
            findViewById(R.id.btnMapTracking).setOnClickListener((view) -> { onMapTrackingButtonClick(view);});
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
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
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it. This method will only be triggered once the user has installed Google Play services
     * and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady");

        mMap = googleMap;

        // Camera event listener.
        mCameraPosition = mMap.getCameraPosition();
        googleMap.setOnCameraMoveListener(() -> {onCameraMove();});

        // Initialise click event listeners.
        googleMap.setOnPoiClickListener(this);
        googleMap.setOnMapClickListener((latLng) -> {
            onMapDefaultClick(latLng);});
        googleMap.setOnMapLongClickListener((latLng) -> {
            onMapDefaultLongClick(latLng);});

        // Highlight elements for the default map type.
        int pad = BTN_STROKE_WIDTH;
        findViewById(R.id.btnMapTypeDefault).setPadding(pad, pad, pad, pad);
        ((TextView)findViewById(R.id.txtMapTypeDefault)).setTextColor(
                ContextCompat.getColor(this, R.color.color_accent_alt));

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

        // Setup location services.
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fetchLocation();
            initLocationRequestServices();
        } else {
            checkHasPermissions(RequestCode.PERMISSION_MULTIPLE,
                    RequestCode.REQUEST_PERMISSIONS_LOCATION);
        }
    }

    @Override
    public void onPoiClick(PointOfInterest poi) {
        if (poi.name.toLowerCase().endsWith("beach")) {
            Toast.makeText(this,
                    "POI: " + poi.name,
                    Toast.LENGTH_LONG).show();

            // On click:
            /*
                Redirect to a new content view for the beach

                Display:
                    Name
                    Town
                    Region, State

                    Update Timestamp

                    Marine
                        Temp
                        Swell
                        Tide
                        Hours
                        Warnings
                        Hazards

                Options:
                    Favourite
                    Directions

             */
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Toast.makeText(this, "Marker: " + marker.getTitle() + " (" + marker.getId() + ")",
                Toast.LENGTH_LONG).show();
        return true;
    }

    private void onCameraMove() {
        mCameraPosition = mMap.getCameraPosition();

    }

    private void initLocationData() {
        fetchLocation();
        initLocationRequestServices();
    }

    @Override
    protected void onLocationReceived() {
        // Request an address from the current location.
        fetchAddress();
    }

    @Override
    protected void onAddressReceived() {
        updateLocationDisplay();
    }

    private void updateLocationDisplay() {
        Log.d(TAG, "updateLocationDisplay");
        String str = null;

        // Debug print the full address.
        for (Address address : mAddress) {
            for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                Log.d(TAG, address.getAddressLine(i));
            }
        }

        if (mMarker == null)
            // Reposition the camera, and zoom in to a reasonably broad scope.
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLocation.getLatitude(), mLocation.getLongitude()), DEFAULT_ZOOM));
        else
            mMarker.remove();

        // Place a marker at the current or chosen location.
        mMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(mLocation.getLatitude(), mLocation.getLongitude())));

        // Set the location title
        // eg. 95 Iris St, Beacon Hill NSW 2100, Australia
        // ==> Beacon Hill NSW 2100
        ((TextView)findViewById(R.id.txtSheetTitle)).setText("");
        ((TextView)findViewById(R.id.txtSheetCoordinates)).setText("");
        if (mAddress != null) {
            // TODO fix out of bounds exception for null address length.....
            str = mAddress.get(0).getAddressLine(0).split(", ", 3)[1];
            ((TextView)findViewById(R.id.txtSheetTitle)).setText(str);

            // Set the coordinates display.
            str = new DecimalFormat("#.##").format(mAddress.get(0).getLongitude());
            str += " " + new DecimalFormat("#.##").format(mAddress.get(0).getLatitude());
            ((TextView)findViewById(R.id.txtSheetCoordinates)).setText(str);

        }
        // Set the coordinates display.
        double lng = mAddress.get(0).getLongitude();
        double lat = mAddress.get(0).getLatitude();
        char bearingLng = lng > 0 ? 'E' : 'W';
        char bearingLat = lat > 0 ? 'N' : 'S';
        str = new DecimalFormat("#.##").format(Math.abs(lng)) + " " + bearingLng
                + "  " + new DecimalFormat("#.##").format(Math.abs(lat)) + " " + bearingLat;
        ((TextView)findViewById(R.id.txtSheetCoordinates)).setText(str);
    }

    private void initLocationRequestServices() {
        // Bind to the tracking service.

        /*
        bindService(new Intent(this, TrackingService.class),
                mServiceConnection, Context.BIND_AUTO_CREATE);
        */

        // Check to continue tracking from the previous app use.
        if (mIsTrackingLocation)
            checkToStartLocationRequests();
    }

    public void checkToStartLocationRequests() {
        // Check for enabled location usage.
        if (Integer.parseInt(mSharedPref.getString(
                SettingsActivity.KEY_PREF_LOCATION_PRIORITY,
                Integer.toString(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)))
                == LocationRequest.PRIORITY_NO_POWER) {
            Log.i(TAG, "Location usage is set to Disabled.");
            Toast.makeText(this,
                    R.string.ma_location_usage_disabled,
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Generate a new location request.
        LocationRequest locationRequest = createLocationRequest();

        // Check whether required location settings are enabled.
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this,
                locationSettingsResponse -> startLocationRequests(locationRequest));
    }

    private void startLocationRequests(LocationRequest locationRequest) {
        // Begin location operations.
        mIsTrackingLocation = true;
        mMap.setMyLocationEnabled(true);
        if (mTrackingService != null)
            mTrackingService.startTracking(locationRequest);

        // Change element styling.
        findViewById(R.id.viewFABPadding).setVisibility(View.VISIBLE);
        findViewById(R.id.laySheetHeader).setBackgroundColor(
                ContextCompat.getColor(this, R.color.color_error));
        ((MaterialButton)findViewById(R.id.btnMapTracking)).setText(
                getString(R.string.ma_tracking_disable));
        findViewById(R.id.btnMapTracking).setBackgroundColor(
                ContextCompat.getColor(this, R.color.color_error));
    }

    protected LocationRequest createLocationRequest() {
        // Inverse floating-point Preferences string-array workarounds
        int updatePriority = Integer.parseInt(mSharedPref.getString(
                SettingsActivity.KEY_PREF_LOCATION_PRIORITY,
                Integer.toString(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)));
        int updateInterval = Integer.parseInt(mSharedPref.getString(
                SettingsActivity.KEY_PREF_LOCATION_RATE,
                Integer.toString(LOCATION_REQUEST_DEFAULT_INTERVAL)));

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setFastestInterval(updateInterval);
        locationRequest.setInterval(updateInterval + LOCATION_REQUEST_DEFAULT_INTERVAL);
        locationRequest.setPriority(updatePriority);

        return locationRequest;
    }

    public void onLocationResultReceived(LocationResult locationResult) {
        Log.d("snowpaws", "Location result received.");

        for (Location location : locationResult.getLocations()) {
            for (Polygon polygon : mPolyList) {
                if (PolyUtil.containsLocation(location.getLatitude(), location.getLongitude(),
                        polygon.getPoints(), false)) {
                    // TODO : Send notification!!!!!! wow!
                    // this is literally the whole app
                    // we finally made it

                    int priority = 0;
                    if (polygon.getStrokeColor() == ContextCompat.getColor(
                            this, R.color.color_risk_high)) {
                        priority = NotificationCompat.PRIORITY_HIGH;
                    } else if (polygon.getStrokeColor() == ContextCompat.getColor(
                            this, R.color.color_risk_med)) {
                        priority = NotificationCompat.PRIORITY_DEFAULT;
                    } else {
                        priority = NotificationCompat.PRIORITY_LOW;
                    }

                    // make it work
                    priority = NotificationCompat.PRIORITY_HIGH;

                    // todo tood it still doesnt work

                    // Consider additional risk weighting.
                    int riskWeight = mSharedPref.getInt("selfanalysis_risk", 0);
                    priority = Math.min(NotificationCompat.PRIORITY_HIGH,
                            priority + (riskWeight != -1 ? riskWeight : 0));

                    // TODO : Customise cooldown timers per priority level doot
                    if (!mIsCooldown) {

                        Log.d("snowpaws", "Pushing location notification.");

                        Notifications.getInstance().show(this, priority);

                        if (mTrackingService != null)
                            mTrackingService.notify(priority);

                        mIsCooldown = true;
                        mCooldownTimer.schedule(
                                new TimerTask(){@Override public void run(){mIsCooldown = false;}},
                                NOTIFICATION_COOLDOWN_INTERVAL);
                    }
                }
            }
        }
    }

    private void stopLocationRequests() {
        mMap.setMyLocationEnabled(false);
        mIsTrackingLocation = false;
        mTrackingService.stopTracking(mFusedLocationClient);
    }

    // Monitors the state of the connection to the service.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TrackingService.LocalBinder binder = (TrackingService.LocalBinder) service;
            mTrackingService = binder.getService();
            mTrackingService.init(mFusedLocationClient, MapsActivity.this);
            mBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mTrackingService = null;
            mBound = false;
        }
    };

    @Override
    protected void onPermissionGranted(String perm) {}

    @Override
    protected void onPermissionBlocked(String perm) {}

    @Override
    protected void onAllPermissionsGranted(String[] permissions) {
        // Reinitialise the activity.
        initActivity();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle bundle = outState.getBundle(BUNDLE_KEY);
        if (bundle != null) {
            bundle = new Bundle();
            outState.putBundle(BUNDLE_KEY, bundle);
            outState.putParcelable(LOCATION_KEY, mLocation);
            outState.putParcelable(CAMERA_KEY, mMap.getCameraPosition());
            outState.putBoolean(ISTRACKING_KEY, mIsTrackingLocation);
        }
        if (mMapView != null)
            mMapView.onSaveInstanceState(bundle);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mCameraPosition = savedInstanceState.getParcelable(CAMERA_KEY);
        mLocation = savedInstanceState.getParcelable(LOCATION_KEY);
        mIsTrackingLocation = savedInstanceState.getBoolean(ISTRACKING_KEY);
        if (mIsTrackingLocation)
            checkToStartLocationRequests();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMapView != null)
            mMapView.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mTrackingReceiver,
                new IntentFilter(TrackingService.ACTION_BROADCAST));
    }

    @Override
    protected void onPause() {
        if (mMapView != null)
            mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mMapView != null)
            mMapView.onStart();

        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.
        bindService(new Intent(this, TrackingService.class), mServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        if (mBound) {
            unbindService(mServiceConnection);
            mBound = false;
        }
        if (mMapView != null)
            mMapView.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mMapView != null)
            mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        if (mMapView != null)
            mMapView.onLowMemory();
        super.onLowMemory();
    }
}
