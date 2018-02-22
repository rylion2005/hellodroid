package com.hellodroid.nio;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;

/**
 * Created by jr on 18-2-21.
 */

public class Neighbor {
    private static final String TAG = "Neighbor";

    private static Neighbor mInstance;

    private final HeartBeat mHeartBeat = new HeartBeat();
    private final List<String>  mNeighbors = new ArrayList<>();
    private final List<String>  mFriends = new ArrayList<>();
    private final List<Callback>  mCallbacks = new ArrayList<>();
    private final List<Handler>  mHandlers = new ArrayList<>();

    private Neighbor(){
    }

    public static Neighbor newInstance(){
        if (mInstance == null){
            mInstance = new Neighbor();
        }
        return mInstance;
    }

    public void regsiter(Handler h, Callback cb){
        if (h != null){
            mHandlers.add(h);
        }

        if (cb != null){
            mCallbacks.add(cb);
        }
    }

    public void triggerUpdating(List<String> neighbors) {
        synchronized (mNeighbors) {
            mNeighbors.clear();
            mNeighbors.addAll(neighbors);
        }

        if (!mHeartBeat.isAlive()){
            mHeartBeat.start();
        }
    }


/* ********************************************************************************************** */

    class HeartBeat extends Thread{
        private final int SOCKET_PORT = 8866;

        @Override
        public void run() {
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
        }

        private void delivery(){
            for (Callback cb : mCallbacks){
                cb.onConnectableNeighbors(mFriends);
            }

            for (Handler h : mHandlers){
                //TODO:
            }
        }
    }

/* ********************************************************************************************** */

    interface Callback {
        void onConnectableNeighbors(List<String> connectableList);
    }
}
