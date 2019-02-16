package com.andreimironov.andrei.photogallery;

import android.content.Context;
import android.os.Build;

public class PollAdapter {
    public static void setServiceAlarm(Context context, boolean isOn) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            PollJobService.schedule(context, isOn);
        } else {
            PollService.setServiceAlarm(context, isOn);
        }
        QueryPreferences.setAlarmOn(context, isOn);
    }

    public static boolean isServiceAlarmOn(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                ? PollJobService.hasBeenScheduled(context)
                : PollService.isServiceAlarmOn(context);
    }
}
