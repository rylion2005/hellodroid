package com.hellodroid.nio;

/*
**
** REVISED HISTORY
**   yl7 | 18-2-11: Created
**     
*/


import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;


public class Streamer {
    private static final String TAG = "Streamer";

    private static final int SOCKET_STREAM_PORT = 62365;
    private static final int MAX_STREAM_BYTE_SIZE = 10 * 1024; // bytes
    private static Streamer mInstance;

    private final Listener mListener = new Listener();
    private final Sender mSender = new Sender();
    private final Thread mSendThread = new Thread(mSender);


/* ********************************************************************************************** */

    private Streamer(){
        (new Thread(mListener)).start();
        mSender.setRunnable(true);
        //(new Thread(mSender)).start();
    }

    public static Streamer newInstance(){
        if (mInstance == null){
            mInstance = new Streamer();
        }

        return mInstance;
    }

    public void register(Callback cb){
        mListener.register(cb);
    }

    public void setHost(String host){
        mSender.setHost(host);
    }

    public void startStream(ByteBuffer bb){
        if (!mSendThread.isAlive()){
            Log.v(TAG, "start thread");
            mSender.setHost("192.168.1.107");
            mSender.flush(bb);
            mSendThread.start();
        } else {
            mSender.flush(bb);
        }
    }

    public void stopStream(){
        mSender.setRunnable(false);
    }

/* ********************************************************************************************** */

    class Listener implements Runnable {

        private Callback mCallback;
        private final ByteBuffer mBuffer = ByteBuffer.allocate(MAX_STREAM_BYTE_SIZE);

        private Listener(){
            Log.v(TAG, "new Listener");
        }

        private void register(Callback cb){
            if (cb != null) {
                mCallback = cb;
            }
        }

        @Override
        public void run() {
            Log.v(TAG, ":Listener: running");

            try {
                ServerSocketChannel ssc = ServerSocketChannel.open();
                ssc.socket().bind(new InetSocketAddress(SOCKET_STREAM_PORT));
                while (true) { // Listener always alive !
                    SocketChannel sc = ssc.accept();
                    Log.v(TAG, ":Listener: accept= " + sc.socket().toString());
                    int count;
                    do {
                        synchronized (mBuffer) {
                            mBuffer.clear();
                            count = sc.read(mBuffer);
                            if (count > 0) {
                                mBuffer.flip();
                                dispatch(mBuffer);
                            }
                        }
                    } while (count > 0);
                    sc.close();
                }
            } catch (IOException e){
                Log.v(TAG, ":Listener: thread died");
            }
            Log.v(TAG, ":Listener: exit ...");
        }

        private void dispatch(ByteBuffer bb){
            Log.v(TAG, "dispatch: " + bb.toString());
            //mCallback.onByteStream(bb.duplicate());
        }
    }


/* ********************************************************************************************** */


    class Sender implements Runnable {

        private String mHost;
        private final ByteBuffer mBuffer = ByteBuffer.allocate(MAX_STREAM_BYTE_SIZE);
        private volatile boolean mRunning = false;
        private final Object mLock = new Object();

        private Sender(){}

        private void setRunnable(boolean running){
            mRunning = running;
        }

        private boolean isRunning(){
            return mRunning;
        }

        private void setHost(String host){
            mHost = host;
        }


        private void flush(ByteBuffer bb) {
            synchronized (mBuffer) {
                mBuffer.clear();
                mBuffer.put(bb);
                mBuffer.flip();
            }
        }

        private void flush(byte[] bytes, int offset, int length){
            synchronized (mBuffer){
                mBuffer.clear();
                mBuffer.put(bytes, offset, length);
                mBuffer.flip();
            }
        }

        @Override
        public void run() {
            Log.v(TAG, ":Sender: running[" + mHost + "]");
            mRunning = true;
            try {
                SocketChannel sc = SocketChannel.open(new InetSocketAddress(mHost, SOCKET_STREAM_PORT));
                Log.d(TAG, ":Sender: connected=" + sc.toString());
                while (mRunning) {
                    if (mBuffer.hasRemaining()) {
                        synchronized (mBuffer) {
                            Log.v(TAG, "Buffer: " + mBuffer.toString());
                            int count = sc.write(mBuffer);
                            Log.v(TAG, "write: " + count);
                        }
                    }
                }
                sc.close();
            } catch (IOException e) {
                Log.e(TAG, ":Sender: thread died !");
            }

            Log.v(TAG, ":Sender: exit [" + mHost + "]");
        }
    }

/* ********************************************************************************************** */

    public interface Callback {
        void onByteStream(ByteBuffer buffer);
    }
}
