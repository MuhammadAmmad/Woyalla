package com.brainup.woyalla;

import android.app.Application;

import com.brainup.woyalla.Database.Database;

/**
 * Created by Roger on 6/27/16.
 */
public class Woyalla extends Application {

   // public static final String API_URL  = "http://192.168.2.100/api.weyala.net/";
    public static final String API_URL  = "http://weyala.net/api/";
    public static final String PREFS_NAME = "weyala_language";

    public static Database myDatabase;

    @Override
    public void onCreate() {
        super.onCreate();

        myDatabase = new Database(this);

    }


}
