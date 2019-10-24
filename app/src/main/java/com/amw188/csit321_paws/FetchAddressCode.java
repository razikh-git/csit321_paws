package com.amw188.csit321_paws;

interface FetchAddressCode {
    int SUCCESS_RESULT = 0;
    int FAILURE_RESULT = 1;
    String PACKAGE_NAME = "com.amw188.csit321_paws";
    String RESULT_DATA_KEY = PACKAGE_NAME + ".RESULT_DATA_KEY";
    String EXTRA_RECEIVER = PACKAGE_NAME + ".extra.RECEIVER";
    String EXTRA_LOCATION = PACKAGE_NAME + ".extra.EXTRA_LATLNG";
    String RESULT_ADDRESSLIST_KEY = PACKAGE_NAME + ".extra.RESULT_ADDRESSLIST_KEY";
    String ACTION_FETCH_ADDRESS = PACKAGE_NAME + ".action.FETCH_ADDRESS";
}
