package com.amw188.csit321_paws;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

class ServiceHandler {

	private static final String TAG = PrefConstValues.tag_prefix + "h_service";

	private Context mContext;
	private ConnectionListener mHostListener;
	interface ConnectionListener {
		void onServiceConnected(ComponentName className, IBinder service);
		void onServiceDisconnected(ComponentName arg0);
	}

	// Notification services
	private NotificationService mNotificationService;
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.d(TAG, "in onServiceConnected()");
			NotificationService.LocalBinder binder = (NotificationService.LocalBinder) service;
			mNotificationService = binder.getService();
			mHostListener.onServiceConnected(className, service);
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			Log.d(TAG, "in onServiceDisconnected()");
			mHostListener.onServiceDisconnected(arg0);
		}
	};

	ServiceHandler(final ConnectionListener listener, final Context context) {
		mContext = context;
		mHostListener = listener;
		bind();
	}

	NotificationService service() { return mNotificationService; }

	void bind() {
		Intent intent = new Intent(mContext, NotificationService.class);
		mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}

	void unbind() {
		mContext.unbindService(mConnection);
	}
}
