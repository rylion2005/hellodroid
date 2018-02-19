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

    public static final int SOCKET_MODE_MESSAGE = 0xA0;
    public static final int SOCKET_MODE_STREAM_FILE = 0xA1;
    public static final int SOCKET_MODE_STREAM_BYTE = 0xA2;

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

    public static SocketChanner newInstance(){
        if (mInstance == null){
            mInstance = new SocketChanner();
        }

        return mInstance;
    }

    public void register(Callback cb){
        mListener.register(cb);
    }

    public void register(Handler h){
        mListener.register(h);
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

    public void sendText(String text){

        ByteBuffer bb = ByteBuffer.allocate(Wrapper.MESSAGE_BUFFER_LENGTH);
        bb.putInt(Wrapper.BUFFER_TYPE_TEXT);
        bb.put(text.getBytes());
        bb.rewind();

        for (String ip : mNeighborList){
            Sender s = new Sender(ip, SOCKET_MODE_MESSAGE);
            // copy buffer to every thread
            s.flush(bb);
            s.start();
        }
        bb.clear();
    }

    private Sender mSender;

    public void startStreams(ByteBuffer bb){
        //if (mSenderPool.isEmpty()){
            Log.v(TAG, "send stream header");
            //for (String ip : mNeighborList){
            mSender = new Sender("10.10.10.101", SOCKET_MODE_STREAM_BYTE);
                //TODO: here we discard the 1st frame and send header frame
                ByteBuffer buf = ByteBuffer.allocate(Wrapper.MESSAGE_BUFFER_LENGTH);
                buf.putInt(Wrapper.BUFFER_TYPE_STREAM_BYTE);
                buf.rewind();
            mSender.flush(buf);
            mSender.start();
                //mSenderPool.add(s);
            //}
        //} else {
        //    for (Sender s : mSenderPool) {
        //        s.flush(bb);
        //    }
        //}
    }

    public void stopStreams(){
        //Log.v(TAG, "stop Streams: " + mSenderPool.size());
        //for (Sender s : mSenderPool){
        mSender.interrupt();
        //}
        //mSenderPool.clear();
    }


/* ********************************************************************************************** */

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

        private final int STATE_READY = 0xC0;
        private final int STATE_WAIT_STREAM_ENDING = 0xCD;

        SocketChannel mSocketChannel;
        //TODO: only one stream thread is support now
        private Reader mStreamReader;
        private final List<Callback> mCallbackList = new ArrayList<>();
        private final List<Handler> mHandlerList = new ArrayList<>();

        private int mState;

        private Listener(){
            Log.v(TAG, "new Listener");
            mState = STATE_READY;
        }

        @Override
        public void run() {
            Log.v(TAG, ":Listener: running");
            ServerSocketChannel ssc;

            try {
                ssc = ServerSocketChannel.open();
                ssc.socket().bind(new InetSocketAddress(SOCKET_PORT));
                ByteBuffer bb =  ByteBuffer.allocate(Wrapper.MESSAGE_BUFFER_LENGTH);
                while (!isInterrupted()) {
                    Log.v(TAG, ":Listener: waiting ......");
                    mSocketChannel = ssc.accept();
                    Log.v(TAG, ":Listener: accept= " + mSocketChannel.socket().toString());
                    if (mStreamReader == null){ // no any stream, dispatch message.
                        while (true) {
                            bb.clear();
                            bb.rewind();
                            int count = mSocketChannel.read(bb);
                            Log.v(TAG, ":Listener: read/" + count + "/: " + bb.toString());
                            if (count > 0) {
                                bb.flip();
                                handleStateMachine(bb);
                                bb.clear();
                                bb.rewind();
                            } else {
                                bb.clear();
                                bb.rewind();
                                break;
                            }
                        }
                        mSocketChannel.close();
                    }
                }
            } catch (IOException e){
                Log.v(TAG, ":Listener: thread died");
            }
            Log.v(TAG, ":Listener: exit ...");
        }

        private void register(Callback cb){
            if (cb != null){
                mCallbackList.add(cb);
            }
        }

        private void register(Handler h){
            if (h != null){
                mHandlerList.add(h);
            }
        }

        private void handleStateMachine(ByteBuffer bb) {
            Log.v(TAG, ":Listener: StateMachine/" + mState + "/: " + bb);
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
            Log.v(TAG, "dispatch: callback=" + mCallbackList.size());
            for (Callback cb : mCallbackList){
                cb.onByteBuffer(bb.duplicate());
                cb.onIncomingFile(null);
            }

            Log.v(TAG, "dispatch: handler=" + mHandlerList.size());
            for (Handler h : mHandlerList){
                Message m = new Message();
                m.arg1 = bb.limit();
                Bundle b = new Bundle();
                b.putByteArray("ByteBuffer", bb.array());
                m.setData(b);
                h.sendMessage(m);
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
            Log.v(TAG, ":Reader: running ........................................");
            Log.v(TAG, ":Reader: " + mSocketChannel.toString());
            try {
                ByteBuffer bb = ByteBuffer.allocate(Wrapper.STREAM_BUFFER_LENGTH);
                Log.v(TAG, ":Reader: thread=" + isInterrupted());
                while (!isInterrupted()) {
                    bb.clear();
                    bb.rewind();

                    int count = mSocketChannel.read(bb);
                    Log.v(TAG, ":Reader: read=" + count);
                    if (count > 0) {
                        dispatch(bb);
                    } else {
                        //break;
                    }
                }
                mSocketChannel.close();
            } catch (IOException e){
                Log.d(TAG, ":Reader: died");
            }

            Log.v(TAG, ":Reader: exit !!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }

        private void dispatch(ByteBuffer bb){
            Log.v(TAG, "dispatch stream *************************************");
            /*
            Log.d(TAG, "dispatch: " + bb.toString());
            if (mFileName != null) {
                //TODO: write buffer to file
            } else {
                // deliver buffer to clients
                for (Callback cb : mCallbacks) {
                    cb.onByteBuffer(bb);
                }
            }
            */
        }
    }


/* ********************************************************************************************** */


    class Sender extends Thread {
        private String mHost;
        private int mSocketMode = SOCKET_MODE_MESSAGE;
        private ByteBuffer mBytesBuffer;

        private Sender(String ip, int mode){
            mHost = ip;
            mSocketMode = mode;
        }

        @Override
        public void run() {
            Log.v(TAG, ":Sender: running : " + mHost);

            try {
                SocketChannel sc = SocketChannel.open();
                boolean connected = sc.connect(new InetSocketAddress(mHost, SOCKET_PORT));
                Log.v(TAG, ":Sender: Connected=" + connected);
                if (connected){
                    Log.d(TAG, ":Sender: Connected//" + sc.toString());
                    //dumpByteBuffer(mBytesBuffer);
                    if (mBytesBuffer.remaining() > 0) {
                        Log.v(TAG, ":Sender: Buffer: " + mBytesBuffer.toString());
                        int count = sc.write(mBytesBuffer);
                        Log.v(TAG, "write: " + count);
                        //mBytesBuffer.flip();
                        //dumpByteBuffer(mBytesBuffer);
                    }
                }
                sc.socket().close();
                sc.close();
            } catch (IOException e) {
                Log.d(TAG, ":Sender: IOException");
            }
            
            Log.v(TAG, ":Sender: exit [ " + mHost + " ]");
        }

        private void flush(ByteBuffer bb){
            mBytesBuffer = bb.duplicate();
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
