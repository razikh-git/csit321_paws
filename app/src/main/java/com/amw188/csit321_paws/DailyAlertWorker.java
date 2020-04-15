package com.amw188.csit321_paws;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class DailyAlertWorker
		extends Worker {

	private static final String TAG = PrefConstValues.tag_prefix + "worker_alert";

	private static final int ALERT_ID = 1339;
	private static final String ALERT_TAG = "paws_alert_channel";
	static final String WORK_TAG = PrefConstValues.package_name + ".daily_alert_work";
	static final String ALERT_CHANNEL_ID = "paws_alert_channel";

	private Context mContext;
	private SharedPreferences mSharedPref;

	public DailyAlertWorker(
			@NonNull Context context,
			@NonNull WorkerParameters params) {
		super(context, params);
	}

	@Override @NonNull
	public Result doWork() {
		Log.d(TAG, "in doWork()");

		Result result = Result.success();
		mContext = getApplicationContext();
		mSharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);

		String weatherStr = mSharedPref.getString(
				PrefKeys.position_weather, PrefConstValues.empty_json_object);
		if (!weatherStr.equals(PrefConstValues.empty_json_object))
			pushNotification(weatherStr);
		else
			result = Result.failure();

		return result;
	}

	/**
	 * Fetches and then posts a weather notification.
	 * @return Operation success.
	 */
	private Result pushNotification(final String response) {
		final NotificationManager manager = (NotificationManager) mContext.getSystemService(
				Context.NOTIFICATION_SERVICE);
		if (manager == null) {
			Log.e(TAG, "Notification manager failed to initialise.");
			return Result.failure();
		}

		final Notification notif = getNotification(response);
		if (notif == null)
			return Result.failure();
		manager.notify(ALERT_TAG, ALERT_ID, notif);
		Log.d(TAG, "Pushing alert notification: " + notif.toString());
		return Result.success();
	}

	private Notification getNotification(final String response) {
		final String notifTitle = mContext.getString(R.string.notif_title_alert);
		final Bitmap icon = BitmapFactory.decodeResource(mContext.getResources(),
				R.drawable.ic_warning);
		String title = "%s UV index from %s to %s";
		String message = "It's recommended for you and any children to bring and re-apply" +
				" adequate sun protection, and to reduce your exposure to the sun " +
				"between %s and %s when outdoors around %s.";
		try {
			final JSONObject locationJson = new JSONObject(response);
			final JSONObject dailyJson = locationJson.getJSONObject("forecasts")
					.getJSONObject("uv").getJSONArray("days").getJSONObject(0);

			if (dailyJson.has("alert")) {
				Date startDateTime = PAWSAPI.parseWillyTimestamp(
						dailyJson.getJSONObject("alert")
								.getString("startDateTime"));
				Date endDateTime = PAWSAPI.parseWillyTimestamp(
						dailyJson.getJSONObject("alert")
								.getString("endDateTime"));
				title = String.format(title,
						StringUtils.capitalize(dailyJson.getJSONObject("alert").getString("scale")),
						PAWSAPI.getClockString(mContext, startDateTime.getTime(), true),
						PAWSAPI.getClockString(mContext, endDateTime.getTime(), true));
				message = String.format(message,
						PAWSAPI.getClockString(mContext, startDateTime.getTime(), true),
						PAWSAPI.getClockString(mContext, endDateTime.getTime(), true),
						locationJson.getJSONObject("location").getString("name"));
			} else {
				JSONObject highEntry = dailyJson.getJSONArray("entries").getJSONObject(0);
				int duration = 0;
				for (int i = 0; i < dailyJson.getJSONArray("entries").length(); ++i) {
					final int curIndex = dailyJson.getJSONArray("entries").getJSONObject(i)
							.getInt("index");
					if (curIndex > highEntry.getInt("index"))
						highEntry = dailyJson.getJSONArray("entries").getJSONObject(i);
					if (curIndex > highEntry.getInt("index"))
						++duration;
				}
				Date startDateTime = PAWSAPI.parseWillyTimestamp(
						dailyJson.getJSONObject("alert")
								.getString("startDateTime"));
				Calendar endDateTime = Calendar.getInstance();
				endDateTime.setTime(startDateTime);
				endDateTime.add(Calendar.HOUR_OF_DAY, duration);
				title = String.format(title,
						StringUtils.capitalize(highEntry.getString("scale")),
						PAWSAPI.getClockString(mContext, startDateTime.getTime(), true),
						PAWSAPI.getClockString(mContext, endDateTime.getTimeInMillis(), true));
				message = String.format(message,
						PAWSAPI.getClockString(mContext, startDateTime.getTime(), true),
						PAWSAPI.getClockString(mContext, endDateTime.getTimeInMillis(), true),
						locationJson.getJSONObject("location").getString("name"));
				Log.d(TAG, "Finished building notification.");
			}
		} catch (JSONException ex) {
			message = "We've detected a harmful UV index for the day, " +
					"but we encountered an error parsing the message. Stay safe!";
			ex.printStackTrace();
		}

		Log.d(TAG, "title: " + title);
		Log.d(TAG, "message: " + message);

		// Assemble and build the notification
		final PendingIntent contentIntent = PendingIntent.getActivity(
				mContext, 0, new Intent(mContext, PlaceInfoActivity.class),
				PendingIntent.FLAG_UPDATE_CURRENT);
		final NotificationCompat.Builder builder = new NotificationCompat.Builder(
				mContext, ALERT_CHANNEL_ID)
				.setSmallIcon(R.drawable.ic_paws_icon)
				.setContentIntent(contentIntent)
				.setContentTitle(title)
				.setContentText(message.split("\n")[0])
				.setLargeIcon(icon)
				.setStyle(new NotificationCompat.BigTextStyle()
						.setBigContentTitle(title)
						.setSummaryText(notifTitle)
						.bigText(message))
				.setPriority(Notification.PRIORITY_DEFAULT);
		return builder.build();
	}
}
