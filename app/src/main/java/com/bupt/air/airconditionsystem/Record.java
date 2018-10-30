package com.bupt.air.airconditionsystem;

/**
 * Created by OnlySaturday on 2015/6/7.
 */
public class Record {
    int []roomNum = new int[1000];
    int []onoff = new int[1000];
    int []speed = new int[1000];
    int []tarTemp = new int[1000];
    double []cost = new double[1000];
    String []mytime = new String[1000];
    int sum = 0;

    public void insert(int troomNum, int tonoff, int tspeed, int ttarTemp, double tcost, String tmytime){
        roomNum[sum] = troomNum;
        onoff[sum] = tonoff;
        speed[sum] = tspeed;
        tarTemp[sum] = ttarTemp;
        cost[sum] = tcost;
        mytime[sum] = tmytime;
        sum ++;
    }

    public String toString(){
        String temp = "";
        for(int i = 0; i < sum; i++){
            temp += roomNum[i] + " " + onoff[i] + " " + speed[i] + " " + tarTemp[i] + " " + cost[i] + " " + mytime[i] +"\n";
        }

        return temp;
    }
}
