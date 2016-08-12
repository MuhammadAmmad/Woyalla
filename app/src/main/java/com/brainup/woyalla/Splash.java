package com.brainup.woyalla;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

import com.brainup.woyalla.Database.Database;


public class Splash extends Activity {


	@Override
    protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

		Thread splash = new Thread(){
			@Override
			public void run() {
				try {
					sleep( 2000);
				} catch(InterruptedException e){
				} finally {
					getNextActivity();
				}
			}
        };
        
        splash.start();
	}

	public synchronized void getNextActivity() {

		int count = Woyalla.myDatabase.count(Database.Table_USER);  //check if there is any user in the database
		Log.i("count", "count "+count);
		if(count ==1){		//if there is one user, proceed to the main page
			Intent intent = new Intent(this, MainActivity.class);
			startActivity(intent);
			finish();
		}
		else if(count>1){	 	//if there is no user or more than one user in case, clear the database and proceed to the login page
			Woyalla.myDatabase.Delete_All(Database.Table_USER);
			Intent intent = new Intent(this, Login.class);
			startActivity(intent);
			finish();
		}
		else{
			Intent intent = new Intent(this, Login.class);
			startActivity(intent);
			finish();
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
}
