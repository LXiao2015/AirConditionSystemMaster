package com.bupt.air.airconditionsystem;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.format.Time;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


public class Host extends ActionBarActivity{
    private static final int SERVERPORT = 8888;
    private static final String SERVER_IP = "10.104.253.205";
    private Socket socket = null;
    private BufferedReader in = null;
    private PrintWriter out = null;
    private String command = "";
    private String text = "";
    TextView info = null, currentTarTemp = null;
    Button modeChange = null, xls = null;
    Switch on = null;
    SeekBar seekbar = null;
    RadioGroup group = null;
    RadioButton flush1 = null, flush2 = null, flush3 = null;
    JSONObject jsonObj = null;
    JSONArray jArray = null;

    private int onoff;  //主机开关标志
    private int gmode;   //全局模式，唯一
    private int onOff;  //接收json参数
    private int random = 1;
    private int roomNum;
    private double temp;
    private double tarTemp;
    private int speed;
    private double cost;
    private int requestType;
    Timer timer = null;
    Time  time = null;
    private SlaveCluster []slaves = new SlaveCluster[15];
    public Record pay = null;

    private Handler vHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            info.setText((String) msg.obj);
        }
    };

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            Toast.makeText(Host.this, "Handle a message",Toast.LENGTH_SHORT).show();//Toast是一个View视图，快速的为用户显示少量的信息
            //显示收到的内容
            try {//用来捕捉异常，有些程序可能运行会产生错误，所以才有了提前处理
                jArray = new JSONArray(msg.obj.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            text = "";
            if(jArray != null)
                for(int i = 0; i < jArray.length(); i++)
                {
                    //获取每一个json对象
                    try {
                        jsonObj = jArray.getJSONObject(i);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    //获取每一个对象的值,如果该对象不存在，后台记录下来，不影响继续运行
                    try {
                        onOff = jsonObj.getInt("on");
                    } catch (JSONException e) {
                        onOff = 0;  //没收到该参数，则置零
                        e.printStackTrace();//在命令行打印异常信息在程序中出错的位置及原因
                    }
                    try {
                        roomNum = jsonObj.getInt("roomNum");
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
                        requestType = jsonObj.getInt("requestType");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    //如果是加入请求，判断是否同意
                    if(requestType == Command.SlaveCommandTypeAdd){
                        int j = 0;
                        for(j = 0; j < 15; j ++){
                            if(slaves[j] == null) {
                                slaves[j] = new SlaveCluster();
                                slaves[j].setOnoff(Command.Off);
                                slaves[j].setMode(gmode);
                                slaves[j].setRoomNum(random++);
                                slaves[j].setTemp(25);  //默认房间温度为25℃
                                slaves[j].setTarTemp(25);  //默认设定温度为25℃
                                slaves[j].setSpeed(Command.Speed_Low);
                                String t = "msg-[{\"roomNum\":" + slaves[j].getRoomNum() + ",\"mode\":" + gmode +",\"temp\":" + slaves[j].getTemp() + ",\"tarTemp\":" + slaves[j].getTarTemp() +
                                        ",\"cost\":" + slaves[j].getCost() + ",\"host\":" + onoff + ",\"requestType\":" + Command.HostCommandTypeAdd + "}]";
                                Host.this.sendMessage(t);
//SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
//Date curDate = new Date(System.currentTimeMillis());//获取当前时间
//String   str   =   formatter.format(curDate);
                                time.setToNow();
                                pay.insert(slaves[j].getRoomNum(), slaves[j].getOnoff(), slaves[j].getSpeed(), (int) slaves[j].getTarTemp(), slaves[j].getCost(), time.format("%Y-%m-%d %H:%M:%S"));
//insert(random-1, Command.Off, Command.Speed_Low, 25, 0, str);
                                break;
                            }
                        }
                        if(j == 15){
                            Toast.makeText(Host.this,"拒绝了一个从机加入请求，原因：已达到最大从机数",Toast.LENGTH_SHORT).show();
                        }
                    }
                    //如果是设置参数请求，则执行下列代码
                    else if(requestType == Command.SlaveCommandTypeSet){
                        int j = 0;
                        for(j = 0; j < 15; j ++){
                            if((slaves[j] != null) && (slaves[j].getRoomNum() == roomNum)){  //从机可以修改的参数只有：从机开关、目标温度、风速
                                if(onOff != 0){
                                    slaves[j].setState(onOff);
//SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
//Date curDate = new Date(System.currentTimeMillis());//获取当前时间
//String str = formatter.format(curDate);
                                    time.setToNow();
                                    pay.insert(slaves[j].getRoomNum(), slaves[j].getOnoff(), slaves[j].getSpeed(), (int) slaves[j].getTarTemp(), slaves[j].getCost(), time.format("%Y-%m-%d %H:%M:%S"));
//insert(slaves[j].getRoomNum(), slaves[j].getOnoff(), slaves[j].getSpeed(), (int) slaves[j].getTarTemp(), (int)slaves[j].getCost(), str);
                                }
                                if(tarTemp != 0){
                                    slaves[j].setTarTemp(tarTemp);
//SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
//Date curDate = new Date(System.currentTimeMillis());//获取当前时间
//String str = formatter.format(curDate);
                                    time.setToNow();
                                    pay.insert(slaves[j].getRoomNum(), slaves[j].getOnoff(), slaves[j].getSpeed(), (int) slaves[j].getTarTemp(), slaves[j].getCost(), time.format("%Y-%m-%d %H:%M:%S"));
//insert(slaves[j].getRoomNum(), slaves[j].getOnoff(), slaves[j].getSpeed(), (int) slaves[j].getTarTemp(), (int)slaves[j].getCost(), str);
                                    if(slaves[j].getMode() == Command.Hot && tarTemp < slaves[j].getTemp())
                                        slaves[j].setState(Command.Standby);
                                    else if(slaves[j].getMode() == Command.Cool && tarTemp > slaves[j].getTemp())
                                        slaves[j].setState(Command.Standby);
                                    else
                                        if(slaves[j].getOnoff() == Command.On || slaves[j].getOnoff() == Command.Standby)  //只有开机\待机的时候才会发生温度变化，不开机只会显示屏上改变，但是温度并不变
                                            slaves[j].setState(Command.On);
                                }
                                if(speed != 0){
                                    slaves[j].setSpeed(speed);
//SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
//Date curDate = new Date(System.currentTimeMillis());//获取当前时间
//String str = formatter.format(curDate);
                                    time.setToNow();
                                    pay.insert(slaves[j].getRoomNum(), slaves[j].getOnoff(), slaves[j].getSpeed(), (int) slaves[j].getTarTemp(), slaves[j].getCost(), time.format("%Y-%m-%d %H:%M:%S"));
//insert(slaves[j].getRoomNum(), slaves[j].getOnoff(), slaves[j].getSpeed(), (int) slaves[j].getTarTemp(), (int)slaves[j].getCost(), str);
                                }
                                else
                                    System.out.println("其他参数，不作修改");
                                String t = "msg-[{\"roomNum\":" + slaves[j].getRoomNum() + ",\"on\":" + slaves[j].getOnoff() + ",\"temp\":" + slaves[j].getTemp() + ",\"tarTemp\":" + slaves[j].getTarTemp() +
                                        ",\"speed\":" + slaves[j].getSpeed() + ",\"host\":" + onoff + ",\"cost\":" + slaves[j].getCost() + ",\"requestType\":" + Command.HostCommandTypeAdd + "}]";
                                Host.this.sendMessage(t);
                                break;
                            }
                        }
                        if(j == 15){
                            Toast.makeText(Host.this,"不存在的从机发出了一个请求，未被响应",Toast.LENGTH_SHORT).show();
                        }
                    }
                    else{  //其他类型的请求不予回复
                        System.out.println("-----------------------------requestType Error!-----------------------------");
                    }
                }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.host_layout);

        onoff = Command.Off;  //默认主机是关闭的
        gmode = Command.Cool;  //默认为制冷模式
        info = (TextView) findViewById(R.id.info);
        currentTarTemp = (TextView) findViewById(R.id.tempnow);
        on = (Switch) findViewById(R.id.on);
        group = (RadioGroup) findViewById(R.id.group);
        flush1 = (RadioButton) findViewById(R.id.flush1);
        flush2 = (RadioButton) findViewById(R.id.flush2);
        flush3 = (RadioButton) findViewById(R.id.flush3);
        modeChange = (Button) findViewById(R.id.modeChange);
        xls = (Button) findViewById(R.id.xls);
        seekbar = (SeekBar) findViewById(R.id.seekBar);
        seekbar.setMax(12);  //seekbar不能设定最小值，最小值只能为0，所以我们用获取到的值加18从而获得18-30度（其实程序检测到的是0-12，人为加18）
        seekbar.setOnSeekBarChangeListener(changeWatcher);
        on.setOnCheckedChangeListener(turnOnOff);
        modeChange.setOnClickListener(watcher);
        xls.setOnClickListener(watcher);
        group.setOnCheckedChangeListener(flush);

//DatabaseHelper dbHelper = new DatabaseHelper(this, "AirConditionSystem.db");
        time = new Time();
        time.setToNow();
        pay = new Record();
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

    private View.OnClickListener watcher = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId())
            {
                case R.id.modeChange:
                    changeMode();
                    break;
                case R.id.xls:
                    Intent intent = new Intent(Host.this, Pay.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("xls", pay.toString());
                    intent.putExtras(bundle);
                    startActivity(intent);
                    break;
                default:
                    System.out.println("Watcher Error!");
                    break;
            }
        }
    };

    //开关机设置
    private CompoundButton.OnCheckedChangeListener turnOnOff = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked){
                //开机
                command = "msg-[{\"host\":" + Command.On + "}]";
                onoff = Command.On;
            } else {
                //关机
                command = "msg-[{\"host\":" + Command.Off + "}]";
                onoff = Command.Off;
                //此时所有从机状态置为关闭
                for(int i = 0; i < 15; i ++){
                    if(slaves[i] != null){
                        slaves[i].setState(Command.Off);
//SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
//Date curDate = new Date(System.currentTimeMillis());//获取当前时间
//String str = formatter.format(curDate);
                        time.setToNow();
                        pay.insert(slaves[i].getRoomNum(), slaves[i].getOnoff(), slaves[i].getSpeed(), (int) slaves[i].getTarTemp(), slaves[i].getCost(), time.format("%Y-%m-%d %H:%M:%S"));
//insert(slaves[i].getRoomNum(), slaves[i].getOnoff(), slaves[i].getSpeed(), (int) slaves[i].getTarTemp(), (int)slaves[i].getCost(), str);
                    }
                }
            }
            knockSend();
        }
    };

    //拖动条监控，实现温度统一设定
    private SeekBar.OnSeekBarChangeListener changeWatcher = new SeekBar.OnSeekBarChangeListener() {
        int init = -1;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            currentTarTemp.setText("统一温度设定：" + (progress + 18) + "℃");
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            //记录开始滑动时候的温度设定值，如果失败则回到该值
            init = seekBar.getProgress();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            double temp = (double) seekBar.getProgress();
            temp += 18;
            if(onoff == Command.On) {
                switch (gmode) {
                    case Command.Hot:
                        if (temp >= Command.Temp_Hot_Low && temp <= Command.Temp_Hot_High) {
                            updateAllSlaves(temp);
                            for(int i = 0; i < 15; i ++){
                                if(slaves[i] != null) {
                                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                                    Date curDate = new Date(System.currentTimeMillis());//获取当前时间
                                    String str = formatter.format(curDate);
//insert(slaves[i].getRoomNum(), slaves[i].getOnoff(), slaves[i].getSpeed(), (int) slaves[i].getTarTemp(), (int)slaves[i].getCost(), str);
                                }
                            }
                            command = "msg-[{\"tarTemp\":" + temp + ",\"requestType\":" + Command.HostCommandTypeSet + "}]";
                            knockSend();
                        } else {
                            Toast.makeText(Host.this, "不符规定的输入！制热模式仅允许温度设置25~30℃", Toast.LENGTH_SHORT).show();
                            seekBar.setProgress(init);
                        }
                        break;
                    case Command.Cool:
                        if (temp >= Command.Temp_Cool_Low && temp <= Command.Temp_Cool_High) {
                            updateAllSlaves(temp);
                            for(int i = 0; i < 15; i ++){
                                if(slaves[i] != null) {
                                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                                    Date curDate = new Date(System.currentTimeMillis());//获取当前时间
                                    String str = formatter.format(curDate);
//insert(slaves[i].getRoomNum(), slaves[i].getOnoff(), slaves[i].getSpeed(), (int) slaves[i].getTarTemp(), (int)slaves[i].getCost(), str);
                                }
                            }
                            command = "msg-[{\"tarTemp\":" + temp + ",\"requestType\":" + Command.HostCommandTypeSet + "}]";
                            knockSend();
                        } else {
                            Toast.makeText(Host.this, "不符规定的输入！制冷模式仅允许温度设置18~25℃", Toast.LENGTH_SHORT).show();
                            seekBar.setProgress(init);
                        }
                        break;
                    default:
                        System.out.println("Error! Mode set error!");
                        break;
                }
            }
            else {
                Toast.makeText(Host.this, "主机未开启，设置无效！", Toast.LENGTH_SHORT).show();
                seekBar.setProgress(7);
            }
        }
    };

    private RadioGroup.OnCheckedChangeListener flush = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (onoff == Command.On) {
                if (checkedId == flush1.getId()) {
                    for (int i = 1; i < 15; i++) {
                        if (slaves[i] != null)
                            slaves[i].setRate(1);
                    }
                } else if (checkedId == flush3.getId()) {
                    for (int i = 1; i < 15; i++) {
                        if (slaves[i] != null)
                            slaves[i].setRate(5);
                    }
                } else {  //默认只有三秒
                    for (int i = 1; i < 15; i++) {
                        if (slaves[i] != null)
                            slaves[i].setRate(3);
                    }
                }
            }
            else{
                Toast.makeText(Host.this, "主机未开启，设置无效！", Toast.LENGTH_SHORT).show();
            }
        }
    };

    //改变工作模式
    private void changeMode(){
        if(onoff == Command.On) {
            gmode = (gmode + 1) % 2;
            updateAllSlaves(gmode);
            //同时把所有目标温度设置为25℃，防止出错
            updateAllSlaves(25.0);
            for(int i = 0; i < 15; i ++){
                if(slaves[i] != null) {
//SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
//Date curDate = new Date(System.currentTimeMillis());//获取当前时间
//String str = formatter.format(curDate);
                    time.setToNow();
                    pay.insert(slaves[i].getRoomNum(), slaves[i].getOnoff(), slaves[i].getSpeed(), (int) slaves[i].getTarTemp(), slaves[i].getCost(), time.format("%Y-%m-%d %H:%M:%S"));
//insert(slaves[i].getRoomNum(), slaves[i].getOnoff(), slaves[i].getSpeed(), (int) slaves[i].getTarTemp(), (int) slaves[i].getCost(), str);
                }
            }
            command = "msg-[{\"mode\":" + gmode + "}]";
            knockSend();
        }
        else
            Toast.makeText(Host.this, "主机未开启，设置无效！", Toast.LENGTH_SHORT).show();
    }

    //通知发送线程发送指令
    private void knockSend(){
        ThreadPoolUtil.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                sendMessage(command);
            }
        });
    }

    //初始化Socket连接，并开始接收消息
    public void init(){
        ThreadPoolUtil.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(SERVER_IP, SERVERPORT);
                    DataOutputStream out = new DataOutputStream(
                            socket.getOutputStream());
                    String t = "choosePrio-0";
                    out.write(t.getBytes("UTF-8"));
                    timer = new Timer();
                    timer.schedule(new FlushData(), 0, 1000);
                    receiveMessage();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //接收从机消息
    public void receiveMessage(){
        try{
            while(true){
                DataInputStream input = new DataInputStream(socket.getInputStream());
                byte[] buffer;
                buffer = new byte[input.available()];
                if(buffer.length != 0){
                    //读取缓冲区
                    input.read(buffer);
                    String msg = new String(buffer, "UTF-8");  //注意转码，不然中文会乱码。
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

    //发送给从机指令
    public void sendMessage(String t){
        try {
            //向服务器端发送数据
            DataOutputStream out = new DataOutputStream(
                    socket.getOutputStream());
            out.write(t.getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //定时刷新从机信息
    public class FlushData extends TimerTask{

        @Override
        public void run() {
            String s = "\n";

            for (int i = 0; i < 15; i++) {
                if (slaves[i] != null) {
                    changeState(slaves[i]);  //从机状态改变
                    if (slaves[i].times == 0) {  //只有刷新频率到的时候才会更新
                        String t = "msg-[{\"on\":" + slaves[i].getOnoff() + ",\"mode\":" + gmode + ",\"roomNum\":" + slaves[i].getRoomNum() + ",\"temp\":" + String.format("%.1f", slaves[i].getTemp()) +
                                ",\"tarTemp\":" + String.format("%.1f", slaves[i].getTarTemp()) + ",\"cost\":" + String.format("%.2f", slaves[i].getCost()) + ",\"speed\":" + slaves[i].getSpeed() +
                                ",\"host\":" + onoff + ",\"requestType\":" + Command.HostCommandTypeWatcher + "}]";
                        sendMessage(t);
                    }
                    if (slaves[i].seconds == 0)
                        slaves[i].addPay();

                    //更新主机的从机信息
                    s += "\n房间号：" + slaves[i].getRoomNum();
                    if (slaves[i].getOnoff() == Command.On)
                        s += "\n开关机：开";
                    else if (slaves[i].getOnoff() == Command.Off)
                        s += "\n开关机：关";
                    else if (slaves[i].getOnoff() == Command.Standby)
                        s += "\n开关机：待机";
                    else
                        s = "开关机：Error";
                    text += "\n房间号：" + slaves[i].getRoomNum();
                    if (gmode == Command.Cool)
                        s += "\n工作模式：制冷";
                    else if (gmode == Command.Hot)
                        s += "\n工作模式：制热";
                    else
                        s += "\n工作模式：Error";
                    s += "\n当前温度：" + String.format("%.1f", slaves[i].getTemp()) + "\n目标温度：" + String.format("%.1f", slaves[i].getTarTemp()) +
                            "\n当前风速：" + slaves[i].getSpeed() + "\n消费金额：" + slaves[i].getCost() + "\n\n";
                }
            }

            //通知主线程让主线程刷新View，更加安全
            Message msg = vHandler.obtainMessage();
            msg.obj = s;
            vHandler.sendMessage(msg);
        }
    }

    //统一设置温度
    private void updateAllSlaves(double temp){
        for(int i = 0; i < 15; i ++){
            if(slaves[i] != null){
                slaves[i].setTarTemp(temp);
                if((slaves[i].getOnoff() == Command.On || slaves[i].getOnoff() == Command.Standby) && (slaves[i].getTemp() != slaves[i].getTarTemp()))  //如果机器开着的话，立刻工作
                    slaves[i].setState(Command.On);
            }
        }
    }

    //统一设置当前模式
    private void updateAllSlaves(int mode){
        for(int i = 0; i < 15; i ++){
            if(slaves[i] != null){
                slaves[i].setMode(mode);
            }
        }
    }

    //用于计算各房间温度变化
    private void changeState(SlaveCluster slave){
        slave.times = (slave.times + 1) % slave.getRate();  //不论是什么状态，刷新是一直在进行的

        if(slave.getOnoff() == Command.On)
        {
            if(onoff == Command.On) {
                slave.seconds = (slave.seconds + 1) % 30;  //只有在开机的状态下，才计费
                //凡是到达目标都返回真，主机自动给从机关机
                switch (slave.getSpeed()) {
                    case Command.Speed_Low:
                        if (slave.getMode() == Command.Cool) {  //制冷模式
                            if (slave.getTemp() > slave.getTarTemp()) {
                                if ((slave.getTemp() - 0.2) > slave.getTarTemp())
                                    slave.setTemp(slave.getTemp() - 0.2);
                                else if ((slave.getTemp() - 0.2) == slave.getTarTemp()) {
                                    slave.setTemp(slave.getTemp() - 0.2);
                                    slave.setState(Command.Standby);  //待机
                                } else {
                                    slave.setTemp(slave.getTarTemp());
                                    slave.setState(Command.Standby);  //待机
                                }
                            } else if (slave.getTemp() == slave.getTarTemp()) {
                                slave.setState(Command.Standby);  //待机
                            } else {  //房间温度已达到目标温度
                                slave.setState(Command.Standby);  //待机
                            }
                        } else {  //制热模式
                            if (slave.getTemp() < slave.getTarTemp()) {
                                if ((slave.getTemp() + 0.2) < slave.getTarTemp())
                                    slave.setTemp(slave.getTemp() + 0.2);
                                else if ((slave.getTemp() + 0.2) == slave.getTarTemp()) {
                                    slave.setTemp(slave.getTemp() + 0.2);
                                    slave.setState(Command.Standby);  //待机
                                } else {
                                    slave.setTemp(slave.getTarTemp());
                                    slave.setState(Command.Standby);  //待机
                                }
                            } else if (slave.getTemp() == slave.getTarTemp()) {
                                slave.setState(Command.Standby);  //待机
                            } else {  //房间温度已达到目标温度
                                slave.setState(Command.Standby);  //待机
                            }
                        }
                        break;
                    case Command.Speed_Medium:
                        if (slave.getMode() == Command.Cool) {  //制冷模式
                            if (slave.getTemp() > slave.getTarTemp()) {
                                if ((slave.getTemp() - 0.3) > slave.getTarTemp())
                                    slave.setTemp(slave.getTemp() - 0.3);
                                else if ((slave.getTemp() - 0.3) == slave.getTarTemp()) {
                                    slave.setTemp(slave.getTemp() - 0.3);
                                    slave.setState(Command.Standby);  //待机
                                } else {
                                    slave.setTemp(slave.getTarTemp());
                                    slave.setState(Command.Standby);  //待机
                                }
                            } else if (slave.getTemp() == slave.getTarTemp()) {
                                slave.setState(Command.Standby);  //待机
                            } else {  //房间温度已达到目标温度
                                slave.setState(Command.Standby);  //待机
                            }
                        } else {  //制热模式
                            if (slave.getTemp() < slave.getTarTemp()) {
                                if ((slave.getTemp() + 0.3) < slave.getTarTemp())
                                    slave.setTemp(slave.getTemp() + 0.3);
                                else if ((slave.getTemp() + 0.3) == slave.getTarTemp()) {
                                    slave.setTemp(slave.getTemp() + 0.3);
                                    slave.setState(Command.Standby);  //待机
                                } else {
                                    slave.setTemp(slave.getTarTemp());
                                    slave.setState(Command.Standby);  //待机
                                }
                            } else if (slave.getTemp() == slave.getTarTemp()) {
                                slave.setState(Command.Standby);  //待机
                            } else {  //房间温度已达到目标温度
                                slave.setState(Command.Standby);  //待机
                            }
                        }
                        break;
                    case Command.Speed_High:
                        if (slave.getMode() == Command.Cool) {  //制冷模式
                            if (slave.getTemp() > slave.getTarTemp()) {
                                if ((slave.getTemp() - 0.4) > slave.getTarTemp())
                                    slave.setTemp(slave.getTemp() - 0.4);
                                else if ((slave.getTemp() - 0.4) == slave.getTarTemp()) {
                                    slave.setTemp(slave.getTemp() - 0.4);
                                    slave.setState(Command.Standby);  //待机
                                } else {
                                    slave.setTemp(slave.getTarTemp());
                                    slave.setState(Command.Standby);  //待机
                                }
                            } else if (slave.getTemp() == slave.getTarTemp()) {
                                slave.setState(Command.Standby);  //待机
                            } else {  //温度回滚
                                slave.setState(Command.Standby);  //待机
                            }
                        } else {  //制热模式
                            if (slave.getTemp() < slave.getTarTemp()) {
                                if ((slave.getTemp() + 0.4) < slave.getTarTemp())
                                    slave.setTemp(slave.getTemp() + 0.4);
                                else if ((slave.getTemp() + 0.4) == slave.getTarTemp()) {
                                    slave.setTemp(slave.getTemp() + 0.4);
                                    slave.setState(Command.Standby);  //待机
                                } else {
                                    slave.setTemp(slave.getTarTemp());
                                    slave.setState(Command.Standby);  //待机
                                }
                            } else if (slave.getTemp() == slave.getTarTemp()) {
                                slave.setState(Command.Standby);  //待机
                            } else {  //温度回滚
                                if ((slave.getTemp() - 0.1) > slave.getTarTemp())
                                    slave.setTemp(slave.getTemp() - 0.1);
                                else if ((slave.getTemp() - 0.1) == slave.getTarTemp()) {
                                    slave.setTemp(slave.getTemp() - 0.1);
                                    slave.setState(Command.Standby);  //待机
                                } else {
                                    slave.setTemp(slave.getTarTemp());
                                    slave.setState(Command.Standby);  //待机
                                }
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        else if (slave.getOnoff() == Command.Standby) {  //待机状态，温度回滚,1s 0.1℃
            if(slave.getTemp() == 25){
                //nothing to do
            }
            else {
                if (slave.getMode() == Command.Hot) {
                    if ((slave.getTemp() - 0.1 - slave.getTarTemp()) <= -1) {
                        slave.setTemp(slave.getTemp() - 0.1);

                        slave.setState(Command.On);
                    } else
                        slave.setTemp(slave.getTemp() - 0.1);
                } else if (slave.getMode() == Command.Cool) {
                    if ((slave.getTemp() - 0.1 - slave.getTarTemp()) >= 1) {
                        slave.setTemp(slave.getTemp() + 0.1);
                        slave.setState(Command.On);
                    } else
                        slave.setTemp(slave.getTemp() + 0.1);
                }
            }
        }
        else //关机时，温度回到默认25℃
        {
            if(slave.getTemp() > 25){
                if(slave.getTemp() - 0.1 > 25)
                    slave.setTemp(slave.getTemp() - 0.1);
                else
                    slave.setTemp(25);
            }
            else if(slave.getTemp() < 25){
                if(slave.getTemp() + 0.1 < 25)
                    slave.setTemp(slave.getTemp() + 0.1);
                else
                    slave.setTemp(25);
            }
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

/*
    //用于向数据库插入数据
    private void insert(int roomNum, int onoff, int speed, int tarTemp, double cost){

        ContentValues values = new ContentValues();
        values.put("roomNum", roomNum);
        values.put("onoff", onoff);
        values.put("speed", speed);
        values.put("tarTemp", tarTemp);
        values.put("cost", cost);
        values.put("mytime", str);
        DatabaseHelper dbHelper = new DatabaseHelper(this, "AirConditionSystem.db");
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.insert("XLS", null, values);
    }
*/
}
