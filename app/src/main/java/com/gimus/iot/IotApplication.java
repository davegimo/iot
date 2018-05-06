/*******************************************************************************
 * Copyright (c) 2014-2016 IBM Corp.
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
 *    Aldo Eisma - location update and light control fixed, updated for Android M
 *******************************************************************************/
package com.gimus.iot;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.os.Build;
import android.util.Log;


import com.gimus.iot.iot.IoTClient;
import com.gimus.iot.iot.IoTDevice;
import com.gimus.iot.utils.Constants;
import com.gimus.iot.utils.MessageFactory;
import com.gimus.iot.utils.MyIoTActionListener;
import com.gimus.iot.utils.MyIoTCallbacks;
import com.google.android.gms.security.ProviderInstaller;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.IOException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * Main class for the IoT Starter application. Stores values for
 * important device and application information.
 */
public class IotApplication extends Application {
    private final static String TAG = IotApplication.class.getName();


    // Current activity of the application, updated whenever activity is changed
    private String currentRunningActivity;




    private Constants.ConnectionType connectionType;
    private boolean useSSL = true;

    private SharedPreferences settings;

    private MyIoTCallbacks myIoTCallbacks;

    // Application state variables
    private boolean connected = false;
    private int publishCount = 0;
    private int receiveCount = 0;
    private int unreadCount = 0;

    private int color = Color.argb(1, 58, 74, 83);
    private boolean isCameraOn = false;
    private float[] accelData;
    private boolean accelEnabled = true;

 //   private DeviceSensor deviceSensor;
    private Location currentLocation;
    private Camera camera;
    private String cameraId;

    // Message log for log activity
    private final ArrayList<String> messageLog = new ArrayList<String>();

    private final List<IoTDevice> profiles = new ArrayList<IoTDevice>();
    private final ArrayList<String> profileNames = new ArrayList<String>();

    /**
     * Called when the application is created. Initializes the application.
     */
    @Override
    public void onCreate() {
        Log.d(TAG, ".onCreate() entered");
        super.onCreate();
        Global g=new Global(this);

        settings = getSharedPreferences(Constants.SETTINGS, 0);



        myIoTCallbacks = MyIoTCallbacks.getInstance(this);
    }

    
    /**
     * Turn flashlight on or off when a light command message is received.
     * @param newState Toggle light when null, otherwise switch on or off.
     */
    @TargetApi(value = 23)
    public void handleLightMessage(Boolean newState) {
        Log.d(TAG, ".handleLightMessage() entered");
        if (this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            Log.d(TAG, "FEATURE_CAMERA_FLASH true");
            boolean setCameraOn = newState == null ? !isCameraOn : newState.booleanValue();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                /*
                 *  A new API to use the camera flash light as torch is available.
                 */
                CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                if (setCameraOn && !isCameraOn) {
                    try {
                        String[] cameraIds = manager.getCameraIdList();
                        for (int i = 0; i < cameraIds.length && !isCameraOn; i++) {
                            cameraId = cameraIds[i];
                            CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(cameraId);
                            if (cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)) {
                                manager.setTorchMode(cameraId, true);
                                isCameraOn = true;
                            }
                        }
                    } catch (CameraAccessException e) {
                        Log.w(TAG, e);
                    }
                } else if (!setCameraOn && isCameraOn) {
                    try {
                        isCameraOn = false;
                        manager.setTorchMode(cameraId, false);
                    } catch (CameraAccessException e) {
                        Log.w(TAG, e);
                    }
                }
            } else {
                /*
                 *  Use deprecated Camera API.
                 */
                if (setCameraOn && !isCameraOn) {
                    Log.d(TAG, "FEATURE_CAMERA_FLASH true");
                    camera = Camera.open();
                    Camera.Parameters p = camera.getParameters();
                    p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    camera.setParameters(p);
                    /*
                     *  On some devices a surface is required to be able to control the flash.
                     */
                    try {
                        camera.setPreviewTexture(new SurfaceTexture(0));
                    } catch (IOException e) {
                        Log.w(TAG, e);
                    }
                    camera.startPreview();
                    isCameraOn = true;
                } else if (!setCameraOn && isCameraOn) {
                    camera.stopPreview();
                    camera.release();
                    isCameraOn = false;
                }
            }
        } else {
            Log.d(TAG, "FEATURE_CAMERA_FLASH false");
        }
    }

    /**
     * Overwrite an existing profile in the stored application settings.
     * @param newProfile The profile to save.
     */
    @TargetApi(value = 11)
    public void overwriteProfile(IoTDevice newProfile) {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= Build.VERSION_CODES.HONEYCOMB) {
            // Put the new profile into the store settings and remove the old stored properties.
            Set<String> profileSet = newProfile.convertToSet();

            SharedPreferences.Editor editor = settings.edit();
            editor.remove(newProfile.getDeviceName());
            editor.putStringSet(newProfile.getDeviceName(), profileSet);
            //editor.apply();
            editor.commit();
        }

        for (IoTDevice existingProfile : profiles) {
            if (existingProfile.getDeviceName().equals(newProfile.getDeviceName())) {
                profiles.remove(existingProfile);
                break;
            }
        }
        profiles.add(newProfile);
    }
    /**
     * Save the profile to the application stored settings.
     * @param profile The profile to save.
     */
    @TargetApi(value = 11)
    public void saveProfile(IoTDevice profile) {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= Build.VERSION_CODES.HONEYCOMB) {
            // Put the new profile into the store settings and remove the old stored properties.
            Set<String> profileSet = profile.convertToSet();

            SharedPreferences.Editor editor = settings.edit();
            editor.putStringSet(profile.getDeviceName(), profileSet);
            //editor.apply();
            editor.commit();
        }
        this.profiles.add(profile);
        this.profileNames.add(profile.getDeviceName());
    }

    /**
     * Remove all saved profile information.
     */
    public void clearProfiles() {
        this.profiles.clear();
        this.profileNames.clear();
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= Build.VERSION_CODES.HONEYCOMB) {
            SharedPreferences.Editor editor = settings.edit();
            editor.clear();
            //editor.apply();
            editor.commit();
        }
    }

    // Getters and Setters
    public String getCurrentRunningActivity() { return currentRunningActivity; }

    public void setCurrentRunningActivity(String currentRunningActivity) { this.currentRunningActivity = currentRunningActivity; }

    public String getOrganization() {
        return Global.G.org;
    }

    public String getDeviceId() {
        return Global.G.deviceId;
    }


    public String getAuthToken() {
        return Global.G.authToken;
    }

    public void setConnectionType(Constants.ConnectionType type) {
        this.connectionType = type;
    }

    public Constants.ConnectionType getConnectionType() {
        return this.connectionType;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public int getPublishCount() {
        return publishCount;
    }

    public void setPublishCount(int publishCount) {
        this.publishCount = publishCount;
    }

    public int getReceiveCount() {
        return receiveCount;
    }

    public void setReceiveCount(int receiveCount) {
        this.receiveCount = receiveCount;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public float[] getAccelData() { return accelData; }

    public void setAccelData(float[] accelData) {
        this.accelData = accelData.clone();
    }

    public ArrayList<String> getMessageLog() {
        return messageLog;
    }

    public boolean isAccelEnabled() {
        return accelEnabled;
    }

    private void setAccelEnabled(boolean accelEnabled) {
        this.accelEnabled = accelEnabled;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(Location currentLocation) {
        this.currentLocation = currentLocation;
    }

    public void setProfile(IoTDevice profile) {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= Build.VERSION_CODES.HONEYCOMB) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("iot:selectedprofile", profile.getDeviceName());
            //editor.apply();
            editor.commit();
        }
    }

    public List<IoTDevice> getProfiles() {
        return profiles;
    }

    public ArrayList<String> getProfileNames() {
        return profileNames;
    }

    public MyIoTCallbacks getMyIoTCallbacks() {
        return myIoTCallbacks;
    }

    public String getDeviceType() {
        return "Android";
    }

    public boolean isUseSSL() {
        return true;
    }



    private boolean checkCanConnect() {
        if (getOrganization().equals(Constants.QUICKSTART)) {
            setConnectionType(Constants.ConnectionType.QUICKSTART);
            if (getDeviceId() == null || getDeviceId().equals("")) {
                return false;
            }
        } else if (getOrganization().equals(Constants.M2M)) {
            setConnectionType(Constants.ConnectionType.M2M);
            if (getDeviceId() == null || getDeviceId().equals("")) {
                return false;
            }
        } else {
            setConnectionType(Constants.ConnectionType.IOTF);
            if (getOrganization() == null || getOrganization().equals("") ||
                    getDeviceId() == null || getDeviceId().equals("") ||
                    getAuthToken() == null || getAuthToken().equals("")) {
                return false;
            }
        }
        return true;
    }

    public boolean Connect(){
        IoTClient iotClient = IoTClient.getInstance(this, Global.G.org, Global.G.deviceId, "Android", Global.G.authToken);
        if (!isConnected()) {
            if (checkCanConnect()) {
                // create ActionListener to handle message published results
                try {
                    SocketFactory factory = null;
                    if (isUseSSL()) {
                        try {
                            ProviderInstaller.installIfNeeded(this);

                            SSLContext sslContext;
                            KeyStore ks = KeyStore.getInstance("bks");
                            ks.load(this.getResources().openRawResource(R.raw.iot), "password".toCharArray());
                            TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
                            tmf.init(ks);
                            TrustManager[] tm = tmf.getTrustManagers();
                            sslContext = SSLContext.getInstance("TLSv1.2");
                            sslContext.init(null, tm, null);
                            factory = sslContext.getSocketFactory();
                        } catch (Exception e) {
                            // TODO: handle exception
                            e.printStackTrace();
                        }
                    }

                    MyIoTActionListener listener = new MyIoTActionListener(this, Constants.ActionStateStatus.CONNECTING);
                    //start connection - if this method returns, connection has not yet happened
                    iotClient.connectDevice(getMyIoTCallbacks(), listener, factory);
                    Global.G.mainActivity.UpdateConnectionStatus(true);

                    return true;

                } catch (MqttException e) {
                    if (e.getReasonCode() == (Constants.ERROR_BROKER_UNAVAILABLE)) {
                        // error while connecting to the broker - send an intent to inform the user
                        Intent actionIntent = new Intent(Constants.ACTION_INTENT_CONNECTIVITY_MESSAGE_RECEIVED);
                        actionIntent.putExtra(Constants.CONNECTIVITY_MESSAGE, Constants.ERROR_BROKER_UNAVAILABLE);
                        sendBroadcast(actionIntent);
                        Global.G.mainActivity.UpdateConnectionStatus(false);
                        return false;
                    }
                }
                return false;
            }
            return false;
        }
        return false;
    }

    public boolean Disconnect() {
        try {
            IoTClient iotClient = IoTClient.getInstance(this, Global.G.org, Global.G.deviceId, "Android", Global.G.authToken);
            MyIoTActionListener listener = new MyIoTActionListener(this, Constants.ActionStateStatus.DISCONNECTING);
            iotClient.disconnectDevice(listener);
            Global.G.mainActivity.UpdateConnectionStatus(false);
            return true;
        } catch (MqttException e) {
            return false;
        }

    }
}
