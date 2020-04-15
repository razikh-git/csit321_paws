package com.amw188.csit321_paws;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class WillyWeatherIntegration {

	private static final String app_url_ww_weather_root =
			"https://api.willyweather.com.au/v2/%s/%s";
	private static final String app_ww_api_key = "NjdhNjk4NjU3MWE1OGQ2YWM4NjFiMz";

	private static final Map<Integer, String> RequestCodes;
	static {
		Map<Integer, String> map = new HashMap<>();
		map.put(WeatherHandler.REQUEST_WILLY_SUMMARY, "summary");
		map.put(WeatherHandler.REQUEST_WILLY_SEARCH, "search.json?query=%s");
		map.put(WeatherHandler.REQUEST_WILLY_FORECAST, "locations/%s/weather.json%s");
		map.put(WeatherHandler.REQUEST_WILLY_WARNING, "locations/%s/warnings.json");
		RequestCodes = Collections.unmodifiableMap(map);
	}

	static String getWillyWeatherURL(
			final int requestCode, final String value, final String args) {
		String extras = RequestCodes.get(requestCode);
		if (extras == null || value == null || value.isEmpty())
			return null;
		return String.format(app_url_ww_weather_root,
				app_ww_api_key,
				String.format(extras, value, args));
	}
}
