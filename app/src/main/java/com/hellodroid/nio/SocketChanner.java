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
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
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

    private static final int SOCKET_PORT = 8866;
    private static SocketChanner mInstance;

    private final List<String> mNeighborList = new ArrayList<>();
    private final Listener mListener = new Listener();
    private final Sender mSender = new Sender();
    private final Streamer mStreamer = new Streamer();


/* ********************************************************************************************** */

    private SocketChanner(){
        mListener.start();
        //mSender.start();
    }

    public static SocketChanner newInstance(){
        if (mInstance == null){
            mInstance = new SocketChanner();
        }

        return mInstance;
    }

    public void register(Callback cb, Handler h){
        mListener.register(cb, h);
    }

    public void updateNeighbors(List<String> neighbors){
        Log.v(TAG, "updateNeighbors: " + neighbors.size());
        synchronized (mNeighborList) {
            mNeighborList.clear();
            mNeighborList.addAll(neighbors);
        }
    }

    public void dialHost(String host){
        mStreamer.setHost(host);
    }

    public void sendText(String text){

        ByteBuffer bb = Wrapper.wrap(text);

        synchronized (mNeighborList) {
            for (String ip : mNeighborList) {
                Sender s = new Sender();
                s.setHost(ip);
                s.setSocketMode(Sender.SOCKET_MODE_MESSAGE);
                s.flush(bb);
                s.start();
            }
        }
        bb.clear();
    }

    public void prepareStream(String fileName){
        Log.v(TAG, "prepareStream");
        ByteBuffer bb = Wrapper.wrap(Wrapper.BUFFER_TYPE_STREAM_FILE, fileName);
        mStreamer.setHost("10.10.10.100");
        mStreamer.flushLocked(bb);
        mStreamer.start();
    }

    public void sendStream(ByteBuffer bb){
        //Log.v(TAG, "sendStream: " + bb.toString());
        Log.v(TAG, ":Streamer: state=" + mStreamer.getState().toString());
        if (!mStreamer.isAlive()){
            Log.v(TAG, ":Streamer: send stream header");
            // discard the first frame
            ByteBuffer head = Wrapper.wrap(Wrapper.BUFFER_TYPE_STREAM_BYTE);
            mStreamer.setHost("10.10.10.100");
            //mStreamer.setHost("192.168.1.101");
            mStreamer.flushLocked(head);
            mStreamer.start();
            head.clear();
        } else {
            mStreamer.flushLocked(bb);
        }
    }

    public void stopStream(){
        //Log.v(TAG, "stopStream");
        mStreamer.interrupt();
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

        private final int STATE_READY = 0xC0;
        private final int STATE_STREAM = 0xCD;

        private final List<Callback> mCallbackList = new ArrayList<>();
        private final List<Handler> mHandlerList = new ArrayList<>();

        private int mState;

        private Listener(){
            Log.v(TAG, "new Listener");
            mState = STATE_READY;
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
                ByteBuffer bb = ByteBuffer.allocate(Wrapper.STREAM_BUFFER_LENGTH);
                ServerSocketChannel ssc = ServerSocketChannel.open();
                ssc.socket().bind(new InetSocketAddress(SOCKET_PORT));
                while (true) { // Listener always alive !
                    Log.d(TAG, ":Listener: waiting ...");
                    SocketChannel sc = ssc.accept();
                    Log.v(TAG, ":Listener: accept= " + sc.socket().toString());
                    int count;
                    do {
                        bb.clear();
                        count = sc.read(bb);
                        Log.v(TAG, ":Listener: read: " + count);
                        if (count > 0) {
                            bb.flip();
                            handleStateMachine(bb);
                        }
                    } while (count > 0);
                    mState = STATE_READY;
                    bb.clear();
                    sc.close();
                }
            } catch (IOException e){
                Log.v(TAG, ":Listener: thread died");
            }
            Log.v(TAG, ":Listener: exit ...");
        }

        private void handleStateMachine(ByteBuffer bb) {
            Log.v(TAG, ":Listener: StateMachine/" + mState + "/: " + bb);

            int header = Wrapper.unwrapHeader(bb);
            switch (mState) {
                case STATE_READY:
                    switch (header) {
                        case Wrapper.BUFFER_TYPE_HEART_BEAT:
                            // discard here
                            Log.v(TAG, "HEART BEAT ...");
                            mState = STATE_READY;
                            break;

                        case Wrapper.BUFFER_TYPE_STREAM_FILE:
                            Log.v(TAG, "FILE name ...");
                            String name = new String(Wrapper.unwrapContentBytes(bb));
                            mState = STATE_STREAM;
                            break;

                        case Wrapper.BUFFER_TYPE_STREAM_BYTE:
                            Log.v(TAG, "BYTE ...");
                            mState = STATE_STREAM;
                            break;

                        case Wrapper.BUFFER_TYPE_TEXT:
                            Log.v(TAG, "TEXT ...");
                            dispatch(Wrapper.cutContent(bb));
                            mState = STATE_READY;
                            break;

                        default:
                            Log.d(TAG, "unknown type");
                            break;
                    }
                    break;

                case STATE_STREAM:
                    Log.v(TAG, ":Listener: STATE_STREAM");
                    if (header == Wrapper.BUFFER_TYPE_STREAM_ENDING){
                        Log.v(TAG, "stream ending");
                        mState = STATE_READY;
                    }else{
                        dispatch(Wrapper.cutContent(bb));
                    }
                    break;

                default:
                    Log.d(TAG, "unknown state: " + mState);
                    break;
            }
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


    class Sender extends Thread {
        private static final int SOCKET_MODE_MESSAGE = 0xC1;
        private static final int SOCKET_MODE_STREAM = 0xC2;

        private String mHost;
        private int mSocketMode = SOCKET_MODE_MESSAGE;
        private ByteBuffer mBytesBuffer;

        private Sender(){
        }

        private void setHost(String host){
            mHost = host;
        }

        private void setSocketMode(int mode){
            mSocketMode = mode;
        }

        private void flush(ByteBuffer bb) {
            mBytesBuffer = bb;
            //Log.v(TAG, ":Sender: flush: " + mBytesBuffer.toString());
        }

        @Override
        public void run() {
            Log.v(TAG, ":Sender: running[" + mHost + "]");

            try {
                SocketChannel sc = SocketChannel.open();
                boolean connected = sc.connect(new InetSocketAddress(mHost, SOCKET_PORT));
                Log.d(TAG, ":Sender: connected=" + sc.toString());
                while (connected && !isInterrupted()) {
                    if (mBytesBuffer.remaining() > 0) {
                        //Log.v(TAG, "BB: " + mBytesBuffer.toString());
                        int count = sc.write(mBytesBuffer);
                        Log.v(TAG, "write: " + count);
                        if (mSocketMode == SOCKET_MODE_MESSAGE) {
                            break;
                        }
                    }
                }
                sc.socket().close();
                sc.close();
            } catch (IOException e) {
                Log.e(TAG, ":Sender: thread died !");
            }
            Log.v(TAG, ":Sender: exit [" + mHost + "]");
        }
    }

/* ********************************************************************************************** */

    class Streamer extends Thread {

        private String mHost;
        private ByteBuffer mByteBuffer;
        private final Object mLock = new Object();

        private Streamer(){
            Log.v(TAG, ":Streamer: new instance");
        }

        private void setHost(String host){
            mHost = host;
        }

        private void flushLocked(ByteBuffer bb){
            //dumpByteBuffer(bb);
            mByteBuffer = bb;
        }

        @Override
        public void run() {
            SocketChannel sc = null;

            Log.v(TAG, ":Streamer: running");
            try {
                sc = SocketChannel.open(new InetSocketAddress(mHost, SOCKET_PORT));
                while (!isInterrupted()){
                    if (mByteBuffer.hasRemaining()){
                        Log.v(TAG, "write: " + mByteBuffer);
                        sc.write(mByteBuffer);
                    }
                }
                // TODO: ??? send stream ending

            } catch (ClosedByInterruptException e) {
                Log.v(TAG, ":Streamer: ClosedByInterruptException");
                e.printStackTrace();
            }  catch (AsynchronousCloseException e) {
                Log.v(TAG, ":Streamer: AsynchronousCloseException");
                e.printStackTrace();
            } catch (IOException e) {
                Log.v(TAG, ":Streamer: IOException");
            }

            try{
                sc.close();
            } catch (IOException e) {
                e.printStackTrace();
            }


            Log.v(TAG, ":Streamer: exit");
        }
    }

/* ********************************************************************************************** */

    static class Wrapper{
        private static final int BUFFER_HEADER_LENGTH = 4; // header
        private static final int MESSAGE_CONTENT_LENGTH = 1024; // contents
        private static final int MESSAGE_BUFFER_LENGTH = BUFFER_HEADER_LENGTH + MESSAGE_CONTENT_LENGTH;
        private static final int STREAM_BUFFER_LENGTH = BUFFER_HEADER_LENGTH + MESSAGE_CONTENT_LENGTH*10;

        private static final int BUFFER_TYPE_HEART_BEAT = 0xFF00;    // header only
        private static final int BUFFER_TYPE_TEXT = 0xFFA0;          // header + contents
        private static final int BUFFER_TYPE_STREAM_FILE = 0xFFBF;   // header + contents
        private static final int BUFFER_TYPE_STREAM_BYTE = 0xFFCF;   // header only
        private static final int BUFFER_TYPE_STREAM_ENDING = 0xFFD0; // header only
        private static final int BUFFER_TYPE_RAW_BYTES = 0xFFEF;     // content only


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
            bb.flip();

            return bb;
        }

        private static ByteBuffer wrap(int streamType, String fileName){
            ByteBuffer bb = ByteBuffer.allocate(Wrapper.MESSAGE_BUFFER_LENGTH);
            bb.putInt(streamType);
            if (fileName != null) {
                bb.put(fileName.getBytes());
            }
            bb.flip();
            return bb;
        }

        private static ByteBuffer wrap(String text){
            if ((text == null) || (text.length() == 0)){
                return null;
            }

            ByteBuffer bb = ByteBuffer.allocate(Wrapper.MESSAGE_BUFFER_LENGTH);
            bb.putInt(BUFFER_TYPE_TEXT);
            bb.put(text.getBytes());
            bb.flip();
            return bb;
        }

        private static ByteBuffer wrap(int value){
            ByteBuffer bb = ByteBuffer.allocate(Wrapper.BUFFER_HEADER_LENGTH);
            bb.putInt(value);
            bb.flip();
            return bb;
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
            bb.flip();
            return bb;
        }
    }


/* ********************************************************************************************** */

    public interface Callback {
        void onIncomingFile(String name);
        void onByteBuffer(ByteBuffer buffer);
    }
}
