package com.hellodroid.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by jr on 18-2-9.
 */

/*
** *************************************************************************************************
**
** MyReceiver
**   This is a global network broadcast receiver which only one instance in an app
**
** USAGE
**
**  1. MyReceiver.create(this)
**  2. MyReceiver.register() / MyReceiver.unregister()
**  3. new ReceiverCallback(){...}
**  4. MyReceiver.registerCallBack()
**
** *************************************************************************************************
*/
public class MyReceiver extends BroadcastReceiver {

    private static MyReceiver mReceiver;
    private static Context mContext;
    private static final List<CallBack> mCallBackList = new ArrayList<>();

    public MyReceiver(Context context){
        mContext = context;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO
        if (mCallBackList.size() > 0){
            for (CallBack cb : mCallBackList) {
                cb.onMobileNetworkConnected();
                cb.onMobileNetworkDisconnected();
            }
        }
    }

    public static MyReceiver create(Context context){
        if (mReceiver == null) {
            mReceiver = new MyReceiver(context);
        }

        return mReceiver;
    }

    public static void register(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(mReceiver, intentFilter);
    }

    public static void unregister(){
        mContext.unregisterReceiver(mReceiver);
    }

    public static void registerCallBack(CallBack cb){
        mCallBackList.add(cb);
    }

    public static void unregisterCallBack(CallBack cb){
        //TODO
    }


/*
** *************************************************************************************************
**
** ReceiverCallback
**   Network status change notifier.
**   If client want to these notifications, he should implement these methods.
**
** *************************************************************************************************
*/
    public interface CallBack{
        abstract public void onWifiNetworkDisconnected();
        abstract public void onWifiNetworkConnected();
        abstract public void onMobileNetworkDisconnected();
        abstract public void onMobileNetworkConnected();
    }
}
