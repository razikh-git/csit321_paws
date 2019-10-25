package com.amw188.csit321_paws;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

class Notifications {

	/// typical usage:
	/// MainActivity: Notifications().init();
	/// HostActivity: Notifications().show(Contex this, int priority);

	private static final String TAG = "snowpaws_no";

	private static final String PACKAGE_NAME = "com.amw188.csit321_paws";
	private static final String EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME +
			".extra.STARTED_FROM_NOTIFICATION";

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

	private Notifications() {};

	static synchronized Notifications getInstance() {
		if (instance == null)
			instance = new Notifications();
		return instance;
	}

	void init(Context ctx) {
		mManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

		// Create notification channels.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			mChannels = new NotificationChannel[NotificationManager.IMPORTANCE_MAX];
			for (int i = NotificationManager.IMPORTANCE_NONE; i < NotificationManager.IMPORTANCE_MAX; i++) {
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
					case 4:
						name = ctx.getString(R.string.noti_chan_name_max);
						desc = ctx.getString(R.string.noti_chan_desc_max);
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

	private void fetchContent(Context ctx, int priority) {
		Intent intent = new Intent(ctx, TrackingService.class);

		// Extra to help us figure out if we arrived in onStartCommand via the notification or not.
		intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

		PendingIntent servicePendingIntent = PendingIntent.getService(ctx, 0, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		PendingIntent activityPendingIntent = PendingIntent.getActivity(ctx, 0,
				new Intent(ctx, MapsActivity.class), 0);

		mBuilder
				.addAction(R.drawable.ic_paws_icon, "HELLO HELLO",
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
		/*
		mBuilder.setSmallIcon(R.drawable.ic_paws_logo);
		mBuilder.setContentTitle(PLACEHOLDER);
		mBuilder.setContentText(PLACEHOLDER);
		mBuilder.setPriority(priority);
		//mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText());
		//mBuilder.setContentIntent()
		mBuilder.addAction(R.drawable.ic_paws_logo, ctx.getString(R.string.app_notification),
				activityPendingIntent);
		//mBuilder.setFullScreenIntent();
		mBuilder.setWhen(System.currentTimeMillis());
		mBuilder.setAutoCancel(true);

		 */
	}

	synchronized void show(Context ctx, int priority) {
		if (!mCompatibilityMode)
			priority += 3;
		mBuilder = new NotificationCompat.Builder(ctx, mChannelIDs[priority]);
		fetchContent(ctx, priority);

		Log.d(TAG, "Notifications show : Pushing notification, priority " + priority + ".");
		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(ctx);
		notificationManager.notify(priority, mBuilder.build());
	}
}
