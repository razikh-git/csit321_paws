package com.amw188.csit321_paws;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import android.app.NotificationChannel;
import android.app.NotificationManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class TrackingService extends Service {

	private static final String TAG = "snowpaws_ts";

	private static final String PACKAGE_NAME = "com.amw188.csit321_paws";
	private static final String EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME +
			".extra.STARTED_FROM_NOTIFICATION";
	static final String EXTRA_LOCATION = PACKAGE_NAME + ".extra.LOCATION";
	static final String EXTRA_APP_PACKAGE = PACKAGE_NAME;
	static final String ACTION_BROADCAST = PACKAGE_NAME + ".action.BROADCAST";

	// Notifications
	private Notification mNotification;
	private NotificationCompat.Builder mNotificationBuilder;
	private NotificationManager mNotificationManager;
	private static final String mNotificationChannelID = "paws_notif_channel";

	// Tracking
	private final IBinder mBinder = new LocalBinder();
	private Handler mServiceHandler;
	private FusedLocationProviderClient mFusedLocationClient;
	private LocationResultListener mHostListener;

	// PAWS Location Service
	private LocationCallback mLocationRequestCallback;

	public class LocalBinder extends Binder {
		TrackingService getService() {
			return TrackingService.this;
		}
	}

	// Interface to send updates to host activity
	interface LocationResultListener {
		void onLocationResultReceived(LocationResult locationResult);
	}

	public TrackingService() {}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.i(TAG, "in onBind()");
		stopForeground(true);
		return mBinder;
	}

	@Override
	public void onRebind(Intent intent) {
		// Called when a client (MainActivity in case of this sample) returns to the foreground
		// and binds once again with this service. The service should cease to be a foreground
		// service when that happens.
		Log.i(TAG, "in onRebind()");
		stopForeground(true);
		super.onRebind(intent);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.i(TAG, "Last client unbound from service");
		startForeground(10, mNotification);
		return true;
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "Destroyed");
		mServiceHandler.removeCallbacksAndMessages(null);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		boolean startedFromNotification = intent.getBooleanExtra(
				EXTRA_STARTED_FROM_NOTIFICATION, false);

		if (startedFromNotification) {
			Log.i(TAG, "Service started from notification");
			stopTracking(mFusedLocationClient);
			// Tells the system to not try to recreate the service after it has been killed.
			return START_NOT_STICKY;
		}

		Log.i(TAG, "Service started");
		return START_STICKY;
	}

	@TargetApi(23)
	public void init(FusedLocationProviderClient fusedClient, LocationResultListener listener) {
		mFusedLocationClient = fusedClient;
		mHostListener = listener;

		mLocationRequestCallback = new LocationCallback() {
			@Override
			public void onLocationResult(LocationResult locationResult) {
				super.onLocationResult(locationResult);
				if (locationResult != null && mHostListener != null) {
					mHostListener.onLocationResultReceived(locationResult);
				} else {
					Log.e(TAG, "Listener or location failed to catch callback.");
				}
			}
		};

		HandlerThread handlerThread = new HandlerThread(TAG, Process.THREAD_PRIORITY_FOREGROUND);
		handlerThread.start();
		mServiceHandler = new Handler(handlerThread.getLooper());

		// Initialise notification channel
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
			// API 23+
			mNotificationManager = getSystemService(NotificationManager.class);
		else
			// API 22 and below
			mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			CharSequence name = getString(R.string.app_name);
			NotificationChannel chan = new NotificationChannel(
					mNotificationChannelID, name, NotificationManager.IMPORTANCE_HIGH);
			mNotificationManager.createNotificationChannel(chan);
		}

		// Generate service notification
		Notification notif = getServiceNotification();
		startForeground(10, notif);
		/*
		LocalBroadcastManager.getInstance(this).registerReceiver(
				NotificationBroadcastReceiver.class, new IntentFilter(BroadcastCodes.INTENT_NAME));
		 */
	}

	public void startTracking(LocationRequest locationRequest) {
		// Start receiving updates.
		Log.i(TAG, "Starting tracking.");
		mFusedLocationClient.requestLocationUpdates(
				locationRequest,
				mLocationRequestCallback,
				Looper.getMainLooper());
	}

	public void stopTracking(FusedLocationProviderClient fusedClient) {
		Log.i(TAG, "Stopping tracking.");
		fusedClient.removeLocationUpdates(mLocationRequestCallback);
		stopForeground(true);
		stopSelf();
	}

	@TargetApi(23)
	private Notification getServiceNotification() {
		// Routines for user interactions with buttons in the notification
		PendingIntent servicePendingIntent = PendingIntent.getService(
				this, 0, new Intent(
						this, this.getClass()
				), PendingIntent.FLAG_UPDATE_CURRENT);
		PendingIntent activityPendingIntent = PendingIntent.getActivity(
				this, 0, new Intent(
						this, mLocationRequestCallback.getClass()
				), 0);
		PendingIntent contentPendingIntent = PendingIntent.getBroadcast(
				this, 1, new Intent(
						this, NotificationBroadcastReceiver.class
				), PendingIntent.FLAG_UPDATE_CURRENT);

		// Generate a notification with a simple template of content
		mNotificationBuilder = new NotificationCompat.Builder(
				this, mNotificationChannelID);
		mNotificationBuilder.addAction(R.drawable.ic_paws_icon, "HELLO HELLO",
						activityPendingIntent)
				.addAction(R.drawable.ic_gps_off, "GOODBYE",
						servicePendingIntent)
				.setContentText("YOU SAY GOODBYE")
				.setContentTitle("I DONT KNOW WHY YOU SAY GOODBYE")
				.setContentIntent(contentPendingIntent)
				.setOngoing(true)
				.setPriority(Notification.PRIORITY_HIGH)
				.setSmallIcon(R.drawable.ic_paws_logo)
				.setTicker("I SAY HELLO")
				.setWhen(System.currentTimeMillis());

		Notification notif = mNotificationBuilder.build();
		notif.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;

		return notif;
	}

	synchronized void notify(int priority) {
		Notification notif = getServiceNotification();
		if (notif != null) {
			Log.i(TAG, "Built notification for notify.");
			mNotificationManager.notify(priority, notif);
		} else {
			Log.i(TAG, "Failed to build notification.");
		}
	}
}
