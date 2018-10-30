package com.bupt.air.airconditionsystem;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.format.Time;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



public class Slave extends ActionBarActivity{
    private static final int SERVERPORT = 8888;
    private static final String SERVER_IP = "10.104.253.205";
    private Socket socket = null;
    private Button tempup = null, tempdown = null, speedup = null, speeddown = null;
    private Switch on = null;
    private TextView textView = null;
    private String command = "";
    private String text = "";
    JSONObject jsonObj = null;
    JSONArray jArray = null;
    private int onoff;  //从机开关标志
    private int mode;  //工作模式，由主机决定，从机无法改变，只能查看
    private int roomNum = -1;  //房间号，连接成功后由主机分配
    private int roomNumget;  //后来获取到的消息可能并不是这个房间的
    private double temp;  //当前温度
    private double tarTemp;  //目标温度
    private int speed;  //风速
    private double cost;  //当前消费
    private int requestType;  //请求类型，为了方便json分析管理
    private int HostOnOff;  //主机开关标志，主机不开亦无法操作
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            //显示收到的内容
            try {
                jArray = new JSONArray(msg.obj.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if(jArray != null)
                for(int i = 0; i < jArray.length(); i++)
                {
                    //获取每一个json对象
                    try {
                        jsonObj = jArray.getJSONObject(i);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    try {
                        requestType = jsonObj.getInt("requestType");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    try {
                        roomNumget = jsonObj.getInt("roomNum");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if(roomNum == -1)  //如果从机还没有分配房间号，只响应回复加入的请求类型
                    {
                        if(requestType == Command.HostCommandTypeAdd){
                            roomNum = roomNumget;
                            try {
                                onoff = jsonObj.getInt("on");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            try {
                                mode = jsonObj.getInt("mode");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            try {
                                temp = jsonObj.getDouble("temp");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            try {
                                tarTemp = jsonObj.getDouble("tarTemp");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            try {
                                speed = jsonObj.getInt("speed");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            try {
                                cost = jsonObj.getDouble("cost");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            try {
                                HostOnOff = jsonObj.getInt("host");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            if(onoff == Command.Off)
                                on.setChecked(false);
                            showInfo(onoff, roomNum, mode, temp, tarTemp, speed, cost);
                        }
                    }
                    else{  //如果已经有房间号了
                        if(roomNum == roomNumget){
                            //确认发送的消息是自己房间的再去获取每一个对象的值并更新
                            try {
                                onoff = jsonObj.getInt("on");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            try {
                                mode = jsonObj.getInt("mode");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            try {
                                temp = jsonObj.getDouble("temp");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            try {
                                tarTemp = jsonObj.getDouble("tarTemp");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            try {
                                speed = jsonObj.getInt("speed");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            try {
                                cost = jsonObj.getDouble("cost");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            try {
                                HostOnOff = jsonObj.getInt("host");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            if(onoff == Command.Off)
                                on.setChecked(false);
                            showInfo(onoff, roomNum, mode, temp, tarTemp, speed, cost);
                        }
                    }
                }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.slave_layout);

        onoff = Command.Off;  //默认关闭
        HostOnOff = Command.Off;//默认关闭
        on = (Switch) findViewById(R.id.on);
        tempup = (Button) findViewById(R.id.tempup);
        tempdown = (Button) findViewById(R.id.tempdown);
        speedup = (Button) findViewById(R.id.speedup);
        speeddown = (Button) findViewById(R.id.speeddown);
        textView = (TextView) findViewById(R.id.textView);
        on.setOnCheckedChangeListener(turnOnOff);
        tempup.setOnClickListener(watcher);
        tempdown.setOnClickListener(watcher);
        speedup.setOnClickListener(watcher);
        speeddown.setOnClickListener(watcher);

        init();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_air_condition_system, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //开关机设置
    private CompoundButton.OnCheckedChangeListener turnOnOff = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(HostOnOff == Command.Off) {
                Toast.makeText(Slave.this, "主机未开启！", Toast.LENGTH_SHORT).show();
                buttonView.setChecked(false);
            }
            else {
                if(isChecked){
                    //开机
                    command = "msg-[{\"requestType\":" + Command.SlaveCommandTypeSet + ",\"on\":" + Command.On + ",\"roomNum\":" + roomNum + "}]";
                } else {
                    //关机
                    command = "msg-[{\"requestType\":" + Command.SlaveCommandTypeSet + ",\"on\":" + Command.Off + ",\"roomNum\":" + roomNum + "}]";
                }

                knockSend();
            }
        }
    };

    private View.OnClickListener watcher = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId())
            {
                case R.id.tempup:
                    addTemp();
                    break;
                case R.id.tempdown:
                    deTemp();
                    break;
                case R.id.speedup:
                    addSpeed();
                    break;
                case R.id.speeddown:
                    deSpeed();
                    break;
                default:
                    System.out.println("Watcher Error!");
                    break;
            }
        }
    };

    private View.OnTouchListener touchWatcher = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            Time start = new Time();
            Time end = new Time();
            switch (event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    start.setToNow();
                    break;
                case MotionEvent.ACTION_UP:
                    end.setToNow();
                    break;
                case MotionEvent.ACTION_CANCEL:
                    end.setToNow();
                    break;
            }

            int seconds = end.second - start.second;


            return false;
        }
    };
    
    //开关机控制
    private void setOnOff(){

    }

    //目标温度加
    private void addTemp(){
        if((onoff == Command.On || onoff == Command.Standby) && HostOnOff == Command.On) {
            switch (mode) {
                case Command.Hot:
                    if (tarTemp + 1 > Command.Temp_Hot_High)
                        Toast.makeText(Slave.this, "当前模式最大温度设定30℃", Toast.LENGTH_SHORT).show();
                    else {
                        command = "msg-[{\"roomNum\":" + roomNum + ",\"tarTemp\":" + (tarTemp + 1) + ",\"requestType\":" + Command.SlaveCommandTypeSet + "}]";
                        knockSend();
                    }
                    break;
                case Command.Cool:
                    if (tarTemp + 1 > Command.Temp_Cool_High)
                        Toast.makeText(Slave.this, "当前模式最大温度设定25℃", Toast.LENGTH_SHORT).show();
                    else {
                        command = "msg-[{\"roomNum\":" + roomNum + ",\"tarTemp\":" + (tarTemp + 1) + ",\"requestType\":" + Command.SlaveCommandTypeSet + "}]";
                        knockSend();
                    }
                    break;
                default:
                    System.out.println("Mode error!");
                    break;
            }
        }
        else
            Toast.makeText(Slave.this, "尚未开机，操作无效！", Toast.LENGTH_SHORT).show();
    }

    //目标温度减
    private void deTemp(){
        if((onoff == Command.On || onoff == Command.Standby)&& HostOnOff == Command.On) {
            switch (mode) {
                case Command.Hot:
                    if (tarTemp - 1 < Command.Temp_Hot_Low)
                        Toast.makeText(Slave.this, "当前模式最低温度设定25℃", Toast.LENGTH_SHORT).show();
                    else {
                        command = "msg-[{\"roomNum\":" + roomNum + ",\"tarTemp\":" + (tarTemp - 1) + ",\"requestType\":" + Command.SlaveCommandTypeSet + "}]";
                        knockSend();
                    }
                    break;
                case Command.Cool:
                    if (tarTemp - 1 < Command.Temp_Cool_Low)
                        Toast.makeText(Slave.this, "当前模式最低温度设定18℃", Toast.LENGTH_SHORT).show();
                    else {
                        command = "msg-[{\"roomNum\":" + roomNum + ",\"tarTemp\":" + (tarTemp - 1) + ",\"requestType\":" + Command.SlaveCommandTypeSet + "}]";
                        knockSend();
                    }
                    break;
                default:
                    System.out.println("Mode error!");
                    break;
            }
        }
        else
            Toast.makeText(Slave.this, "尚未开机，操作无效！", Toast.LENGTH_SHORT).show();
    }

    //风速加
    private void addSpeed(){
        if((onoff == Command.On || onoff == Command.Standby) && HostOnOff == Command.On) {
            if (speed + 1 > Command.Speed_High)
                Toast.makeText(Slave.this, "已经在最高风速！", Toast.LENGTH_SHORT).show();
            else {
                command = "msg-[{\"roomNum\":" + roomNum + ",\"speed\":" + (speed + 1) + ",\"requestType\":" + Command.SlaveCommandTypeSet + "}]";
                knockSend();
            }
        }
        else
            Toast.makeText(Slave.this, "尚未开机，操作无效！", Toast.LENGTH_SHORT).show();
    }

    //风速减
    private void deSpeed(){
        if((onoff == Command.On || onoff == Command.Standby) && HostOnOff == Command.On) {
            if (speed - 1 < Command.Speed_Low)
                Toast.makeText(Slave.this, "已经在最低风速！", Toast.LENGTH_SHORT).show();
            else {
                command = "msg-[{\"roomNum\":" + roomNum + ",\"speed\":" + (speed - 1) + ",\"requestType\":" + Command.SlaveCommandTypeSet + "}]";
                knockSend();
            }
        }
        else
            Toast.makeText(Slave.this, "尚未开机，操作无效！", Toast.LENGTH_SHORT).show();
    }

    //通知发送线程发送指令
    private void knockSend(){
        ThreadPoolUtil.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                sendMessage();
            }
        });
    }

    //初始化Socket连接，并开始接收信息
    public void init(){
        ThreadPoolUtil.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(SERVER_IP, SERVERPORT);
                    DataOutputStream out = new DataOutputStream(
                            socket.getOutputStream());
                    command = "choosePrio-1";
                    out.write(command.getBytes("UTF-8"));
                    command = "msg-[{\"requestType\":" + Command.SlaveCommandTypeAdd + "}]";
                    out.write(command.getBytes("UTF-8"));
                    receiveMessage();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //接收主机信息
    public void receiveMessage(){
        try{
            while(true){
                DataInputStream input = new DataInputStream(socket.getInputStream());
                byte[] buffer;
                buffer = new byte[input.available()];
                if(buffer.length != 0){
                    // 读取缓冲区
                    input.read(buffer);
                    String msg = new String(buffer, "UTF-8");//注意转码，不然中文会乱码。
                    Message m = new Message();
                    m.obj = msg;
                    mHandler.sendMessage(m);
                }
            }
        }catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    //向主机发送信息
    public void sendMessage(){
        try {
            //向服务器端发送数据
            DataOutputStream out = new DataOutputStream(
                    socket.getOutputStream());
            out.write(command.getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy(){
        if(socket != null)
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        socket = null;
        super.onDestroy();
    }

    private void showInfo(int onoff, int roomNum, int mode, double temp, double tarTemp, int speed, double cost){
        String text = "";

        if(onoff == Command.On)
            text = "开关机：开";
        else if(onoff == Command.Off)
            text = "开关机：关";
        else if(onoff == Command.Standby)
            text = "开关机：待机";
        else
            text = "开关机：Error";
        text += "\n房间号：" + roomNum;
        if(mode == Command.Cool)
            text += "\n工作模式：制冷";
        else if(mode == Command.Hot)
            text += "\n工作模式：制热";
        else
            text += "\n工作模式：Error";
        text += "\n当前温度：" + String.format("%.1f", temp) + "\n目标温度：" + String.format("%.1f", tarTemp) + "\n当前风速：" + speed + "\n消费金额：" + cost;
        textView.setText(text);
    }
}
