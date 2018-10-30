package com.bupt.air.airconditionsystem;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by OnlySaturday on 2015/6/2.
 */
public class Pay extends Activity{
    TextView info = null;
    RadioGroup group = null;
    RadioButton flush1 = null, flush2 = null, flush3 = null;
    DatabaseHelper dbHelper = null;
    SQLiteDatabase db = null;
    String str = "", basic = "RoomID--State--Speed--TarTemp--Cost--Timestamp\n";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pay);

        Bundle bundle = this.getIntent().getExtras();
        str = bundle.getString("xls");
        info = (TextView) findViewById(R.id.info1);
        group = (RadioGroup) findViewById(R.id.group);
        flush1 = (RadioButton) findViewById(R.id.flush1);
        flush2 = (RadioButton) findViewById(R.id.flush2);
        flush3 = (RadioButton) findViewById(R.id.flush3);
        group.setOnCheckedChangeListener(xlsListener);
        dbHelper = new DatabaseHelper(this, "AirConditionSystem.db");
        db = dbHelper.getWritableDatabase();

    }

    private RadioGroup.OnCheckedChangeListener xlsListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            /*
            Cursor p = null;
            int num, onoff, speed, tarTemp;
            double cost;
            String str = "", msg = "";
            Date date = null;
            */
            if(checkedId == flush3.getId()){  //每月
                str = basic + str;
                info.setText(str);
                /*
                p = db.query("XLS", new String[]{"roomNum", "onoff", "speed", "tarTemp", "cost", "'mytime'"}, null, null, null, null, null, null);
                while (p.moveToNext()) {
                    num = p.getInt(p.getColumnIndex("roomNum"));
                    onoff = p.getInt(p.getColumnIndex("onoff"));
                    speed = p.getInt(p.getColumnIndex("speed"));
                    tarTemp = p.getInt(p.getColumnIndex("tarTemp"));
                    cost = p.getDouble(p.getColumnIndex("cost"));
                    str = p.getString(p.getColumnIndex("mytime"));

                    msg += num + " " + onoff + " " + speed + " " + tarTemp + " " + cost + " " + str + "\n";
                }

                info.setText(msg);
                */
            }
            else if(checkedId == flush2.getId()){  //每周
                str = basic + str;
                info.setText(str);
                /*
                p = db.query("XLS", new String[]{"roomNum", "onoff", "speed", "tarTemp", "cost", "'mytime'"}, null, null, null, null, null, null);
                while (p.moveToNext()) {
                    num = p.getInt(p.getColumnIndex("roomNum"));
                    onoff = p.getInt(p.getColumnIndex("onoff"));
                    speed = p.getInt(p.getColumnIndex("speed"));
                    tarTemp = p.getInt(p.getColumnIndex("tarTemp"));
                    cost = p.getDouble(p.getColumnIndex("cost"));
                    str = p.getString(p.getColumnIndex("mytime"));

                    msg += num + " " + onoff + " " + speed + " " + tarTemp + " " + cost + " " + str + "\n";
                }

                info.setText(msg);
                */
            } else { //默认都是看当日的
                str = basic + str;
                info.setText(str);
                /*msg = "roomNum  onoff  speed  tarTemp  cost\n";
                p = db.rawQuery("select roomNum,onoff,speed,tarTemp from XLS",null);
                while (p.moveToNext()) {
                    num = p.getInt(p.getColumnIndex("roomNum"));
                    onoff = p.getInt(p.getColumnIndex("onoff"));
                    speed = p.getInt(p.getColumnIndex("speed"));
                    tarTemp = p.getInt(p.getColumnIndex("tarTemp"));
                    //cost = p.getInt(p.getColumnIndex("cost"));

                    msg += num + "                   " + onoff + "            " + speed + "            " + tarTemp + "         "  + "\n";
                }

                info.setText(msg);*/
            }


        }
    };


}
