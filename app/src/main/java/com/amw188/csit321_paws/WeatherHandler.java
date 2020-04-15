package com.amw188.csit321_paws;

import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import androidx.work.ListenableWorker;

abstract class WeatherHandler {

	private static final String TAG = PrefConstValues.tag_prefix + "h_w_abstract";

	static final int REQUEST_WILLY_SUMMARY = 0;
	static final int REQUEST_WILLY_SEARCH = 1;
	static final int REQUEST_WILLY_FORECAST = 2;
	static final int REQUEST_WILLY_WARNING = 3;
	static final int REQUEST_OPEN_WEATHER = 4;

	// Interface to send updates to host activity
	interface WeatherReceivedListener {
		void onWeatherReceived(final int requestCode, String response);
	}

	void getFromURL(final Context context, final StringRequest stringRequest, final boolean retry) {
		Log.d(TAG, "URL: " + stringRequest.getUrl());
		RequestQueue queue = Volley.newRequestQueue(context);
		if (retry)
			stringRequest.setRetryPolicy(new DefaultRetryPolicy(
					DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
					DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
					DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
		queue.add(stringRequest);
	}
}
