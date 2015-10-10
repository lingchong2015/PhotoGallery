package com.dirk41.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by lingchong on 15-10-4.i
 */
public class ThumbnailDownloader<T> extends HandlerThread {
    private Handler mHandler;
    private Handler mMainThreadHandler;
    private Map<T, String> mRequestMap = Collections.synchronizedMap(new HashMap<T, String>());
    private Listener<T> mListener;

    private static final int MESSAGE_DOWNLOAD = 0;
    private static final String TAG = "ThumbanailDownloader";

    public interface Listener<T> {
        void onThumbnailDownloaded(T t, Bitmap bitmap);
    }

    public void setListener(Listener<T> listener) {
        mListener = listener;
    }

    public ThumbnailDownloader(Handler mainThreadHandler) {
        super(TAG);
        mMainThreadHandler = mainThreadHandler;
    }

    public void queueThumbnail(T t, String url)
    {
        Log.i(TAG, "Got an url: " + url);
        mRequestMap.put(t, url);
        mHandler.obtainMessage(MESSAGE_DOWNLOAD, t).sendToTarget();
    }

    @Override
    protected void onLooperPrepared() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    T t = (T) msg.obj;
                    Log.i(TAG, "Got a request for url: " + mRequestMap.get(t));
                    handleRequest(t);
                }
            }
        };
    }

    private void handleRequest(final T t) {
        final String url = mRequestMap.get(t);
        if (url == null) {
            return;
        }

        try {
            byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            Log.i(TAG, "Bitmap created");

            mMainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mRequestMap.get(t) != url) {
                        return;
                    }

                    mRequestMap.remove(t);
                    mListener.onThumbnailDownloaded(t, bitmap);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearQueue() {
        mHandler.removeMessages(MESSAGE_DOWNLOAD);
        mRequestMap.clear();
    }
}
