package com.gimus.iot.utils;

import android.os.Handler;

public class Ticker {
    protected Handler h = new Handler();
    protected boolean enabled;
    protected long interval;
    protected OnTickReceiver tickReceiver;
    protected int callerId;

    public Ticker( OnTickReceiver receiver, int id) {
        tickReceiver=receiver;
        callerId=id;
    }

    protected Runnable r = new Runnable() {
        @Override
        public void run() {

            onTick();

            if (enabled) {
                h.postDelayed(this, interval);
            }
        }
    };

    public void start(int _interval) {
        interval=_interval;
        enabled = true;
        h.postDelayed(r, interval);
    }

    public void stop() {
        enabled = false;
    }

    protected void onTick(){
        tickReceiver.onTick(callerId);
    }

    public interface OnTickReceiver {
        public void onTick( int Id );
    }

}
