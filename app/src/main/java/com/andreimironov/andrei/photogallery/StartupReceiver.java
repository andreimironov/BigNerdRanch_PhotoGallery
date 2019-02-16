package com.andreimironov.andrei.photogallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StartupReceiver extends BroadcastReceiver {
    private static final String TAG = "StartupReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        boolean isOn = QueryPreferences.isAlarmOn(context);
        PollAdapter.setServiceAlarm(context, isOn);
    }
}
