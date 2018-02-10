package com.hellodroid.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

/**
 * Created by jr on 18-2-10.
 */

/*
** *************************************************************************************************
**
** CLASSNAME
**   ......
**
** *************************************************************************************************
*/
public class MyNetworkListener {
    private static final String TAG = "MyNetworkListener";

    private Context mContext;
    private final ConnectivityManager.NetworkCallback mCallBack;

/* ********************************************************************************************** */

    public MyNetworkListener(Context context){
        mContext = context;
        mCallBack = new ConnectivityCallback();
    }


    private void registerNetworkCallback(){
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        // 设置指定的网络传输类型(蜂窝传输) 等于手机网络
        builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        // 设置感兴趣的网络功能
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        // 设置感兴趣的网络：计费网络
        // builder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
        NetworkRequest request = builder.build();
        cm.registerNetworkCallback(request, mCallBack);
        cm.requestNetwork(request, mCallBack);
    }


/* ********************************************************************************************** */


/*
** *************************************************************************************************
**
** ConnectivityCallback
**   callback class for network connectivity
**
** *************************************************************************************************
*/
    public class ConnectivityCallback extends ConnectivityManager.NetworkCallback{
        @Override
        public void onAvailable(Network network) {
            super.onAvailable(network);
        }

        @Override
        public void onLosing(Network network, int maxMsToLive) {
            super.onLosing(network, maxMsToLive);
        }

        @Override
        public void onLost(Network network) {
            super.onLost(network);
        }

        @Override
        public void onUnavailable() {
            super.onUnavailable();
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
        }

        @Override
        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
            super.onLinkPropertiesChanged(network, linkProperties);
        }
    }
}
