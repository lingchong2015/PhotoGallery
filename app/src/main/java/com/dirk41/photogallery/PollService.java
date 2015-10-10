package com.dirk41.photogallery;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by lingchong on 15-10-10.
 */
public class PollService extends IntentService {
    private static final String TAG = "PollService";

    private static final int POLL_INTERVAL = 1000 * 60;

    public PollService() {
        super(TAG);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onHandleIntent(Intent intent) {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isNetworkAvailable = connectivityManager.getBackgroundDataSetting() && (connectivityManager.getActiveNetworkInfo() != null);
        if (!isNetworkAvailable) {
            return;
        }

        Log.i(TAG, "Received an intent: " + intent);
        int page = intent.getIntExtra(PhotoGalleryFragment.MPAGE, 0);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String query = sharedPreferences.getString(FlickrFetchr.PREF_SEARCH_QUERY, null);
        String lastResultId = sharedPreferences.getString(FlickrFetchr.PREF_LAST_RESULT_ID, null);

        ArrayList<GalleryItem> galleryItemArrayList;
        if (query != null) {
            galleryItemArrayList = new FlickrFetchr().search(query, page);
        } else {
            if (page <1) {
              page = 1;
            }
            galleryItemArrayList = new FlickrFetchr().fetchItems(page);
        }

        if (galleryItemArrayList.size() < 1) {
            return;
        }

        String currentResultId = galleryItemArrayList.get(0).getId();
        if (currentResultId.equals(lastResultId)) {
            Log.i(TAG, "Got an old result: " + currentResultId);
        } else {
            Log.i(TAG, "Got a new result: " + currentResultId);

            Resources resources = getResources();
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, PhotoGalleryActivity.class), 0);

            Notification notification = new NotificationCompat.Builder(this)
                    .setTicker(resources.getString(R.string.new_picture_title))
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setContentTitle(resources.getString(R.string.new_picture_title))
                    .setContentText(resources.getString(R.string.new_picture_text))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build();

            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(0, notification);
        }

        sharedPreferences.edit()
                .putString(FlickrFetchr.PREF_LAST_RESULT_ID, currentResultId)
                .commit();
    }

    public static void setServiceAlarm(Context context, boolean isOn) {
        Intent intent = new Intent(context, PollService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (isOn) {
            alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), POLL_INTERVAL, pendingIntent);
        } else {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    public static boolean isServiceAlarmOn(Context context) {
        Intent intent = new Intent(context, PollService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE);
        return pendingIntent != null;
    }
}
