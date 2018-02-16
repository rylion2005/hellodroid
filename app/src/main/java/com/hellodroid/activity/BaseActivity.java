package com.hellodroid.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.hellodroid.lan.Scanner;
import com.hellodroid.network.MyNetworkReceiver;
import com.hellodroid.nio.SocketChanner;
import com.hellodroid.service.MyDaemonService;

import java.util.ArrayList;

/*
** ********************************************************************************
**
** BaseActivity
**   This is a top level activity without layout.
**   It processes some global somethings such as request permissions, copy asserts,
**     check security, check libraries, etc.
**
** USAGE:
**   ......
**
** ********************************************************************************
*/
public class BaseActivity extends AppCompatActivity {
    private static final String TAG = "BaseActivity";

    public static final int PERMISSION_REQUEST_CODE = 0xA00F;

    // Network broadcast receiver
    //protected MyNetworkReceiver myNetworkReceiver;
    //protected MyNetworkReceiver.CallBack myNetworkReceiverCallback;

    // Lan Scanner
    protected Scanner mLanScanner;
    protected Scanner.Callback mLanScannerCallback;

    // SocketChanner
    protected SocketChanner.Callback mySocketCallback;

    // MyDaemonService
    protected final ServiceConnection myServiceConnection = new MyServiceConnection();
    protected MyDaemonService myDaemonService;


/* ********************************************************************************************** */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(myServiceConnection);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            Log.e(TAG, "Not all permissions are granted");
                        }
                    }
                } else {
                    Log.e(TAG, "unknown permission error");
                }
                break;
            default:
        }
    }


/* ********************************************************************************************** */

    private void init(){
        initPermissions();

        bindService(
                new Intent(this, MyDaemonService.class),
                myServiceConnection,
                BIND_AUTO_CREATE);

        //myNetworkReceiver = MyNetworkReceiver.getInstance(this);
        //myNetworkReceiver.register(myNetworkReceiverCallback);

        mLanScanner = Scanner.newInstance();
    }

    /**
     *  >android 6.0: permission granted must be allowed
     */
    private void initPermissions() {
        final String[] PERMISSIONS = {
                Manifest.permission.INTERNET,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_SETTINGS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE
        };

        Log.i(TAG, "Init permissions");

        ArrayList<String> permissionList = new ArrayList<String>();
        for (String perm : PERMISSIONS) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                permissionList.add(perm);
            }
        }

        if ( !permissionList.isEmpty() ) {
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }else{
            Log.i(TAG, "all permissions are granted!");
        }
    }


/* ********************************************************************************************** */
/*
    public class NetworkStatusCallback implements MyNetworkReceiver.CallBack {
        @Override
        public void onWifiConnectivity(boolean connected) {

        }

        @Override
        public void onMobileConnectivity(boolean connected) {

        }
    }
*/
    public class MyServiceConnection implements ServiceConnection{
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v(TAG, "onServiceConnected: " + name);
            myDaemonService = ((MyDaemonService.MyBinder) service).getService();
            myDaemonService.registerSocketCallback(mySocketCallback);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v(TAG, "onServiceDisconnected: " + name.toString());
        }

        @Override
        public void onBindingDied(ComponentName name) {
            Log.v(TAG, "onBindingDied: " + name.toString());
        }
    }
}
