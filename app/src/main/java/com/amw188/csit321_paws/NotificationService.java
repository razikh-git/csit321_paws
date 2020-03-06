package com.amw188.csit321_paws;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.model.LatLng;

import androidx.core.app.NotificationCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NotificationService
		extends Service
		implements WeatherHandler.WeatherReceivedListener {

	// Logging
	private static final String TAG = PrefConstValues.tag_prefix + "service";

	// Intent extras
	private static final String EXTRA_STARTED_FROM_NOTIFICATION = PrefConstValues.package_name +
			".extra.STARTED_FROM_NOTIFICATION";

	// Binder
	private final IBinder binder = new LocalBinder();
	class LocalBinder extends Binder {
		NotificationService getService() {
			return NotificationService.this;
		}
	}

	// Notifications
	private static final int NOTIFICATION_ID = 1337;
	private static final String NOTIFICATION_CHANNEL_ID = "paws_notif_channel";

	// Locations
	private Location mLastBestLocation;
	private boolean mIsRequestingLocationUpdates;
	private FusedLocationProviderClient mLocationClient;
	private LocationCallback mLocationCallback;
	private LocationResultListener mHostListener;
	interface LocationResultListener {
		void onLocationResultReceived(LocationResult locationResult);
	}

	public NotificationService() {}

	/* Binder Methods */

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "in onBind()");
		return binder;
	}

	/* Service Methods */

	/**
	 * Runs only once per lifetime of the service.
	 * Initialises the foreground service and notification setup.
	 */
	@Override
	public void onCreate() {
		Log.d(TAG, "in onCreate()");
		super.onCreate();
		init();
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "in onDestroy()");
		super.onDestroy();
	}

	/**
	 * Runs each time the startForeground() or startService() is called.
	 * Starts the business of the service.
	 * @return State handling style
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		boolean startedFromNotification = intent.getBooleanExtra(
				EXTRA_STARTED_FROM_NOTIFICATION, false);
		if (startedFromNotification) {
			Log.d(TAG, "Service started from notification");

			// Service will not be revived after it has been killed.
			stopLocationUpdates();
			return START_NOT_STICKY;
		}

		// todo: load up on actions here

		Log.d(TAG, "Service started from activity");
		return START_STICKY;
	}

	/* Custom Service Methods */

	/**
	 * Initialises notification channels, managers, and methods.
	 */
	@TargetApi(23)
	private void init() {
		Log.d(TAG, "Initialising notification service.");

		// Initialise location utilities
		mLocationClient = new FusedLocationProviderClient(getApplicationContext());
		mLocationCallback = new LocationCallback() {
			@Override
			public void onLocationResult(LocationResult locationResult) {
				super.onLocationResult(locationResult);
				NotificationService.this.onLocationResult(locationResult);
			}
		};
		startLocationUpdates();

		// Create notification channel
		NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		if (manager == null) {
			Log.e(TAG, "Notification manager failed to initialise.");
			return;
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			createNotificationChannels(manager);

		// Generate service notification
		Notification notif = getServiceNotification();
		startForeground(NOTIFICATION_ID, notif);

		// Push an initial weather notification
		pushOneTimeWeatherNotification();

		// Schedule regular weather notifications
		SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
				PrefKeys.app_global_preferences, MODE_PRIVATE);
		final int timeInterval = Integer.parseInt(
				sharedPref.getString(PrefKeys.weather_notif_interval,
				PrefDefValues.weather_notif_interval));
		if (timeInterval > 0) {
			final String[] timeInitial = sharedPref.getString(PrefKeys.weather_notif_time_start,
					PrefDefValues.weather_notif_time_start)
					.split(":");
			scheduleWeatherNotifications(
					Integer.parseInt(timeInitial[0]), Integer.parseInt(timeInitial[1]),
					timeInterval);
		}
	}

	/**
	 * Generates or updates notification channels for this app.
	 * Channels persist for each installed lifetime of the app on the device.
	 * @param manager Notification manager object generating channels.
	 */
	@TargetApi(26)
	private void createNotificationChannels(NotificationManager manager) {
		CharSequence name;
		String desc;
		int importance;
		NotificationChannel channel;

		// Service channel
		name = getString(R.string.notif_name_foreground);
		desc = getString(R.string.notif_desc_foreground);
		importance = NotificationManager.IMPORTANCE_DEFAULT;
		channel = new NotificationChannel(
				NOTIFICATION_CHANNEL_ID, name, importance);
		channel.setDescription(desc);
		manager.createNotificationChannel(channel);

		// Weather channel
		name = getString(R.string.notif_name_weather);
		desc = getString(R.string.notif_desc_weather);
		importance = NotificationManager.IMPORTANCE_DEFAULT;
		channel = new NotificationChannel(
				DailyWeatherWorker.WEATHER_CHANNEL_ID, name, importance);
		channel.setDescription(desc);
		manager.createNotificationChannel(channel);
	}

	/**
	 * Generates the notification the foreground service is tied to.
	 * @return Foreground service notification object.
	 */
	@TargetApi(23)
	private Notification getServiceNotification() {
		// Generate a notification with a simple template of content
		PendingIntent servicePendingIntent = PendingIntent.getService(
				this, 0, new Intent(
						this, this.getClass()),
				PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder builder;
		builder = new NotificationCompat.Builder(
				this, NOTIFICATION_CHANNEL_ID)
				.addAction(R.drawable.ic_paws_icon, getString(R.string.label_service),
						servicePendingIntent)
				.setOngoing(true)
				.setPriority(Notification.PRIORITY_HIGH)
				.setSmallIcon(R.drawable.ic_paws_logo)
				.setWhen(System.currentTimeMillis());

		Notification notif = builder.build();
		notif.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
		return notif;
	}

	/* Custom Location Methods */

	protected void setHostListener(LocationResultListener hostListener) { mHostListener = hostListener; }

	boolean isRequestingLocationUpdates() {
		return mIsRequestingLocationUpdates;
	}

	void toggleLocationUpdates() {
		mIsRequestingLocationUpdates = !mIsRequestingLocationUpdates;
		if (mIsRequestingLocationUpdates)
			startLocationUpdates();
		else
			stopLocationUpdates();
	}

	private void startLocationUpdates() {
		Log.d(TAG, "in startLocationUpdates()");

		SharedPreferences sharedPref = getSharedPreferences(
				PrefKeys.app_global_preferences, MODE_PRIVATE);
		LocationRequest locationRequest = new LocationRequest()
				.setPriority(Integer.parseInt(sharedPref.getString(
						PrefKeys.location_priority,
						PrefDefValues.location_priority)))
				.setInterval(Integer.parseInt(sharedPref.getString(
						PrefKeys.location_rate,
						PrefDefValues.location_rate)));

		mIsRequestingLocationUpdates = true;
		mLocationClient.requestLocationUpdates(
				locationRequest, mLocationCallback, Looper.getMainLooper());

		Toast.makeText(this,
				"Started location updates.",
				Toast.LENGTH_LONG).show();
	}

	private void stopLocationUpdates() {
		mIsRequestingLocationUpdates = false;
		mLocationClient.removeLocationUpdates(mLocationCallback);

		Toast.makeText(this,
				"Stopped location updates.",
				Toast.LENGTH_LONG).show();
	}

	protected Location getLastBestLocation() {
		return mLastBestLocation;
	}

	/**
	 * Location result was received when awaiting update on device location.
	 * @param locationResult Last best location result for this device.
	 */
	private void onLocationResult(LocationResult locationResult) {
		if (locationResult == null) {
			Log.e(TAG, "Location result or request was null.");
			return;
		}

		// todo: assign hostListener when needed to reflect location changes in MapsActivity
		if (mHostListener != null)
			mHostListener.onLocationResultReceived(locationResult);
	}

	/* Custom Weather Methods */

	private void doSomethingWithWeather(String weatherStr) {
		Toast.makeText(this,
				"NotificationService.doSomethingWithWeather()",
				Toast.LENGTH_LONG).show();
	}

	private void updateWeatherData() {
		try {
			SharedPreferences sharedPref = getSharedPreferences(
					PrefKeys.app_global_preferences, MODE_PRIVATE);
			final boolean isMetric = PAWSAPI.preferredUnits(sharedPref);
			final JSONObject weatherJson = new JSONObject(sharedPref.getString(
					PrefKeys.last_weather_json, PrefConstValues.empty_json));
			LatLng latLng = new LatLng(
					weatherJson.getJSONObject("lat_lng").getDouble("latitude"),
					weatherJson.getJSONObject("lat_lng").getDouble("longitude"));
			WeatherHandler weatherHandler = new WeatherHandler(this);
			if (!weatherHandler.updateWeather(this, latLng, isMetric))
				// Do something with weather immediately if it needn't wait to be updated
				doSomethingWithWeather(sharedPref.getString(
						PrefKeys.last_weather_json, PrefConstValues.empty_json));
		} catch (JSONException ex) {
			Log.e(TAG, "Failed to parse weather JSON in Service.updateWeatherData().");
			ex.printStackTrace();
		}
	}

	/**
	 * Override of WeatherHandler.WeatherReceivedListener.
	 * Called from WeatherHandler.getWeather in WeatherHandler.updateWeather.
	 * @param latLng Latitude/longitude of weather data batch.
	 * @param response Incredibly long string containing weather 5-day forecast.
	 * @param isMetric Metric or imperial measurements.
	 */
	@Override
	public void onWeatherReceived(LatLng latLng, String response, boolean isMetric) {
		doSomethingWithWeather(response);
	}

	private Constraints getWorkConstraints() {
		return new Constraints.Builder()
				.setRequiredNetworkType(NetworkType.NOT_REQUIRED)
				.build();
	}

	/**
	 * Post a single weather notification as soon as possible.
	 */
	protected void pushOneTimeWeatherNotification() {
		Log.d(TAG, "in pushOneTimeWeatherNotification()");

		OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(
				DailyWeatherWorker.class)
				.setConstraints(getWorkConstraints())
				.build();
		WorkManager.getInstance(this).enqueue(workRequest);
	}

	/**
	 * Schedule periodic weather notifications starting at a certain coming hour.
	 * @param hour Hour where notifications may begin to be sent.
	 * @param minute Minute of the hour where notifications may begin to be sent.
	 * @param interval Hours between each notification.
	 */
	private void scheduleWeatherNotifications(int hour, int minute, int interval) {
		// todo: add an hour/minute picker in settings activity
		// todo: ensure time intervals is a denominator of 24
		// eg. once a day, twice a day, four times a day, every two days, every four days

		long timeNow = System.currentTimeMillis();
		long timeDelay = PAWSAPI.getTimeUntil(timeNow, hour, minute);

		if (timeDelay < 0)
			timeDelay += 1000 * 60 * 60 * 24;

		double hours = PAWSAPI.msToHours(timeDelay);
		int minutes = PAWSAPI.minuteOfHour(hours);

		Log.d(TAG, "Current time: " +
				new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
						.format(timeNow));
		Log.d(TAG, "Target time:  " +
				new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
						.format(timeNow + timeDelay));

		Toast.makeText(getApplicationContext(),
				"Scheduling notifications starting in "
						+ (int)Math.floor(hours) + " hrs " + minutes + " minutes.",
				Toast.LENGTH_LONG).show();

		// Queue up daily weather notifications for the user
		PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(
				DailyWeatherWorker.class, interval, TimeUnit.HOURS)
				//DailyWeatherWorker.class, 15, TimeUnit.MINUTES)
				.setConstraints(getWorkConstraints())
				.setInitialDelay(timeDelay, TimeUnit.MILLISECONDS)
				.addTag(DailyWeatherWorker.WORK_TAG)
				.build();
		WorkManager.getInstance(this).enqueueUniquePeriodicWork(
				DailyWeatherWorker.WORK_TAG, ExistingPeriodicWorkPolicy.REPLACE,
				periodicWorkRequest);

		// todo: re-enqueue the work from settings activity when initial/interval time is changed
	}
}
