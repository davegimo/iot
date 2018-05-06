package com.gimus.iot;

import com.gimus.iot.utils.MyIoTCallbacks;

public class Global {

    public static Global G;
    public MainActivity mainActivity;
    public IotApplication context;
    public float ax;
    public float ay;
    public float az;

    public String org="paakh2";
    public String deviceId="123456";
    public String authToken="secret123";
    public MyIoTCallbacks myIoTCallbacks;

    public Global(IotApplication iotApp) {
        this.G=this;
        context=iotApp;
    }

}
