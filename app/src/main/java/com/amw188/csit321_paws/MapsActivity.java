package com.amw188.csit321_paws;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
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
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
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
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.view.View.VISIBLE;

// todo: custom marker style/drawable for favourite locations

public class MapsActivity
        extends
                LocationActivity
        implements
                AddressHandler.AddressReceivedListener,
                LocationHandler.LocationReceivedListener,
				ServiceHandler.ConnectionListener,
                OnMapReadyCallback,
                GoogleMap.OnPoiClickListener,
                GoogleMap.OnMarkerClickListener
{
    private static final String TAG = PrefConstValues.tag_prefix + "a_map";

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

    // Google Maps
    private Bundle mBundle;
    private MapView mMapView;
    private GoogleMap mMap;
    private CameraPosition mCameraPosition;

    // Map elements
    private Marker mMarker;
    private boolean mIsPolyDrawing;
    private Polyline mPolyLine;
    private List<Polygon> mPolyList = new ArrayList<>();
    private View mInfoWindow;

    // OpenWeatherMaps
    private Map<String, TileOverlay> mTileOverlayMap;
    private Map<String, TileProvider> mTileProviderMap = new HashMap<>();
    private Map<String, TileOverlayOptions> mTileOverlayOptionsMap = new HashMap<>();

    // Locations
    private Address mSelectedAddress;

    // Notification Service connections
	private ServiceHandler mServiceHandler;

    /**
     * Custom info window implementation for location map markers.
     */
    public class CustomInfoAdapter implements GoogleMap.InfoWindowAdapter {
        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            return setupInfoWindow(marker);
        }
    }

    private View setupInfoWindow(Marker marker) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean isMetric = PAWSAPI.preferredMetric(sharedPref);

        // Title - Location name
		final String title = AddressHandler.getBestAddressTitle(mSelectedAddress);
        ((TextView)mInfoWindow.findViewById(R.id.txtInfoTitle)).setText(title);

        // Subtitle - Location broader area
        ((TextView)mInfoWindow.findViewById(R.id.txtInfoSubtitle)).setText(
                String.format("%s %s",
                        AddressHandler.getAustralianStateCode(mSelectedAddress),
                        mSelectedAddress.getPostalCode()));

        // Subtitle - Distance from last best location
        Location selectedLocation = new Location(LocationManager.GPS_PROVIDER);
        selectedLocation.setLatitude(marker.getPosition().latitude);
        selectedLocation.setLongitude(marker.getPosition().longitude);

        Location lastBestLocation = PAWSAPI.getLastBestLocation(sharedPref);
        if (lastBestLocation != null)
            ((TextView)mInfoWindow.findViewById(R.id.txtInfoDistance)).setText(
                    PAWSAPI.getDistanceString(isMetric,
                            lastBestLocation.distanceTo(selectedLocation)));

        // Body text - Place and weather summary
        ((TextView)mInfoWindow.findViewById(R.id.txtInfoContent)).setText(
                marker.getSnippet());

        // todo: check for favourite location
        try {
            JSONArray historyJson = new JSONArray(sharedPref.getString(
                    PrefKeys.position_history, PrefConstValues.empty_json_array));
            final int index = PAWSAPI.getPlaceIndexInHistory(historyJson, title);
            if (index >= 0)
                if (historyJson.getJSONObject(index) != null)
                    if (historyJson.getJSONObject(index).getBoolean("favorite"))
                        mInfoWindow.findViewById(R.id.imgFavorite).setBackgroundResource(
                                R.drawable.ic_star);
        } catch (JSONException ex) {
            Log.e(TAG, "Failed to parse history JSON for infowindow init.");
            ex.printStackTrace();
        }
    	return mInfoWindow;
	}

	private void onInfoWindowClick(Marker marker) {
        weatherRedirect();
    }

    private void weatherRedirect() {
        // Redirect to weather screen focusing on the current marker
        Intent intent = new Intent(this, PlaceInfoActivity.class);
        if (mSelectedLocation != null) {
            intent.putExtra(RequestCodes.EXTRA_LATLNG,
                    new LatLng(mSelectedLocation.getLatitude(), mSelectedLocation.getLongitude()));
            intent.putExtra(RequestCodes.EXTRA_PLACENAME,
                    ((TextView)mInfoWindow.findViewById(R.id.txtInfoTitle)).getText());
        }
        startActivityForResult(intent, RequestCodes.REQUEST_WEATHER_BY_LOCATION);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_maps);
		BottomNavigationView nav = findViewById(R.id.bottomNavigation);
		nav.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

		if (!init(savedInstanceState)) {
			Log.d(TAG, "Did not initialise MapsActivity immediately.");
		}
    }

    private boolean init(Bundle savedInstanceState) {
		// Load saved state
		if (savedInstanceState != null) {
			mBundle = savedInstanceState.getBundle(BUNDLE_KEY);
			mCameraPosition = savedInstanceState.getParcelable(CAMERA_KEY);
			mSelectedLocation = savedInstanceState.getParcelable(LOCATION_KEY);
		}

		mInfoWindow = getLayoutInflater().inflate(
				R.layout.view_infowindow, mMapView, false);

		// Bind to the notification service
		mServiceHandler = new ServiceHandler(this, this);

		// Beg for permissions
		// Don't continue with loading the activity without necessary permits
		if (checkHasPermissions(RequestCodes.PERMISSION_MULTIPLE,
				RequestCodes.REQUEST_PERMISSIONS_LOCATION)) {
			if (checkHasPermissions(RequestCodes.PERMISSION_MULTIPLE,
					RequestCodes.REQUEST_PERMISSIONS_NETWORK)) {
				return initActivity(savedInstanceState) && initClickables();
			}
		}
    	return false;
	}

    private boolean initActivity(Bundle savedInstanceState) {
        // Prepare the map
        mMapView = findViewById(R.id.mapView);
        mMapView.onCreate(mBundle);
        mMapView.getMapAsync(this);
        return true;
    }

	private boolean initClickables() {
		try {
			//findViewById(R.id.cardSearch).setOnClickListener(this::onSearchClick);
			findViewById(R.id.laySheetHeader).setOnClickListener(this::onSheetHeaderClick);
			findViewById(R.id.btnMapPolyDraw).setOnClickListener(this::onMapPolyDrawClick);
			findViewById(R.id.btnMapPolyErase).setOnClickListener(this::onMapPolyEraseClick);
			findViewById(R.id.btnMapLastLocation).setOnClickListener(this::onMapLastLocationClick);
			findViewById(R.id.btnMapTypePopout).setOnClickListener(this::onMapTypePopoutClick);
			findViewById(R.id.btnMapTypeDefault).setOnClickListener(this::onMapTypeButtonClick);
			findViewById(R.id.btnMapTypeSatellite).setOnClickListener(this::onMapTypeButtonClick);
			findViewById(R.id.btnMapTypeTerrain).setOnClickListener(this::onMapTypeButtonClick);
			findViewById(R.id.btnMapOverlayWind).setOnClickListener(this::onMapOverlayButtonClick);
			findViewById(R.id.btnMapOverlayPrecip).setOnClickListener(this::onMapOverlayButtonClick);
			findViewById(R.id.btnMapOverlayRisk).setOnClickListener(this::onMapOverlayButtonClick);
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}

    /**
     * Relocate the last known location of this device on the map and mark it.
     */
    private void onMapLastLocationClick(View view) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Location lastBestLocation = PAWSAPI.getLastBestLocation(sharedPref);
        new AddressHandler(this, this).awaitAddress(lastBestLocation);
    }

    private void onMapPolyDrawClick(View view) {

        // TODO Change button styles (this solution is deprecated).

        if (mMap != null) {
            if (mIsPolyDrawing) {
                // Clear polylines in progress
                mPolyLine.setPoints(new ArrayList<>());
                // Reset interface layout
                findViewById(R.id.btnMapPolyDraw).setBackgroundColor(
                        ContextCompat.getColor(this, R.color.color_on_primary));
                findViewById(R.id.btnMapPolyDraw).setBackgroundDrawable(
                        getDrawable(R.drawable.ic_draw_selected));
                findViewById(R.id.btnMapPolyErase).setVisibility(View.GONE);
                findViewById(R.id.btnMapLastLocation).setVisibility(VISIBLE);
                // Reset click event listeners
                mMap.setOnMapClickListener(this::onMapDefaultClick);
                mMap.setOnMapLongClickListener(this::onMapDefaultLongClick);
            } else {
                // Show contextual interface
                findViewById(R.id.btnMapPolyDraw).setBackgroundColor(
                        ContextCompat.getColor(this, R.color.color_primary_alt));
                findViewById(R.id.btnMapPolyDraw).setBackgroundDrawable(
                        getDrawable(R.drawable.ic_draw));
                findViewById(R.id.btnMapPolyErase).setVisibility(VISIBLE);
                findViewById(R.id.btnMapLastLocation).setVisibility(View.GONE);
                // Use click event listeners for live drawing
                mMap.setOnMapClickListener(this::onMapDrawingClick);
                mMap.setOnMapLongClickListener(this::onMapDrawingLongClick);
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
        final int pad = BTN_STROKE_WIDTH;
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

        // Reset element styles
        findViewById(R.id.btnMapTypeDefault).setPadding(0, 0, 0, 0);
        findViewById(R.id.btnMapTypeSatellite).setPadding(0, 0, 0, 0);
        findViewById(R.id.btnMapTypeTerrain).setPadding(0, 0, 0, 0);
        ((TextView)findViewById(R.id.txtMapTypeDefault)).setTextColor(
                ContextCompat.getColor(this, R.color.color_midtone));
        ((TextView)findViewById(R.id.txtMapTypeSatellite)).setTextColor(
                ContextCompat.getColor(this, R.color.color_midtone));
        ((TextView)findViewById(R.id.txtMapTypeTerrain)).setTextColor(
                ContextCompat.getColor(this, R.color.color_midtone));

        // Highlight elements for the selected map type
        findViewById(idBtn).setPadding(pad, pad, pad, pad);
        ((TextView)findViewById(idTxt)).setTextColor(
                ContextCompat.getColor(this, R.color.color_accent_alt));

        // Set the map type.
        mMap.setMapType(type);
    }

    private void onMapOverlayButtonClick(View view) {
        String str = null;

        // Toggle visibility of the additional map tile overlays
        switch (view.getId()) {
            // Only one OpenWeatherMaps overlay may be visible at a time
            case R.id.btnMapOverlayWind: {
                str = ConstStrings.app_url_map_layer_wind;
                break;
            }
            case R.id.btnMapOverlayPrecip: {
                str = ConstStrings.app_url_map_layer_precip;
                break;
            }
            case R.id.btnMapOverlayRisk: {
                // Toggle visibility of the polydraw overlay
                boolean isVis = true;
                if (!mPolyList.isEmpty())
                    if (mPolyList.get(0).isVisible())
                        isVis = false;
                for (Polygon poly : mPolyList)
                    poly.setVisible(isVis);
                break;
            }
        }
        // Toggle tile overlay
        if (str != null && mTileOverlayMap.containsKey(str) && mTileOverlayMap.get(str) != null) {
            mTileOverlayMap.get(str).setVisible(!mTileOverlayMap.get(str).isVisible());
        }
    }

    private void onMapDefaultClick(LatLng latLng) {
        // Toggle map type popout menu visibility
        if (findViewById(R.id.cardMapType).getVisibility() == VISIBLE) {
            // Hide the map type picker popout and show the popout button
            togglePopoutButton();
        }
        else
        {
            // Toggle searchbar visibility
            /*
            if (findViewById(R.id.cardSearch).getVisibility() != View.VISIBLE)
                findViewById(R.id.cardSearch).setVisibility(View.VISIBLE);
            else
                findViewById(R.id.cardSearch).setVisibility(View.GONE);
             */

            // Toggle bottomsheet visibility
            BottomSheetBehavior sheetBehavior = BottomSheetBehavior.from(
                    findViewById(R.id.sheetView));
            if (sheetBehavior.getState() != BottomSheetBehavior.STATE_COLLAPSED) {
                // Show the bottom sheet
                sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        }
    }

    private void onMapDefaultLongClick(LatLng latLng) {
        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setLatitude(latLng.latitude);
        location.setLongitude(latLng.longitude);
        new AddressHandler(this, this).awaitAddress(location);
        // todo: redirect marker to nearest location when address invalid or null
    }

    private void placeNewMarker(ArrayList<Address> addressResults) {
        try {
            if (addressResults == null || addressResults.size() <= 0)
                return;

            // Values outside of Australia are excluded and ignored
            final Address address = addressResults.get(0);
            if (!address.getCountryCode().equals("AU"))
                return;

            mSelectedAddress = address;

            // Remove existing marker
            if (mMarker != null)
                mMarker.remove();

            // Place a new marker on the held location and move the camera
            final LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
            mMarker = mMap.addMarker(new MarkerOptions().position(latLng));

            // Update the global location information
            mSelectedLocation = new Location(LocationManager.GPS_PROVIDER);
            mSelectedLocation.setLatitude(address.getLatitude());
            mSelectedLocation.setLongitude(address.getLongitude());

            // Update activity
            updateLocationDisplay(address);
        } catch (NullPointerException ex) {
            Log.e(TAG, "Failed to generate marker from null address.");
            ex.printStackTrace();
        }
    }

    private void onMapDrawingClick(LatLng latLng) {
        List<LatLng> polyPoints = mPolyLine.getPoints();

        if (polyPoints.size() > 0) {
            // Identify whether the user closed the polygon
            final LatLng startPoint = polyPoints.get(0);
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
            final double scale = Math.pow(mCameraPosition.zoom, pow);
            final double range = POLY_SELECT_RANGE / scale;

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
                    // Generate a new polygon from the points
                    PolygonOptions polyOptions = new PolygonOptions();
                    polyOptions.strokeWidth(POLY_STROKE_WIDTH);
                    int strokeColor = ContextCompat.getColor(
                            this, R.color.color_risk_low);
                    int fillColor = ContextCompat.getColor(
                            this, R.color.color_risk_low_fill);

                    // Check for bounding box overlaps
                    for (LatLng polyPoint : polyPoints) {
                        for (Polygon polygon : mPolyList) {
                            if (PolyUtil.containsLocation(polyPoint, polygon.getPoints(), false)) {
                                // Change polygon style
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

                    // Clear the polyline from the map
                    mPolyLine.setPoints(new ArrayList<>());
                    return;
                }
            }
        }

        // Add another coordinate point to the polyline
        polyPoints.add(latLng);
        mPolyLine.setPoints(polyPoints);
    }

    private void onMapDrawingLongClick(LatLng latLng) {
        List<LatLng> polyPoints = mPolyLine.getPoints();
        if (polyPoints.size() > 0) {
            // Pop the last point on the polyline
            polyPoints.remove(polyPoints.size() - 1);
            // Clear the list if there are no lines visible
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

    private void togglePopoutButton() {
        if (findViewById(R.id.cardMapType).getVisibility() == VISIBLE) {
            // Change button style
            (findViewById(R.id.btnMapTypePopout)).setBackgroundColor(
                    ContextCompat.getColor(this, R.color.color_primary_alt));
            ((FloatingActionButton)findViewById(R.id.btnMapTypePopout)).setImageDrawable(
                    getDrawable(R.drawable.ic_eye_settings));

            // Reveal other FABs and hide the map type picker popout
            findViewById(R.id.btnMapPolyDraw).setVisibility(VISIBLE);
            findViewById(R.id.btnMapLastLocation).setVisibility(VISIBLE);
            findViewById(R.id.cardMapType).setVisibility(View.GONE);
        } else {
            // Change button style
            (findViewById(R.id.btnMapTypePopout)).setBackgroundColor(
                    ContextCompat.getColor(this, R.color.color_on_primary));
            ((FloatingActionButton)findViewById(R.id.btnMapTypePopout)).setImageDrawable(
                    getDrawable(R.drawable.ic_eye_settings_outline));

            // Hide other FABs and show the map type picker popout
            mIsPolyDrawing = true;
            onMapPolyDrawClick(null);
            findViewById(R.id.btnMapPolyDraw).setVisibility(View.GONE);
            findViewById(R.id.btnMapLastLocation).setVisibility(View.GONE);
            findViewById(R.id.cardMapType).setVisibility(VISIBLE);
        }
    }

    private void initTileOverlayMaps(String layer) {
        mTileProviderMap.put(
                layer, new UrlTileProvider(MAP_TILE_WIDTH, MAP_TILE_WIDTH) {
                    @Override
                    public URL getTileUrl(int x, int y, int zoom) {
                        String url = OpenWeatherMapIntegration.getOWMTileURL(layer, x, y, zoom);
                        try {
                            return new URL(url);
                        } catch (MalformedURLException ex) {
                            throw new AssertionError(ex);
                        }
                    }
                }
        );
        mTileOverlayOptionsMap.put(
                layer, new TileOverlayOptions().visible(false).fadeIn(true)
                        .tileProvider(mTileProviderMap.get(layer)));
        mTileOverlayMap.put(
                layer, mMap.addTileOverlay(mTileOverlayOptionsMap.get(layer)));
    }

    private void initTileOverlays() {
        mTileOverlayMap = new HashMap<>();
        String str = "";
        try {
            // Wind layer
            str = ConstStrings.app_url_map_layer_wind;
            initTileOverlayMaps(str);
            // Precipitation layer
            str = ConstStrings.app_url_map_layer_precip;
            initTileOverlayMaps(str);

        } catch (NullPointerException ex) {
            Log.e(TAG, "No tile overlay was returned by provider for " + str + ".");
            ex.printStackTrace();
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

        // Camera event listener
        mCameraPosition = mMap.getCameraPosition();
        googleMap.setOnCameraMoveListener(this::onCameraMove);

        // Initialise click event listeners
        googleMap.setOnPoiClickListener(this);
        googleMap.setOnMapClickListener(this::onMapDefaultClick);
        googleMap.setOnMapLongClickListener(this::onMapDefaultLongClick);

        // Enable custom info windows
        googleMap.setInfoWindowAdapter(new CustomInfoAdapter());
        googleMap.setOnInfoWindowClickListener(this::onInfoWindowClick);

        // Highlight elements for the default map type
        final int pad = BTN_STROKE_WIDTH;
        findViewById(R.id.btnMapTypeDefault).setPadding(pad, pad, pad, pad);
        ((TextView)findViewById(R.id.txtMapTypeDefault)).setTextColor(
                ContextCompat.getColor(this, R.color.color_accent_alt));

        // Set map type
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Set map style
        final int nightMode = getResources().getConfiguration()
                .uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightMode == Configuration.UI_MODE_NIGHT_YES)
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(
                    this, R.raw.google_maps_night_style));

        // Setup map polyline graphics
        PolylineOptions polyOptions = new PolylineOptions();
        polyOptions.color(ContextCompat.getColor(this, R.color.color_error));
        polyOptions.pattern(Arrays.asList(
                new Dash(DASH_WIDTH), new Gap(GAP_WIDTH)
        ));
        polyOptions.startCap(new RoundCap());
        polyOptions.endCap(new RoundCap());
        mPolyLine = mMap.addPolyline(polyOptions);

        // Setup map overlays
        initTileOverlays();

        // Setup location services
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
        	// Mark the location from a provided position, otherwise use last best location
        	LatLng latLng = getLatLngFromIntent(getIntent().getExtras());
			Location location = new Location(LocationManager.GPS_PROVIDER);
        	if (latLng == null) {
				try {
					SharedPreferences sharedPref = PreferenceManager
                            .getDefaultSharedPreferences(this);
					JSONObject positionJson = new JSONObject(sharedPref.getString(
							PrefKeys.last_best_position, PrefConstValues.empty_json_object));
					if (positionJson.length() == 0) {
					    mServiceHandler.service().awaitLocation(this);
						Log.e(TAG, "Failed to read last best location.");
						return;
					}
					latLng = new LatLng(
							positionJson.getDouble("latitude"),
							positionJson.getDouble("longitude"));
				} catch (JSONException ex) {
					Log.e(TAG, "Failed to read last best location.");
					ex.printStackTrace();
					return;
				}
			}
			location.setLatitude(latLng.latitude);
			location.setLongitude(latLng.longitude);
			new AddressHandler(this, this).awaitAddress(location);
        } else {
            checkHasPermissions(RequestCodes.PERMISSION_MULTIPLE,
                    RequestCodes.REQUEST_PERMISSIONS_LOCATION);
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
        Toast.makeText(this,
                "Marker: " + marker.getTitle() + " (" + marker.getId() + ")",
                Toast.LENGTH_LONG).show();
        return true;
    }

    private void onCameraMove() {
        mCameraPosition = mMap.getCameraPosition();
    }

    @Override
    public void onLastLocationReceived(Location location) {
        mSelectedLocation = location;
        onLocationReceived();
    }

    @Override
    public void onLocationReceived(LocationResult locationResult) {
        // Request an address from the current location
        new AddressHandler(this, this).awaitAddress(
                locationResult.getLastLocation());
    }

    @Override
    protected void onLocationReceived() {
        // Request an address from the current location
        new AddressHandler(this, this).awaitAddress(
                mSelectedLocation);
    }

    @Override
    public void onAddressReceived(ArrayList<Address> addressResults) {
        placeNewMarker(addressResults);
    }

	private LatLng getLatLngFromIntent(Bundle extras) {
		// Initialise all weather data
		LatLng latLng = null;
		if (extras != null)
			latLng = extras.getParcelable(RequestCodes.EXTRA_LATLNG);
		return latLng;
	}

    private void updateLocationDisplay(Address address) {
        Log.d(TAG, "updateLocationDisplay");
        String str;

        // Reposition the camera away from the default position,
        // and zoom in to a reasonably broad scope
        if (0.0d - mCameraPosition.target.latitude <= 1.0d
                && 0.0d - mCameraPosition.target.longitude <= 1.0d)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mMarker.getPosition().latitude, mMarker.getPosition().longitude),
                    DEFAULT_ZOOM));

        // Add marker info
        ((TextView)findViewById(R.id.txtSheetTitle)).setText("");
        ((TextView)findViewById(R.id.txtSheetCoordinates)).setText("");
        if (address != null) {

            // Set the location title
            // eg. 95 Iris St, Beacon Hill NSW 2100, Australia
            // ==> Beacon Hill NSW 2100
            str = address.getLocality() +
                    ", " + AddressHandler.getAustralianStateCode(address) +
                    " " + address.getPostalCode();
            ((TextView)findViewById(R.id.txtSheetTitle)).setText(str);

            // Give address to the InfoWindow view to format
            mMarker.setTitle(str);

            // todo: infowindow body text
            // Fill out body text summary for the location
            str = getString(R.string.app_txt_placeholder);
            mMarker.setSnippet("");
            mMarker.showInfoWindow();

            // Set the coordinates setupInfoWindow.
            final double lng = address.getLongitude();
            final double lat = address.getLatitude();
            final char bearingLng = lng > 0 ? 'E' : 'W';
            final char bearingLat = lat > 0 ? 'N' : 'S';
            str = new DecimalFormat("#.##").format(Math.abs(lng)) + " " + bearingLng
                    + "  " + new DecimalFormat("#.##").format(Math.abs(lat)) + " " + bearingLat;
            ((TextView)findViewById(R.id.txtSheetCoordinates)).setText(str);
        }
    }

	@Override
	public void onServiceConnected(ComponentName className, IBinder service) {
		Log.d(TAG, "in onServiceConnected()");

		// Debug code: Reveal test buttons.
		findViewById(R.id.btnDebugSendNotification).setOnClickListener(
				this::debugSendNotification);
		findViewById(R.id.btnDebugSendNotification).setVisibility(VISIBLE);
		findViewById(R.id.btnDebugToggleLocation).setOnClickListener(
				this::debugToggleLocation);
		findViewById(R.id.btnDebugToggleLocation).setVisibility(VISIBLE);
	}

	@Override
	public void onServiceDisconnected(ComponentName arg0) {
		Log.d(TAG, "in onServiceDisconnected()");
		if (mServiceHandler.service().isRequestingLocationUpdates())
			mServiceHandler.service().toggleLocationUpdates();
	}

	private void debugSendNotification(View view) {
		Log.d(TAG, "in debugSendNotification()");
		mServiceHandler.service().pushOneTimeWeatherNotification();
	}

	private void debugToggleLocation(View view) {
		Log.d(TAG, "in debugToggleLocation()");
		mServiceHandler.service().toggleLocationUpdates();
	}

    @Override
    protected void onPermissionGranted(String perm) {}

    @Override
    protected void onPermissionBlocked(String perm) {}

    @Override
    protected void onAllPermissionsGranted(String[] permissions) {
        // Reinitialise the activity
        if (!init(null)) {
        	Log.e(TAG, "Failed to initialise MapsActivity after permissions granted.");
		}
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle bundle = outState.getBundle(BUNDLE_KEY);
        if (bundle != null) {
            bundle = new Bundle();
            outState.putBundle(BUNDLE_KEY, bundle);
            outState.putParcelable(LOCATION_KEY, mSelectedLocation);
            outState.putParcelable(CAMERA_KEY, mMap.getCameraPosition());
        }
        if (mMapView != null)
            mMapView.onSaveInstanceState(bundle);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mCameraPosition = savedInstanceState.getParcelable(CAMERA_KEY);
        mSelectedLocation = savedInstanceState.getParcelable(LOCATION_KEY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMapView != null)
            mMapView.onResume();
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
        // Start the map view
        if (mMapView != null)
            mMapView.onStart();
        // Bind to the notification service
		mServiceHandler.bind();
    }

    @Override
    protected void onStop() {
        // Unbind from the notification service
		mServiceHandler.unbind();
		// Disable the map view
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
