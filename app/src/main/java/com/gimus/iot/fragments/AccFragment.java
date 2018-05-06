package com.gimus.iot.fragments;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gimus.iot.Global;
import com.gimus.iot.R;
import com.gimus.iot.iot.IoTClient;
import com.gimus.iot.utils.Constants;
import com.gimus.iot.utils.MessageFactory;
import com.gimus.iot.utils.MyIoTActionListener;

import org.eclipse.paho.client.mqttv3.MqttException;

import static android.content.Context.SENSOR_SERVICE;

public class AccFragment extends Fragment  implements SensorEventListener {
    private SensorManager sm;
    private Sensor acc;

    protected TextView tv;
    protected TextView tv_x;
    protected TextView tv_y;
    protected TextView tv_z;

    public boolean sensorActive=false;

    public AccFragment() {
    }

    public static AccFragment newInstance() {
        AccFragment fragment = new AccFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_acc , container, false);

        sm= (SensorManager)  getActivity().getSystemService(SENSOR_SERVICE);
        acc = sm.getDefaultSensor( Sensor.TYPE_ACCELEROMETER);

        tv = (TextView) rootView.findViewById(R.id.contatore);
        tv_x = (TextView) rootView.findViewById(R.id.acc_x);
        tv_y = (TextView) rootView.findViewById(R.id.acc_y);
        tv_z = (TextView) rootView.findViewById(R.id.acc_z);


        sm.registerListener( this, acc,SensorManager.SENSOR_DELAY_NORMAL);
        sensorActive=true;
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        sm.unregisterListener( this);
        sensorActive=false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER) {

            Global.G.ax = event.values[0];
            Global.G.ay = event.values[1];
            Global.G.az = event.values[2];

            tv_x.setText("x = " + String.valueOf(Global.G.ax) );
            tv_y.setText("y = " + String.valueOf(Global.G.ay) );
            tv_z.setText("z = " + String.valueOf(Global.G.az) );

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onTick() {
        if (sensorActive){
            String messageData = MessageFactory.getAccelerationMsg(Global.G.ax, Global.G.ay, Global.G.az );

            try {
                // create ActionListener to handle message published results
                MyIoTActionListener listener = new MyIoTActionListener(getActivity(), Constants.ActionStateStatus.PUBLISH);

                IoTClient iotClient = IoTClient.getInstance(getActivity(), Global.G.org, Global.G.deviceId, "Android", Global.G.authToken);
                iotClient.publishEvent(Constants.ACCEL_EVENT, "json", messageData, 0, false, listener);

            } catch (MqttException e) {
                Log.d("?", ".run() received exception on publishEvent()");
            }
        }
    }



}
