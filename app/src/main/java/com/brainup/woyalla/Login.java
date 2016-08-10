package com.brainup.woyalla;


import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.brainup.woyalla.Database.Database;
import com.brainup.woyalla.Model.User;


public class Login extends AppCompatActivity {

    /*
    * Object declarations
    * */

    private static final String TAG = "Log_In";
    private  AlertDialog.Builder builder;
	private ProgressDialog pDialog;
	private Context myContext;
	private EditText ed_phoneNumber, ed_name;
	private Button bt_login;
	private TextInputLayout inputLayoutPhone, inputLayoutName;
    private User Main_User;
    private ProgressDialog myDialog;

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

        inputLayoutPhone = (TextInputLayout) findViewById(R.id.login_txtinput_phone);
        inputLayoutName = (TextInputLayout) findViewById(R.id.login_inputtxt_name);

		ed_phoneNumber.addTextChangedListener(new MyTextWatcher(ed_phoneNumber));
        ed_name.addTextChangedListener(new MyTextWatcher(ed_name));
        Main_User = new User();
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
        myDialog.setMessage("Creating the Account ....");
        myDialog.show();


        Thread splash = new Thread(){
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

        splash.start();

    }

    public void createAccount(){
        Main_User.setName(ed_name.getText().toString());
        Main_User.setPhone(ed_phoneNumber.getText().toString());

        ContentValues cv = new ContentValues();
        cv.put(Database.USER_FIELDS[0], Main_User.getName());
        cv.put(Database.USER_FIELDS[1], Main_User.getPhone());

        long checkAdd = Woyalla.myDatabase.insert(Database.Table_USER,cv);
        if(checkAdd!=-1){
            //Toast.makeText(this,"Account has been created",Toast.LENGTH_SHORT).show();
            myDialog.dismiss();
            Intent intent = new Intent(this,MainActivity.class);
            startActivity(intent);
            finish();
        }
        else{
            //Toast.makeText(this,"An error occured! Please try again",Toast.LENGTH_LONG).show();
            myDialog.dismiss();
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
                .setPositiveButton("Ok", dialogClickListener).show();
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
                .setPositiveButton("Ok", dialogClickListener).show();
    }

    private boolean validatePhone() {
        if (ed_phoneNumber.getText().toString().trim().isEmpty() || ed_phoneNumber.getText().toString().length()>12 || ed_phoneNumber.getText().toString().length()<6) {
            inputLayoutPhone.setError(getString(R.string.err_msg_phone));
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
            inputLayoutName.setError(getString(R.string.err_msg_name));
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
