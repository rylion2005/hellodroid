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
import java.io.FileNotFoundException;
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
    private static final int FILE_HEADER_TAG = 0xFFAF;
    private static Filer mInstance;

    private final Listener mListener = new Listener();
    private final Sender mSender = new Sender();
    private final Thread mSenderThread = new Thread(mSender);


/* ********************************************************************************************** */

    private Filer(){
        (new Thread(mListener)).start();
        //mSenderThread.start();
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

    // save internal for incoming file
    public void configure(Context context){
        mListener.configure(context);
    }

    // save external for incoming file
    public void configure(String path){
        mListener.configure(path);
    }

    public void send(Context context, String fileName){
        FileInputStream fis = null;

        if ((context == null) || (fileName == null) || (fileName.isEmpty())){
            Log.v(TAG, "illegal parameters");
            return;
        }

        //TODO: preprocess file information

        try {
            fis = context.openFileInput(fileName);
            mSender.configure(fis);
            mSender.configure(fileName);
            if (!mSenderThread.isAlive()) {
                mSenderThread.start();
            }
        } catch (IOException|NullPointerException e){
            //TODO
        }
    }

    public void send(String path, String fileName){
        FileInputStream fis = null;
        if ((path == null) || (path.isEmpty()) || (fileName == null) || (fileName.isEmpty())){
            Log.v(TAG, "illegal parameters");
            return;
        }

        //TODO: preprocess file information

        File f = new File(path, fileName);
        try {
            fis = new FileInputStream(f);
            mSender.configure(fis);
            mSender.configure(fileName);
            if (!mSenderThread.isAlive()) {
                mSenderThread.start();
            }
        } catch (FileNotFoundException|SecurityException e) {
            e.printStackTrace();
        }
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
        private Context mContext; // for internal directory
        private String mPath;     // for external directory
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

        private void configure(Context context){
            mContext = context;
        }

        private void configure(String path){
            mPath = path;
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
                    if (header == FILE_HEADER_TAG){
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
        private ByteBuffer mBytesBuffer = ByteBuffer.allocate(MAX_BUFFER_BYTES);
        private String mFileName;
        private FileInputStream mFileInputStream;
        private boolean mHeaderSent = false;

        private Sender(){}

        private void setHost(String host){
            mHost = host;
        }

        private void configure(String fileName){
            mFileName = fileName;
        }

        private void configure(FileInputStream fis){
            mFileInputStream = fis;
        }

        @Override
        public void run() {
            Log.v(TAG, ":Sender: running[" + mHost + "]");

            mHeaderSent = false;

            try {
                SocketChannel sc = SocketChannel.open();
                boolean connected = sc.connect(new InetSocketAddress(mHost, SOCKET_FILE_PORT));
                Log.d(TAG, ":Sender: connected=" + sc.toString());
                if (connected) {
                    byte[] bytes = new byte[MAX_BUFFER_BYTES];
                    int count = 0;
                    while (mFileInputStream.available() > 0){
                        if (mHeaderSent) {
                            int readBytes = mFileInputStream.read(bytes);
                            mBytesBuffer.clear();
                            mBytesBuffer.put(bytes, 0, readBytes);
                            mBytesBuffer.flip();
                            count = sc.write(mBytesBuffer);
                            Log.v(TAG, "write: " + count);
                        } else { // send header information
                            mBytesBuffer.clear();
                            mBytesBuffer.putInt(FILE_HEADER_TAG);
                            mBytesBuffer.put(mFileName.getBytes(), 0, mFileName.length());
                            // TODO: file size, MD5 verification, etc.
                            mBytesBuffer.flip();
                            count = sc.write(mBytesBuffer);
                            Log.v(TAG, "write: " + count);
                            mHeaderSent = true;
                        }
                    }

                    mFileInputStream.close();
                }
                sc.close();
            } catch (IOException e) {
                Log.e(TAG, ":Sender: thread died !!!");
                e.printStackTrace();
            }

            Log.v(TAG, ":Sender: exit [" + mHost + "]");
        }
    }


/* ********************************************************************************************** */

    public interface Callback {
        void onFileIncoming(String name);
    }
}
