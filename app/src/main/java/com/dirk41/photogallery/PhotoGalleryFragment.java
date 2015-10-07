package com.dirk41.photogallery;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshGridView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by lingchong on 15-9-25.
 */
public class PhotoGalleryFragment extends Fragment {
    private GridView mGridView;
    private PullToRefreshGridView mPullToRefreshGridView;
    private ArrayList<GalleryItem> mGalleryItemArrayList;
    private FetchItemTask mFetchItemTask;
    private int mPage = 1;

    private ThumbnailDownloader<ImageView> mThumbnailThread;

    private static final String TAG = "PhotoGalleryFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
        mFetchItemTask = new FetchItemTask();
        mFetchItemTask.execute();

        mThumbnailThread = new ThumbnailDownloader<>();
        mThumbnailThread.start();
        mThumbnailThread.getLooper();
        Log.i(TAG, "Background thread start...");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mPullToRefreshGridView = (PullToRefreshGridView) view.findViewById(R.id.pull_refresh_grid);
        mGridView = mPullToRefreshGridView.getRefreshableView();

        mPullToRefreshGridView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (mFetchItemTask != null) {
                    mFetchItemTask.cancel(true);
                }

                if (mPage <= 1 && mPullToRefreshGridView.hasPullFromTop()) {
                    Toast.makeText(getActivity(), "This is already first Page!", Toast.LENGTH_SHORT).show();
                    mPullToRefreshGridView.onRefreshComplete();
                    return;
                } else if (mPage >= 10 && !mPullToRefreshGridView.hasPullFromTop()) {
                    Toast.makeText(getActivity(), "This is already last Page!", Toast.LENGTH_SHORT).show();
                    mPullToRefreshGridView.onRefreshComplete();
                    return;
                } else {
                    if (mPullToRefreshGridView.hasPullFromTop()) {
                        --mPage;
                    } else {
                        ++mPage;
                    }

                    new FetchItemTask().execute();
                }
            }
        });

        setupAdapter();

        return view;
    }

    private void setupAdapter() {
        if (getActivity() == null || mGridView == null) {
            return;
        }

        if (mGalleryItemArrayList != null) {
//            mGridView.setAdapter(new ArrayAdapter<GalleryItem>(getActivity(), android.R.layout.simple_gallery_item, mGalleryItemArrayList));
            mGridView.setAdapter(new GalleryItemAdapter(getActivity(), 0, mGalleryItemArrayList));
        } else {
            mGridView.setAdapter(null);
        }
    }

    private class GalleryItemAdapter extends ArrayAdapter<GalleryItem> {

        public GalleryItemAdapter(Context context, int resource, ArrayList<GalleryItem> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.gallery_item_imageview, parent, false);
            }

            ImageView imageView = (ImageView) convertView.findViewById(R.id.gallery_image_view);
            imageView.setImageResource(R.drawable.acmilan);

            return convertView;
        }
    }

    private class FetchItemTask extends AsyncTask<Void, Void, ArrayList<GalleryItem>> {

        @Override
        protected ArrayList<GalleryItem> doInBackground(Void... params) {
//            try {
//                String htmlData = new FlickFetchr().getUrl("http://www.google.com");
//                Log.i(TAG, "Fetched contents of URL: " + htmlData);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            return new FlickFetchr().fetchItems(getActivity(), mPage);

//            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<GalleryItem> galleryItemArrayList) {
            mGalleryItemArrayList = galleryItemArrayList;
            setupAdapter();
            mPullToRefreshGridView.onRefreshComplete();
        }
    }
}
