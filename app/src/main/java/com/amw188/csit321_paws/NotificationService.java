package com.amw188.csit321_paws;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.widget.Toast;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.model.LatLng;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class NotificationService
		extends Service
		implements
		AddressHandler.AddressReceivedListener,
		LocationHandler.LocationReceivedListener,
		WeatherHandler.WeatherReceivedListener
{
	// Logging
	private static final String TAG = PrefConstValues.tag_prefix + "service";

	// Intent extras
	private static final String EXTRA_STARTED_FROM_NOTIFICATION =
			PrefConstValues.package_name + ".extra.STARTED_FROM_NOTIFICATION";

	// Binder
	private final IBinder mBinder = new LocalBinder();
	class LocalBinder extends Binder {
		NotificationService getService() {
			return NotificationService.this;
		}
	}

	// Notifications
	private static final int NOTIFICATION_ID = 1337;
	private static final String NOTIFICATION_CHANNEL_ID = "paws_notif_channel";

	// Locations
	private SharedPreferences mSharedPref;
	private boolean mIsRequestingLocationUpdates;
	private LocationHandler mLocationHandler;
	private LocationHandler.LocationReceivedListener mLocationListener;

	public NotificationService() {}

	/* Binder Methods */

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "in onBind()");
		return mBinder;
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

			// Service will not be revived after it has been killed
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
		Log.d(TAG, "Initialising notification service");

		mSharedPref = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		// Initialise location utilities
		mLocationHandler = new LocationHandler(this, this);
		startLocationUpdates();

		// Create notification channel
		NotificationManager manager = (NotificationManager)
				getSystemService(NOTIFICATION_SERVICE);
		if (manager == null) {
			Log.e(TAG, "Notification manager failed to initialise.");
			return;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			createNotificationChannels(manager);

		// Pin foreground service
		pushServiceNotification();

		// Schedule weather notifications when a weather forecast is received
		updateWeatherData();

		// Schedule periodic updates to stored data for notifications
		schedulePeriodicDataUpdates();
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

		// todo: add new channel for emergency notifications

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

		// Alert channel
		name = getString(R.string.notif_name_alert);
		desc = getString(R.string.notif_desc_alert);
		importance = NotificationManager.IMPORTANCE_DEFAULT;
		channel = new NotificationChannel(
				DailyAlertWorker.ALERT_CHANNEL_ID, name, importance);
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
		NotificationCompat.Builder builder;
		builder = new NotificationCompat.Builder(
				this, NOTIFICATION_CHANNEL_ID)
				.setOngoing(true)
				.setPriority(Notification.PRIORITY_HIGH)
				.setSmallIcon(R.drawable.ic_paws_icon)
				.setWhen(System.currentTimeMillis());

		Notification notif = builder.build();
		notif.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
		return notif;
	}

	private void pushServiceNotification() {
		// Generate service notification
		Notification notif = getServiceNotification();
		startForeground(NOTIFICATION_ID, notif);
	}

	/* Custom Address Methods */

	@Override
	public void onAddressReceived(ArrayList<Address> addressResults) {
		// Update device location history
		PAWSAPI.addPlaceToHistory(this, addressResults);

		// Get a weather forecast for this location from WillyWeather for weather alerts
		new WillyWeatherHandler(this, this).awaitWeatherUpdate(
				WeatherHandler.REQUEST_WILLY_FORECAST, addressResults.get(0),
				"?days=1&forecasts=uv");
	}

	/* Custom Location Methods */

	void awaitLocation(LocationHandler.LocationReceivedListener listener) {
		mLocationListener = listener;
		mLocationHandler.getLastBestLocation(this);
	}

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
		Log.d(TAG, "in start()");

		final int interval = Integer.parseInt(mSharedPref.getString(
				PrefKeys.location_rate,
				PrefDefValues.location_rate));

		// Ignore 'never' location update rates
		if (interval < 1)
			return;

		LocationRequest locationRequest = new LocationRequest()
				.setPriority(Integer.parseInt(mSharedPref.getString(
						PrefKeys.location_priority,
						PrefDefValues.location_priority)))
				.setInterval(interval);

		mIsRequestingLocationUpdates = mLocationHandler.start(locationRequest);
		if (mIsRequestingLocationUpdates)
			Toast.makeText(this,
					"Started location updates.",
					Toast.LENGTH_LONG).show();
	}

	private void stopLocationUpdates() {
		mIsRequestingLocationUpdates = mLocationHandler.stop();
		if (!mIsRequestingLocationUpdates)
			Toast.makeText(this,
					"Stopped location updates.",
					Toast.LENGTH_LONG).show();
	}

	@Override
	public void onLastLocationReceived(Location location) {
		if (location != null && mLocationListener != null)
			mLocationListener.onLastLocationReceived(location);
	}

	/**
	 * Location result was received when awaiting update on device location.
	 * @param locationResult Last best location result for this device.
	 */
	@Override
	public void onLocationReceived(LocationResult locationResult) {
		//Toast.makeText(this, "onLocationResult()", Toast.LENGTH_LONG).show();

		if (locationResult == null) {
			Log.e(TAG, "Location result or request was null.");
			return;
		}

		// todo: assign hostListener when needed to reflect location changes in MapsActivity
		/*
		if (mHostListener != null)
			mHostListener.onLocationResultReceived(locationResult);
		*/

		// Await an address to add to the device position history
		final LatLng latLng = new LatLng(
				locationResult.getLastLocation().getLatitude(),
				locationResult.getLastLocation().getLongitude());
		new AddressHandler(this, this).awaitAddress(latLng);

		// Update the last best position for the device
		final String lastBestLatLngStr = PAWSAPI.getLatLngJsonObjectString(
				latLng.latitude, latLng.longitude);
		SharedPreferences.Editor sharedEditor = mSharedPref.edit();
		sharedEditor.putString(PrefKeys.last_best_lat_lng, lastBestLatLngStr);
		sharedEditor.apply();
	}

	/* Custom Weather Methods */

	/**
	 * Override of WeatherHandler.WeatherReceivedListener.
	 * Called from WeatherHandler.getWeather in WeatherHandler.awaitWeatherUpdate.
	 * @param response Incredibly long string containing weather 5-day forecast.
	 */
	@Override
	public void onWeatherReceived(int requestCode, String response) {
		doSomethingWithWeather(requestCode, response);
	}

	private boolean isWeatherScheduled() {
		try {
			return WorkManager.getInstance(this).getWorkInfosByTag(
					DailyWeatherWorker.WORK_TAG).get().size() <= 0;
		} catch (ExecutionException | InterruptedException ex) {
			ex.printStackTrace();
		}
		return false;
	}

	private boolean isAlertScheduled() {
		try {
			return WorkManager.getInstance(this).getWorkInfosByTag(
					DailyAlertWorker.WORK_TAG).get().size() <= 0;
		} catch (ExecutionException | InterruptedException ex) {
			ex.printStackTrace();
		}
		return false;
	}

	private void doSomethingWithWeather(final int requestCode, final String response) {
		Toast.makeText(this, "doSomethingWithWeather()", Toast.LENGTH_LONG).show();
		try {
			if (requestCode == WeatherHandler.REQUEST_WILLY_FORECAST) {
				// Stow the weather forecast in global preferences for the DailyAlertWorker
				SharedPreferences.Editor sharedEditor = mSharedPref.edit();
				sharedEditor.putString(PrefKeys.position_weather, response);
				sharedEditor.apply();

				if (!isAlertScheduled()) {
					Log.d(TAG, "Scheduling alert notifications.");
					scheduleAlertNotifications();
				}
			}
			else if (!isWeatherScheduled()) {
				Log.d(TAG, "Scheduling weather notifications.");
				scheduleWeatherNotifications();
			}
		} catch (Exception ex) {
			Log.d(TAG, "Scheduling failure.");
			ex.printStackTrace();
		}
	}

	private void updateWeatherData() {
		try {
			final JSONObject weatherJSON = new JSONObject(mSharedPref.getString(
					PrefKeys.last_weather_json, PrefConstValues.empty_json_object));
			if (!weatherJSON.has("lat_lng")) {
				mLocationHandler.getLastBestLocation(this);
			}
			LatLng latLng;
			if (weatherJSON.has("lat_lng")) {
				latLng = new LatLng(
						weatherJSON.getJSONObject("lat_lng").getDouble("latitude"),
						weatherJSON.getJSONObject("lat_lng").getDouble("longitude"));
			} else {
				final JSONObject lastBestLatLngJSON = new JSONObject(mSharedPref.getString(
						PrefKeys.last_best_lat_lng, PrefConstValues.empty_json_object));
				latLng = new LatLng(
						lastBestLatLngJSON.getJSONObject("lat_lng").getDouble("latitude"),
						lastBestLatLngJSON.getJSONObject("lat_lng").getDouble("longitude"));
			}
			if (!new OpenWeatherHandler(this, this).awaitWeatherUpdate(latLng))
				// Do something with weather data immediately if it needn't wait to be updated
				doSomethingWithWeather(-1, mSharedPref.getString(
						PrefKeys.last_weather_json, PrefConstValues.empty_json_object));
			// Start the runup to sending alert notifications
			new AddressHandler(this, this).awaitAddress(latLng);
		} catch (JSONException ex) {
			Log.e(TAG, "Failed to parse weather JSON in updateWeatherData().");
			ex.printStackTrace();
		}
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
	 * Post a single alert notification as soon as possible.
	 */
	protected void pushOneTimeAlertNotification() {
		Log.d(TAG, "in pushOneTimeAlertNotification()");

		OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(
				DailyAlertWorker.class)
				.setConstraints(getWorkConstraints())
				.build();
		WorkManager.getInstance(this).enqueue(workRequest);
	}

	/**
	 * Schedule periodic weather notifications starting at a certain coming hour.
	 */
	private void scheduleWeatherNotifications() {
		// todo: add an hour/minute picker in settings activity
		// todo: ensure time intervals is a denominator of 24
		// eg. once a day, twice a day, four times a day, every two days, every four days

		// Push an initial weather notification
		pushOneTimeWeatherNotification();

		// Schedule regular weather notifications
		final int timeInterval = Integer.parseInt(
				mSharedPref.getString(PrefKeys.weather_notif_interval,
						PrefDefValues.weather_notif_interval));
		if (timeInterval <= 0)
			return;

		final String[] timeInitial =
				mSharedPref.getString(PrefKeys.weather_notif_time_start,
						PrefDefValues.weather_notif_time_start)
				.split(":");
		final int hour = Integer.parseInt(timeInitial[0]);
		final int minute = Integer.parseInt(timeInitial[1]);

		long timeNow = System.currentTimeMillis();
		long timeDelay = PAWSAPI.getTimeUntilNext(timeNow, hour, minute);

		double hours = PAWSAPI.msToHours(timeDelay);
		int minutes = PAWSAPI.minuteOfHour(hours);

		Log.d(TAG, "Current time: " +
				new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
						.format(timeNow));
		Log.d(TAG, "Target time:  " +
				new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
						.format(timeNow + timeDelay));

		Toast.makeText(getApplicationContext(),
				"Scheduling weather starting in "
						+ (int)Math.floor(hours) + " hrs " + minutes + " minutes.",
				Toast.LENGTH_LONG).show();

		// Queue up daily weather notifications for the user
		PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(
				DailyWeatherWorker.class, timeInterval, TimeUnit.HOURS)
				//DailyWeatherWorker.class, 15, TimeUnit.MINUTES)
				.setConstraints(getWorkConstraints())
				.setInitialDelay(timeDelay, TimeUnit.MILLISECONDS)
				.addTag(DailyWeatherWorker.WORK_TAG)
				.build();
		WorkManager.getInstance(this).enqueueUniquePeriodicWork(
				DailyWeatherWorker.WORK_TAG, ExistingPeriodicWorkPolicy.REPLACE,
				periodicWorkRequest);
	}

	/**
	 * Schedule daily alert notifications starting at a certain coming hour.
	 */
	private void scheduleAlertNotifications() {
		// Push an initial alert notification
		pushOneTimeAlertNotification();

		final String[] timeInitial =
				mSharedPref.getString(PrefKeys.weather_notif_time_start,
						PrefDefValues.weather_notif_time_start)
						.split(":");
		final int hour = Integer.parseInt(timeInitial[0]);
		final int minute = Integer.parseInt(timeInitial[1]);

		// Schedule regular alert notifications
		long timeNow = System.currentTimeMillis();
		long timeDelay = PAWSAPI.getTimeUntilNext(timeNow, hour, minute);
		long hours = (long)Math.floor(PAWSAPI.msToHours(timeDelay));

		Log.d(TAG, "Current time: " +
				new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
						.format(timeNow));
		Log.d(TAG, "Target time:  " +
				new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
						.format(timeNow + timeDelay));

		Toast.makeText(getApplicationContext(),
				"Scheduling alerts starting in "
						+ (int)Math.floor(hours) + " hrs.",
				Toast.LENGTH_LONG).show();

		// Queue up daily weather notifications for the user
		PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(
				DailyAlertWorker.class, 24, TimeUnit.HOURS)
				.setInitialDelay(hours, TimeUnit.HOURS)
				.setConstraints(getWorkConstraints())
				.addTag(DailyAlertWorker.WORK_TAG)
				.build();
		WorkManager.getInstance(this).enqueueUniquePeriodicWork(
				DailyAlertWorker.WORK_TAG, ExistingPeriodicWorkPolicy.REPLACE,
				periodicWorkRequest);
	}

	boolean rescheduleNotifications() {
		if (isWeatherScheduled()) {
			// Cancel existing work
			WorkManager.getInstance(this).cancelAllWorkByTag(DailyWeatherWorker.WORK_TAG);

			// Schedule new work with new parameters from settings
			scheduleWeatherNotifications();
			return true;
		}
		return false;
	}

	private void schedulePeriodicDataUpdates() {
		PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(
				PeriodicDataUpdateWorker.class, 3, TimeUnit.HOURS)
				.setInitialDelay(0, TimeUnit.HOURS)
				.addTag(PeriodicDataUpdateWorker.WORK_TAG)
				.build();
		WorkManager.getInstance(this).enqueueUniquePeriodicWork(
				PeriodicDataUpdateWorker.WORK_TAG, ExistingPeriodicWorkPolicy.REPLACE,
				periodicWorkRequest);
	}

	private Constraints getWorkConstraints() {
		return new Constraints.Builder()
				.setRequiredNetworkType(NetworkType.NOT_REQUIRED)
				.build();
	}
}
