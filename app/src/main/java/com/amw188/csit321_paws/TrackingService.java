package com.amw188.csit321_paws;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import android.app.NotificationChannel;
import android.app.NotificationManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class TrackingService extends Service {

	private static final String TAG = "snowpaws_ts";

	private static final String PACKAGE_NAME = "com.amw188.csit321_paws";
	private static final String EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME +
			".extra.STARTED_FROM_NOTIFICATION";
	static final String EXTRA_LOCATION = PACKAGE_NAME + ".extra.LOCATION";
	static final String ACTION_BROADCAST = PACKAGE_NAME + ".action.BROADCAST";

	// Notifications
	private static Notifications instance = null;
	private NotificationCompat.Builder mBuilder;
	private NotificationManager mManager;
	private boolean mCompatibilityMode;
	private static NotificationChannel[] mChannels;
	private static final String[] mChannelIDs = {
			"paws_chan_none",
			"paws_chan_low",
			"paws_chan_default",
			"paws_chan_high",
			"paws_chan_max"
	};
	private static final String mForegroundChannel = "paws_chan_foreground";

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
		mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
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

		HandlerThread handlerThread = new HandlerThread(TAG);
		handlerThread.start();
		mServiceHandler = new Handler(handlerThread.getLooper());

		mManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			CharSequence name = getString(R.string.app_name);
			NotificationChannel mChannel =
					new NotificationChannel(mForegroundChannel, name,
							NotificationManager.IMPORTANCE_HIGH);
			mManager.createNotificationChannel(mChannel);
		}
	}

	public void setHostListener(LocationResultListener listener) {
		mHostListener = listener;
	}

	public void startTracking(LocationRequest locationRequest) {
		// Start receiving updates.
		Log.d(TAG, "Starting tracking.");
		mFusedLocationClient.requestLocationUpdates(
				locationRequest,
				mLocationRequestCallback,
				Looper.getMainLooper());
	}

	public void stopTracking() {
		Log.d(TAG, "Stopping tracking.");
		mFusedLocationClient.removeLocationUpdates(mLocationRequestCallback);
		stopSelf();
	}

	void initNotifications(Context ctx) {
		mManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

		// Create notification channels.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			mChannels = new NotificationChannel[NotificationManager.IMPORTANCE_HIGH];
			for (int i = NotificationManager.IMPORTANCE_NONE; i < NotificationManager.IMPORTANCE_HIGH; i++) {
				CharSequence name;
				String desc;
				// Can't initialise translatable strings to static arrays aaa
				switch (i) {
					case 1:
						name = ctx.getString(R.string.noti_chan_name_low);
						desc = ctx.getString(R.string.noti_chan_desc_low);
						break;
					case 2:
						name = ctx.getString(R.string.noti_chan_name_default);
						desc = ctx.getString(R.string.noti_chan_desc_default);
						break;
					case 3:
						name = ctx.getString(R.string.noti_chan_name_high);
						desc = ctx.getString(R.string.noti_chan_desc_high);
						break;
					default:
						name = ctx.getString(R.string.noti_chan_name_unused);
						desc = ctx.getString(R.string.noti_chan_desc_unused);
						break;
				}

				mChannels[i] = new NotificationChannel(
						mChannelIDs[i], name, i);
				mChannels[i].setDescription(desc);
				mManager.createNotificationChannel(mChannels[i]);
			}
		} else {
			mCompatibilityMode = true;
		}
	}

	private void buildNotification(Context ctx, int priority) {
		Intent intent = new Intent(ctx, TrackingService.class);

		// Extra to help us figure out if we arrived in onStartCommand via the notification or not.
		intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

		PendingIntent servicePendingIntent = PendingIntent.getService(ctx, 0, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		PendingIntent activityPendingIntent = PendingIntent.getActivity(ctx, 0,
				new Intent(ctx, MapsActivity.class), 0);

		mBuilder.addAction(R.drawable.ic_paws_icon, "HELLO HELLO",
						activityPendingIntent)
				.addAction(R.drawable.ic_gps_off, "GOODBYE",
						servicePendingIntent)
				.setContentText("YOU SAY GOODBYE")
				.setContentTitle("I DONT KNOW WHY YOU SAY GOODBYE")
				.setOngoing(true)
				.setPriority(Notification.PRIORITY_HIGH)
				.setSmallIcon(R.drawable.ic_paws_logo)
				.setTicker("I SAY HELLO")
				.setWhen(System.currentTimeMillis());
		if (!mCompatibilityMode)
			mBuilder.setChannelId(mChannelIDs[priority]);
	}

	synchronized void notify(Context ctx, int priority) {
		if (!mCompatibilityMode)
			priority += 3;
		mBuilder = new NotificationCompat.Builder(ctx, mChannelIDs[priority]);
		buildNotification(ctx, priority);

		Log.d(TAG, "Notifications show : Pushing notification, priority " + priority + ".");
		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(ctx);
		notificationManager.notify(priority, mBuilder.build());
	}

	public Notification startForegroundNotification(TrackingService trackingService) {
		Intent intent = new Intent(trackingService, TrackingService.class);
		intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

		// Starts the Tracking service.
		PendingIntent servicePendingIntent = PendingIntent.getService(trackingService,
				0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		// Returns to Mapping activity.
		PendingIntent activityPendingIntent = PendingIntent.getActivity(trackingService,
				0, new Intent(trackingService, MapsActivity.class), 0);

		mBuilder = new NotificationCompat.Builder(trackingService, mForegroundChannel)
				.addAction(R.drawable.ic_paws_logo, "HELLO HELLO",
						activityPendingIntent)
				.addAction(R.drawable.ic_gps_off, "GOODBYE",
						servicePendingIntent)
				.setContentText("YOU SAY GOODBYE")
				.setContentTitle("I DONT KNOW WHY YOU SAY GOODBYE")
				.setCategory(Notification.CATEGORY_SERVICE)
				.setOngoing(true)
				.setPriority(Notification.PRIORITY_MAX)
				.setSmallIcon(R.mipmap.ic_launcher)
				.setTicker("I SAY HELLO")
				.setWhen(System.currentTimeMillis());
		if (!mCompatibilityMode)
			mBuilder.setChannelId(mForegroundChannel);

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
			stopTracking();
			stopSelf();
		}
		// Tells the system to not try to recreate the service after it has been killed.
		return START_NOT_STICKY;
	}
}
