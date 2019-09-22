package com.example.csit321_paws;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import java.io.InputStream;
import java.net.URL;

public class DownloadImageTask extends AsyncTask<String, Void, Drawable> {

    private Exception e;

    protected Drawable doInBackground(String... urls) {
        try {
            InputStream is = (InputStream) new URL(urls[0]).getContent();
            Drawable drawable = Drawable.createFromStream(is, "src");
            return drawable;
        } catch (Exception e) {
            this.e = e;
            return null;
        }
    }

    protected void onPostExecute(Drawable drawable) {
        if (e != null) {
            e.printStackTrace();
        }
    }
}
