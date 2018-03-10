package com.hellodroid.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.util.Log;
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
**
** USAGE
**
**  1. MyNetworkReceiver receiver  = MyNetworkReceiver.getInstance(this)
**  2. receiver.register() / receiver.unregister()
**  3. MyNetworkReceiver.Callback cb = new MyNetworkReceiver.Callback(){...}
**  4. receiver.registerCallBack(cb)
**
** *************************************************************************************************
*/
public class MyNetworkReceiver extends BroadcastReceiver {
    private static final String TAG = "MyNetworkReceiver";
    private static MyNetworkReceiver mInstance;

    private Context mContext;
    private final List<CallBack> mCallBackList = new ArrayList<>();

    public static MyNetworkReceiver getInstance(Context context){
        if (mInstance == null){
            mInstance = new MyNetworkReceiver(context);
        }
        return mInstance;
    }


    public MyNetworkReceiver(Context context){
            mContext = context;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: " + intent.toString());
        //dumpConnectivity(intent);

        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)){

            int networkType = intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE, -1);
            if ( networkType == ConnectivityManager.TYPE_WIFI){
                boolean connectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                for (CallBack cb : mCallBackList) {
                    cb.onWifiConnectivity(connectivity);
                }
            }

            if ( networkType == ConnectivityManager.TYPE_MOBILE){
                boolean connectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                for (CallBack cb : mCallBackList) {
                    cb.onMobileConnectivity(connectivity);
                }
            }

        }

    }


    public void register(CallBack cb){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(this, intentFilter);

        if (cb != null) {
            mCallBackList.add(cb);
        }
    }

    public void unregister(){
        mContext.unregisterReceiver(this);
    }

    private void notifyStatusChanged(){
        for (CallBack cb : mCallBackList) {
            cb.onNetworkStatusChanged();
        }
    }

    private void dumpConnectivity(Intent intent){
        Log.d(TAG, "> > > > > > > > > > > > > > > > > > > > > > > > > > > > > >");
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP){
            // TODO
            //Log.d(TAG, "NW Type: " + b.get(ConnectivityManager.EXTRA_NETWORK_INFO));
        } else {

            if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                Log.d(TAG, "<--  WIFI_STATE_CHANGED_ACTION -->");
                Log.d(TAG, "EXTRA_WIFI_STATE: " + intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1));
            }

            if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                Log.d(TAG, "<--  NETWORK_STATE_CHANGED_ACTION -->");
                Log.d(TAG, "EXTRA_WIFI_INFO(CONNECTED): " + intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO));
                Log.d(TAG, "EXTRA_NETWORK_INFO: " + intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO));
            }

            if (intent.getAction().equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                Log.d(TAG, "<--  SUPPLICANT_CONNECTION_CHANGE_ACTION -->");
                Log.d(TAG, "EXTRA_SUPPLICANT_CONNECTED: " + intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false));
            }

            if (intent.getAction().equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
                Log.d(TAG, "<--  SUPPLICANT_STATE_CHANGED_ACTION -->");
                Log.d(TAG, "EXTRA_NEW_STATE: " + intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE));
                Log.d(TAG, "EXTRA_SUPPLICANT_ERROR: " + intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1));
            }

            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                Log.d(TAG, "<--  CONNECTIVITY_ACTION -->");
                Log.d(TAG, "EXTRA_NETWORK_TYPE: " + intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE, -1));
                Log.d(TAG, "EXTRA_IS_FAILOVER: " + intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false));
                Log.d(TAG, "EXTRA_NO_CONNECTIVITY: " + intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false));
                Log.d(TAG, "EXTRA_REASON: " + intent.getStringExtra(ConnectivityManager.EXTRA_REASON));
                Log.d(TAG, "EXTRA_NETWORK: " + intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK));
                Log.d(TAG, "EXTRA_EXTRA_INFO: " + intent.getStringExtra(ConnectivityManager.EXTRA_EXTRA_INFO));
                Log.d(TAG, "EXTRA_CAPTIVE_PORTAL: " + intent.getParcelableExtra(ConnectivityManager.EXTRA_CAPTIVE_PORTAL));
                Log.d(TAG, "EXTRA_CAPTIVE_PORTAL_URL: " + intent.getParcelableExtra(ConnectivityManager.EXTRA_CAPTIVE_PORTAL_URL));
            }
        }
        Log.d(TAG, "> > > > > > > > > > > > > > > > > > > > > > > > > > > > > >");
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
        void onNetworkStatusChanged();
        void onWifiConnectivity(boolean connected);
        void onMobileConnectivity(boolean connected);
    }
}
