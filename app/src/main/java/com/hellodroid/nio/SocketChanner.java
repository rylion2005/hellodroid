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
    private static final int SOCKET_MODE_STREAM = 0xA1;
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

    public void sendStream(ByteBuffer bb){

        if (mReaderMode == SOCKET_MODE_STREAM){
            Log.v(TAG, "Error: reader has been in stream mode !");
            return;
        }

        Log.v(TAG, "send stream: " + bb.toString());
        // new sender thread pool
        if (mSenderPool.isEmpty()){
            Log.v(TAG, "start sender thread pool: ");
            // new and start thread pool
            for (String ip : mNeighborList){
                Sender s = new Sender(ip, SOCKET_MODE_STREAM);
                mSenderPool.add(s);
                s.start();
            }
        }

        Log.v(TAG, "Sender pool: " + mSenderPool.size());
        for ( Sender s : mSenderPool) {
            if (mFirstStream){
                mFirstStream = false;
                ByteBuffer buff = Wrapper.wrapStreamHeader(bb);
                s.flush(buff);
            } else {
                s.flush(bb);
            }
        }
    }

    public void stopStream(){
        stopSenderThreads();
    }


/* ********************************************************************************************** */

    private void stopSenderThreads(){

        synchronized (mSenderPool) {
            for (Sender s : mSenderPool){
                s.interrupt();
            }
            mSenderPool.clear();
        }
    }


/* ********************************************************************************************** */

    class Listener extends Thread {

        private final int STATE_READY = 0xC0;
        private final int STATE_WAIT_ENDING = 0xCC;
        private final int STATE_WAIT_STREAM_ENDING = 0xCD;

        SocketChannel mSocketChannel;
        //TODO: only one stream thread is support now
        private Reader mStreamReader;
        private final List<Callback> mCallbackList = new ArrayList<>();

        private int mState;
        private String mFileName;

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
            } catch (IOException e){
                Log.v(TAG, ":Listener: thread died");
                e.printStackTrace();
                return;
            }

            while (!isInterrupted()) {
                try {
                    Log.v(TAG, ":Listener: stop and waiting ......");
                    mSocketChannel = ssc.accept();
                    if (mSocketChannel != null){
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
                    Log.v(TAG, ":Listener: thread died !!!");
                    e.printStackTrace();
                    break;
                }
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

                        case Wrapper.BUFFER_TYPE_FILE_NAME:
                            Log.v(TAG, "FILE name ...");
                            mFileName = new String(Wrapper.unwrapContentBytes(bb));
                            startStreamReader();
                            mFileName = null;
                            mState = STATE_WAIT_ENDING;
                            break;

                        case Wrapper.BUFFER_TYPE_TEXT:
                            Log.v(TAG, "TEXT ...");
                            dispatch(Wrapper.cutContent(bb));
                            mState = STATE_READY;
                            break;

                        case Wrapper.BUFFER_TYPE_RAW_BYTES:
                            dispatch(bb);
                            break;

                        case Wrapper.BUFFER_TYPE_STREAM_START:
                            //TODO: stream mode, do not accept any message
                            startStreamReader();
                            mState = STATE_WAIT_STREAM_ENDING;
                            break;

                        default:
                            break;
                    }
                    break;

                case STATE_WAIT_ENDING:
                    if (header == Wrapper.BUFFER_TYPE_FILE_ENDING) {
                        Log.v(TAG, "file ending");
                        mReaderMode = SOCKET_MODE_MESSAGE;
                        mState = STATE_READY;
                        mStreamReader.interrupt();
                    }
                    break;

                case STATE_WAIT_STREAM_ENDING:
                    if (header == Wrapper.BUFFER_TYPE_STREAM_END){
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

        private void startStreamReader(){
            mStreamReader = new Reader(mSocketChannel, mCallbackList, mFileName);
            mReaderMode = SOCKET_MODE_STREAM;
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

            ByteBuffer bb = ByteBuffer.allocate(Wrapper.STREAM_BUFFER_LENGTH);
            while (!isInterrupted()){
                bb.rewind();
                bb.clear();
                try {
                    int count = mSocketChannel.read(bb);
                    Log.v(TAG, "read: " + count);
                    if (count > 0) {
                        bb.flip();
                        dispatch(bb);
                    } else {
                        break;
                    }
                } catch (IOException e) {
                    Log.v(TAG, "IOException");
                    break;
                }
            }

            try {
                mSocketChannel.close();
            } catch (IOException e){
                //TODO
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
        private ByteBuffer mBytesBuffer;   // buffer pointer


        private Sender(String ip, int mode){
            mHost = ip;
            mSocketMode = mode;
        }

        @Override
        public void run() {
            Log.v(TAG, ":Sender: running : " + mHost);
            while (!isInterrupted()) {
                try {
                    SocketChannel sc = SocketChannel.open();
                    boolean connected = sc.connect(new InetSocketAddress(mHost, SOCKET_PORT));
                    if (connected) { // connected !
                        if (mSocketMode == SOCKET_MODE_MESSAGE) {
                            if (mBytesBuffer.remaining() > 0){
                                int count = sc.write(mBytesBuffer);
                                Log.v(TAG, "write: " + count);
                            }
                            sc.close();
                            break;
                        } else {
                            //write by frame
                            while((mBytesBuffer.remaining() > 0) && (!isInterrupted())) {
                                int writeCount = sc.write(mBytesBuffer);
                                Log.v(TAG, "write: " + writeCount);
                            }

                            // no any data, exit thread
                            sc.close();
                            break;
                        }
                    } else {
                        sc.close();
                    }
                } catch (IOException e) {
                    break;
                }
            }
            Log.v(TAG, ":Sender: running over [ " + mHost + " ] !!!");
        }

        private void setSocketMode(int mode){
            mSocketMode = mode;
        }

        private void flush(ByteBuffer bb){
            mBytesBuffer = bb;
        }
    }

/* ********************************************************************************************** */

    static class Wrapper{
        private static final int BUFFER_HEADER_LENGTH = 8; // header
        private static final int MESSAGE_CONTENT_LENGTH = 1024; // contents
        private static final int MESSAGE_BUFFER_LENGTH = 8 + 1024;
        private static final int STREAM_BUFFER_LENGTH = 8 + 1024*10;

        private static final int BUFFER_TYPE_HEART_BEAT = 0xFF00;  // no message content
        private static final int BUFFER_TYPE_TEXT = 0xFFA0;        // header + contents
        private static final int BUFFER_TYPE_FILE_NAME = 0xFFB0;   // header + contents
        private static final int BUFFER_TYPE_RAW_BYTES = 0xFFBA; // no message header
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

        private static ByteBuffer wrapFileName(String fileName){
            return wrap(BUFFER_TYPE_FILE_NAME, fileName.getBytes(), fileName.getBytes().length);
        }

        private static ByteBuffer wrapRawBytes(byte[] bytes, int length){
            return wrap(BUFFER_TYPE_RAW_BYTES, bytes, length);
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
        void onByteBuffer(ByteBuffer buffer);
    }
}
