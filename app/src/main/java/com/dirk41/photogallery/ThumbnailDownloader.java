package com.dirk41.photogallery;

import android.os.HandlerThread;
import android.util.Log;

/**
 * Created by lingchong on 15-10-4.
 */
public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbanailDownloader";

    public ThumbnailDownloader() {
        super(TAG);
    }

    public void queueThumbnail(T t, String url) {
        Log.i(TAG, "Got an url: " + url);
    }
}
