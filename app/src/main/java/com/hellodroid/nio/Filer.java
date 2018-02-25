package com.hellodroid.nio;

/*
**
** REVISED HISTORY
**   yl7 | 18-2-11: Created
**     
*/


import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
    private static final int BUFFER_HEADER_BYTES = 4;
    private static final int MAX_BUFFER_BYTES = 1028;
    private static final int FILE_HEADER = 0xFFAF;
    private static Filer mInstance;

    private final Listener mListener = new Listener();
    private final Sender mSender = new Sender();

    private Context mContext; // for internal directory
    private String mPath;     // for external directory


/* ********************************************************************************************** */

    private Filer(){
        //mListener.start();
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

    // save internal
    public void configure(Context context){
        mContext = context;
    }

    // save external
    public void configure(String path){
        mPath = path;
    }

    public void sendFileInformation(String fileName) {

    }

    public void sendFileStream(FileInputStream fis){

    }

/* ********************************************************************************************** */

    class Listener implements Runnable {

        private static final int STATE_READY = 0xE0;
        private static final int STATE_STREAM = 0xEE;
        private static final int STATE_ENDING = 0xEF;

        private final List<Callback> mCallbackList = new ArrayList<>();
        private final List<Handler> mHandlerList = new ArrayList<>();
        private final ByteBuffer mBuffer = ByteBuffer.allocate(MAX_BUFFER_BYTES);

        private String mFileName;
        private FileOutputStream mFileOutputStream;
        private int mState = STATE_READY;

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

            try {
                ServerSocketChannel ssc = ServerSocketChannel.open();
                ssc.socket().bind(new InetSocketAddress(SOCKET_FILE_PORT));
                while (true) { // Listener always alive !
                    SocketChannel sc = ssc.accept();
                    Log.v(TAG, ":Listener: accept= " + sc.toString());
                    int count;
                    do {
                        mBuffer.clear();
                        count = sc.read(mBuffer);
                        if (count > 0) {
                            mBuffer.flip();
                            handleState(mBuffer);
                        } else {
                            Log.d(TAG, "Stream ending");
                            mState = STATE_ENDING;
                            //dispatch(mFileName);
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

            switch (mState){
                case STATE_READY:
                    int header  = bb.getInt(0);
                    if (header == FILE_HEADER){
                        // TODO: parse more file information
                        String fileName = new String(unwrapContent(bb));
                        create(fileName);
                        mState = STATE_STREAM;
                    } else { // forward stream
                        Log.d(TAG, "unknown header");
                    }
                    break;
                case STATE_STREAM:
                    append(bb);
                    break;
                default: break;
            }
        }

        private byte[] unwrapContent(ByteBuffer buffer){
            int length = buffer.limit() - BUFFER_HEADER_BYTES;
            byte[] content = new byte[length];
            buffer.position(BUFFER_HEADER_BYTES);
            buffer.get(content);
            return content;
        }

        private void create(String fileName){
            // TODO: write to external SD card
            try {
                if (mPath != null) {
                    File f = new File(mPath, mFileName);
                    if(f.exists()){
                        f.delete();
                    }
                    f.createNewFile();
                    mFileOutputStream = new FileOutputStream(f, true);
                } else if (mContext != null) {
                    mFileOutputStream = mContext.openFileOutput(fileName, Context.MODE_APPEND);
                } else {
                    Log.d(TAG, "file is not configured !");
                }
            } catch (IOException e) {
                //
            }
        }

        private void append(ByteBuffer bb){
            try {
                if (mFileOutputStream != null) {
                    mFileOutputStream.write(bb.array(), 0, bb.limit());
                }
            } catch (IOException e) {
                e.printStackTrace();
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
