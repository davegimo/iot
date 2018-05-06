package com.gimus.iot.utils;

import com.google.gson.Gson;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MessageFactory {

    protected static class IotMsg {
        public Object d;
        public IotMsg(Object _d){this.d=_d;}
    }

    protected static class AccelerationMsg {
        public float acceleration_x;
        public float acceleration_y;
        public float acceleration_z;
        public float roll;
        public float pitch;
        public float yaw;
        public double longitude;
        public double latutude;
        public float heading;
        public float speed;
        public long trip_id;
        public String timestamp;
    }

    protected static class TextMsg {
        String text;
    }

    /**
     * Construct a JSON formatted string accel event message
     * @param ax accelerometer x  data
     * @param ay accelerometer y data
     * @param az accelerometer z data
     * @return String containing JSON formatted message
     */
    public static String getAccelerationMsg(float ax, float ay, float az) {
        // Android does not support the X pattern, so use Z and insert ':' if required.
        DateFormat isoDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
//        isoDateTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String isoTimestamp = isoDateTimeFormat.format(new Date());
        if (!isoTimestamp.endsWith("Z")) {
            int pos = isoTimestamp.length() - 2;
            isoTimestamp = isoTimestamp.substring(0, pos) + ':' + isoTimestamp.substring(pos);
        }

        AccelerationMsg msg = new AccelerationMsg();
        msg.acceleration_x=ax;
        msg.acceleration_y=ay;
        msg.acceleration_z=az;
        msg.timestamp=isoTimestamp;

        return new Gson().toJson(new IotMsg(msg));
    }

   /**
     * Construct a JSON formatted string text event message
     * @param text String of text message to send
     * @return String containing JSON formatted message
     */
    public static String getTextMessage(String text) {
        TextMsg  msg = new TextMsg();
        msg.text=text;
        return new Gson().toJson(new IotMsg(msg));
    }

}
