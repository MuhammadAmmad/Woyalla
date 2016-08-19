package com.brainup.woyalla;


import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.brainup.woyalla.Database.Database;
import com.brainup.woyalla.Model.User;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class Login extends AppCompatActivity {

    /*
    * Object declarations
    * */

    private static final String TAG = "Log_In";
    private  AlertDialog.Builder builder;
	private ProgressDialog pDialog;
	private Context myContext;
	private EditText ed_phoneNumber, ed_name;
	private Button bt_login, bt_change_lang;
	private TextInputLayout inputLayoutPhone, inputLayoutName;
    private User Main_User;
    private ProgressDialog myDialog;

    OkHttpClient client;    //this object will handle http requests
    MediaType mediaType;
    RequestBody body;
    Request request;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);

		init();
        setButtonActions();
    }

	public void init(){

        /*
        * this will handle object initialization
        * */

		myContext = this;
        ed_phoneNumber = (EditText) findViewById(R.id.login_phone);
        ed_name = (EditText) findViewById(R.id.login_name);
		bt_login = (Button) findViewById(R.id.btnLogin);
        bt_change_lang = (Button)findViewById(R.id.login_btn_change_lang);
        inputLayoutPhone = (TextInputLayout) findViewById(R.id.login_txtinput_phone);
        inputLayoutName = (TextInputLayout) findViewById(R.id.login_inputtxt_name);

		ed_phoneNumber.addTextChangedListener(new MyTextWatcher(ed_phoneNumber));
        ed_name.addTextChangedListener(new MyTextWatcher(ed_name));
        Main_User = new User();

        /*
        * Initialize the http request objects
        * */
        client = new OkHttpClient();   //initialize the okHttpClient to send http requests
        mediaType = MediaType.parse("application/x-www-form-urlencoded");

        handleChangeLanguage();
    }

    private void handleChangeLanguage() {
        bt_change_lang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectLanguage();
            }
        });
    }

    public void setButtonActions(){

		/*
				Button login works
		*/

		bt_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitForm();
            }

        });


	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}


    /*
    * This method will validate the login forms and then perform the login action
    * */

    private void submitForm() {

        if (!validatePhone()) {
            return;
        }

        if (!validateName()) {
            return;
        }

        myDialog = new ProgressDialog(this);
        myDialog.setTitle(R.string.app_name);
        myDialog.setMessage("Logging in ....");
        myDialog.setCancelable(false);
        myDialog.show();


        Thread login = new Thread(){
            @Override
            public void run() {
                try {
                    sleep( 2000);
                } catch(InterruptedException e){
                } finally {
                    createAccount();   //this method will handle the actual login action
                }
            }
        };

        login.start();

    }

    public void createAccount(){
        Main_User.setName(ed_name.getText().toString());
        Main_User.setPhone(ed_phoneNumber.getText().toString());

        //initialize the body object for the http post request
        body = RequestBody.create(mediaType,
                "phoneNumber="+Main_User.getPhone() +
                        "&name="+Main_User.getName());

        //create the request object from http post
        request = new Request.Builder()
                .url(Woyalla.API_URL + "clients/register")
                .post(body)
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
                    Log.i("resposeJsonStatus", myObject.get("status").toString());
                    Log.i("resposeJsonMessage", myObject.get("message").toString());

                    JSONObject json_response = myObject.getJSONObject("data");
                    Log.i("responseJsonName", json_response.get("name").toString());
                    Log.i("responseJsonPhoneNumber", json_response.get("phoneNumber").toString());

                    ContentValues cv = new ContentValues();
                    cv.put(Database.USER_FIELDS[0], Main_User.getName());
                    cv.put(Database.USER_FIELDS[1], Main_User.getPhone());


                    long checkAdd = Woyalla.myDatabase.insert(Database.Table_USER,cv);
                    if(checkAdd!=-1){
                        myDialog.dismiss();

                        Login.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(Login.this,Login.this.getResources().getString(R.string.toast_register_ok_new),Toast.LENGTH_LONG).show();
                            }
                        });
                        Intent intent = new Intent(this,MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    else{
                        myDialog.dismiss();
                        ShowDialog(Login.this.getResources().getString(R.string.error_general));
                    }
                }

                /**
                 * If we get OK response & we don't get a data object with in the response json
                 * This is an existing user but the account is updated with new info
                 * */
                else{

                    ContentValues cv = new ContentValues();
                    cv.put(Database.USER_FIELDS[0], Main_User.getName());
                    cv.put(Database.USER_FIELDS[1], Main_User.getPhone());

                    long checkAdd = Woyalla.myDatabase.insert(Database.Table_USER,cv);
                    if (checkAdd != -1) {
                        Log.i("user","An already account is updated");
                        myDialog.dismiss();

                        Login.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(Login.this,Login.this.getResources().getString(R.string.toast_register_ok_update),Toast.LENGTH_LONG).show();
                            }
                        });
                        Intent intent = new Intent(this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    else {
                        myDialog.dismiss();
                        Login.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ShowDialog(Login.this.getString(R.string.error_general));
                            }
                        });
                    }

                }

            }
            /**
             * If we get error response
             *
             * */
            else if(myObject.get("status").toString().startsWith("error") ){
                final String errorMessage = myObject.get("description").toString();
                myDialog.dismiss();
                Login.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ShowDialog(Login.this.getString(R.string.error_general));
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
                    ShowDialog(Login.this.getString(R.string.error_general));
                }
            });
        } catch(JSONException e){
            myDialog.dismiss();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ShowDialog(Login.this.getString(R.string.error_general));
                }
            });
        }
    }

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
        builder = new AlertDialog.Builder(myContext);
        builder.setTitle(R.string.app_name)
                .setMessage(message)
                .setPositiveButton(Login.this.getString(R.string.ok), dialogClickListener).show();
    }

    public void ShowErrorDialog(String message) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        // Yes button clicked
                        startActivity(getIntent());
                        finish();
                        break;
                }
            }
        };
        builder = new AlertDialog.Builder(myContext);
        builder.setTitle(R.string.app_name)
                .setMessage(message)
                .setPositiveButton(Login.this.getString(R.string.ok), dialogClickListener).show();
    }

    private boolean validatePhone() {
        if (ed_phoneNumber.getText().toString().trim().isEmpty() || ed_phoneNumber.getText().toString().length()>12 || ed_phoneNumber.getText().toString().length()<6) {
            ed_phoneNumber.setError(getString(R.string.err_msg_phone));
            requestFocus(ed_phoneNumber);
            return false;
        }
        else {
            inputLayoutPhone.setErrorEnabled(false);
        }
        return true;
    }
    private boolean validateName() {
        if (ed_name.getText().toString().trim().isEmpty()) {
            ed_name.setError(getString(R.string.err_msg_name));
            requestFocus(ed_name);
            return false;
        } else {
            inputLayoutName.setErrorEnabled(false);
        }
        return true;
    }


    private void requestFocus(View view) {
        if (view.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
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
        startActivity( new Intent(Login.this,Login.class));
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
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(Login.this);
        builder.setTitle(R.string.app_name)
                .setMessage(getResources().getString(R.string.dialog_select_language))
                .setPositiveButton("English", dialogClickListener)
                .setNegativeButton("አማርኛ",dialogClickListener).show();
    }



    /*
    * Text watcher class which checks the values in the text fields
    * */
    private class MyTextWatcher implements TextWatcher {

        private View view;

        private MyTextWatcher(View view) {
            this.view = view;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void afterTextChanged(Editable editable) {
            switch (view.getId()) {
                case R.id.login_phone:
                    validatePhone();
                    break;
                case R.id.login_name:
                    validateName();
                    break;
            }
        }
    }


}
