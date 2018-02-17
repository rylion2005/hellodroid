package com.hellodroid.nio;

/*
**
** REVISED HISTORY
**   yl7 | 18-2-11: Created
**     
*/


import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

/*
** ********************************************************************************
**
** SocketChanner
**   This is a multi-IO socket channel.
**      - Accept/Read multi-clients        N Clients ==> 1 Server
**      - Connect/Write to multi-servers   1 Client  ==> N Servers
**
** USAGE:
**   ......
**
** ********************************************************************************
*/
public class SocketChanner extends Handler {
    private static final String TAG = "SocketChanner";

    private static final int SOCKET_PORT = 8866;

    // Internal Message: Listener/Reader ==> SocketChanner
    private static final int MESSAGE_CONNECT = 0xC0;
    private static final int MESSAGE_INCOMING = 0xC1;
    // Internal Message: SocketChanner ==> Sender
    private static final int MESSAGE_OUTGOING = 0xC2;

    // Message: SocketChanner ==> Clients
    public static final int MESSAGE_RAW_BYTE =0xD0;
    public static final int MESSAGE_FILE_NAME = 0xD1;
    public static final int MESSAGE_TEXT = 0xD2;

    private static SocketChanner mInstance;

    private List<String> mNeighborList = new ArrayList<>();
    private final static Object mNeighborLock = new Object();
    private List<String> mAvailableNeighbors = new ArrayList<>();

    private final Thread mListener = new Thread(new Listener());
    private final Object mReaderLock = new Object();
    private final List<Reader> mReaders = new ArrayList<>();

    private final Object mIncomingQueueLock = new Object();
    private final Object mOutgoingQueueLock = new Object();
    private final List<ByteBuffer> mOutgoingQueue = new ArrayList<>();
    private final List<byte[]> mIncomingQueue = new ArrayList<>();

    private final List<Callback>  mCallbackList = new ArrayList<>();
    private Context mContext;

    private final List<Sender> mSenderPool = new ArrayList<>();
    private boolean mFirstStream = true;


/* ********************************************************************************************** */

    private SocketChanner(Context context){
        mContext = context;
        start();
    }

    private SocketChanner(Context context, Callback cb){
        mContext = context;
        if (cb != null) {
            mCallbackList.add(cb);
        }
        start();
    }

    public static SocketChanner newInstance(Context context, Callback cb){
        if (mInstance == null){
            mInstance = new SocketChanner(context, cb);
        }

        return mInstance;
    }

    @Override
    public void handleMessage(Message msg) {
        Log.v(TAG, "handleMessage: " + msg.what);
        switch (msg.what) {
            case MESSAGE_CONNECT:
                synchronized (mReaderLock) {
                    // start reader thread
                    for (Reader rr : mReaders) {
                        Thread t = new Thread(rr);
                        t.start();
                    }
                    mReaders.clear();
                }
                break;

            case MESSAGE_INCOMING:
                // messages from all threads will be queued here
                int message_type = msg.arg1;
                int message_length = msg.arg2;
                byte[] message = msg.getData().getByteArray("ByteBuffer");
                enqueueIncomings(message);
                // TODO: delivery message: handler or callback?
                delivery();
                break;

            case MESSAGE_OUTGOING:
                synchronized (mOutgoingQueueLock) {
                    // scan all neighbors
                    synchronized (mNeighborLock) {
                        Log.v(TAG, "Neighbors: " + mNeighborList.size());
                        for (String host : mNeighborList) {
                            Sender sr = new Sender(host);
                            sr.cloneQueue(mOutgoingQueue);
                            sr.setMode(0);
                            // TODO: later we only start connectable thread.
                            sr.start();
                        }
                    }
                    mOutgoingQueue.clear();
                }
                break;

            default:
                break;
        }
    }

    public void register(Callback cb){
        if (cb != null){
            mCallbackList.add(cb);
        }
    }

    public void setNeighbors(ArrayList<String> neighbors){
        synchronized (mNeighborLock) {
            mNeighborList.clear();
            mNeighborList.addAll(neighbors);

            for (String ip : mNeighborList) {
                Log.v(TAG, "IP:[" + ip + "]");
            }
        }
    }

    public void sendText(String shortText){
        enqueueOutgoings(Wrapper.wrapTextMessage(shortText));
    }

    public void sendFile(String path, String fileName){
        //
    }

    public void sendFile(String pathname){
        Log.v(TAG, "sendFile: " + pathname);
        File f = new File(pathname);
        String name = pathname.substring(pathname.lastIndexOf('/') + 1, pathname.length());
        Log.v(TAG, "name: " + name);
        if ( !f.exists() ){
            Log.e(TAG, "File not exist");
            return;
        }

        // send file name
        enqueueOutgoings(Wrapper.wrapFileName(name));
        try {
            FileInputStream fis = new FileInputStream(f);
            // send contents buffer by buffer
            while (fis.available() > 0){
                byte[] bytes = new byte[Wrapper.MESSAGE_BUFFER_LENGTH];
                int counts = fis.read(bytes);
                Log.v(TAG, "count: " + counts);
                enqueueOutgoings(Wrapper.wrapRawContent(bytes, counts));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // send ending
        enqueueOutgoings(Wrapper.wrapFileEnding());

    }

    public void sendFile(InputStream is){
        // TODO:
    }

    public void sendStream(ByteBuffer bb){
        if (mSenderPool.isEmpty()){
            startSenderThreads();
        }

        for ( Sender s : mSenderPool) {
            ByteBuffer b = s.getStreamBuffer();
            b.clear();
            if (mFirstStream){
                mFirstStream = false;
                ByteBuffer buff = Wrapper.wrapStreamHeader(bb);
                b.put(buff);
                b.rewind();
            } else {
                b.put(bb);
                b.rewind();
            }
        }
    }

    public void sendChannel(FileChannel fc){

    }


/* ********************************************************************************************** */

    private void start(){
        mListener.start();
    }

    private void enqueueOutgoings(ByteBuffer bb){
        Log.v(TAG, "enqueueOutgoings");
        synchronized (mOutgoingQueueLock){
            mOutgoingQueue.add(bb);
        }
        sendEmptyMessage(MESSAGE_OUTGOING);
    }

    private void enqueueIncomings(byte[] message){
        Log.v(TAG, "enqueueIncomings");
        synchronized (mIncomingQueueLock){
            mIncomingQueue.add(message);
        }
    }

    private void delivery(){
        Log.v(TAG, "deliver incomings");
        synchronized (mIncomingQueueLock) {
            Log.v(TAG, "incoming queue = " + mIncomingQueue.size());
            for ( byte[] msg : mIncomingQueue ) {
                for ( Callback cb : mCallbackList ) {
                    cb.onTextMessageArrived(new String(msg));
                }
            }
            mIncomingQueue.clear();
        }
    }

    private void startSenderThreads(){

        synchronized (mSenderPool) {
            mSenderPool.clear();
        }

        for (String ip : mNeighborList){
            Sender s = new Sender(ip);
            s.setMode(1);
            mSenderPool.add(s);
            s.start();
        }
    }


/* ********************************************************************************************** */

    class Listener extends Thread {
        private Listener(){}

        @Override
        public void run() {
            Log.v(TAG, ":Listener: running");
            ServerSocketChannel ssc;

            try {
                ssc = ServerSocketChannel.open();
                ssc.socket().bind(new InetSocketAddress(SOCKET_PORT));
                //ssc.configureBlocking(false);
            } catch (IOException e){
                Log.v(TAG, ":Listener: thread died");
                e.printStackTrace();
                return;
            }

            while (!isInterrupted()) {
                try {
                    Log.v(TAG, ":Listener: stop and waiting ......");
                    SocketChannel sc = ssc.accept();
                    if (sc!=null){
                        Log.v(TAG, ":Listener: accept= " + sc.socket().toString());
                        Reader rr = new Reader(sc);
                        synchronized (mReaderLock) {
                            mReaders.add(rr);
                        }
                        sendEmptyMessage(MESSAGE_CONNECT);
                    }
                    //Thread.sleep(1000);
                } catch (IOException e){
                    Log.v(TAG, ":Listener: thread died !!!");
                    e.printStackTrace();
                    break;
                }
            }
        }
    }


/* ********************************************************************************************** */


    class Reader extends Thread {

        private final int STATE_READY = 0xC0;
        private final int STATE_WAIT_ENDING = 0xCC;
        private final int STATE_WAIT_STREAM_ENDING = 0xCD;
        private int mState;
        private String mFileName;

        private SocketChannel mSocketChannel;

        private Reader(SocketChannel sc) {
            mSocketChannel = sc;
            mState = STATE_READY;
        }

        @Override
        public void run() {
            Log.v(TAG, ":Reader: running");
            try {
                ByteBuffer bb = ByteBuffer.allocate(Wrapper.MESSAGE_BUFFER_LENGTH);
                int count = mSocketChannel.read(bb);
                //TODO: more read process here
                bb.flip();
                handleStateMachine(bb);

                // we must close socket channel after read all data!
                if (!mSocketChannel.socket().isClosed()) {
                    mSocketChannel.close();
                }
            } catch (IOException e) {
                // TODO
            }

            Log.v(TAG, ":Reader: thread exit !");
        }

        private SocketChannel getSocketChannel() {
            return mSocketChannel;
        }

        private void handleStateMachine(ByteBuffer bb) {
            Log.v(TAG, ":Reader: StateMachine/" + mState + ": " + bb);
            int header = Wrapper.unwrapHeader(bb);
            switch (mState) {
                case STATE_READY:
                    switch (header) {
                        case Wrapper.BUFFER_TYPE_HEART_BEAT:
                            Log.v(TAG, "Heart Beat ...");
                            mState = STATE_READY;
                            break;

                        case Wrapper.BUFFER_TYPE_FILE_NAME:
                            Log.v(TAG, "FILE name ...");
                            mFileName = new String(Wrapper.unwrapContentBytes(bb));
                            mState = STATE_WAIT_ENDING;
                            break;

                        case Wrapper.BUFFER_TYPE_TEXT:
                            Log.v(TAG, "TEXT ...");
                            int length = Wrapper.getContentByteCount(bb);
                            dispatch(MESSAGE_TEXT, Wrapper.unwrapContentBytes(bb), length);
                            mState = STATE_READY;
                            break;

                        case Wrapper.BUFFER_TYPE_STREAM_START:
                            mState = STATE_WAIT_STREAM_ENDING;
                            break;

                        default:
                            break;
                    }
                    break;

                case STATE_WAIT_ENDING:
                    if (header == Wrapper.BUFFER_TYPE_FILE_ENDING) {
                        Log.v(TAG, "file ending");
                        mState = STATE_READY;
                        dispatch(MESSAGE_FILE_NAME, mFileName.getBytes(), mFileName.getBytes().length);
                    } else {
                        Log.v(TAG, "file contents");
                        // TODO: process file contents
                    }
                    break;

                case STATE_WAIT_STREAM_ENDING:
                    if (header == Wrapper.BUFFER_TYPE_STREAM_END){
                        // TODO:
                    } else {
                        dispatch(Wrapper.cutContent(bb));
                    }
                    break;

                default:
                    break;
            }

            Log.v(TAG, "next >> " + mState);
        }

        private void dispatch(int type, byte[] bytes, int length) {
            Log.v(TAG, "enqueue incoming/" + type + ": length=" + length);
            Message msg = new Message();
            msg.what = MESSAGE_INCOMING;
            msg.arg1 = type;
            msg.arg2 = length;
            Bundle b = new Bundle();
            b.putByteArray("ByteBuffer", bytes);
            msg.setData(b);
            sendMessage(msg);
        }

        private void dispatch(ByteBuffer bb){
            Log.d(TAG, "dispatch: bb = " + bb.toString());
            for (Callback cb : mCallbackList){
                cb.onStreamBufferArrived(bb);
            }
        }
    }


/* ********************************************************************************************** */


    class Sender extends Thread {
        private String mHost;
        private SocketChannel mChannel;

        private int mMode = 0; // 0: message mode; 1: stream mode
        private ByteBuffer mStreamBuffer; // = ByteBuffer.allocate(Wrapper.STREAM_BUFFER_LENGTH);

        private final List<ByteBuffer> mOutgoingQueue = new ArrayList<>();

        private Sender(String ip){
            mHost = ip;
        }

        @Override
        public void run() {
            Log.v(TAG, ":Sender: running : " + mHost);
            while(!isInterrupted()) {
                try {
                    SocketChannel sc = SocketChannel.open();
                    boolean connected = sc.connect(new InetSocketAddress(mHost, SOCKET_PORT));
                    if (connected) { // connected !
                        if (mMode == 0) {
                            Log.v(TAG, ":Sender: queue=" + mOutgoingQueue.size());
                            for (ByteBuffer bb : mOutgoingQueue) {
                                Log.v(TAG, ":Sender: bb=" + bb);
                                int writeCount = sc.write(bb);
                                Log.v(TAG, ":Sender: write count=" + writeCount);
                                bb.clear();  // clear buffer
                            }
                            mOutgoingQueue.clear(); // clear queue
                            sc.close();
                            break;
                        } else {
                            while((mStreamBuffer.remaining() > 0) && (!isInterrupted())) {
                                int writeCount = sc.write(mStreamBuffer);
                                Log.v(TAG, "write: " + writeCount);
                            }
                        }
                    } else {
                        Thread.sleep(500);
                        sc.close();
                        continue;
                    }
                } catch (IOException | InterruptedException e) {
                    //Log.v(TAG, ":Sender: IOException");
                    break;
                }
            }
            Log.v(TAG, ":Sender: running over [ " + mHost + " ] !!!");
        }

        private boolean initSocketChannel(int mode ){
            boolean connected = false;

            try {
                mChannel = SocketChannel.open();
                connected = mChannel.connect(new InetSocketAddress(mHost, SOCKET_PORT));
            } catch (IOException e) {
                //e.printStackTrace(); //TODO
            }

            return connected;
        }

        private void cloneQueue(List<ByteBuffer> source){
            mOutgoingQueue.addAll(source);
        }

        private void setMode(int mode){
            mMode = mode;
        }

        private ByteBuffer getStreamBuffer(){
           return mStreamBuffer;
        }

        private void writeStream(){

        }
    }

/* ********************************************************************************************** */

    static class Wrapper{
        private static final int BUFFER_HEADER_LENGTH = 4; // header
        private static final int MESSAGE_CONTENT_LENGTH = 1024; // contents
        private static final int MESSAGE_BUFFER_LENGTH = 4 + 1024;
        private static final int STREAM_BUFFER_LENGTH = 4 + 1024*10;

        private static final int BUFFER_TYPE_HEART_BEAT = 0xFF00;  // no message content
        private static final int BUFFER_TYPE_TEXT = 0xFFA0;        // header + contents
        private static final int BUFFER_TYPE_FILE_NAME = 0xFFB0;   // header + contents
        private static final int BUFFER_TYPE_RAW_CONTENT = 0xFFBA; // no message header
        private static final int BUFFER_TYPE_FILE_ENDING = 0xFFBB; // no message content

        private static final int BUFFER_TYPE_STREAM_START = 0xFFD0;
        private static final int BUFFER_TYPE_STREAM_END = 0xFFD1;

        private static ByteBuffer wrap(int type, byte[] bytes, int length){
            ByteBuffer bb;

            if (length > MESSAGE_CONTENT_LENGTH) {
                Log.v(TAG, "byte error");
                return null;
            }

            bb = ByteBuffer.allocate(length + BUFFER_HEADER_LENGTH);
            if (type != BUFFER_TYPE_RAW_CONTENT){
                bb.putInt(type);
            }

            if (bytes != null) { // allow content has nothing
                bb.put(bytes);
            }
            bb.rewind();

            return bb;
        }

        private static ByteBuffer wrapTextMessage(String text){
            return wrap(BUFFER_TYPE_TEXT, text.getBytes(), text.getBytes().length);
        }

        private static ByteBuffer wrapFileName(String fileName){
            return wrap(BUFFER_TYPE_FILE_NAME, fileName.getBytes(), fileName.getBytes().length);
        }

        private static ByteBuffer wrapRawContent(byte[] bytes, int length){
            return wrap(BUFFER_TYPE_RAW_CONTENT, bytes, length);
        }

        private static ByteBuffer wrapFileEnding(){
            return wrap(BUFFER_TYPE_FILE_ENDING, null, 0);
        }

        private static ByteBuffer wrapStreamHeader(ByteBuffer bb){
            int capacity = BUFFER_HEADER_LENGTH + bb.capacity();
            ByteBuffer buffer = ByteBuffer.allocate(capacity);
            buffer.putInt(BUFFER_TYPE_STREAM_START);
            buffer.put(bb);
            buffer.rewind();
            return buffer;
        }

        private static int unwrapHeader(ByteBuffer buffer){
            if (buffer.limit() == 0){
                return 0;
            }

            return buffer.getInt(0);
        }

        private static byte[] unwrapContentBytes(ByteBuffer buffer){
            int length = buffer.limit() - BUFFER_HEADER_LENGTH;
            byte[] content = new byte[length];
            buffer.position(BUFFER_HEADER_LENGTH);
            buffer.get(content);
            return content;
        }

        private static ByteBuffer cutContent(ByteBuffer buffer){
            int length = buffer.limit() - BUFFER_HEADER_LENGTH;
            ByteBuffer bb = ByteBuffer.allocate(length);
            bb.put(unwrapContentBytes(buffer));
            bb.rewind();
            return bb;
        }

        private static int getContentByteCount(ByteBuffer bb){
            return bb.limit() - BUFFER_HEADER_LENGTH;
        }
    }


/* ********************************************************************************************** */

    public interface Callback {
        void onTextMessageArrived(String text);
        void onStreamBufferArrived(ByteBuffer buffer);
    }
}
