package com.amw188.csit321_paws;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
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
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class TrackingService extends Service {

	private static final String TAG = "snowpaws_ts";

	private static final String PACKAGE_NAME = "com.amw188.csit321_paws";
	private static final String EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME +
			".extra.STARTED_FROM_NOTIFICATION";
	static final String EXTRA_LOCATION = PACKAGE_NAME + ".extra.LOCATION";
	static final String ACTION_BROADCAST = PACKAGE_NAME + ".action.BROADCAST";

	// Notifications
	private NotificationCompat.Builder mBuilder;
	private NotificationManager mManager;
	private boolean mCompatibilityMode;
	private static NotificationChannel mChannel;
	private static final String mChannelID = "paws_chan_default";
	private static NotificationChannel mForegroundChannel;
	private static final String mForegroundChannelID = "paws_chan_foreground";

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

		mManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			CharSequence name = getString(R.string.app_name);
			mForegroundChannel = new NotificationChannel(mForegroundChannelID, name,
							NotificationManager.IMPORTANCE_HIGH);
			mManager.createNotificationChannel(mForegroundChannel);
		}
	}

	public void init(FusedLocationProviderClient fusedClient, LocationResultListener listener) {
		mFusedLocationClient = fusedClient;
		mHostListener = listener;
		initNotifications();
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
		stopSelf();
	}

	void initNotifications() {
		mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		// Create notification channels.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			CharSequence name = getString(R.string.noti_chan_name_default);
			String desc = getString(R.string.noti_chan_desc_default);

			mChannel = new NotificationChannel(
					mChannelID, name, NotificationManager.IMPORTANCE_HIGH);
			mChannel.setDescription(desc);
			mChannel.enableLights(true);
			mChannel.enableVibration(true);
			mChannel.setLightColor(
					ContextCompat.getColor(this, R.color.color_error));
			mChannel.setVibrationPattern(new long[] { 500, 500, 500, 500, 500, 500 });
			mChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
			mManager.createNotificationChannel(mChannel);
		} else {
			Log.i(TAG, "Ignoring channels. " +
					"System is in compatibility mode for OS below Android O.");
			mCompatibilityMode = true;
		}
	}

	private boolean buildNotification(int priority) {
		Intent intent = new Intent(this, TrackingService.class);

		// Extra to help us figure out if we arrived in onStartCommand via the notification or not.
		intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

		// View the Mapping activity.
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, MapsActivity.class), 0);

		String title, subtitle, text;
		int priorityAdjusted = mCompatibilityMode ? priority - 3 : priority;
		switch (priorityAdjusted) {
			case Notification.PRIORITY_MIN:
				return false;
			case Notification.PRIORITY_LOW:
				title = getString(R.string.noti_title_risk_low);
				subtitle = getString(R.string.noti_subtitle_risk_low);
				text = getString(R.string.noti_text_risk_low);
				break;
			case Notification.PRIORITY_DEFAULT:
				title = getString(R.string.noti_title_risk_med);
				subtitle = getString(R.string.noti_subtitle_risk_med);
				text = getString(R.string.noti_text_risk_med);
				break;
			default:
				title = getString(R.string.noti_title_risk_high);
				subtitle = getString(R.string.noti_subtitle_risk_high);
				text = getString(R.string.noti_text_risk_high);
				break;
		}

		mBuilder.addAction(R.drawable.ic_paws_icon, getString(R.string.noti_action_view),
				pendingIntent)
				.setContentText(subtitle)
				.setContentTitle(title)
				.setOngoing(false)
				.setCategory(Notification.CATEGORY_ALARM)
				.setPriority(Notification.PRIORITY_HIGH)
				.setFullScreenIntent(pendingIntent, true)
				.setStyle(new NotificationCompat.BigTextStyle().bigText(text))
				.setSmallIcon(R.drawable.ic_paws_logo)
				.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
				.setTicker(getString(R.string.noti_title))
				.setWhen(System.currentTimeMillis());
		if (!mCompatibilityMode)
			mBuilder.setChannelId(mChannelID);
		return true;
	}

	synchronized void notify(int priority) {
		if (!mCompatibilityMode)
			// Convert from PRIORITY to IMPORTANCE
			priority += 3;
		mBuilder = new NotificationCompat.Builder(this, mChannelID);
		if (!buildNotification(priority)) {
			Log.i(TAG, "Notifications show : Aborting notification, priority " + priority + ".");
		} else {
			Log.i(TAG, "Notifications show : Pushing notification, priority " + priority + ".");
			NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
			notificationManager.notify(priority, mBuilder.build());
		}
	}

	public Notification startForegroundNotification(TrackingService trackingService) {
		Log.i(TAG, "Starting foreground notification.");

		Intent intent = new Intent(trackingService, TrackingService.class);
		intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

		// Ends the Tracking service.
		PendingIntent servicePendingIntent = PendingIntent.getService(trackingService,
				0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		// Returns to Mapping activity.
		PendingIntent pendingIntent = PendingIntent.getActivity(trackingService,
				0, new Intent(trackingService, MapsActivity.class), 0);

		// Build the foreground watchdog.
		mBuilder = new NotificationCompat.Builder(trackingService, mForegroundChannelID)
				.addAction(R.drawable.ic_paws_icon, getString(R.string.noti_action_view),
						pendingIntent)
				.addAction(R.drawable.ic_gps_off, getString(R.string.noti_action_dismiss),
						servicePendingIntent)
				.setContentText(getString(R.string.noti_desc_foreground))
				.setContentTitle(getString(R.string.noti_name_foreground))
				.setCategory(Notification.CATEGORY_SERVICE)
				.setOngoing(true)
				.setPriority(Notification.PRIORITY_HIGH)
				.setSmallIcon(R.drawable.ic_paws_icon)
				.setTicker(getString(R.string.noti_title))
				.setWhen(System.currentTimeMillis())
				.setChannelId(mForegroundChannelID);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			Log.i(TAG, "Managing channels for foreground notification.");
			mForegroundChannel.enableLights(true);
			mForegroundChannel.enableVibration(true);
			mForegroundChannel.setLightColor(
					ContextCompat.getColor(this, R.color.color_primary));
			mForegroundChannel.setVibrationPattern(new long[] { 500, 500, 500, 500 });
			mForegroundChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
		}

		return mBuilder.build();
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
		startForeground(1337, startForegroundNotification(this));
		return true;
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "Destroyed");
		mServiceHandler.removeCallbacksAndMessages(null);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Service started");
		boolean startedFromNotification = intent.getBooleanExtra(
				EXTRA_STARTED_FROM_NOTIFICATION, false);

		if (startedFromNotification) {
			stopTracking(mFusedLocationClient);
			// Tells the system to not try to recreate the service after it has been killed.
			return START_NOT_STICKY;
		}

		return START_STICKY;
	}
}
