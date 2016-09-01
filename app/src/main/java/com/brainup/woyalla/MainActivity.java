package com.brainup.woyalla;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.brainup.woyalla.Database.Database;
import com.brainup.woyalla.Model.Driver;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    //declaration
    private GoogleMap mMap;             //map object
    SupportMapFragment mapFragment;     //map fragment
    Button call;                        //call button declaration
    GPSTracker gps;
    ProgressDialog myDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //drawer layout initialization
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //initialize the gps tracker object
        gps  = new GPSTracker(this);

        //map view initialization
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //call button initialization
        call = (Button) findViewById(R.id.call);

        //initialize the progress dialog
        myDialog = new ProgressDialog(this);

        checkGPS();
        handleCallButton();
    }

    private boolean checkGPS() {
        gps = new GPSTracker(MainActivity.this);
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //if it permission not granted, request a dialog box that asks the client to grant the permission
            //the response will be handled by onRequestPermissionResult method
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},2);
            return false;
        }
        else if(!Checkups.isNetworkAvailable(MainActivity.this)){
            Checkups.showDialog(MainActivity.this.getString(R.string.no_connection_found),MainActivity.this);
            return false;
        }
        else if(!gps.canGetLocation()){
            Checkups.showSettingsAlert(MainActivity.this);
            return false;
        }
        else {
            return true;
        }
    }

    public void handleCallButton() {
        call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check if the app has call permission
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                   //if it permission not granted, request a dialog box that asks the client to grant the permission
                    //the response will be handled by onRequestPermissionResult method
                    ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.CALL_PHONE},1);
                    return;
                }
                if(checkGPS()){
                    startCall();
                }
            }
        });

    }

    public void startCall() {
        Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.toast_calling_server), Toast.LENGTH_SHORT).show();

        String phoneNumber = getResources().getString(R.string.call_center_number);
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:"+phoneNumber));
        sendGPSLocation();
        startActivity(intent);

    }

    private void sendGPSLocation() {
        gps = new GPSTracker(MainActivity.this);
        if(gps.canGetLocation()) {

            final double latitude = gps.getLatitude();
            final double longitude = gps.getLongitude();
            final String userPhone = Woyalla.myDatabase.get_Value_At_Top(Database.Table_USER,Database.USER_FIELDS[1]);
            final int user_id = Woyalla.myDatabase.get_Top_ID(Database.Table_USER);

            ContentValues cv = new ContentValues();
            cv.put(Database.USER_FIELDS[2],latitude+"");
            cv.put(Database.USER_FIELDS[3],longitude+"");

            Woyalla.myDatabase.update(Database.Table_USER,cv,user_id);  //update my current location

            Thread send = new Thread(){
                @Override
                public void run() {
                    try {
                        OkHttpClient client  = new OkHttpClient();     //this object will handle http requests
                        MediaType mediaType  = MediaType.parse("application/x-www-form-urlencoded");
                        RequestBody body = RequestBody.create(mediaType,
                                "phoneNumber="+userPhone +
                                        "&gpsLatitude="+latitude +
                                        "&gpsLongitude="+longitude);
                        Request request = new Request.Builder()
                                .url(Woyalla.API_URL + "clients/update/"+userPhone)
                                .put(body)
                                .addHeader("authorization", "Basic dGhlVXNlcm5hbWU6dGhlUGFzc3dvcmQ=")
                                .addHeader("cache-control", "no-cache")
                                .addHeader("content-type", "application/x-www-form-urlencoded")
                                .build();

                        Response response = client.newCall(request).execute();
                        final String responseBody = response.body().string().toString();
                        Log.i("responseClient", responseBody);

                        JSONObject myObject = (JSONObject) new JSONTokener(responseBody).nextValue();

                    /**
                     * If we get OK response
                     *
                     * */
                        if(myObject.get("status").toString().startsWith("ok") ){

                            boolean isDataExist = false;
                            try {
                                isDataExist = myObject.get("data").equals(null) ? false : true;
                            } catch (Exception e) {
                                isDataExist = false;
                            }
                            /**
                             * If the driver status is on service
                             */
                            if(isDataExist) {
                                JSONObject json_response = myObject.getJSONObject("data");
                                JSONArray json_drivers = json_response.getJSONArray("nearbyDrivers");

                                Log.i("nearbyedriver", json_drivers.toString());
                                Log.i("nearbyedriverCount", json_drivers.length()+"");
                                addDriver(json_drivers);

                                if(json_drivers.length()>0) {
                                    //once we get the list of drivers, build and send a notification to the client
                                    Notifications notifications = new Notifications(getApplicationContext());
                                    notifications.buildNotification();
                                }

                                MainActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Checkups.showDialog(MainActivity.this.getString(R.string.toast_location_sent),MainActivity.this);
                                    }
                                });

                            }

                        }
                    /**
                     * If we get error response
                     *
                     * */
                        if(myObject.get("status").toString().startsWith("error") ) {
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Checkups.showDialog(MainActivity.this.getString(R.string.toast_error_sending_location),MainActivity.this);
                                }
                            });
                        }

                        }catch(Exception e){

                    }
                }
            };

            send.start();

        }
        else{
            gps.showSettingsAlert();
        }

    }

    private void addDriver(JSONArray json_drivers) {
            try{
                if(json_drivers.length()>0){
                    for(int i=0;i<json_drivers.length();i++){
                        JSONObject obj = json_drivers.getJSONObject(i);

                        ContentValues cv = new ContentValues();
                        cv.put(Database.NEARBYE_DRIVERS_FIELDS[0], obj.getString("distanceFromClient"));
                        cv.put(Database.NEARBYE_DRIVERS_FIELDS[1], obj.getString("gpsLatitude"));
                        cv.put(Database.NEARBYE_DRIVERS_FIELDS[2], obj.getString("gpsLongitude"));
                        cv.put(Database.NEARBYE_DRIVERS_FIELDS[3], obj.getString("name"));

//                        Log.i("drivers",)

                        long check = Woyalla.myDatabase.insert(Database.Table_NEARBYE_DRIVER,cv);
                        if(check>0){
                            Log.i("driverarray","New drivers array added ");
                        }
                    }
                }
            }catch (Exception e){
            }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            switch (requestCode){
                case 1:
                    if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                        startCall();
                    }
                    else{
                        Toast.makeText(MainActivity.this,MainActivity.this.getString(R.string.toast_permission_denied),Toast.LENGTH_LONG).show();
                    }
                    break;
                case 2:
                    if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(MainActivity.this,MainActivity.this.getString(R.string.toast_permission_granted),Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Toast.makeText(MainActivity.this,MainActivity.this.getString(R.string.toast_permission_denied),Toast.LENGTH_SHORT).show();
                        checkGPS();
                    }
            }
    }



    private void setLanguage(String lang) {
        SharedPreferences settings = getSharedPreferences(Woyalla.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        Locale locale;
        Configuration configuration;
        switch (lang){
            case "am":
                editor.putString("lang","am");
                editor.commit();
                locale = new Locale("am");
                Locale.setDefault(locale);
                configuration = new Configuration();
                configuration.locale = locale;
                getBaseContext().getResources().updateConfiguration(configuration,getBaseContext().getResources().getDisplayMetrics());
                break;
            case "en":
                editor.putString("lang","en");
                editor.commit();
                locale = new Locale("en");
                Locale.setDefault(locale);
                configuration = new Configuration();
                configuration.locale = locale;
                getBaseContext().getResources().updateConfiguration(configuration,getBaseContext().getResources().getDisplayMetrics());
                break;
        }
        this.finish();
        startActivity(new Intent(MainActivity.this,MainActivity.class));
    }

    /**
     * Select language dialog
     * */
    public void selectLanguage() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        setLanguage("en");
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        setLanguage("am");
                        break;
                }
            }
        };
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.app_name)
                .setMessage(getResources().getString(R.string.dialog_select_language))
                .setPositiveButton("English", dialogClickListener)
                .setNegativeButton("አማርኛ",dialogClickListener).show();
    }




    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();
        if (id == R.id.menu_update_my_location) {
            reload();
            return true;
        }
        if (id == R.id.menu_show_coming_drivers) {
            handleComingTaxi();
            return true;
        }
        if (id == R.id.menu_show_drivers) {
            showNearByeCars();
            return true;
        }
        if (id == R.id.menu_change_map_type) {
            handleMapTypeChange();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Intent intent = null;
        if (id == R.id.nav_home) {
            // Handle the camera action
        } else if (id == R.id.nav_about) {
            intent = new Intent(this,About.class);
            startActivity(intent);
        } else if (id == R.id.nav_lang) {
            selectLanguage();
        } else if (id == R.id.nav_logout) {
            logOut();
        } else if (id == R.id.nav_share) {
            shareApp();
        } else if (id == R.id.nav_rate) {
            rateMyApp();
        } else if (id == R.id.nav_comment) {
            intent = new Intent(this,Comment.class);
            startActivity(intent);
        }
        else if (id == R.id.nav_exit) {
           finish();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Handle map type change from normal to satellite and vise versal
     */
    private void handleMapTypeChange() {

        if(mMap != null) {
            if (mMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL) {
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            } else if (mMap.getMapType() == GoogleMap.MAP_TYPE_HYBRID) {
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            }
        }
        else{
            Log.i("changemap","Map type is null");
        }

    }


    private void handleComingTaxi() {

        myDialog.setTitle(R.string.app_name);
        myDialog.setMessage(this.getString(R.string.dialog_coming_taxi));
        myDialog.setCancelable(false);
        myDialog.show();


        Thread login = new Thread(){
            @Override
            public void run() {
                try {
                    sleep( 2000);
                } catch(InterruptedException e){
                } finally {
                    showComingTaxi();   //this method will handle the request
                }
            }
        };

        login.start();

    }

    public void showComingTaxi(){
         if(!Checkups.isNetworkAvailable(this)) {
             runOnUiThread(new Runnable() {
                 @Override
                 public void run() {
                     myDialog.dismiss();
                     ShowDialog(MainActivity.this.getString(R.string.no_connection_found));

                 }
             });
             return;
         }
        String phone = Woyalla.myDatabase.get_Value_At_Top(Database.Table_USER, Database.USER_FIELDS[1]);

        OkHttpClient client = new OkHttpClient();    //this object will handle http requests

        Request request  = new Request.Builder()
                .url(Woyalla.API_URL + "clients/assigned/"+phone)
                .get()
                .addHeader("authorization", "Basic dGhlVXNlcm5hbWU6dGhlUGFzc3dvcmQ=")
                .addHeader("cache-control", "no-cache")
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .build();
        try {
            //make the http post request and get the server response
            Response response = client.newCall(request).execute();
            String responseBody = response.body().string().toString();
            Log.i("responseFull", responseBody);

            //get the json response object
            JSONObject myObject = (JSONObject) new JSONTokener(responseBody).nextValue();

            /**
             * If we get OK response
             *
             * */
            if(myObject.get("status").toString().startsWith("ok") ){

                boolean isDataExist = false;
                try {
                    isDataExist = myObject.get("data").equals(null) ? false : true;
                } catch (Exception e) {
                    isDataExist = false;
                }
                /**
                 * If we get OK response & we get a data object with in the response json
                 * This is a new user
                 * */
                if(isDataExist) {
                    JSONObject json_response = myObject.getJSONObject("data");
                    final double gpsLatitude = Double.parseDouble(json_response.get("gpsLatitude").toString());
                    final double gpsLongitude = Double.parseDouble(json_response.get("gpsLongitude").toString());
                    final double driver_phone = Double.parseDouble(json_response.get("phoneNumber").toString());



                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMap.clear();
                            moveMapToMyLocation();
                            LatLng latLng = new LatLng(gpsLatitude,gpsLongitude);
                            createMarkers(latLng,MainActivity.this.getString(R.string.taxi),"Phone: " + driver_phone);
                            myDialog.dismiss();
                            Toast.makeText(MainActivity.this,MainActivity.this.getResources().getString(R.string.toast_coming_taxi_ok),Toast.LENGTH_LONG).show();
                        }
                    });

                }

            }
            /**
             * If we get error response
             *
             * */
            else if(myObject.get("status").toString().startsWith("error") ){
                final String errorMessage = myObject.get("description").toString();
                myDialog.dismiss();
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ShowDialog(MainActivity.this.getString(R.string.toast_coming_taxi_error));
                    }
                });
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            myDialog.dismiss();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ShowDialog(MainActivity.this.getString(R.string.error_general));
                }
            });
        } catch(JSONException e){
            myDialog.dismiss();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ShowDialog(MainActivity.this.getString(R.string.error_general));
                }
            });
        }
    }

    /**
     * This method will get near bye taxi info from local database and points them on the map
     */
    public void showNearByeCars(){
        ArrayList<Driver> drivers = Woyalla.myDatabase.getNearByeCars();
        if(drivers.size()>0){
            mMap.clear();
            moveMapToMyLocation();
            Toast.makeText(this,MainActivity.this.getString(R.string.toast_near_bye_card),Toast.LENGTH_LONG).show();
            for(int i = 0; i <drivers.size(); i++){
                LatLng latLng = new LatLng(drivers.get(i).getLatitude(),drivers.get(i).getLongitude());
                createMarkers(latLng,MainActivity.this.getString(R.string.taxi)+ i," : "+ Math.round(drivers.get(i).getDistance()));
            }
        }
        else{
            Toast.makeText(this,this.getString(R.string.toast_make_call_first),Toast.LENGTH_LONG).show();
        }

    }

    /**
     * plot and display a near bye taxi on the map
     * @param latLng the current latitude and longitude of the taxi
     * @param title the title that will be displayed on the marker
     * @param snippet a small description of the taxi
     */

    public void createMarkers(LatLng latLng,String title,String snippet){
        mMap.addMarker(new MarkerOptions()
                .position(latLng) //setting position
                .snippet(snippet)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car_map))
                .draggable(true) //Making the marker draggable
                .title(title)); //Adding a title
    }

    /**
     * reload the client map & data
     */
    public void reload(){
        //remove all near bye drivers
        Woyalla.myDatabase.Delete_All(Database.Table_NEARBYE_DRIVER);
        mMap.clear();
        moveMapToMyLocation();
        Toast.makeText(MainActivity.this,this.getString(R.string.toast_reload),Toast.LENGTH_LONG).show();
    }

    /**
     * get current location from the gps tracker object
     * then view it on the map
     */
    private void moveMapToMyLocation() {
/*        //Creating a LatLng Object to store Coordinates
        if(gps.canGetLocation()) {
            LatLng latLng = new LatLng(gps.getLatitude(), gps.getLongitude());

            //Adding marker to map
            mMap.addMarker(new MarkerOptions()
                    .position(latLng) //setting position
                    .draggable(true) //Making the marker draggable
                    .title("My Location")); //Adding a title

            //Moving the camera
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

            //Animating the camera
            mMap.animateCamera(CameraUpdateFactory.zoomTo(25));

        }
        else {
            gps.showSettingsAlert();
        }*/

        //Creating a LatLng Object to store Coordinates
        gps = new GPSTracker(this);
        double latitude = gps.getLatitude();
        double longitude = gps.getLongitude();

        if(!mMap.equals(null)) {
            mMap.clear();
            LatLng latLng = new LatLng(latitude, longitude);

            CameraPosition cameraPosition = new CameraPosition.Builder().target(latLng).zoom(15).build();
            //Adding marker to map
            mMap.addMarker(new MarkerOptions()
                    .position(latLng) //setting position
                    .draggable(true) //Making the marker draggable
                    .title(this.getString(R.string.my_location))); //Adding a title

            //Moving the camera
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

            //Animating the camera
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    public void logOut(){
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        Woyalla.myDatabase.Delete_All(Database.Table_USER);
                        Intent intent = new Intent(MainActivity.this,Login.class);
                        startActivity(intent);
                        finish();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.app_name).setMessage(this.getString(R.string.logout_title))
                .setPositiveButton(this.getString(R.string.yes), dialogClickListener)
                .setNegativeButton(this.getString(R.string.no), dialogClickListener)
                .show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        moveMapToMyLocation();
    }

    //share app method

    public void shareApp(){
        String shareBody = "Get Weyala app at  market://details?id=com.brainup.woyalla";
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Weyala");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.app_name)));
    }

    //rate the app method

    public void rateMyApp() {
        Uri uri = Uri.parse("market://details?id=com.brainup.woyalla");
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=com.brainup.woyalla")));
        }
    }

    public void ShowDialog(String message) {
        android.support.v7.app.AlertDialog.Builder builder;
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        // Yes button clicked
                        break;
                }
            }
        };
        builder = new android.support.v7.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.app_name)
                .setMessage(message)
                .setPositiveButton(MainActivity.this.getString(R.string.ok), dialogClickListener).show();
    }


}
