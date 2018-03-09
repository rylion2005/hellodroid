package com.hellodroid.nio;

/*
**
** REVISED HISTORY
**   yl7 | 18-2-11: Created
**     
*/


import android.util.Log;

import com.hellodroid.talkie.User;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;


public class HeartBeating {
    private static final String TAG = "HeartBeating";

    private static final int SOCKET_HEART_BEAT_PORT = 61542;
    private static HeartBeating mInstance;

    private Callback mCallback;
    private final List<String> mNeighbors = new ArrayList<>();

    private final Listener mListener = new Listener();


/* ********************************************************************************************** */

    private HeartBeating(){
        (new Thread(mListener)).start();
    }

    public static HeartBeating newInstance(){
        if (mInstance == null){
            mInstance = new HeartBeating();
        }

        return mInstance;
    }

    public void register(Callback cb){
        if (cb != null) {
            mCallback = cb;
        }
    }


    public void updateNeighbors(List<String> neighbors, String myself){
        synchronized (mNeighbors){
            mNeighbors.clear();
            mNeighbors.addAll(neighbors);
            Sender sender = new Sender();
            (new Thread(sender)).start();
        }
    }

/* ********************************************************************************************** */

    class Listener implements Runnable {

        private volatile boolean mStop = false;

        private Listener(){
            Log.v(TAG, "Listener: " + SOCKET_HEART_BEAT_PORT);
        }

        @Override
        public void run() {
            Log.v(TAG, ":Listener: running");
            mStop = false;

            try {
                ServerSocketChannel ssc = ServerSocketChannel.open();
                ssc.socket().bind(new InetSocketAddress(SOCKET_HEART_BEAT_PORT));
                while (!mStop) { // Listener always alive !
                    SocketChannel sc = ssc.accept();
                    Log.v(TAG, ":Listener: accept= " + sc.socket().toString());
                    sc.close();
                }
            } catch (IOException e){
                Log.v(TAG, ":Listener: thread died");
            }
            Log.v(TAG, ":Listener: exit ...");
        }
    }

/* ********************************************************************************************** */


    class Sender implements Runnable {

        private Sender(){
            Log.d(TAG, ":Sender: " + SOCKET_HEART_BEAT_PORT);
        }

        @Override
        public void run() {
            Log.v(TAG, ":HeartBeat: running");

            for (String host : mNeighbors){
                boolean connected = false;
                try {
                    SocketChannel sc = SocketChannel.open();
                    connected = sc.connect(new InetSocketAddress(host, SOCKET_HEART_BEAT_PORT));
                    sc.close();
                } catch (IOException e) {
                    // do nothing
                }

                mCallback.onConnectionUpdate(host, connected);
            }
            Log.v(TAG, ":HeartBeat: exit");
        }
    }

/* ********************************************************************************************** */

    public interface Callback {
        void onConnectionUpdate(String ip, boolean connected);
    }
}
