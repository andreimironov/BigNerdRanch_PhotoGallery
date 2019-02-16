package com.andreimironov.andrei.photogallery;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Build;
import android.util.Log;

import java.util.List;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import static java.util.concurrent.TimeUnit.MINUTES;

public class PollAdapter {
    private static final long POLL_INTERVAL_MS = MINUTES.toMillis(1);
    private static final String TAG = "PollAdapter";
    public static final String ACTION_SHOW_NOTIFICATION =
            "com.andreimironov.andrei.photogallery.SHOW_NOTIFICATION";
    public static final String PERM_PRIVATE = "com.andreimironov.andrei.photogallery.PRIVATE";

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

    private static boolean isNetworkAvailableAndConnected(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
        boolean isNetworkConnected = isNetworkAvailable && cm.getActiveNetworkInfo().isConnected();
        return isNetworkConnected;
    }

    public static void doJob(Context context) {
        if (!isNetworkAvailableAndConnected(context)) {
            return;
        }
        Log.d(TAG, "doJob");
        String query = QueryPreferences.getStoredQuery(context);
        String lastResultId = QueryPreferences.getLastResultId(context);
        List<GalleryItem> items;
        if (query == null) {
            items = new FlickrFetchr().fetchRecentPhotos(1);
        } else {
            items = new FlickrFetchr().searchPhotos(query, 1);
        }

        if (items.size() == 0) {
            return;
        }

        String resultId = items.get(0).getId();
        if (resultId.equals(lastResultId)) {
            Log.d(TAG, "Got an old result: " + resultId);
        } else {
            Log.d(TAG, "Got a new result: " + resultId);
            Resources resources = context.getResources();
            Intent i = PhotoGalleryActivity.newIntent(context);
            PendingIntent pendingIntent =
                    PendingIntent.getActivity(context, 0, i, 0);
            String channelId = "channel id";

            Notification notification =
                    new NotificationCompat.Builder(context, channelId)
                            .setTicker(resources.getString(R.string.new_pictures_title))
                            .setSmallIcon(android.R.drawable.ic_menu_report_image)
                            .setContentTitle(resources.getString(R.string.new_pictures_title))
                            .setContentText(resources.getString(R.string.new_pictures_text))
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .build();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = "channel name";
                String description = "description";
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel(channelId, name, importance);
                channel.setDescription(description);
                NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }
            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
            notificationManagerCompat.notify(0, notification);
            context.sendBroadcast(new Intent(ACTION_SHOW_NOTIFICATION), PERM_PRIVATE);
        }
        QueryPreferences.setLastResultId(context, resultId);
    }
}
