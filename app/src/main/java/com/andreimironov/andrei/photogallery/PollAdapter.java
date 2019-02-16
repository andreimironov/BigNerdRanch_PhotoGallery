package com.andreimironov.andrei.photogallery;

import android.content.Context;
import android.os.Build;

import static java.util.concurrent.TimeUnit.MINUTES;

public class PollAdapter {
    private static final long POLL_INTERVAL_MS = MINUTES.toMillis(1);

    public static void setServiceAlarm(Context context, boolean isOn) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            PollJobService.schedule(context, isOn, POLL_INTERVAL_MS);
        } else {
            PollService.setServiceAlarm(context, isOn, POLL_INTERVAL_MS);
        }
        QueryPreferences.setAlarmOn(context, isOn);
    }

    public static boolean isServiceAlarmOn(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                ? PollJobService.hasBeenScheduled(context)
                : PollService.isServiceAlarmOn(context);
    }
}
