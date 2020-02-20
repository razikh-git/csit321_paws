package com.amw188.csit321_paws;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
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

public class NotificationService extends Service {

	// Logging
	private static final String TAG = "snowpaws_service";

	// Intent extras
	private static final String PACKAGE_NAME = "com.amw188.csit321_paws";

	private static final String EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME +
			".extra.STARTED_FROM_NOTIFICATION";
	static final String EXTRA_LOCATION = PACKAGE_NAME + ".extra.LOCATION";

	static final String ACTION_BROADCAST = PACKAGE_NAME + ".action.BROADCAST";
	static final String ACTION_RECEIVE = PACKAGE_NAME + ".action.RECEIVE";

	// Binder
	private final IBinder binder = new LocalBinder();
	public class LocalBinder extends Binder {
		NotificationService getService() {
			return NotificationService.this;
		}
	}

	// Notifications
	private static final int NOTIFICATION_ID = 1337;
	private static final String NOTIFICATION_CHANNEL_ID = "paws_notif_channel";

	// Locations
	private boolean mIsRequestingLocationUpdates;
	private FusedLocationProviderClient mLocationClient;
	private LocationCallback mLocationCallback;
	private LocationResultListener mHostListener;
	private LocationRequest mLocationRequest;
	interface LocationResultListener {
		void onLocationResultReceived(LocationResult locationResult);
	}


	public NotificationService() {}

	/* Binder Methods */

	@Override
	public IBinder onBind(Intent intent) {
		Log.i(TAG, "in onBind()");
		//stopForeground(true);
		return binder;
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
		//LocalBroadcastManager.getInstance(this).unregisterReceiver();
		super.onDestroy();
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

		// todo: load up on actions here

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

		mLocationCallback = new LocationCallback() {
			@Override
			public void onLocationResult(LocationResult locationResult) {
				super.onLocationResult(locationResult);
				if (locationResult != null && mHostListener != null) {
					mHostListener.onLocationResultReceived(locationResult);
				} else {
					Log.e(TAG, "Listener or location failed to catch location callback.");
				}
			}
		};

		// Create notification channel
		NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		if (manager == null) {
			Log.e(TAG, "Notification manager failed to initialise.");
			return;
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			createNotificationChannel(manager);

		// Generate service notification
		Notification notif = getServiceNotification();
		startForeground(NOTIFICATION_ID, notif);
	}

	@TargetApi(26)
	private void createNotificationChannel(NotificationManager manager) {
		CharSequence name = getString(R.string.notif_name_foreground);
		String desc = getString(R.string.notif_desc_foreground);
		int importance = NotificationManager.IMPORTANCE_DEFAULT;
		NotificationChannel channel = new NotificationChannel(
				NOTIFICATION_CHANNEL_ID, name, importance);
		channel.setDescription(desc);
		manager.createNotificationChannel(channel);
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

		// Generate a notification with a simple template of content
		NotificationCompat.Builder builder;
		builder = new NotificationCompat.Builder(
				this, NOTIFICATION_CHANNEL_ID)
				.addAction(R.drawable.ic_paws_icon, "HELLO HELLO",
						servicePendingIntent)
				.setContentText("YOU SAY GOODBYE")
				.setContentTitle("I DONT KNOW WHY YOU SAY GOODBYE")
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

	public void startLocationUpdates() {
		Log.i(TAG, "in startLocationUpdates()");

		if (mIsRequestingLocationUpdates) {
			Log.i(TAG, "Will not start location updates.");
			return;
		}
		mIsRequestingLocationUpdates = true;
		mLocationClient.requestLocationUpdates(
				mLocationRequest, mLocationCallback, Looper.getMainLooper());
	}

	public void stopLocationUpdates() {
		Log.i(TAG, "in stopLocationUpdates()");

		mIsRequestingLocationUpdates = false;
		mLocationClient.removeLocationUpdates(mLocationCallback);
	}
}
