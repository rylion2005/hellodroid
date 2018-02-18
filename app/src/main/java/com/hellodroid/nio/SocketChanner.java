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
public class SocketChanner {
    private static final String TAG = "SocketChanner";

    private static final int SOCKET_MODE_MESSAGE = 0xA0;
    private static final int SOCKET_MODE_STREAM_FILE = 0xA1;
    private static final int SOCKET_MODE_STREAM_BYTE = 0xA2;

    private static final int SOCKET_PORT = 8866;

    private static SocketChanner mInstance;

    // ip address
    private List<String> mNeighborList = new ArrayList<>();
    private final static Object mNeighborLock = new Object();

    // listener thread
    private final Listener mListener = new Listener();
    private final List<Sender> mSenderPool = new ArrayList<>();
    private boolean mFirstStream = true;

    /*
    ** only one reader is allowed to be in stream mode.
    ** if reader is in stream mode, sender can not be in stream mode.
    ** if reader is not in stream mode, sender can support more than one stream threads
    */
    private int mReaderMode = SOCKET_MODE_MESSAGE;
    private int mSenderMode = SOCKET_MODE_MESSAGE;

/* ********************************************************************************************** */

    private SocketChanner(){
        mListener.start();
    }

    private SocketChanner(Callback cb){
        mListener.register(cb);
        mListener.start();
    }

    public static SocketChanner newInstance(Callback cb){
        if (mInstance == null){
            mInstance = new SocketChanner(cb);
        }

        return mInstance;
    }

    public void register(Callback cb){
        mListener.register(cb);
    }

    public void setNeighbors(ArrayList<String> neighbors){
        Log.v(TAG, "setNeighbors: " + neighbors.size());
        synchronized (mNeighborLock) {
            mNeighborList.clear();
            mNeighborList.addAll(neighbors);

            for (String ip : mNeighborList) {
                Log.v(TAG, "IP:[" + ip + "]");
            }
        }
    }

    public void prepareStream(int streamType, String fileName){
        Log.d(TAG, "prepare stream: " + streamType);
        mSenderPool.clear();
        for (String ip : mNeighborList){
            Sender s = null;
            ByteBuffer bb = null;
            if (streamType == SOCKET_MODE_STREAM_FILE){ //file stream
                s = new Sender(ip, SOCKET_MODE_STREAM_FILE);
                // TODO: fill file information: name, size, MD5 validation
                if (fileName != null) {
                    bb.putInt(Wrapper.BUFFER_TYPE_STREAM_FILE);
                    bb.put(fileName.getBytes());
                    s.flush(bb);
                }
            } else if (streamType == SOCKET_MODE_STREAM_BYTE){ //byte stream
                s = new Sender(ip, SOCKET_MODE_STREAM_BYTE);
                bb.putInt(Wrapper.BUFFER_TYPE_STREAM_BYTE);
                s.flush(bb);
            } else {
                Log.d(TAG, "unknown stream: " + streamType);
            }

            if (s != null) {
                s.start();
                mSenderPool.add(s);
            }
        }
    }

    public void sendStream(ByteBuffer bb){

        for (Sender s : mSenderPool) {
            Log.d(TAG, "s connected: " + s.isConnected());
            if (s.isConnected()) {
                s.flush(bb);
            } else {
                s.interrupt();
            }
        }
    }

    public void destroyStream(){
        Log.d(TAG, "destroy Stream");
        for (Sender s : mSenderPool){
            s.interrupt();
        }
        mSenderPool.clear();
    }


/* ********************************************************************************************** */



/* ********************************************************************************************** */

    class Listener extends Thread {

        private final int STATE_READY = 0xC0;
        private final int STATE_WAIT_STREAM_ENDING = 0xCD;

        SocketChannel mSocketChannel;
        //TODO: only one stream thread is support now
        private Reader mStreamReader;
        private final List<Callback> mCallbackList = new ArrayList<>();

        private int mState;
        //private String mFileName;

        private Listener(){
            Log.v(TAG, "new Listener");
        }

        @Override
        public void run() {
            Log.v(TAG, ":Listener: running");
            ServerSocketChannel ssc;

            try {
                ssc = ServerSocketChannel.open();
                ssc.socket().bind(new InetSocketAddress(SOCKET_PORT));

                while (!isInterrupted()) {
                    Log.v(TAG, ":Listener: waiting ......");
                    mSocketChannel = ssc.accept();
                    Log.v(TAG, ":Listener: accept= " + mSocketChannel.socket().toString());
                    if (mStreamReader == null){ // no any stream, dispatch message.
                        int count;
                        ByteBuffer bb;
                        while (true) {
                            bb = ByteBuffer.allocate(Wrapper.MESSAGE_BUFFER_LENGTH);
                            count = mSocketChannel.read(bb);
                            if (count > 0) {
                                handleStateMachine(bb);
                            } else {
                                break;
                            }
                        }
                        mSocketChannel.close();
                    }
                }

            } catch (IOException e){
                Log.v(TAG, ":Listener: thread died");
                //e.printStackTrace();
            }

            Log.v(TAG, ":Listener: exit ...");
        }

        private void register(Callback cb){
            if (cb != null){
                mCallbackList.add(cb);
            }
        }

        private void handleStateMachine(ByteBuffer bb) {
            Log.v(TAG, ":Reader: StateMachine/" + mState + ": " + bb);
            int header = Wrapper.unwrapHeader(bb);
            switch (mState) {
                case STATE_READY:
                    switch (header) {
                        case Wrapper.BUFFER_TYPE_HEART_BEAT:
                            // discard here
                            Log.v(TAG, "Heart Beat ...");
                            mState = STATE_READY;
                            break;

                        case Wrapper.BUFFER_TYPE_STREAM_FILE:
                            Log.v(TAG, "FILE name ...");
                            String name = new String(Wrapper.unwrapContentBytes(bb));
                            mStreamReader = new Reader(mSocketChannel, mCallbackList, name);
                            mStreamReader.start();
                            mState = STATE_WAIT_STREAM_ENDING;
                            break;

                        case Wrapper.BUFFER_TYPE_TEXT:
                            Log.v(TAG, "TEXT ...");
                            dispatch(Wrapper.cutContent(bb));
                            mState = STATE_READY;
                            break;

                        case Wrapper.BUFFER_TYPE_RAW_BYTES:
                            dispatch(bb);
                            break;

                        case Wrapper.BUFFER_TYPE_STREAM_BYTE:
                            //TODO: stream mode, do not accept any message
                            mStreamReader = new Reader(mSocketChannel, mCallbackList, null);
                            mStreamReader.start();
                            mState = STATE_WAIT_STREAM_ENDING;
                            break;

                        default:
                            break;
                    }
                    break;

                case STATE_WAIT_STREAM_ENDING:
                    if (header == Wrapper.BUFFER_TYPE_STREAM_ENDING){
                        Log.v(TAG, "stream ending");
                        mReaderMode = SOCKET_MODE_MESSAGE;
                        mState = STATE_READY;
                        mStreamReader.interrupt();
                    }
                    break;

                default:
                    break;
            }

            Log.v(TAG, "next >> " + mState);
        }

        private void dispatch(ByteBuffer bb){
            for (Callback cb : mCallbackList){
                //cb.onMessage(bb);
            }
        }
    }


/* ********************************************************************************************** */


    class Reader extends Thread {
        private SocketChannel mSocketChannel;
        private List<Callback> mCallbacks;

        private String mFileName;

        private Reader(SocketChannel sc, List<Callback> list, String fileName) {
            mSocketChannel = sc;
            mCallbacks = list;
            mFileName = fileName;
        }

        @Override
        public void run() {
            Log.v(TAG, ":reader: running");

            try {
                ByteBuffer bb = ByteBuffer.allocate(Wrapper.STREAM_BUFFER_LENGTH);
                while (!isInterrupted()) {
                    bb.clear();
                    bb.rewind();

                    int count = mSocketChannel.read(bb);
                    Log.v(TAG, "read: " + count);
                    if (count > 0) {
                        bb.flip();
                        dispatch(bb);
                    } else {
                        break;
                    }
                }
                mSocketChannel.close();
            } catch (IOException e){
                Log.d(TAG, ":Reader: died");
            }

            Log.v(TAG, ":reader: exit");
        }

        private void dispatch(ByteBuffer bb){
            Log.d(TAG, "dispatch: " + bb.toString());
            if (mFileName != null) {
                //TODO: write buffer to file
            } else {
                // deliver buffer to clients
                for (Callback cb : mCallbacks) {
                    cb.onByteBuffer(bb);
                }
            }
        }
    }


/* ********************************************************************************************** */


    class Sender extends Thread {
        private String mHost;
        private int mSocketMode = SOCKET_MODE_MESSAGE;
        private ByteBuffer mBytesBuffer;
        private boolean mConnected = false;

        private Sender(String ip, int mode){
            mHost = ip;
            mSocketMode = mode;
        }

        @Override
        public void run() {
            Log.v(TAG, ":Sender: running : " + mHost);
            try {
                SocketChannel sc = SocketChannel.open();
                mConnected = sc.connect(new InetSocketAddress(mHost, SOCKET_PORT));
                Log.d(TAG, ":Sender: connect=" + mConnected);
                if (mConnected){
                    while (!isInterrupted()) {
                        if ((mBytesBuffer != null) && (mBytesBuffer.remaining() > 0)) {
                            int count = sc.write(mBytesBuffer);
                            Log.v(TAG, "write: " + count);
                        } else {
                            if (mSocketMode == SOCKET_MODE_MESSAGE){
                                break;
                            }
                        }
                    }
                }
                sc.close();
            } catch (IOException e) {
                Log.d(TAG, ":Sender: IOException");
            }
            
            Log.v(TAG, ":Sender: running over [ " + mHost + " ] !!!");
        }


        private void setSocketMode(int mode){
            mSocketMode = mode;
        }

        private boolean isConnected(){
            return mConnected;
        }

        private void flush(ByteBuffer bb){
            mBytesBuffer = bb;
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
        private static final int BUFFER_TYPE_STREAM_FILE = 0xFFB0;   // header + contents
        private static final int BUFFER_TYPE_STREAM_BYTE = 0xFFD0;   // only header
        private static final int BUFFER_TYPE_RAW_BYTES = 0xFFBA;     // no message header
        private static final int BUFFER_TYPE_STREAM_ENDING = 0xFFBB; // no message content


        private static ByteBuffer wrap(int type, byte[] bytes, int length){
            ByteBuffer bb;

            if (length > MESSAGE_CONTENT_LENGTH) {
                Log.v(TAG, "byte error");
                return null;
            }

            bb = ByteBuffer.allocate(length + BUFFER_HEADER_LENGTH);
            if (type != BUFFER_TYPE_RAW_BYTES){
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
        void onIncomingFile(String name);
        void onByteBuffer(ByteBuffer buffer);
    }
}
