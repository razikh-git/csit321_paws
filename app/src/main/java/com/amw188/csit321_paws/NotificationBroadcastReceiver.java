package com.amw188.csit321_paws;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

// todo: deprecate and delete

public class NotificationBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Location location = intent.getParcelableExtra(NotificationService.EXTRA_LOCATION);
    }
}
