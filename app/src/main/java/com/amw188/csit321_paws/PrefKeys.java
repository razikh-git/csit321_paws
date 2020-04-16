package com.amw188.csit321_paws;

import com.android.volley.toolbox.StringRequest;

final class PrefKeys {
	// App
	final static String app_init = "app_init";
	final static String debug_mode = "debug_mode";

	// General
	final static String units = "units";
	final static String hourformat = "hourformat";

	// Survey
	final static String survey_answer_ = "survey_answer_";
	final static String survey_time_completed = "survey_time_completed";
	final static String survey_last_question = "survey_last_question";

	// Notifications
	final static String weather_notif_time_start = "weather_notif_time_start";
	final static String weather_notif_time_end = "weather_notif_time_end";
	final static String weather_notif_interval = "weather_notif_interval";

	// Data and Battery Usage
	final static String location_priority = "location_priority";
	final static String location_rate = "location_rate";
	final static String last_weather_json = "last_weather_json";

	// Locations
	final static String last_best_lat_lng = "last_best_position";
	final static String position_history = "position_history";
	final static String position_weather = "position_weather";
	final static String position_ignored = "position_ignored";
}
