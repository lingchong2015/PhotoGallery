package com.dirk41.photogallery;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by lingchong on 15-9-25.
 */
public class FlickFetchr {
    public static final String TAG = "FlickFetchr";

    private static final String ENDPOINT = "https://api.flickr.com/services/rest/";
    private static final String API_KEY = "7de4766621de70a17c5ebbd6dffe618f";
    private static final String METHOD_GET_RECENT = "flickr.photos.getRecent";
    private static final String PARAM_EXTRAS = "extras";
    private static final String EXTRA_SMALL = "url_s";
    private static final String PER_PAGE = "300";

    private static final String XML_PHOTO = "photo";

    public byte[] getUrlBytes(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            InputStream inputStream = httpURLConnection.getInputStream();

            if (httpURLConnection.getResponseCode() != httpURLConnection.HTTP_OK) {
                return null;
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = inputStream.read(buffer)) > 0) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }

            byteArrayOutputStream.close();
            return byteArrayOutputStream.toByteArray();
        } finally {
            httpURLConnection.disconnect();
        }
    }

    public String getUrl(String urlString) throws IOException {
        return new String(getUrlBytes(urlString));
    }

    public ArrayList<GalleryItem> fetchItems(Context context, int page) {
        ArrayList<GalleryItem> galleryItemArrayList = new ArrayList<>();

        try {
            String url = Uri.parse(ENDPOINT).buildUpon()
                    .appendQueryParameter("method", METHOD_GET_RECENT)
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL)
                    .appendQueryParameter("per_page", PER_PAGE)
                    .appendQueryParameter("page", String.valueOf(page))
                    .build().toString();
            Log.i(TAG, url);
            String xmlString = getUrl(url);
            Log.i(TAG, "Received xml: " + xmlString);

            XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser = xmlPullParserFactory.newPullParser();
            xmlPullParser.setInput(new StringReader(xmlString));
            parseItems(galleryItemArrayList, xmlPullParser);
        } catch (IOException ex) {
            Log.e(TAG, "Failed to fetch items", ex);
        } catch (XmlPullParserException ex) {
            Log.e(TAG, "Failed to parse items", ex);
        }

        return galleryItemArrayList;
    }

    private void parseItems(ArrayList<GalleryItem> galleryItemArrayList, XmlPullParser xmlPullParser) throws IOException, XmlPullParserException {
        int eventType = xmlPullParser.next();

        while (eventType != xmlPullParser.END_DOCUMENT) {
            if (eventType == xmlPullParser.START_TAG && XML_PHOTO.equals(xmlPullParser.getName())) {
                GalleryItem galleryItem = new GalleryItem();
                galleryItem.setCaption(xmlPullParser.getAttributeValue(null, "title"));
                galleryItem.setId(xmlPullParser.getAttributeValue(null, "id"));
                galleryItem.setUrl(xmlPullParser.getAttributeValue(null, EXTRA_SMALL));
                galleryItemArrayList.add(galleryItem);
            }

            eventType = xmlPullParser.next();
        }
    }
}
