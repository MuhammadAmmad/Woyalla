
package com.brainup.woyalla.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.brainup.woyalla.Model.Driver;

import java.util.ArrayList;

public class Database {
    public static final String Table_USER = "USER";
    public static final String Table_NEARBYE_DRIVER = "NEARBYE_DRIVERS";

    public static final String[] USER_FIELDS = { "Name","Phone","Latitude","Longitude"};
    public static final String[] NEARBYE_DRIVERS_FIELDS = { "distanceFromClient","gpsLatitude","gpsLongitude","name"};

    public static final String[] USER_COLUMN = { "id", "Name","Phone","Latitude","Longitude"};
    public static final String[] NEARBYE_DRIVERS_COLUMN = { "id","distanceFromClient","gpsLatitude","gpsLongitude","name"};


	private SQLiteDatabase myDatabase;
	private SQL_Helper mySQL;
	private Context myContext;
    public static final String TAG = "Database";

    public Database(Context context){
        myContext = context;
        mySQL = new SQL_Helper(myContext);
        myDatabase = mySQL.getWritableDatabase();

        mySQL.createTables(Table_USER, USER_FIELDS);
        mySQL.createTables(Table_NEARBYE_DRIVER, NEARBYE_DRIVERS_FIELDS);

    }

    public long insert(String DB_Table,ContentValues cv){
        long state = myDatabase.insert(DB_Table, null, cv);
        Log.i(TAG, "Inserting->: " + cv.toString());
        return state;
    }
    public long Delete_All(String DB_Table){
        long state = myDatabase.delete(DB_Table, "1", null);
        return state;
    }
    public long remove(String DB_Table,int id){
        String[] args = {""+id};
        long val = myDatabase.delete(DB_Table, "id = ?", args);
        return val;
    }

    public long update(String DB_Table,ContentValues cv,int id){
        Log.i(TAG, "Updating Table: "+DB_Table);
        String[] args = {""+id};
        long state = myDatabase.update(DB_Table, cv, "id = ?", args);
        Log.i(TAG, "Updating Data: "+cv.toString());
        Log.i(TAG, "Updating Completed: "+state+"\n");
        return state;
    }

    public int count(String DB_Table){
        Cursor c = myDatabase.query(DB_Table, getColumns(DB_Table), null, null, null, null, null);
        if(c != null){
            return c.getCount();
        }else{
            return 0;
        }
    }
    public Cursor getAll(String DB_Table){
        Cursor c = myDatabase.query(DB_Table, getColumns(DB_Table), null, null, null, null, null);
        return c;
    }

    public String get_Value_At_Top(String DB_Table,String column){
        String str = "";
        try {
            Cursor c = myDatabase.query(DB_Table, getColumns(DB_Table), null, null, null, null, null);
            c.moveToFirst();
            str = c.getString(c.getColumnIndex(column));
        }catch (Exception e){

        }

        return str;
    }

    public String get_Value_At_Bottom(String DB_Table,String column){
        String str = "";
        try{
            Cursor c = myDatabase.query(DB_Table, getColumns(DB_Table), null, null, null, null, null);
            c.moveToLast();
            str = c.getString(c.getColumnIndex(column));
        }catch (Exception e){

        }
        return str;
    }

    public Cursor get_value_by_ID (String DB_Table,String id){
        Cursor cur = myDatabase.rawQuery("select * from " + DB_Table + " where id=" + id, null);
        return cur;
    }


    public long Delete_By_ID(String DB_Table,int pos){
        String[] args = {""+pos};
        long val = myDatabase.delete(DB_Table, "id = ?", args);
        return val;
    }
    public long Delete_By_Column(String DB_Table,String column,String val){
        String[] args = {val};
        long v = myDatabase.delete(DB_Table, column + " = ?", args);
        return v;
    }
    public int get_Top_ID(String DB_Table){
        int pos = -1;
        try{
            Cursor c = myDatabase.query(DB_Table, getColumns(DB_Table), null, null, null, null, null);
            c.moveToFirst();
            pos = Integer.valueOf(c.getString(c.getColumnIndex("id")));
        }catch (Exception e){

        }
        return pos;
    }

    public ArrayList<Driver> getNearByeCars(){
        ArrayList<Driver> drivers = new ArrayList<Driver>();
        try{
            Cursor c = myDatabase.query(Table_NEARBYE_DRIVER, getColumns(Table_NEARBYE_DRIVER), null, null, null, null, null);
            if (c.getCount() > 0) {
                c.moveToFirst();
                for(int i=0;i<c.getCount();i++){
                    c.moveToPosition(i);
                    Driver dis = new Driver();
                    dis.setDistance(Double.parseDouble(c.getString(c.getColumnIndex(NEARBYE_DRIVERS_COLUMN[1]))));
                    dis.setLatitude(Double.parseDouble(c.getString(c.getColumnIndex(NEARBYE_DRIVERS_COLUMN[2]))));
                    dis.setLongitude(Double.parseDouble(c.getString(c.getColumnIndex(NEARBYE_DRIVERS_COLUMN[3]))));
                    dis.setName(c.getString(c.getColumnIndex(NEARBYE_DRIVERS_COLUMN[4])));
                    drivers.add(dis);
                }
            }
        }catch (Exception e){

        }

        return drivers;
    }

    private String[] getColumns(String DB_Table){
        String[] strs = null;
        if(DB_Table == Table_USER){
            strs = USER_COLUMN;
        }
        else if(DB_Table == Table_NEARBYE_DRIVER){
            strs = NEARBYE_DRIVERS_COLUMN;
        }
        return strs;
    }
}
