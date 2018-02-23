package com.hellodroid.lan;

import android.os.Handler;
import android.util.Log;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jr on 18-2-21.
 */

public class Neighbor {
    private static final String TAG = "Neighbor";

    private static Neighbor mInstance;
    //private final Connector mConnector = new Connector();
    private final List<String>  mNeighbors = new ArrayList<>();
    private final List<String>  mFriends = new ArrayList<>();
    private final List<Callback>  mCallbacks = new ArrayList<>();
    private final List<Handler>  mHandlers = new ArrayList<>();

    private Neighbor(){
        Log.v(TAG, "new Neighbor");
    }

    public static Neighbor newInstance(){
        if (mInstance == null){
            mInstance = new Neighbor();
        }
        return mInstance;
    }

    public void register(Handler h, Callback cb){
        if (h != null){
            mHandlers.add(h);
        }

        if (cb != null){
            mCallbacks.add(cb);
        }
    }

    public void triggerUpdating(List<String> neighbors) {
        Log.v(TAG, "triggerUpdating: " + neighbors.size());
        synchronized (mNeighbors) {
            mNeighbors.clear();
            mNeighbors.addAll(neighbors);
        }
        Thread t = new Connector();
        t.start();
    }


/* ********************************************************************************************** */


    class Connector extends Thread {
        private final int SOCKET_PORT = 8866;

        @Override
        public void run() {
            Log.v(TAG, ":Neighbor.Connector: running ...");
            List<String> connectedList = new ArrayList<>();

            synchronized (mNeighbors) {
                for (String ip : mNeighbors) {
                    try {
                        SocketChannel sc = SocketChannel.open();
                        boolean connected = sc.connect(new InetSocketAddress(ip, SOCKET_PORT));
                        if (connected) {
                            Log.d(TAG, ":HB: " + sc.toString());
                            //TODO: can do more communications
                            connectedList.add(ip);
                        }
                        sc.close();
                    } catch (IOException e) {
                        //
                    }
                }

                mNeighbors.clear();
            }

            if (!connectedList.isEmpty()){
                synchronized (mFriends){
                    mFriends.clear();
                    mFriends.addAll(connectedList);
                }
            }

            delivery();
            Log.v(TAG, ":Neighbor.Connector: exit ...");
        }

        private void delivery(){
            for (Callback cb : mCallbacks){
                cb.onConnectedNeighbors(mFriends);
            }

            for (Handler h : mHandlers){
                //TODO:
            }
        }
    }


/* ********************************************************************************************** */


    public interface Callback {
        void onConnectedNeighbors(List<String> connectedList);
    }
}
