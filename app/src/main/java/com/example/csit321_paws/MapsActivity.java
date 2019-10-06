package com.example.csit321_paws;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.os.ResultReceiver;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.Places;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.text.DecimalFormat;
import java.util.ArrayList;

import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED;

public class MapsActivity extends BottomNavBarActivity
        implements  OnMapReadyCallback {

    SharedPreferences mSharedPref;
    SharedPreferences.Editor mSharedEditor;

    private static final String TAG = "snowpaws_maps";

    private static final String BUNDLE_KEY = "MapViewBundleKey";
    private static final String CAMERA_KEY = "MapCameraPositionKey";
    private static final String LOCATION_KEY = "MapLocationKey";

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        // Load global preferences.
        mSharedPref = this.getSharedPreferences(
                getResources().getString(R.string.app_global_preferences), Context.MODE_PRIVATE);
        mSharedEditor = mSharedPref.edit();

        // Load saved state.
        Bundle bundle = null;
        if (savedInstanceState != null) {
            bundle = savedInstanceState.getBundle(BUNDLE_KEY);
            mCameraPosition = savedInstanceState.getParcelable(CAMERA_KEY);
            mLastLocation = savedInstanceState.getParcelable(LOCATION_KEY);
        }

        // Load the activity layout.
        setContentView(R.layout.activity_maps);

        // Button functionality.
        findViewById(R.id.laySheetHeader).setOnClickListener((view) -> {onSheetHeaderClick(view);});

        // Bottom navigation bar functionality.
        BottomNavigationView nav = (BottomNavigationView)findViewById(R.id.bottomNavigation);
        nav.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // Construct a GeoDataClient.
        mGeoDataClient = Places.getGeoDataClient(this, null);

        // Construct a PlaceDetectionClient.
        mPlaceDetectionClient = Places.getPlaceDetectionClient(this, null);

        // Construct a FusedLocationProviderClient.
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialise bottom sheet fields.
        mResultReeceiver = new AddressResultReceiver(new Handler());
        initSheet();

        // Prepare the map.
        mMapView = findViewById(R.id.mapView);
        mMapView.onCreate(bundle);
        mMapView.getMapAsync(this);
    }

    private void onSheetHeaderClick(View view) {
        BottomSheetBehavior sheetBehavior = BottomSheetBehavior.from(findViewById(R.id.sheetView));
        if (sheetBehavior.getState() != STATE_COLLAPSED)
            sheetBehavior.setState(STATE_COLLAPSED);
        else
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

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

        // Update the bottom sheet:

        // Set the location title
        // eg. 95 Iris St, Beacon Hill NSW 2100, Australia
        // ==> Beacon Hill NSW 2100
        str = mAddressResult.get(0).getAddressLine(0).split(", ", 3)[1];
        ((TextView)findViewById(R.id.txtSheetTitle)).setText(str);

        // Set the coordinates display
        str = new DecimalFormat("#.##").format(mAddressResult.get(0).getLongitude());
        str += " " + new DecimalFormat("#.##").format(mAddressResult.get(0).getLatitude());
        ((TextView)findViewById(R.id.txtSheetCoordinates)).setText(str);
    }

    protected void startIntentService() {
        Log.d(TAG, "startIntentService");
        FetchAddressIntentService.startActionFetchAddress(this,
                mResultReeceiver, mLastLocation);
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

        // Add a marker in the current location and move the camera

        // TODO that

        // Add a marker in Sydney and move the camera
        LatLng pos = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(pos).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(pos));
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
            if (error == "") {
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
