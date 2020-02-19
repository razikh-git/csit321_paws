package com.amw188.csit321_paws;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import android.app.NotificationChannel;
import android.app.NotificationManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;

import androidx.core.app.NotificationCompat;

public class TrackingService extends Service {

	// Logging
	private static final String TAG = "snowpaws_ts";

	// Intent extras
	private static final String PACKAGE_NAME = "com.amw188.csit321_paws";
	private static final String EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME +
			".extra.STARTED_FROM_NOTIFICATION";
	static final String EXTRA_LOCATION = PACKAGE_NAME + ".extra.LOCATION";
	static final String ACTION_BROADCAST = PACKAGE_NAME + ".action.BROADCAST";

	// Notifications
	private static final String NOTIFICATION_CHANNEL_ID = "paws_notif_channel";
	private static final int NOTIFICATION_ID = 1337;

	// Locations
	private boolean _requestingLocationUpdates;
	private FusedLocationProviderClient _locationClient;
	private LocationCallback _locationCallback;
	private LocationResultListener _hostListener;
	private LocationRequest _locationRequest;
	interface LocationResultListener {
		void onLocationResultReceived(LocationResult locationResult);
	}


	public TrackingService() {}

	/* Binder Methods */

	@Override
	public IBinder onBind(Intent intent) {
		Log.i(TAG, "in onBind()");
		stopForeground(true);
		return null;
	}

	@Override
	public void onRebind(Intent intent) {
		Log.i(TAG, "in onRebind()");
		stopForeground(true);
		super.onRebind(intent);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.i(TAG, "in onUnbind()");
		startForeground(NOTIFICATION_ID, null);
		return true;
	}

	/* Service Methods */

	/**
	 * Runs only once per lifetime of the service.
	 * Initialises the foreground service and notification setup.
	 */
	@Override
	public void onCreate() {
		Log.i(TAG, "in onCreate()");
		super.onCreate();
		init();
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "in onDestroy()");
	}

	/**
	 * Runs each time the startForeground() or startService() is called.
	 * Starts the business of the service.
	 * @return State handling style
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "in onStartCommand()");

		boolean startedFromNotification = intent.getBooleanExtra(
				EXTRA_STARTED_FROM_NOTIFICATION, false);

		if (startedFromNotification) {
			Log.i(TAG, "Service started from notification");

			stopLocationUpdates();

			// Service will not be revived after it has been killed.
			return START_NOT_STICKY;
		}

		startLocationUpdates();

		Log.i(TAG, "Service started from activity");
		return START_STICKY;
	}

	/* Custom Service Methods */

	/**
	 * Initialises notification channels, managers, and methods.
	 */
	@TargetApi(23)
	private void init() {
		Log.i(TAG, "in init()");

		_locationCallback = new LocationCallback() {
			@Override
			public void onLocationResult(LocationResult locationResult) {
				super.onLocationResult(locationResult);
				if (locationResult != null && _hostListener != null) {
					_hostListener.onLocationResultReceived(locationResult);
				} else {
					Log.e(TAG, "Listener or location failed to catch location callback.");
				}
			}
		};

		// Initialise notification channel
		NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		if (manager == null) {
			Log.e(TAG, "Notification manager failed to initialise.");
			return;
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			CharSequence name = getString(R.string.app_name);
			NotificationChannel channel = new NotificationChannel(
					NOTIFICATION_CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH);
			manager.createNotificationChannel(channel);
		}

		// Generate service notification
		Notification notif = getServiceNotification();
		startForeground(NOTIFICATION_ID, notif);
	}

	/**
	 * Generates the notification the foreground service is tied to.
	 */
	@TargetApi(23)
	private Notification getServiceNotification() {
		// Routines for user interactions with buttons in the notification
		PendingIntent servicePendingIntent = PendingIntent.getService(
				this, 0, new Intent(
						this, this.getClass()
				), PendingIntent.FLAG_UPDATE_CURRENT);
		PendingIntent contentPendingIntent = PendingIntent.getBroadcast(
				this, 1, new Intent(
						this, NotificationBroadcastReceiver.class
				), PendingIntent.FLAG_UPDATE_CURRENT);

		// Generate a notification with a simple template of content
		NotificationCompat.Builder builder;
		builder = new NotificationCompat.Builder(
				this, NOTIFICATION_CHANNEL_ID);
		builder
				.addAction(R.drawable.ic_paws_icon, "HELLO HELLO",
						servicePendingIntent)
				.setContentText("YOU SAY GOODBYE")
				.setContentTitle("I DONT KNOW WHY YOU SAY GOODBYE")
				.setContentIntent(contentPendingIntent)
				.setOngoing(true)
				.setPriority(Notification.PRIORITY_HIGH)
				.setSmallIcon(R.drawable.ic_paws_logo)
				.setTicker("I SAY HELLO")
				.setWhen(System.currentTimeMillis());

		Notification notif = builder.build();
		notif.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;

		return notif;
	}

	/* Custom Location Methods */

	private void startLocationUpdates() {
		Log.i(TAG, "in startLocationUpdates()");

		if (_requestingLocationUpdates) {
			Log.i(TAG, "Will not start location updates.");
			return;
		}
		_requestingLocationUpdates = true;
		_locationClient.requestLocationUpdates(
				_locationRequest, _locationCallback, Looper.getMainLooper());
	}

	private void stopLocationUpdates() {
		Log.i(TAG, "in stopLocationUpdates()");

		_requestingLocationUpdates = false;
		_locationClient.removeLocationUpdates(_locationCallback);
	}
}
