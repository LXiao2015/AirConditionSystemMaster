package com.bupt.air.airconditionsystem;

/**
 * Created by OnlySaturday on 2015/5/27.
 */
public class SlaveCluster {
    private int onoff;  //开关机
    private int mode;  //模式
    private int roomNum;  //房间号
    private double temp;  //当前温度
    private double tarTemp;  //目标温度
    private int speed;  //风速
    private double cost;  //消费金额
    private int rate;  //从机刷新频率，每rate秒一次
    public int times;  //用于实现刷新频率 times = (times+1)%rate
    public int seconds;  //用于每分钟计费计时，因为主机发送频率是1秒1次，所以seconds = 60 时金额就变化，为了试验效果明显，我们把一分钟看做只有30秒，即seconds = 30即消费增加

    SlaveCluster(){
        onoff = Command.Off;
        mode = Command.Cool;
        roomNum = -1;  //-1表示无效
        temp = 25;
        tarTemp = 25;
        speed = Command.Speed_Low;
        cost = 0;
        rate = 3;  //默认三秒刷新一次
        times = 0;
        seconds = 1;
    }

    public void setTemp(double temp) {
        this.temp = temp;
    }

    public void setRoomNum(int roomNum) {
        this.roomNum = roomNum;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public void setOnoff(int onoff) {
        this.onoff = onoff;
    }

    public void setTarTemp(double tarTemp) {
        this.tarTemp = tarTemp;
    }

    public double getCost() {
        return cost;
    }

    public double getTarTemp() {
        return tarTemp;
    }

    public double getTemp() {
        return temp;
    }

    public int getMode() {
        return mode;
    }

    public int getOnoff() {
        return onoff;
    }

    public int getRoomNum() {
        return roomNum;
    }

    public int getSpeed() {
        return speed;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    public int getRate(){
        return rate;
    }



    public void setState(int state){
        onoff = state;
    }

    //消费金额统计
    public void rePay(){
        cost = 0;
    }

    public void addPay(){
        if(onoff == Command.On)
        {
            if (speed == Command.Speed_Low){
                cost += 4;
            }
            else if (speed == Command.Speed_Medium){
                cost += 5;
            }
            else{
                cost += 6.5;
            }

        }
    }

    @Override
    public String toString() {
        String text = "";

        text = "房间号：" + roomNum + "\n当前温度：" + temp + "\n目标温度：" + tarTemp + "\n当前风速：" + speed + "\n消费金额：" + cost + "\n";
        return text;
    }
}
