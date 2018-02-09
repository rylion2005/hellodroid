package com.hellodroid.socket;


import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.net.Socket;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/*
** *************************************************************************************************
**
** SocketListener
**   This is a p2p socket class which support one pair of client-server.
**
**   Default port is defined by SOCKET_PORT, if there is conflict with other port, we will switch to
**     SOCKET_BACKUP_PORT.
**
**   It can take as server socket or client socket by configuring mIsServer.
**   In default it is a server socket.
**
**   There are three sub threads: sender and receiver.
**     For server socket, only receiver thread is started and running
**     For client socket, only sender thread are started and running.
**
**   For client socket, a timer task is used to keep heart-beat connection with server.
**
** USAGE:
**
**    Client  <==> |-- Sender   <-- -->   Receiver --| <==> Server
**
**
** *************************************************************************************************
*/
public class SocketListener extends Handler {
    public static final String TAG = "SocketListener";

    // socket related
    private final int SOCKET_PORT = 9876;
    private final int SOCKET_BACKUP_PORT = 8765;

    // Timer for heart beat
    private final long TIMER_PERIOD = 60 * 1000; // 60 seconds
    private final long TIMER_DELAY = 6000;       // 6 seconds

    private Context mContext;
    private CallBack mCallBack;

    // Threads and timer
    private Sender mSender;
    private Thread mSenderThread;
    private Receiver mReceiver;
    private Thread mReceiverThread;
    private Timer mTimer;
    private TimerTask mTimerTask;

    // indicator if this socket is a server socket
    private boolean mIsServer = true;

    private String mIpAddress = "10.10.10.103";  // Test host IP


/* ============================================================================================== */


    public SocketListener(Context context){
        mContext = context;
        mSender = new Sender();
        mSenderThread = new Thread(mSender);
        mReceiver = new Receiver();
        mReceiverThread = new Thread(mReceiver);

        //restartThread(mReceiverThread);
    }

    public void becomeAsServer(){
        //if (!mIsServer){
            Log.v(TAG, "becomeAsServer");
            mIsServer = true;
            stopThread(mSenderThread);
            restartThread(mReceiverThread);
            stopTimerTask();
        //}
    }

    public void becomeAsClient(){
        //if (mIsServer){
            Log.v(TAG, "becomeAsClient");
            mIsServer = false;
            restartThread(mSenderThread);
            stopThread(mReceiverThread);
            stopTimerTask();
            restartTimerTask();
        //}
    }

    public void setIpAddress(String ip){
        mIpAddress = ip;
    }

    public void registerCallback(CallBack cb){
        mCallBack = cb;
    }

    /*
    ** ---------------------------------------------------------------------------
    ** sendFile
    **   send file to socket.
    **   This file MUST come from app file directory, not SD card
    **
    ** @PARAM fileName, IN: file name with all-path
    ** @RETURN int: >=0 if success, otherwise -1
    **
    ** NOTES:
    **   we send file with a state machine
    **
    **  ------------     -------------     ------------     --------------
    **  | HEAD TAG | ==> | FILE NAME | ==> | CONTENTS | ==> | ENDING TAG |
    **  ------------     -------------     ------------     --------------
    **
    ** ---------------------------------------------------------------------------
    */
    public void sendFile(String fileName){
        if ((fileName == null) || (fileName.length() == 0)) {
            return;
        }

        // send file name
        mSender.enqueue(BytesWrapper.MSG_TYPE_FILE_NAME, fileName.getBytes(), fileName.length());

        // send data bytes in file
        try {
            FileInputStream fis = mContext.openFileInput(fileName);
            if (fis.available() > 0) {
                while (true) {
                    // send bytes
                    if (fis.available() > 0) {
                        byte[] sendBytes = new byte[BytesWrapper.MAX_MESSAGE_LENGTH];
                        int readCounts = fis.read(sendBytes, 0, BytesWrapper.MAX_MESSAGE_LENGTH);
                        mSender.enqueue(BytesWrapper.MSG_TYPE_FILE_CONTENT, sendBytes, readCounts);
                    } else {
                        // send ending tag
                        mSender.enqueue(BytesWrapper.MSG_TYPE_FILE_ENDING, null, 0);
                        break;
                    }
                }
            }
            fis.close();
        } catch (IOException e) {
            // TODO
        } catch (NullPointerException e) {
            // TODO
        }
    }

    /*
    ** ---------------------------------------------------------------------------
    ** sendText
    **   send a text message by socket
    **
    ** @PARAM text, IN: text to be sent
    ** @RETURN int: >=0 if success, otherwise -1
    **
    ** NOTES:
    **   we send text with a state machine
    **
    **  ------------     --------------     --------------
    **  | HEAD TAG | ==> | TEXT BYTES | ==> | ENDING TAG |
    **  ------------     --------------     --------------
    **
    ** ---------------------------------------------------------------------------
    */
    public void sendText(String text) {
        try {
            mSender.enqueue(BytesWrapper.MSG_TYPE_TEXT, text.getBytes(), text.length());
        } catch (NullPointerException e) {
            Log.v(TAG, "text has nothing");
        }
    }


/* ============================================================================================== */


    private void restartThread(Thread thread){
        if (!thread.isInterrupted()) {
            thread.interrupt();
        }
        thread.start();
    }

    private void stopThread(Thread thread){
        if (!thread.isInterrupted()) {
            thread.interrupt();
        }
    }


    private void stopTimerTask(){
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }

        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    private void restartTimerTask(){
        mTimer = new Timer();
        mTimerTask = new HeartBeatTask();
        mTimer.schedule(mTimerTask, TIMER_DELAY, TIMER_PERIOD);
    }

    private void heartBeat(){
        Log.v(TAG, "< -- heart beat -- >");
        final String msg = "Heart Beat";
        //mSender.enqueue(BytesWrapper.MSG_TYPE_HEART_BEAT, msg.getBytes(), msg.length());
        mSender.enqueue(BytesWrapper.MSG_TYPE_TEXT, msg.getBytes(), msg.length());
    }


/* ============================================================================================== */

    /*
    ** ********************************************************************************
    **
    ** Receiver
    **   Socket Receive Runnable implementation
    **   This class is only supported in server side now.
    **   It receives file and text message from client and do not send back any message.
    **
    ** USAGE:
    **   This receiver receive socket data
    **
    ** ********************************************************************************
    */
    class Receiver extends Handler implements Runnable{

        private final int MSG_RECEIVE_BYTES = 0xCF;
        private final int STATE_READY = 0xC0;
        private final int STATE_WAIT_ENDING = 0xCC;
        private int mState;

        private String mFileName;

        public Receiver(){
            mState = STATE_READY;
        }

        @Override
        public void run(){
            Log.v(TAG, "::Receiver:: running");
            while (true){
                waitAndRead();
            }
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_RECEIVE_BYTES){
                int length = msg.arg1;
                byte[] bytes = msg.getData().getByteArray("ReceivedBytes");
                ByteBuffer bb = BytesWrapper.fill(bytes, length);
                handleStateMachine(bb);
            }
        }

        /*
        ** ---------------------------------------------------------------------------
        ** read
        **   read active socket data and enqueue to client thread
        **
        ** @PARAM s, IN: active socket
        ** @RETURN: None
        **
        ** NOTES:
        **   ......
        **
        ** ---------------------------------------------------------------------------
        */
        private void waitAndRead(){
            try {
                ServerSocket ss = new ServerSocket(SOCKET_PORT);
                ss.setReceiveBufferSize(1028);
                //ss.setSoTimeout(THREAD_SLEEP_TIME);
                Log.v(TAG, "::Server:: wait client");
                Socket s = ss.accept();
                Log.v(TAG, "::Server:: client connected:" + s);
                InputStream is = s.getInputStream();
                while (true) {
                    if (is.available() > 0) {
                        Log.v(TAG, "available: " + is.available());
                        int counts = is.available();
                        byte[] bytes = new byte[counts];
                        int length = is.read(bytes);
                        dispatch(bytes, length);
                    } else {
                        break;
                    }
                }
                is.close();
                s.isClosed();
                ss.close();
            } catch (IOException e) {
                Log.e(TAG, "::Server:: IOException");
            }
        }

        private void dispatch(byte[] bytes, int length){
            Message msg = new Message();
            msg.what = MSG_RECEIVE_BYTES;
            msg.arg1 = length; // bytes length
            Bundle b = new Bundle(length);
            b.putByteArray("ReceivedBytes", bytes); // bytes data
            msg.setData(b);
            sendMessage(msg);
        }

        /*
        ** ---------------------------------------------------------------------------
        ** handleState
        **   receiver state machine center
        **   It will handle switch state machine and dispatch events to clients
        **
        **
        ** @PARAM bs, IN:
        ** @PARAM len, IN:
        ** @RETURN: None
        **
        ** NOTES:
        **   State machine:
        **
        **                    |--> FILE NAME --> FILE CONTENTS --> ENDING TAG |
        **   | HEAD TAG | ==> |                                               | ==> IDLE
        **                    |-->              TEXT CONTENTS                 |
        **
        ** ---------------------------------------------------------------------------
        */
        public void handleStateMachine(ByteBuffer buffer){
            Log.v(TAG, "current: " + mState);
            buffer.flip();
            int header = BytesWrapper.unwrapHeader(buffer);
            switch (mState){
                case STATE_READY:
                    switch (header){
                        case BytesWrapper.MSG_TYPE_HEART_BEAT:
                            Log.v(TAG, "Heart Beat ...");
                            mState = STATE_READY;
                            break;

                        case BytesWrapper.MSG_TYPE_FILE_NAME:
                            Log.v(TAG, "FILE name ...");
                            mFileName = new String(BytesWrapper.unwrapContents(buffer));
                            mState = STATE_WAIT_ENDING;
                            break;

                        case BytesWrapper.MSG_TYPE_TEXT:
                            Log.v(TAG, "TEXT ...");
                            handleCommand(new String(BytesWrapper.unwrapContents(buffer)));
                            mState = STATE_READY;
                            break;

                        default:
                            break;
                    }
                    break;

                case STATE_WAIT_ENDING:
                    if (header == BytesWrapper.MSG_TYPE_FILE_ENDING){
                        Log.v(TAG, "file ending");
                        mState = STATE_READY;
                        handleFile(mFileName);
                    } else {
                        Log.v(TAG, "file contents");
                        // TODO: process file contents
                    }
                    break;

                default:
                    break;
            }

            Log.v(TAG, "next: " + mState);
        }


        private void handleCommand(String command){
            // TODO:
            Log.v(TAG, "COMMAND: " + command);

            if (mCallBack != null){
                mCallBack.onMessageReceived(command);
            }
        }


        private void handleFile(String fileName){
            Log.v(TAG, "handleFile: " + fileName);
            // TODO:

            if (mCallBack != null){
                mCallBack.onFileReceived(fileName);
            }
        }
    }

    /*
    ** ********************************************************************************
    **
    ** Sender
    **   This class sends socket data.
    **   This class can send byte data and file by byte data
    **
    ** USAGE:
    **   ......
    **
    ** ********************************************************************************
    */
    class Sender implements Runnable {

        private final List<ByteBuffer> mOutQueue = new ArrayList<>();
        private final Object mSignal = new Object();
        private final Object mLock = new Object();

        @Override
        public void run() {
            Log.v(TAG, "::Sender:: running");
            while (true) {
                try {
                    synchronized (mSignal) {
                        Log.v(TAG, "::Signal:: wait awake signal ...");
                        mSignal.wait();
                        Log.v(TAG, "::Signal:: awake !");
                    }

                    synchronized (mLock) {
                        flush();
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, ":Client: InterruptedException");
                    //e.printStackTrace();
                }
            }
        }

        public void enqueue(int type, byte[] bytes, int length){
            ByteBuffer bb = BytesWrapper.wrap(type, bytes, length);
            synchronized (mLock) {
                mOutQueue.add(bb);
            }

            synchronized (mSignal) {
                Log.v(TAG, "wake up sender!");
                mSignal.notify();
            }
        }

        private void flush(){
            try {
                // we send message one by one !
                for (ByteBuffer bb : mOutQueue) {

                    bb.flip();
                    byte[] bytes = new byte[bb.remaining()];
                    bb.get(bytes);

                    Socket s = new Socket();
                    SocketAddress a = new InetSocketAddress(mIpAddress, SOCKET_PORT);
                    s.connect(a, 30000);
                    s.setSoTimeout(30000);
                    s.setReceiveBufferSize(BytesWrapper.MAX_MESSAGE_LENGTH);
                    s.setSendBufferSize(BytesWrapper.MAX_MESSAGE_LENGTH);

                    OutputStream os = s.getOutputStream();
                    os.write(bytes);
                    os.close();
                    s.close();
                }
                mOutQueue.clear();
            } catch (IOException e) {
                // TODO: here is a connection fail state, we should notify to client.
                Log.e(TAG, "::Client:: IOException");
            }
        }
    }

    static class BytesWrapper{
        private static final int MESSAGE_HEADER_LENGTH = 4;
        private static final int MAX_MESSAGE_LENGTH = 1028; // header + contents

        private static final int MSG_TYPE_HEART_BEAT = 0xFF00;
        private static final int MSG_TYPE_TEXT = 0xFFA0;
        private static final int MSG_TYPE_FILE_NAME = 0xFFB0;
        private static final int MSG_TYPE_FILE_CONTENT = 0xFFBA;
        private static final int MSG_TYPE_FILE_ENDING = 0xFFBB;

        private static ByteBuffer fill(byte[] bytes, int length){
            ByteBuffer bb = ByteBuffer.allocate(length);
            bb.put(bytes);
            return bb;
        }

        private static ByteBuffer wrap(int type, byte[] bytes, int length){
            ByteBuffer bb = ByteBuffer.allocate(length + MESSAGE_HEADER_LENGTH);

            // for file content, we do put header
            if (type != MSG_TYPE_FILE_CONTENT) {
                bb.putInt(type);
            }

            if (bytes != null) {
                bb.put(bytes);
            }
            return bb;
        }

        private static int unwrapHeader(ByteBuffer buffer){
            return buffer.getInt(0);
        }

        public static byte[] unwrapContents(ByteBuffer buffer){
            int length = buffer.limit() - MESSAGE_HEADER_LENGTH;
            byte[] content = new byte[length];
            buffer.position(MESSAGE_HEADER_LENGTH);
            buffer.get(content);
            buffer.clear();
            return content;
        }
    }

    class HeartBeatTask extends TimerTask{

        @Override
        public void run() {
            heartBeat();
        }
    }

    public interface CallBack {
        abstract public void onMessageReceived(String text);
        abstract public void onFileReceived(String fileName);
    }
}
