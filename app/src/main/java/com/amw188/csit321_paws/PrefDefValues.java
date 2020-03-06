package com.amw188.csit321_paws;

import com.google.android.gms.location.LocationRequest;

final class PrefDefValues {
	final static boolean app_init = false;
	final static String app_global_preferences = PrefConstValues.package_name + "_preferences";
	final static String units = "metric";
	final static String weather_notif_time_start = "08:00";
	final static String weather_notif_time_end = "20:00";
	final static String weather_notif_interval = "24";
	final static String location_priority = Integer.toString(
			LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
	final static String location_rate = "5000";
}
