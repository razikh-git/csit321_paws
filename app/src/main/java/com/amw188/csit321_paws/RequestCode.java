package com.amw188.csit321_paws;

import android.Manifest;

interface RequestCode {
    int PERMISSION_MULTIPLE = 0;
    int PERMISSION_LOCATION_FINE = 1;
    int PERMISSION_LOCATION_COARSE = 2;
    int PERMISSION_INTERNET = 3;
    int PERMISSION_NETWORK_STATE = 4;

    String[] REQUEST_PERMISSIONS_LOCATION = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION};
    String[] REQUEST_PERMISSIONS_NETWORK = {
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE};

    int SURVEY_CONTINUE = 0;

    int REQUEST_WEATHER_BY_LOCATION = 0;
    String EXTRA_LATLNG = PrefConstValues.package_name + ".extra.LOCATION";
    String EXTRA_PLACENAME = PrefConstValues.package_name + ".extra.PLACENAME";
}
