package com.brainup.woyalla;


import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

import com.brainup.woyalla.Database.Database;


public class Splash extends Activity {

	Language language;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

		language = new Language(this);
		language.init();

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

		ContentValues cv = new ContentValues();
		cv.put(Database.NEARBYE_DRIVERS_FIELDS[0],"2");
		cv.put(Database.NEARBYE_DRIVERS_FIELDS[1],"8.98123");
		cv.put(Database.NEARBYE_DRIVERS_FIELDS[2],"38.86160252");


		ContentValues cv2 = new ContentValues();
		cv2.put(Database.NEARBYE_DRIVERS_FIELDS[0],"2");
		cv2.put(Database.NEARBYE_DRIVERS_FIELDS[1],"8.9912012");
		cv2.put(Database.NEARBYE_DRIVERS_FIELDS[2],"38.86160252");


		ContentValues cv3 = new ContentValues();
		cv3.put(Database.NEARBYE_DRIVERS_FIELDS[0],"2");
		cv3.put(Database.NEARBYE_DRIVERS_FIELDS[1],"8.9095957");
		cv3.put(Database.NEARBYE_DRIVERS_FIELDS[2],"38.86160252");


		Woyalla.myDatabase.insert(Database.Table_NEARBYE_DRIVER,cv);
		Woyalla.myDatabase.insert(Database.Table_NEARBYE_DRIVER,cv2);
		Woyalla.myDatabase.insert(Database.Table_NEARBYE_DRIVER,cv3);

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
