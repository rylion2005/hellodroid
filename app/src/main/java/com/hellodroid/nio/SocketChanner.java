package com.hellodroid.nio;

/*
**
** REVISED HISTORY
**   yl7 | 18-2-11: Created
**     
*/


import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    private static final String HOST_ADDRESS = "192.168.1.100"; //"10.10.10.103";
    private static final int BUFFER_BYTES = 1028;
    private static final int MESSAGE_OUTGOING = 0xC1;
    private static final int MESSAGE_INCOMING = 0xC2;

    private List<String> mNeighborList = new ArrayList<>();
    private final Thread mAcceptThread = new Thread(new AcceptRunnable());
    private final Thread mConnectThread = new Thread(new ConnectRunnable());
    private final List<WriteRunnable> mWriteRunnableList = new ArrayList<>();
    private final List<ReadRunnable> mReadRunnableList = new ArrayList<>();

    private Selector mAcceptSelector;
    private Selector mReadSelector;
    private Selector mConnectSelector;
    private Selector mWriteSelector;
    private final Object mIncomingQueueLock = new Object();
    private final Object mOutgoingQueueLock = new Object();
    private final List<ByteBuffer> mOutgoingQueue = new ArrayList<>();
    private final List<ByteBuffer> mIncomingQueue = new ArrayList<>();

    private ChannelCallback  mCallback;


/* ********************************************************************************************** */


    public SocketChanner(){
        initSelectors();
    }

    public void registerCallback(ChannelCallback cb){
        mCallback = cb;
    }

    public void setNeighbors(ArrayList<String> neighbors){
        mNeighborList.addAll(neighbors);
        for (String ip : mNeighborList){
            Log.v(TAG, "IP:[" + ip + "]");
        }
    }

    public void start(){
        mAcceptThread.start();
        mConnectThread.start();
    }

    /*
    ** ---------------------------------------------------------------------------
    ** handleMessage
    **   prepare incoming events into incoming queue
    **
    ** @PARAM p1, IN:
    ** @PARAM p2, OUT:
    ** @RETURN boolean: true if success, otherwise false
    ** @PARAM : None
    ** @RETURN: None
    **
    ** NOTES:
    **   ......
    **
    ** ---------------------------------------------------------------------------
    */
    @Override
    public void handleMessage(Message msg) {
        //Log.v(TAG, "handleMessage: " + msg.what);
        switch (msg.what) {
            case MESSAGE_INCOMING:
                //synchronized (mIncomingQueueLock){
                    Log.v(TAG, "ReadRunnableList: " + mReadRunnableList.size());
                    for (ReadRunnable rr : mReadRunnableList){
                        Thread r = new Thread(rr);
                        r.start();
                    }
                //}
                break;

            case MESSAGE_OUTGOING:
                synchronized (mOutgoingQueueLock) {
                    Log.v(TAG, "WriteRunnableList: " + mWriteRunnableList.size());
                    for (WriteRunnable wr : mWriteRunnableList) {
                        wr.copyQueue(mOutgoingQueue);
                        Thread w = new Thread(wr);
                        w.start();
                    }
                    mOutgoingQueue.clear();
                }
                break;

            default:
                break;
        }
    }

    public void sendText(String shortText){
        int length = shortText.length();
        ByteBuffer bb = ByteBuffer.allocate(length);
        bb.put(shortText.getBytes());
        //Log.v(TAG, "bb: " + bb);
        bb.flip();
        //Log.v(TAG, "bb: " + bb);
        if (bb.remaining() > 0) {
            enqueueOutgoings(bb);
        }
        bb.clear();
    }

/* ********************************************************************************************** */

    private void initSelectors(){
        try {
            mAcceptSelector = Selector.open();
            mReadSelector = Selector.open();
            mConnectSelector = Selector.open();
            mWriteSelector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void enqueueIncomings(ByteBuffer bb){
        Log.v(TAG, "enqueue incoming");
        bb.flip();
        Log.v(TAG, "Buffer: " + bb);
        Log.v(TAG, "remaining: " + bb.remaining());
        //bb.get(bytes);
        //Log.v(TAG, "Bytes: " + new String(bytes));
        if (mCallback != null){
            mCallback.onReadEvent(bb);
        }
    }

    private void enqueueOutgoings(ByteBuffer bb){
        Log.v(TAG, "enqueueOutgoings");
        synchronized (mOutgoingQueueLock){
            mOutgoingQueue.add(bb);
        }

        sendEmptyMessage(MESSAGE_OUTGOING);
    }


/* ********************************************************************************************** */

    class AcceptRunnable implements Runnable {

        private AcceptRunnable() {
        }

        @Override
        public void run() {
            try {
                Log.v(TAG, ":Accept: running");
                ServerSocketChannel ssc = ServerSocketChannel.open();
                ssc.socket().bind(new InetSocketAddress(SOCKET_PORT));
                ssc.configureBlocking(false);
                ssc.register(mAcceptSelector, SelectionKey.OP_ACCEPT);

                while (true) {
                    doAccept(ssc);
                }

            } catch (IOException e){
                e.printStackTrace();
            }
        }

        private void doAccept(ServerSocketChannel ssc){
            //Log.v(TAG, "doAccept");
            try {
                int num = mAcceptSelector.select();
                //Log.v(TAG, ":Accept: keys = " + num);
                Set<SelectionKey> keys = mAcceptSelector.selectedKeys();
                for (SelectionKey sk : keys) {
                    if (sk.isValid() && sk.isAcceptable()) {
                        SocketChannel sc = ssc.accept();
                        if (sc!=null) {
                            Log.v(TAG, ":Accept: connected to " + sc.socket().toString());
                            ReadRunnable rr = new ReadRunnable(sc);
                            mReadRunnableList.add(rr); //TODO: lock
                            sendEmptyMessage(MESSAGE_INCOMING);
                        }
                    }
                    sk.cancel();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private boolean hasReadRunnable(SocketChannel sc){
            boolean has = false;
            for ( ReadRunnable rr : mReadRunnableList ) {
                String host = rr.getSocketChannel().socket().getRemoteSocketAddress().toString();
                String newHost = sc.socket().getRemoteSocketAddress().toString();
                if (host.equals(newHost)){
                    Log.v(TAG, "find a same ReadRunnable");
                    has = true;
                    break;
                }
            }
            return has;
        }
    }

    class ReadRunnable implements Runnable {
        private SocketChannel mSocketChannel;
        //private final List<ByteBuffer> mBufferQueue = new ArrayList<>();

        private ReadRunnable(SocketChannel sc){
            try {
                mSocketChannel = sc;
                mSocketChannel.configureBlocking(false);
                mSocketChannel.register(mReadSelector, SelectionKey.OP_READ);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            Log.v(TAG, ":READ: running");
            while (true) {
                doRead();
            }
        }

        private void doRead(){
            Log.v(TAG, "doRead");
            try {
                int num = mReadSelector.select();
                Log.v(TAG, "read keys: " + num);
                Set<SelectionKey> keys = mReadSelector.selectedKeys();
                Log.v(TAG, "keyset : " + keys.size());
                for (SelectionKey sk : keys){
                    if (sk.isValid() && sk.isReadable()) {
                        SocketChannel sc = (SocketChannel) sk.channel();
                        Log.v(TAG, "read channel: " + sc.socket().toString());
                        ByteBuffer bb = ByteBuffer.allocate(BUFFER_BYTES);
                        int count = sc.read(bb);
                        enqueueIncomings(bb);
                        bb.clear();
                        sk.cancel();
                    }
                }
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        private SocketChannel getSocketChannel(){
            return mSocketChannel;
        }
    }

    class ConnectRunnable implements Runnable {

        @Override
        public void run() {
            Log.v(TAG, ":Connect: running");
            while (true) {
                try {
                    doConnect();
                    Thread.sleep(6000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void doConnect(){
            //Log.v(TAG, "doConnect: " + mNeighborList.size());
            // try to connect all possible neighbors
            for (String ip : mNeighborList){
                try {
                    SocketChannel sc = SocketChannel.open();
                    sc.configureBlocking(false);
                    sc.connect(new InetSocketAddress(ip, SOCKET_PORT));
                    sc.register(mConnectSelector, SelectionKey.OP_CONNECT);

                    //Log.v(TAG, ":Connect: connecting server");
                    int num = mConnectSelector.select(1000);
                    if (num > 0) {
                        //Log.v(TAG, ":Connect: connected = " + num);
                        Set<SelectionKey> keys = mConnectSelector.selectedKeys();
                        //Log.v(TAG, "SelectionKey Set: " + keys.size());
                        for (SelectionKey sk : keys) {
                            if (sk.isValid() && sk.isConnectable()) {
                                SocketChannel ch = (SocketChannel) sk.channel();
                                if (ch.finishConnect()) {
                                    if (!hasWriteRunnable(ch)) {
                                        Log.v(TAG, ":connect: !!! connected to " + ch.socket().toString());
                                        WriteRunnable wr = new WriteRunnable(ch);
                                        mWriteRunnableList.add(wr); // TODO: add lock or synchronized
                                    }
                                }
                                sk.cancel();
                            }
                        }
                    }
                } catch (IOException e) {
                    //Log.v(TAG, ":Connect: IOException");
                    //e.printStackTrace();
                }
            }
            // TODO: mNeighborList.clear();
        }

        private boolean hasWriteRunnable(SocketChannel sc){
            boolean has = false;
            for ( WriteRunnable wr : mWriteRunnableList ) {
                String host = wr.getSocketChannel().socket().getRemoteSocketAddress().toString();
                String newHost = sc.socket().getRemoteSocketAddress().toString();
                if (host.equals(newHost)){
                    //Log.v(TAG, "find a same WriteRunnable");
                    has = true;
                    break;
                }
            }
            return has;
        }
    }

    class WriteRunnable implements Runnable {
        private final List<ByteBuffer> mBufferQueue = new ArrayList<>();
        private SocketChannel mChannel;

        private WriteRunnable (SocketChannel sc){
            try {
                mChannel = sc;
                mChannel.configureBlocking(false);
                mChannel.register(mWriteSelector, SelectionKey.OP_WRITE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private SocketChannel getSocketChannel(){
            return mChannel;
        }

        private void copyQueue(List<ByteBuffer> source){
            mBufferQueue.addAll(source);
        }

        @Override
        public void run() {
            Log.v(TAG, ":WriteRunnable: running");
            doWrite();
        }

        private void doWrite(){
            Log.v(TAG, ":Write: doWrite");
            try {
                int num = mWriteSelector.select(200);
                Set<SelectionKey> keys = mWriteSelector.selectedKeys();
                for (SelectionKey sk : keys){
                    if (sk.isWritable()) {
                        SocketChannel sc = (SocketChannel) sk.channel();
                        Log.v(TAG, "Channel: " + sc.socket().toString());
                        Log.v(TAG, "Buffer: " + mBufferQueue.size());
                        for (ByteBuffer bb : mBufferQueue) {
                            //bb.flip();
                            Log.v(TAG, "write: " + bb);
                            while (bb.remaining() > 0) {
                                Log.v(TAG, "while write: " + bb.remaining());
                                sc.write(bb);
                            }
                        }
                        mBufferQueue.clear();
                        sk.cancel();
                    }
                }
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    abstract public interface ChannelCallback {
        abstract void onReadEvent(ByteBuffer bb);
    }

}
