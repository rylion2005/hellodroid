package com.hellodroid.nio;

/*
**
** REVISED HISTORY
**   yl7 | 18-2-11: Created
**     
*/


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;


public class Filer {
    private static final String TAG = "Filer";

    private static final int SOCKET_FILE_PORT = 62246;
    private static final int HEADER_INFORMATION = 0xFFAF;
    private static Filer mInstance;

    private final Listener mListener = new Listener();
    private final Sender mSender = new Sender();


/* ********************************************************************************************** */

    private Filer(){
        mListener.start();
        //mSender.start();
    }

    public static Filer newInstance(){
        if (mInstance == null){
            mInstance = new Filer();
        }

        return mInstance;
    }

    public void register(Callback cb, Handler h){
        mListener.register(cb, h);
    }

    public void sendFileInformation(String fileName) {

    }

    public void sendFileStream(FileInputStream fis){

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

    class Listener extends Thread {

        private final List<Callback> mCallbackList = new ArrayList<>();
        private final List<Handler> mHandlerList = new ArrayList<>();

        ByteBuffer mBuffer = ByteBuffer.allocate(1028);
        private volatile boolean mStop = false;

        private String mFileName;

        private Listener(){
            Log.v(TAG, "new Listener");
        }

        private void register(Callback cb, Handler h){
            if (cb != null){
                mCallbackList.add(cb);
            }

            if (h != null){
                mHandlerList.add(h);
            }
        }

        @Override
        public void run() {
            Log.v(TAG, ":Listener: running");
            mStop = false;

            try {
                ServerSocketChannel ssc = ServerSocketChannel.open();
                ssc.socket().bind(new InetSocketAddress(SOCKET_FILE_PORT));
                while (!mStop) { // Listener always alive !
                    SocketChannel sc = ssc.accept();
                    Log.v(TAG, ":Listener: accept= " + sc.socket().toString());
                    int count;
                    do {
                        mBuffer.clear();
                        count = sc.read(mBuffer);
                        if (count > 0) {
                            mBuffer.flip();
                            handleState(mBuffer);
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

        private void handleState(ByteBuffer bb) {
            int header  = bb.getInt(0);
            if (header == HEADER_INFORMATION){

            } else { // forward stream
                // write file
            }
        }

        private void dispatch(String name){
            Log.v(TAG, "dispatch: callback=" + mCallbackList.size());
            for (Callback cb : mCallbackList){
                cb.onFileIncoming(name);
            }

            Log.v(TAG, "dispatch: handler=" + mHandlerList.size());
            for (Handler h : mHandlerList){
                Message m = new Message();

                h.sendMessage(m);
            }

        }
    }

/* ********************************************************************************************** */


    class Sender extends Thread {

        private String mHost;
        private ByteBuffer mBytesBuffer;

        private Sender(){}

        private void setHost(String host){
            mHost = host;
        }

        private void flush(ByteBuffer bb) {
            mBytesBuffer = bb;
        }

        @Override
        public void run() {
            Log.v(TAG, ":Sender: running[" + mHost + "]");
            try {
                SocketChannel sc = SocketChannel.open();
                boolean connected = sc.connect(new InetSocketAddress(mHost, SOCKET_FILE_PORT));
                Log.d(TAG, ":Sender: connected=" + sc.toString());
                if (connected && mBytesBuffer.hasRemaining()) {
                    int count = sc.write(mBytesBuffer);
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
        void onFileIncoming(String name);
    }
}
