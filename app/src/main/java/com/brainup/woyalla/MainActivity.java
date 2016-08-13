package com.brainup.woyalla;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;

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

        //map view initialization
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //call button initialization
        call = (Button) findViewById(R.id.call);

        //initialize the gps tracker object
        gps  = new GPSTracker(this);

        checkGPS();
        handleCallButton();
    }

    private void checkGPS() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //if it permission not granted, request a dialog box that asks the client to grant the permission
            //the response will be handled by onRequestPermissionResult method
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},2);
            return;
        }
        if(!gps.canGetLocation()){
            gps.showSettingsAlert();
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
                    startCall();
            }
        });

    }

    public void startCall() {
        Toast.makeText(MainActivity.this, "Calling the server ", Toast.LENGTH_SHORT).show();
        String phoneNumber = getResources().getString(R.string.call_center_number);
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(intent);

        sendGPSLocation();
    }

    private void sendGPSLocation() {
        gps = new GPSTracker(MainActivity.this);
        if(gps.canGetLocation()) {

            final double latitude = gps.getLatitude();
            final double longitude = gps.getLongitude();
            final String userPhone = Woyalla.myDatabase.get_Value_At_Top(Database.Table_USER,Database.USER_FIELDS[1]);

            ContentValues cv = new ContentValues();
            cv.put(Database.USER_FIELDS[2],latitude+"");
            cv.put(Database.USER_FIELDS[3],longitude+"");

            Woyalla.myDatabase.insert(Database.Table_USER,cv);
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
                                .url(Woyalla.API_URL + "clients/update/phoneNumber")
                                .put(body)
                                .addHeader("authorization", "Basic dGhlVXNlcm5hbWU6dGhlUGFzc3dvcmQ=")
                                .addHeader("cache-control", "no-cache")
                                .addHeader("content-type", "application/x-www-form-urlencoded")
                                .build();

                        Response response = client.newCall(request).execute();
                        final String responseBody = response.body().string().toString();
                        Log.i("responseFull", responseBody);

                        JSONObject myObject = (JSONObject) new JSONTokener(responseBody).nextValue();

                    /**
                     * If we get OK response
                     *
                     * */
                        if(myObject.get("status").toString().startsWith("ok") ){

                            /**
                             * If the driver status is on service
                             */
                            if(myObject.get("data")!=null) {
                                JSONObject json_response = myObject.getJSONObject("data");
                                JSONArray json_drivers = json_response.getJSONArray("nearbyDrivers");
                                addDriver(json_drivers);

                                MainActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ShowDialog("Your location is sent.\nNear bye drivers also added.");
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
                                    ShowDialog("An error occured while sending your location to the server. ");
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
                        cv.put(Database.NEARBYE_DRIVERS_FIELDS[0], obj.getString("distanceFromCilent"));
                        cv.put(Database.NEARBYE_DRIVERS_FIELDS[1], obj.getString("gpsLatitude"));
                        cv.put(Database.NEARBYE_DRIVERS_FIELDS[2], obj.getString("gpsLongitude"));

                        long check = Woyalla.myDatabase.insert(Database.Table_NEARBYE_DRIVER,cv);
                        if(check>0){
                            Log.i("driverarray","New drivers array added ");
                        }
                    }
                }
            }catch (Exception e){
            }

    }

    /**
     * Show message
     * */
    public void ShowDialog(String message) {
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
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.app_name)
                .setMessage(message)
                .setPositiveButton("Ok", dialogClickListener).show();
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
                        Toast.makeText(MainActivity.this,"Permission denied! Please Grant Permission",Toast.LENGTH_LONG).show();
                    }
                    break;
                case 2:
                    if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(MainActivity.this,"Permission granted! Thank you!",Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Toast.makeText(MainActivity.this,"Permission denied! Please Grant Permission",Toast.LENGTH_SHORT).show();
                        checkGPS();
                    }
            }
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
        else if (id == R.id.menu_show_drivers) {
            showNearByeCars();
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

    public void showNearByeCars(){
        ArrayList<Driver> drivers = Woyalla.myDatabase.getNearByeCars();
        if(drivers.size()>0){
            mMap.clear();
            moveMapToMyLocation();
            Toast.makeText(this,"Getting list of near bye cars. ",Toast.LENGTH_LONG).show();
            for(int i = 0; i <drivers.size(); i++){
                LatLng latLng = new LatLng(drivers.get(i).getLatitude(),drivers.get(i).getLongitude());
                createMarkers(latLng,"Taxi "+i,"Distance: "+drivers.get(i).getDistance());
            }
        }
        else{
            Toast.makeText(this,"Please make a call first",Toast.LENGTH_LONG).show();
        }

    }

    public void createMarkers(LatLng latLng,String title,String snippet){
        mMap.addMarker(new MarkerOptions()
                .position(latLng) //setting position
                .snippet(snippet)
                .draggable(true) //Making the marker draggable
                .title(title)); //Adding a title
    }

    //reload the client map & data
    public void reload(){
        //remove all near bye drivers
        Woyalla.myDatabase.Delete_All(Database.Table_NEARBYE_DRIVER);
        moveMapToMyLocation();
        Toast.makeText(MainActivity.this,"Location is reloaded \nPrevious near bye drivers has been removed also." +
                " \nThe map is also set to your current location.",Toast.LENGTH_LONG).show();
    }

    /**
     * get current location from the gps tracker object
     * then view it on the map
     */
    private void moveMapToMyLocation() {
        //Creating a LatLng Object to store Coordinates
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
        builder.setTitle(R.string.app_name).setMessage("Are you sure you want to Logout?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener)
                .show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        moveMapToMyLocation();
    }

    //share app method

    public void shareApp(){
        String shareBody = "Get Free amharic dictionary  market://details?id=com.brainup.woyalla";
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Amharic dictionary");
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

}
