package com.example.csit321_paws;

import android.os.Bundle;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.List;

public class GeofencingClient {

    /*
    private GeofencingClient geofencingClient;
    private List<Geofence> geofenceList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.
        geofencingClient = LocationServices.getGeofencingClient(this);
        geofenceList.add(new Geofence.Builder()
                .setRequestId(entry.getKey())
                .setCircularRegion()
                .setExpirationDuration()
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT |
                        Geofence.GEOFENCE_TRANSITION_DWELL)
                .build());
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofenceList);
        return builder.build();
    }
    */
}
