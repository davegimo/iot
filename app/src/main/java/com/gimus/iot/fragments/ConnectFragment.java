/*******************************************************************************
 * Copyright (c) 2014-2015 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *    Mike Robertson - initial contribution
 *******************************************************************************/
package com.gimus.iot.fragments;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.gimus.iot.Global;
import com.gimus.iot.IotApplication;
import com.gimus.iot.MainActivity;
import com.gimus.iot.R;
import com.gimus.iot.iot.IoTClient;
import com.gimus.iot.utils.Constants;

import java.sql.Timestamp;
import java.util.Date;

public class ConnectFragment extends Fragment {
    private final static String TAG = ConnectFragment.class.getName();

    Context context;
    IotApplication app;
    BroadcastReceiver broadcastReceiver;

    /**************************************************************************
     * Fragment functions for establishing the fragment
     **************************************************************************/

    public static ConnectFragment newInstance() {
        return new ConnectFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_connect, container, false);
    }

    @Override
    public void onResume() {
        Log.d(TAG, ".onResume() entered");

        super.onResume();
        app = (IotApplication) getActivity().getApplication();
        app.setCurrentRunningActivity(TAG);

        if (broadcastReceiver == null) {
            Log.d(TAG, ".onResume() - Registering loginBroadcastReceiver");
            broadcastReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, ".onReceive() - Received intent for loginBroadcastReceiver");
                    processIntent(intent);
                    Log.d(TAG, ".onReceive() - exit");
                }
            };
        }

        getActivity().getApplicationContext().registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.APP_ID + Constants.INTENT_LOGIN));

        // initialise
        initializeLoginActivity();
    }

    /**
     * Called when the fragment is destroyed.
     */
    @Override
    public void onDestroy() {
        Log.d(TAG, ".onDestroy() entered");

        try {
            getActivity().getApplicationContext().unregisterReceiver(broadcastReceiver);
        } catch (IllegalArgumentException iae) {
            // Do nothing
        }
        super.onDestroy();
    }

    /**
     * Initializing onscreen elements and shared properties
     */
    private void initializeLoginActivity() {
        Log.d(TAG, ".initializeLoginFragment() entered");

        context = getActivity().getApplicationContext();

        updateViewStrings();

        // setup button listeners
        initializeButtons();
    }

    /**
     * Update strings in the fragment based on IoTStarterApplication values.
     */

    void updateViewStrings() {
        Log.d(TAG, ".updateViewStrings() entered");


        // Set 'Connected to IoT' to Yes if MQTT client is connected. Leave as No otherwise.
        if (app.isConnected()) {
            updateConnectedValues();
            //processConnectIntent();
        }

        // TODO: Update badge value?
        //int unreadCount = app.getUnreadCount();
        //((MainActivity) getActivity()).updateBadge(getActivity().getActionBar().getTabAt(2), unreadCount);
    }

    /**
     * Setup listeners for buttons.
     */
    private void initializeButtons() {
        Log.d(TAG, ".initializeButtons() entered");
        Button button;


        button = (Button) getActivity().findViewById(R.id.activateButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleActivate    ();
            }
        });
    }

     /**
     * If button is currently 'Activate', then connect the MQTT client.
     * If button is currently 'Deactivate', then disconnect the MQTT client.
     */
    private void handleActivate() {
        Log.d(TAG, ".handleActivate() entered");
        String buttonTitle = ((Button) getActivity().findViewById(R.id.activateButton)).getText().toString();
        Button activateButton = (Button) getActivity().findViewById(R.id.activateButton);

        IoTClient iotClient = IoTClient.getInstance(context, app.getOrganization(), app.getDeviceId(), app.getDeviceType(), app.getAuthToken());
        activateButton.setEnabled(false);
        if (buttonTitle.equals(getResources().getString(R.string.activate_button)) && !app.isConnected()) {
            app.Connect();
        } else if (buttonTitle.equals(getResources().getString(R.string.deactivate_button)) && app.isConnected()) {
            app.Disconnect();
        }
        Log.d(TAG, ".handleActivate() exit");
    }


    /**************************************************************************
     * Functions to process intent broadcasts from other classes
     **************************************************************************/

    /**
     * Process the incoming intent broadcast.
     *
     * @param intent The intent which was received by the fragment.
     */
    private void processIntent(Intent intent) {
        Log.d(TAG, ".processIntent() entered");

        // No matter the intent, update log button based on app.unreadCount.
        updateViewStrings();

        String data = intent.getStringExtra(Constants.INTENT_DATA);
        assert data != null;
        if (data.equals(Constants.INTENT_DATA_CONNECT)) {
            processConnectIntent();
            openIoT();
        } else if (data.equals(Constants.INTENT_DATA_DISCONNECT)) {
            processDisconnectIntent();
        } else if (data.equals(Constants.ALERT_EVENT)) {
            String message = intent.getStringExtra(Constants.INTENT_DATA_MESSAGE);
            //also log message
            logToPage(message);
            //popup alert
            new AlertDialog.Builder(getActivity())
                    .setTitle(getResources().getString(R.string.alert_dialog_title))
                    .setMessage(message)
                    .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    }).show();
        }
        Log.d(TAG, ".processIntent() exit");
    }

    void openIoT() {
        Log.d(TAG, ".openIoT() entered");
        ((MainActivity) getActivity()).selectFragment(MainActivity.FRG_LOGIN);
    }
    /**
     * Intent data contained INTENT_DATA_CONNECT.
     * Update Connected to Yes.
     */
    private void processConnectIntent() {
        Log.d(TAG, ".processConnectIntent() entered");
        updateConnectedValues();

        logToPage("Connected to server");

        if (app.isAccelEnabled()) {
 //           LocationUtils locUtils = LocationUtils.getInstance(context);
 //           locUtils.connect();
 //           app.setDeviceSensor(DeviceSensor.getInstance(context));
 //           app.getDeviceSensor().enableSensor();
        }
    }

    private void updateConnectedValues() {
        Button activateButton = (Button) getActivity().findViewById(R.id.activateButton);
        activateButton.setEnabled(true);
        String connectedString = this.getString(R.string.is_connected);
        connectedString = connectedString.replace("No", "Yes");
        ((TextView) getActivity().findViewById(R.id.isConnected)).setText(connectedString);
        activateButton.setText(getResources().getString(R.string.deactivate_button));
        Global.G.mainActivity.UpdateConnectionStatus(app.isConnected());
    }

    private void processDisconnectIntent() {
        Log.d(TAG, ".processDisconnectIntent() entered");
        Button activateButton = (Button) getActivity().findViewById(R.id.activateButton);
        activateButton.setEnabled(true);
        ((TextView) getActivity().findViewById(R.id.isConnected)).setText(this.getString(R.string.is_connected));
        activateButton.setText(getResources().getString(R.string.activate_button));

        logToPage("Disonnected from server");

        Global.G.mainActivity.UpdateConnectionStatus(app.isConnected());

        Log.d(TAG, ".processDisconnectIntent() exit");
    }

    /**
     * Log message to the log page in the app. Add timestamp.
     * @param message
     */
    private void logToPage(String message){
        // Log message with the following format:
        // [yyyy-mm-dd hh:mm:ss.S] message
        Date date = new Date();
        String logMessage = "["+new Timestamp(date.getTime())+"]:"+message;
        app.getMessageLog().add(logMessage);
        Intent actionIntent = new Intent(Constants.APP_ID + Constants.INTENT_LOG);
        actionIntent.putExtra(Constants.INTENT_DATA, Constants.TEXT_EVENT);
        context.sendBroadcast(actionIntent);
    }
}