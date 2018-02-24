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
    private static Streamer mInstance;

    private final Listener mListener = new Listener();
    private final Sender mSender = new Sender();
    private final Thread mSendThread = new Thread(mSender);


/* ********************************************************************************************** */

    private Streamer(){
        (new Thread(mListener)).start();
        mSender.setStop(true);
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
            mSender.setHost("10.10.10.106");
            mSender.flush(bb);
            mSendThread.start();
        } else {
            mSender.flush(bb);
        }
    }

    public void stopStream(){
        mSender.setStop(true);
    }


/* ********************************************************************************************** */

    private void dumpThread(Thread t){
        Log.v(TAG, " <<<<<<<<<< Thread >>>>>>>>>>");
        Log.v(TAG, "Thread: " + t.toString());
        Log.v(TAG, "ID: " + t.getId());
        Log.v(TAG, "Name: " + t.getName());
        Log.v(TAG, "Priority: " + t.getPriority());
        Log.v(TAG, "State: " + t.getState().toString());
        Log.v(TAG, "isAlive: " + t.isAlive());
        Log.v(TAG, "isInterrupted: " + t.isInterrupted());
        Log.v(TAG, " <<<<<<<<<<        >>>>>>>>>>");
    }

    private void dumpByteBuffer(ByteBuffer bb){
        Log.v(TAG, ">>>>>>>>>>");
        Log.v(TAG, "Byte Buffer: " + bb.toString());
        Log.v(TAG, "Byte Buffer: capacity=" + bb.capacity());
        Log.v(TAG, "Byte Buffer: limit=" + bb.limit());
        Log.v(TAG, "Byte Buffer: position=" + bb.position());
        Log.v(TAG, "Byte Buffer: mark=" + bb.mark());
        Log.v(TAG, "Byte Buffer: remaining=" + bb.remaining());
        Log.v(TAG, "Byte Buffer: arrayOffset=" + bb.arrayOffset());
        Log.v(TAG, ">>>>>>>>>>");
    }

/* ********************************************************************************************** */

    class Listener implements Runnable {

        private static final int MAX_BUFFER_SIZE = 10 * 1024;

        private Callback mCallback;
        private final ByteBuffer mBuffer = ByteBuffer.allocate(MAX_BUFFER_SIZE);
        private volatile boolean mStop = false;

        private Listener(){
            Log.v(TAG, "new Listener");
        }

        private void register(Callback cb){
            mCallback = cb;
        }

        private void setStop(boolean stop){
            mStop = stop;
        }

        @Override
        public void run() {
            Log.v(TAG, ":Listener: running");

            mStop = false;

            try {
                ServerSocketChannel ssc = ServerSocketChannel.open();
                ssc.socket().bind(new InetSocketAddress(SOCKET_STREAM_PORT));
                while (!mStop) { // Listener always alive !
                    SocketChannel sc = ssc.accept();
                    Log.v(TAG, ":Listener: accept= " + sc.socket().toString());
                    int count;
                    do {
                        mBuffer.clear();
                        count = sc.read(mBuffer);
                        if (count > 0) {
                            mBuffer.flip();
                            dispatch(mBuffer);
                        }
                    } while (count > 0);
                    mBuffer.clear();
                    sc.close();
                }
            } catch (IOException e){
                Log.v(TAG, ":Listener: thread died");
            }
            Log.v(TAG, ":Listener: exit ...");
        }

        private void dispatch(ByteBuffer bb){
            Log.v(TAG, "dispatch: " + bb.toString());
            //mCallback.onByteBuffer(bb.duplicate());
        }
    }


/* ********************************************************************************************** */


    class Sender implements Runnable {

        private String mHost;
        private ByteBuffer mBytesBuffer; // buffer pointer
        private volatile boolean mStop = false;
        private final Object mLock = new Object();

        private Sender(){}

        private void setStop(boolean stop){
            mStop = stop;
        }

        private boolean getStop(){
            return mStop;
        }

        private void setHost(String host){
            mHost = host;
        }

        private void flush(ByteBuffer bb) {
            synchronized (mLock) {
                mBytesBuffer = bb;
            }
        }

        @Override
        public void run() {
            Log.v(TAG, ":Sender: running[" + mHost + "]");
            mStop = false;
            try {
                SocketChannel sc = SocketChannel.open(new InetSocketAddress(mHost, SOCKET_STREAM_PORT));
                Log.d(TAG, ":Sender: connected=" + sc.toString());
                while (!mStop) {
                    if (mBytesBuffer.hasRemaining()) {
                        synchronized (mLock) {
                            Log.v(TAG, "Buffer: " + mBytesBuffer.toString());
                            int count = sc.write(mBytesBuffer);
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
        void onByteBuffer(ByteBuffer buffer);
    }
}
