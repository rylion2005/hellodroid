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
import java.util.ArrayList;
import java.net.Socket;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/*
** *************************************************************************************************
**
** SocketListener
**   This is a p2p socket class.
**
**   Default port is defined by SOCKET_PORT, if there is conflict with other port, we will switch to
**     SOCKET_BACKUP_PORT.
**
**   It can take as server socket or client socket by configuring mIsServer.
**   In default it is a server socket.
**
**   There are two sub threads: sender and receiver.
**     For server socket, only receiver thread is started and running
**     For client socket, only sender thread are started and running.
**
**
** USAGE:
**                 |-- Sender   <--......--> Receiver --|
**    Client  <==> |                                    | <==> Server
**                 |-- Receiver <--......-->   Sender --|
**
** *************************************************************************************************
*/
public class SocketListener extends Handler {
    public static final String TAG = "SocketListener";

    private static int RESULT_ERROR_UNKOWN = -1;
    private static int RESULT_SUCCESS = 0;

    // socket related
    private final int SOCKET_PORT = 9876;
    private final int SOCKET_BACKUP_PORT = 8765;
    private final int SOCKET_BUFFER_BYTES = (1 * 1024);

    private final int REQUEST_SEND_BYTES = 0xDA;     // sender message
    private final int INDICATE_RECEIVE_BYTES = 0xDB; // receiver message
    private final String BUNDLE_KEY_SEND_MESSAGE = "MessageToSend";
    private final String BUNDLE_KEY_RECEIVE_MESSAGE = "MessageReceived";

    // Socket Message data header type
    private final int MESSAGE_HEADER_TEXT = 0xC;    // 'C'
    private final int MESSAGE_HEADER_FILE = 0xF;    // 'F'
    private final int MESSAGE_HEADER_ENDING = 0xE;  // 'E'
    private final int MESSAGE_HEADER_TEST = 0x0;    // 'T'
    // Server will be ready to send data to client.
    private final int MESSAGE_HEADER_SEND_READY = 0xB;

    // Timer releated
    private final long TIMER_PERIOD = 10 * 1000;
    private final int THREAD_SLEEP_TIME = 10 * 1000;

    private Context mContext;
    private final List<Handler> mHandlerList = new ArrayList<>();

    private Socket mSocket;
    private ServerSocket mServerSocket;

    private Sender mSender;
    private Thread mSenderThread;
    private Receiver mReceiver;
    private Thread mReceiverThread;
    private Timer mTimer;
    private TimerTask mTimerTask;

    // indicator if this socket is a server socket
    private boolean mIsServer = true;
    private String mIpAddress;


/* ============================================================================================== */


    /*
    ** ---------------------------------------------------------------------------
    ** SocketListener
    **   Constructor
    **
    ** @PARAM context, IN:
    ** @PARAM handler, IN: handler which notify socket events to client
    ** @RETURN: None
    **
    ** NOTES:
    **   ......
    **
    ** ---------------------------------------------------------------------------
    */
    public SocketListener(Context context){
        mContext = context;
        mSender = new Sender();
        mSenderThread = new Thread(mSender);
        mReceiver = new Receiver();
        mReceiverThread = new Thread(mReceiver);
    }

    public void registerHandler(Handler handler){
        mHandlerList.add(handler);
    }

    public void becomeAsServer(){
        Log.v(TAG, "becomeAsServer");
        mIsServer = true;
        stopThread(mSenderThread);
        restartThread(mReceiverThread);

        cancelTimerTask();
    }

    public void becomeAsClient(){
        Log.v(TAG, "becomeAsClient");
        mIsServer = false;
        restartThread(mSenderThread);
        stopThread(mReceiverThread);

        cancelTimerTask();
        restartTimerTask();
    }

    public void setIpAddress(String ip){
        mIpAddress = ip;
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
    **  -----------------     -------------     --------------     -------------------
    **  | FILE HEAD TAG | ==> | FILE NAME | ==> | FILE BYTES | ==> | FILE ENDING TAG |
    **  -----------------     -------------     --------------     -------------------
    **
    ** ---------------------------------------------------------------------------
    */
    public int sendFile(String fileName){

        int result = RESULT_ERROR_UNKOWN;
        byte[] bytes;
        FileInputStream fis;

        if ((fileName == null) || (fileName.length() == 0)) {
            return -1;
        }

        // Send TAG for file
        bytes = new byte[1];
        bytes[0] = (byte) MESSAGE_HEADER_FILE;
        mSender.enqueue(bytes, 1);

        // send file name
        bytes = fileName.getBytes();
        mSender.enqueue(bytes, bytes.length);

        // send data bytes in file
        try {
            fis = mContext.openFileInput(fileName);
            if (fis.available() > 0) {
                while (true) {
                    // send bytes
                    if (fis.available() > 0) {
                        byte[] sendBytes = new byte[SOCKET_BUFFER_BYTES];
                        int readCounts = fis.read(sendBytes, 0, SOCKET_BUFFER_BYTES);
                        mSender.enqueue(sendBytes, readCounts);
                    } else {
                        // send ending tag
                        Log.e(TAG, "Send Ending");
                        bytes = new byte[1];
                        bytes[0] = (byte) MESSAGE_HEADER_ENDING;
                        mSender.enqueue(bytes, 1);
                        break;
                    }
                }
                result = RESULT_SUCCESS;
            } else {
                result = RESULT_ERROR_UNKOWN;
            }
            fis.close();
        } catch (IOException e) {
            result = RESULT_ERROR_UNKOWN;
        } catch (NullPointerException e) {
            result = RESULT_ERROR_UNKOWN;
        }

        return result;
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
    public int sendText(String text) {
        int result = RESULT_ERROR_UNKOWN;
        byte[] bytes;

        if ((text == null) || (text.length() == 0)) {
            return RESULT_ERROR_UNKOWN;
        }

        // Send head TAG
        bytes = new byte[1];
        bytes[0] = (byte) MESSAGE_HEADER_TEXT;
        mSender.enqueue(bytes, 1);

        // Send bytes
        bytes = text.getBytes();
        mSender.enqueue(bytes, bytes.length);

        // Send ending
        bytes = new byte[1];
        bytes[0] = (byte) MESSAGE_HEADER_ENDING;
        mSender.enqueue(bytes, 1);

        return result;
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

    private void cancelTimerTask(){
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
        mTimer.schedule(mTimerTask, 0, TIMER_PERIOD);
    }

    private Socket prepareSocket(boolean isServer){
        Log.v(TAG, "prepareSocket: " + isServer);
        Socket s = null;
        try{
            if (isServer){
                ServerSocket ss = new ServerSocket(SOCKET_PORT);
                ss.setReceiveBufferSize(SOCKET_BUFFER_BYTES);
                //ss.setSoTimeout(THREAD_SLEEP_TIME);
                s = ss.accept();

                // FIXME: 18-2-7 Here is a fatal issue: who will close this server socket??
            } else {
                setIpAddress("10.10.10.103");
                s = new Socket();
                SocketAddress a = new InetSocketAddress(mIpAddress, SOCKET_PORT);
                if (s != null) {
                    s.connect(a, THREAD_SLEEP_TIME);
                    s.setSoTimeout(THREAD_SLEEP_TIME);
                    s.setReceiveBufferSize(SOCKET_BUFFER_BYTES);
                    s.setSendBufferSize(SOCKET_BUFFER_BYTES);
                }
            }
        }catch (IOException e){
            Log.e(TAG, "prepareSocket: failed");
            //e.printStackTrace();
        }
        return s;
    }

    private void prepareSocket2(boolean isServer){
        Log.v(TAG, "prepareSocket: " + isServer);

        // server to client
        // server to server
        // client to server
        // client to client

        try{
            if (isServer){
                if (mServerSocket == null){
                    mServerSocket = new ServerSocket(SOCKET_PORT);
                    mServerSocket.setReceiveBufferSize(SOCKET_BUFFER_BYTES);
                    //mServerSocket.setSoTimeout(THREAD_SLEEP_TIME);
                    if (mSocket != null) {
                        // It can be a client socket just before. we must close and free it.
                        mSocket.close();
                        mSocket = null;
                    }
                    mSocket = mServerSocket.accept();
                }
            } else {
                // if it is a server socket just before, we must close and free it.
                if (mServerSocket != null){
                    if (mSocket != null){
                        mSocket.close();
                        mSocket = null;
                    }
                    mServerSocket.close();
                    mServerSocket = null;
                }

                if (mSocket == null) {
                    setIpAddress("10.10.10.103");
                    mSocket = new Socket();
                    SocketAddress a = new InetSocketAddress(mIpAddress, SOCKET_PORT);
                    if (mSocket != null) {
                        mSocket.connect(a, THREAD_SLEEP_TIME);
                        mSocket.setSoTimeout(THREAD_SLEEP_TIME);
                        mSocket.setReceiveBufferSize(SOCKET_BUFFER_BYTES);
                        mSocket.setSendBufferSize(SOCKET_BUFFER_BYTES);
                    }
                }
            }
        }catch (IOException e){
            Log.e(TAG, "prepareSocket: failed");
            //e.printStackTrace();
        }
    }

    /*
    ** ---------------------------------------------------------------------------
    ** heartBeat
    **   client socket is used to query server socket in order to keep connection
    **
    ** @PARAM : None
    ** @RETURN: None
    **
    ** NOTES:
    **   ......
    **
    ** ---------------------------------------------------------------------------
    */
    private void heartBeat(){
        Log.v(TAG, "heart beat ......");
        byte[] bytes = new byte[1];
        bytes[0] = (byte) MESSAGE_HEADER_TEST;
        mSender.enqueue(bytes, 1);
    }


/* ============================================================================================== */

    /*
    ** ********************************************************************************
    **
    ** Receiver
    **   Socket Receive Runnable implementation
    **
    ** USAGE:
    **   This receiver receive socket data
    **
    ** ********************************************************************************
    */

    class Receiver extends Handler implements Runnable{

        private final int STATE_READY = 0xD11;
        private final int STATE_WAIT_FILE_NAME = 0xD12;
        private final int STATE_WAIT_ENDING = 0xD13;
        private final int STATE_WAIT_TEXT = 0xD14;
        private final int STATE_TEST = 0xD15;

        private int mState;

        public Receiver(){
            mState = STATE_READY;
        }

        @Override
        public void run(){
            Log.v(TAG, "Receiver: running");
            while (true){
                try {
                    Socket s = prepareSocket(mIsServer);
                    if (s != null) {
                        read(s);
                        s.close();
                    } else {
                        Thread.sleep(THREAD_SLEEP_TIME);
                    }
                } catch (IOException e) {
                    Log.v(TAG, "socket fail");
                    //e.printStackTrace();
                } catch (InterruptedException e) {
                    Log.v(TAG, " receiver sleep error");
                }
            }
        }

        @Override
        public void handleMessage(Message msg) {
            Log.v(TAG, "handleMessage: " + msg);
            switch (msg.what){
                case INDICATE_RECEIVE_BYTES:
                    Bundle b = msg.getData();
                    byte[] d = b.getByteArray(BUNDLE_KEY_RECEIVE_MESSAGE);
                    handleState(d, d.length);
                    break;

                default:
                    break;
            }
        }

        private void read(Socket s){
            if (s == null){
                return;
            }
            Log.v(TAG, "read socket: " + s);
            try {
                InputStream is = s.getInputStream();
                while (true) {
                    if (is.available() > 0) {
                        byte[] bytes = new byte[SOCKET_BUFFER_BYTES];
                        is.read(bytes);
                        enqueue(bytes, is.available());
                    } else {
                        is.close();
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void enqueue(byte[] bytes, int length){
            Log.v(TAG, "enqueue: " + length);
            Message m = new Message();
            m.what = INDICATE_RECEIVE_BYTES;
            Bundle b = new Bundle(length);
            b.putByteArray(BUNDLE_KEY_RECEIVE_MESSAGE, bytes);
            m.setData(b);
            sendMessage(m);
        }

        private void handleState(byte[] bs, int len){
            Log.d(TAG, mState + ", counts: " + len);
            switch(mState){
                case STATE_READY:
                    if(len == 1){
                        switch(bs[0]){
                            case MESSAGE_HEADER_FILE:
                                Log.e(TAG, "FILE ...");
                                mState = STATE_WAIT_FILE_NAME;
                                break;

                            case MESSAGE_HEADER_TEXT:
                                Log.e(TAG, "TEXT ...");
                                mState = STATE_WAIT_TEXT;
                                handleCommand(bs, len);
                                break;

                            case MESSAGE_HEADER_TEST:
                                break;

                            default:
                                Log.e(TAG, "Don't know 1st byte!");
                                break;
                        }
                    }
                    break;

                case STATE_WAIT_FILE_NAME:
                    mState = STATE_WAIT_ENDING;
                    break;

                case STATE_WAIT_ENDING:
                    if(len > 1){
                        mState = STATE_WAIT_ENDING;
                    }

                    if((len == 1) && (bs[0] == MESSAGE_HEADER_ENDING)){
                        mState = STATE_READY;
                    }
                    break;

                case STATE_TEST:
                    Log.e(TAG, "TEST nothing");
                    mState = STATE_READY;
                    break;

                default:
                    break;
            }
            Log.e(TAG, "next state ==> " + mState);
        }

        private void handleCommand(byte[] bytes, int len){
            String command = new String(bytes);
            // TODO:
        }

        private void notifyMessageReceived(byte[] bytes){
            for (Handler h : mHandlerList){
                Message msg = new Message();
                Bundle b = new Bundle();
                b.putByteArray("BroadcastMessage", bytes);
                msg.setData(b);
                h.sendMessage(msg);
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
    class Sender extends Handler implements Runnable {

        private final List<byte[]> mSendQueue = new ArrayList<>();
        private final Object mLock = new Object();

        @Override
        public void run() {
            Log.v(TAG, "Sender: running");
            while (true) {
                synchronized (mLock) {
                    try {
                        mLock.wait();
                        Log.v(TAG, "sender awake");
                        Socket s = prepareSocket(mIsServer);
                        if (s != null) {
                            flush(s);
                            s.close();
                        } else {
                            Thread.sleep(THREAD_SLEEP_TIME);
                        }
                    } catch (IOException e) {
                        //e.printStackTrace();
                        Log.e(TAG, "Socket failed");
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Synchronized lock exception !!!");
                        //e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void handleMessage(Message msg) {
            Log.v(TAG, "handleMessage: " + msg);
            switch (msg.what){
                case REQUEST_SEND_BYTES:
                    Bundle b = msg.getData();
                    byte[] d = b.getByteArray(BUNDLE_KEY_SEND_MESSAGE);
                    synchronized (mLock){
                        mSendQueue.add(d);
                        Log.v(TAG, "wake up sender");
                        mLock.notify();
                    }
                    break;

                default:
                    break;
            }
        }

        public void enqueue(byte[] bytes, int length){
            Log.v(TAG, "enqueue: " + length);
            Message m = new Message();
            m.what = REQUEST_SEND_BYTES;
            Bundle b = new Bundle(length);
            b.putByteArray(BUNDLE_KEY_SEND_MESSAGE, bytes);
            m.setData(b);
            sendMessage(m);
        }

        /*
        ** ---------------------------------------------------------------------------
        ** flush
        **   flush the whole queue to socket
        **
        ** @PARAM : None
        ** @RETURN: None
        **
        ** NOTES:
        **   ......
        **
        ** ---------------------------------------------------------------------------
        */
        private void flush(Socket s){

            if (s == null){
                return;
            }

            Log.v(TAG, "flush socket: " + s);
            try {
                for (int i = 0; i < mSendQueue.size(); i++) {
                    OutputStream os = s.getOutputStream();
                    os.write(mSendQueue.get(i));
                    os.close();
                }
                mSendQueue.clear();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    class HeartBeatTask extends TimerTask{

        @Override
        public void run() {
            heartBeat();
        }
    }

    interface SocketMessageCallBack {
        abstract public void onMessageReceived(Message message);
    }
}
