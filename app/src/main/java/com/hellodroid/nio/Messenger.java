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



public class Messenger {
    private static final String TAG = "Messenger";

    private static final int SOCKET_MESSENGER_PORT = 60246;
    private static Messenger mInstance;

    private final Listener mListener = new Listener();
    private final Sender mSender = new Sender();


/* ********************************************************************************************** */

    private Messenger(){
        (new Thread(mListener)).start();
        //mSender.start();
    }

    public static Messenger newInstance(){
        if (mInstance == null){
            mInstance = new Messenger();
        }

        return mInstance;
    }

    public void register(Callback cb, Handler h){
        mListener.register(cb, h);
    }

    public void sendText(String text){
        mSender.setHost("10.10.10.106");
        ByteBuffer bb = Wrapper.wrap(text);
        mSender.flush(bb);
        (new Thread(mSender)).start();
        bb.clear();
    }

    public void sendBytes(byte[] bytes, int offset, int length){
        ByteBuffer bb = Wrapper.wrap(Wrapper.HEAD_TAG_RAW_BYTES, bytes, offset, length);
        mSender.flush(bb);
        (new Thread(mSender)).start();
        bb.clear();
    }

/* ********************************************************************************************** */

    class Listener implements Runnable {

        private final List<Callback> mCallbackList = new ArrayList<>();
        private final List<Handler> mHandlerList = new ArrayList<>();

        ByteBuffer mBuffer = ByteBuffer.allocate(Wrapper.MAX_BUFFER_BYTES);
        private volatile boolean mStop = false;

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
                ssc.socket().bind(new InetSocketAddress(SOCKET_MESSENGER_PORT));
                while (!mStop) { // Listener always alive !
                    SocketChannel sc = ssc.accept();
                    Log.v(TAG, ":Listener: accept= " + sc.socket().toString());
                    int count;
                    do {
                        mBuffer.clear();
                        count = sc.read(mBuffer);
                        if (count > 0) {
                            mBuffer.flip();
                            handleMessage(mBuffer);
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

        private void handleMessage(ByteBuffer bb) {
            int header  = Wrapper.unwrapHeader(bb);

            switch (header) {
                case Wrapper.HEAD_TAG_HEART_BEAT:
                    // discard here
                    Log.v(TAG, "HEART BEAT ...");
                    break;

                case Wrapper.HEAD_TAG_TEXT:
                    Log.v(TAG, "TEXT ...");
                    dispatch(Wrapper.cutContent(bb));
                    break;

                default:
                    Log.d(TAG, "unknown type");
                    break;
            }
        }

        private void dispatch(ByteBuffer bb){
            Log.v(TAG, "dispatch: callback=" + mCallbackList.size());
            for (Callback cb : mCallbackList){
                cb.onByteBuffer(bb.duplicate());
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


    class Sender implements Runnable {

        private String mHost;
        private ByteBuffer mBytesBuffer;

        private Sender(){}

        private void setHost(String host){
            mHost = host;
        }

        private void flush(ByteBuffer bb) {
            mBytesBuffer = bb.duplicate();
        }

        @Override
        public void run() {
            Log.v(TAG, ":Sender: running[" + mHost + "]");
            try {
                SocketChannel sc = SocketChannel.open();
                boolean connected = sc.connect(new InetSocketAddress(mHost, SOCKET_MESSENGER_PORT));
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

    static class Wrapper{
        private static final int HEADER_TAG_BYTES = 4; // header
        private static final int CONTENT_BYTES = 1024; // contents
        private static final int MAX_BUFFER_BYTES = 1028;

        private static final int HEAD_TAG_HEART_BEAT = 0xFF00;    // header + user ID information
        private static final int HEAD_TAG_TEXT = 0xFFA0;          // header + contents
        private static final int HEAD_TAG_RAW_BYTES = 0xFFEF;     // header + contents


        private static ByteBuffer wrap(int tag, byte[] bytes, int offset, int length){
            ByteBuffer bb;

            if (length > CONTENT_BYTES) {
                Log.v(TAG, "length error");
                return null;
            }

            bb = ByteBuffer.allocate(MAX_BUFFER_BYTES);
            bb.putInt(tag);
            bb.put(bytes, offset, length);
            bb.flip();

            return bb;
        }

        private static ByteBuffer wrap(String text){
            if ((text == null) || (text.length() == 0)){
                return null;
            }

            ByteBuffer bb = ByteBuffer.allocate(Wrapper.MAX_BUFFER_BYTES);
            bb.putInt(HEAD_TAG_TEXT);
            bb.put(text.getBytes());
            bb.flip();
            return bb;
        }

        private static ByteBuffer wrap(int value){
            ByteBuffer bb = ByteBuffer.allocate(Wrapper.HEADER_TAG_BYTES);
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
            int length = buffer.limit() - HEADER_TAG_BYTES;
            byte[] content = new byte[length];
            buffer.position(HEADER_TAG_BYTES);
            buffer.get(content);
            return content;
        }

        private static ByteBuffer cutContent(ByteBuffer buffer){
            int length = buffer.limit() - HEADER_TAG_BYTES;
            ByteBuffer bb = ByteBuffer.allocate(length);
            bb.put(unwrapContentBytes(buffer));
            bb.flip();
            return bb;
        }
    }


/* ********************************************************************************************** */

    public interface Callback {
        void onByteBuffer(ByteBuffer buffer);
    }
}
