package com.brainup.woyalla;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

/**
 * Created by Rog on 8/21/16.
 */

public class About extends AppCompatActivity {

    private TextView FaceBook_Link;  //facebook link button

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_about));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //initialize the button
        FaceBook_Link = (TextView) findViewById(R.id.txt_facebook);

        //set the on click listener for the facebook link button
        FaceBook_Link.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/woyalla"));
                startActivity(browserIntent);
            }
        });
    }
}
