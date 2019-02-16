package com.andreimironov.andrei.photogallery;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import static java.util.concurrent.TimeUnit.MINUTES;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class PollJobService extends JobService {
    private static final int JOB_ID = 1;
    private static final long POLL_INTERVAL_MS = MINUTES.toMillis(1);
    private static final String TAG = "PollJobService";

    @Override
    public boolean onStartJob(final JobParameters params) {
        Log.d(TAG, "onStartJob");
        new StartJobTask(this).execute(params);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "onStopJob");

        return false;
    }

    public static boolean hasBeenScheduled(Context context) {
        Log.d(TAG, "hasBeenScheduled");
        JobScheduler jobScheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        for (JobInfo jobInfo : jobScheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == JOB_ID) {
                return true;
            }
        }
        return false;
    }

    public static void schedule(Context context, boolean isOn) {
        Log.d(TAG, "schedule(" + isOn + ")");
        JobScheduler jobScheduler = (JobScheduler)
                context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JobInfo.Builder jobInfoBuilder =
                new JobInfo.Builder(JOB_ID, new ComponentName(context, PollJobService.class))
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                        .setPersisted(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            jobInfoBuilder.setMinimumLatency(POLL_INTERVAL_MS);
        } else {
            jobInfoBuilder.setPeriodic(POLL_INTERVAL_MS);
        }

        if (isOn) {
            jobScheduler.schedule(jobInfoBuilder.build());
        } else {
            jobScheduler.cancel(JOB_ID);
        }
    }

    private static class StartJobTask extends AsyncTask<JobParameters, Void, JobParameters> {
        private PollJobService mPollJobService;

        public StartJobTask(PollJobService pollJobService) {
            mPollJobService = pollJobService;
        }

        @Override
        protected JobParameters doInBackground(JobParameters... jobParameters) {
            if (!mPollJobService.isNetworkAvailableAndConnected()) {
                return null;
            }
            String query = QueryPreferences.getStoredQuery(mPollJobService.getApplicationContext());
            String lastResultId = QueryPreferences.getLastResultId(mPollJobService.getApplicationContext());
            List<GalleryItem> items;
            if (query == null) {
                items = new FlickrFetchr().fetchRecentPhotos(1);
            } else {
                items = new FlickrFetchr().searchPhotos(query, 1);
            }

            if (items.size() == 0) {
                return null;
            }

            String resultId = items.get(0).getId();
            if (resultId.equals(lastResultId)) {
                Log.d(TAG, "Got an old result: " + resultId);
            } else {
                Log.d(TAG, "Got a new result: " + resultId);
                Resources resources = mPollJobService.getResources();
                Intent i = PhotoGalleryActivity.newIntent(mPollJobService.getApplicationContext());
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        mPollJobService.getApplicationContext(),
                        0,
                        i,
                        0);
                String channelId = "channel id";

                Notification notification =
                        new NotificationCompat.Builder(mPollJobService.getApplicationContext(), channelId)
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
                    NotificationManager notificationManager =
                            mPollJobService.getSystemService(NotificationManager.class);
                    notificationManager.createNotificationChannel(channel);
                }
                NotificationManagerCompat notificationManagerCompat =
                        NotificationManagerCompat.from(mPollJobService.getApplicationContext());
                notificationManagerCompat.notify(0, notification);
            }
            QueryPreferences.setLastResultId(mPollJobService.getApplicationContext(), resultId);
            return jobParameters[0];
        }

        @Override
        protected void onPostExecute(JobParameters jobParameters) {
            mPollJobService.jobFinished(jobParameters, false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                schedule(mPollJobService.getApplicationContext(), true);
            }
        }

    }

    private boolean isNetworkAvailableAndConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
        boolean isNetworkConnected = isNetworkAvailable && cm.getActiveNetworkInfo().isConnected();
        return isNetworkConnected;
    }
}
