package com.dirk41.photogallery;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshGridView;

import java.util.ArrayList;

/**
 * Created by lingchong on 15-9-25.
 */
public class PhotoGalleryFragment extends VisibleFragment {
    private GridView mGridView;
    private PullToRefreshGridView mPullToRefreshGridView;
    private ArrayList<GalleryItem> mGalleryItemArrayList;
    private FetchItemTask mFetchItemTask;
    private int mPage = 1;

    private ThumbnailDownloader<ImageView> mThumbnailThread;

    private static final String TAG = "PhotoGalleryFragment";
    public static final String MPAGE = "com.dirk41.photogallery.photogalleryfragment.mpage";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
        setHasOptionsMenu(true);

        updateItems();

//        Intent intent = new Intent(getActivity(), PollService.class);
//        intent.putExtra(MPAGE, mPage);
//        getActivity().startService(intent);

        mThumbnailThread = new ThumbnailDownloader<>(new Handler());
        mThumbnailThread.setListener(new ThumbnailDownloader.Listener<ImageView>() {

            @Override
            public void onThumbnailDownloaded(ImageView imageView, Bitmap bitmap) {
                if (isVisible()) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        });
        mThumbnailThread.start();
        mThumbnailThread.getLooper();
        Log.i(TAG, "Background thread start...");
    }

    public void updateItems() {
        mFetchItemTask = new FetchItemTask();
        mFetchItemTask.execute();
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
            GalleryItem galleryItem = getItem(position);
            mThumbnailThread.queueThumbnail(imageView, galleryItem.getUrl());

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

//            String query = "android";//Just for testing
            Activity parent = getActivity();
            if (parent == null) {
                return new ArrayList<GalleryItem>();
            }

            String query = PreferenceManager.getDefaultSharedPreferences(parent).getString(FlickrFetchr.PREF_SEARCH_QUERY, null);

            if (query != null) {
                return new FlickrFetchr().search(query, mPage);
            } else {
                return new FlickrFetchr().fetchItems(mPage);
            }

//            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<GalleryItem> galleryItemArrayList) {
            mGalleryItemArrayList = galleryItemArrayList;
            setupAdapter();
            mPullToRefreshGridView.onRefreshComplete();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailThread.quit();
        Log.i(TAG, "Background thread destroyed...");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailThread.clearQueue();
    }

    @Override
    @TargetApi(11)
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            MenuItem searchItem = menu.findItem(R.id.menu_item_search);
            SearchView searchView = (SearchView) searchItem.getActionView();

            SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
            ComponentName componentName = getActivity().getComponentName();
            SearchableInfo searchableInfo = searchManager.getSearchableInfo(componentName);

            searchView.setSearchableInfo(searchableInfo);
            int id = searchView.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
            TextView textView = (TextView) searchView.findViewById(id);
            textView.setTextColor(Color.parseColor("#ffb7b7"));
            textView.setHighlightColor(Color.WHITE);
        }
    }

    @TargetApi(11)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_search:
//                getActivity().onSearchRequested();
                getActivity().startSearch("PhotoGallery", true, null, false);

                return true;
            case R.id.menu_item_clear:
                PreferenceManager.getDefaultSharedPreferences(getActivity())
                        .edit()
                        .putString(FlickrFetchr.PREF_SEARCH_QUERY, null)
                        .commit();
                updateItems();

                return true;
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(), shouldStartAlarm);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    getActivity().invalidateOptionsMenu();
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem menuItem = menu.findItem(R.id.menu_item_toggle_polling);
        if (PollService.isServiceAlarmOn(getActivity())) {
            menuItem.setTitle(R.string.stop_polling);
        } else {
            menuItem.setTitle(R.string.start_polling);
        }
    }
}
