package com.bupt.air.airconditionsystem;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by OnlySaturday on 2015/6/2.
 */
public class DatabaseHelper extends SQLiteOpenHelper{

    private static final int VERSION = 1; //默认数据库版本
    DatabaseHelper(Context context, String dbName, SQLiteDatabase.CursorFactory factory, int version){
        super(context, dbName, factory, version);
    }

    DatabaseHelper(Context context, String dbName, int version){
        this(context, dbName, null, version);
    }

    DatabaseHelper(Context context, String dbName){
        this(context, dbName, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE XLS (roomNum int, onoff int, speed int, tarTemp int, cost int, mytime varchar(20))";
        System.out.print(sql);
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        System.out.println("SQLite onUpgrade");
    }
}
