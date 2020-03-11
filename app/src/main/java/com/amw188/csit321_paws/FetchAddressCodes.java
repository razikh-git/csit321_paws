package com.amw188.csit321_paws;

interface FetchAddressCodes {
    int SUCCESS_RESULT = 0;
    int FAILURE_RESULT = 1;
    String RESULT_DATA_KEY = PrefConstValues.package_name + ".RESULT_DATA_KEY";
    String EXTRA_RECEIVER = PrefConstValues.package_name + ".extra.RECEIVER";
    String EXTRA_LOCATION = PrefConstValues.package_name + ".extra.EXTRA_LATLNG";
    String RESULT_ADDRESSLIST_KEY = PrefConstValues.package_name + ".extra.RESULT_ADDRESSLIST_KEY";
    String ACTION_FETCH_ADDRESS = PrefConstValues.package_name + ".action.FETCH_ADDRESS";
}
