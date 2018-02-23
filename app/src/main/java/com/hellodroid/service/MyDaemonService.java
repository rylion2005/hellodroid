package com.hellodroid.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import com.hellodroid.audio.MyAudioRecorder;
import com.hellodroid.audio.MyAudioTracker;
import com.hellodroid.lan.LanScanner;
import com.hellodroid.lan.Neighbor;
import com.hellodroid.nio.SocketChanner;



/*
** ********************************************************************************
**
** MyDaemonService
**   This is a daemon service which processes global events for application
**   It will be always running.
**
** USAGE:
**   ......
**
** ********************************************************************************
*/
public class MyDaemonService extends Service {
    private static final String TAG = "MyDaemonService";

    // final objects
    private final SocketChanner mSocketChanner = SocketChanner.newInstance();
    private final Neighbor mNeighbors = Neighbor.newInstance();
    private final MyAudioRecorder mAudioRecord = MyAudioRecorder.newInstance();
    private final MyAudioTracker mAudioTrack = MyAudioTracker.newInstance();
    private final LanScanner mLanScanner = LanScanner.newInstance();

    private final String mFileName = "audio.pcm";
    private FileOutputStream mFos;


/* ********************************************************************************************** */


    public MyDaemonService() {
        Log.v(TAG, "new an instance");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");

        initLanScanner();
        initSocket();
        initAudio();
        initNeighbors();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.v(TAG, "onRebind: " + intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(TAG, "onUnbind: " + intent);
        return super.onUnbind(intent);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy()");
        super.onDestroy();
    }

/* ********************************************************************************************** */

    // Service APIs




/* ********************************************************************************************** */
    // SocketChanner APIs

    public void registerSocketCallback(SocketChanner.Callback cb){
        if (cb != null){
            mSocketChanner.register(cb, null);
        }
    }

    public void sendText(String text){
        //mSocketChanner.sendText(text);
    }

    // Audio operations
    public void record(boolean start){
        if (start){
            mAudioRecord.start();
        } else {
            // stop record
            mAudioRecord.stop();
        }
    }

    public void dialHost(String host){
        mSocketChanner.dialHost(host);
    }


/* ********************************************************************************************** */

    private void initLanScanner(){
        mLanScanner.register(new CallbackImpl());
    }

    private void initSocket(){
        mSocketChanner.register(new CallbackImpl(), null);
    }

    private void initAudio(){
        mAudioRecord.setMode(MyAudioRecorder.RECORD_MODE_STREAM);
        mAudioRecord.register(new CallbackImpl());
        mAudioTrack.setPlayMode(MyAudioTracker.PLAY_MODE_STREAM);
    }

    private void initNeighbors(){
        mNeighbors.register(null, new CallbackImpl());
    }

    private void save(ByteBuffer bb){
        Log.v(TAG, "save: bb=" + bb);

        try {
            if (mFos == null){
                mFos = this.openFileOutput(mFileName, Context.MODE_APPEND);
            } else {
                mFos.write(bb.array());
            }
        } catch (IOException | NullPointerException e) {
            //e.printStackTrace();
        }
    }

/* ********************************************************************************************** */


    public class MyBinder extends Binder {
        public MyDaemonService getService(){
            return MyDaemonService.this;
        }
    }


/* ********************************************************************************************** */


    public class CallbackImpl implements MyAudioRecorder.Callback,
            SocketChanner.Callback, Neighbor.Callback, LanScanner.Callback {

        @Override
        public void onByteStream(ByteBuffer frameBytes) {
            Log.v(TAG, "onByteStream: " + frameBytes.toString());

            // send buffer to socket
            mSocketChanner.sendStream(frameBytes);
        }

        @Override
        public void onIncomingFile(String name) {

        }

        @Override
        public void onByteBuffer(ByteBuffer buffer) {
            Log.v(TAG, "onByteBuffer: " + buffer.toString());
            // send buffer to audio track
            //mAudioTrack.play(buffer);
            save(buffer);
        }

        @Override
        public void onConnectedNeighbors(List<String> connectedList) {
            Log.v(TAG, "onConnectedNeighbors: " + connectedList.size());
            mSocketChanner.updateNeighbors(connectedList);
        }

        @Override
        public void onUpdateNeighbors(List<String> neighbors) {
            Log.v(TAG, "onUpdateNeighbors: " + neighbors.size());
            mNeighbors.triggerUpdating(neighbors);
        }

        @Override
        public void onUpdateLocalAddress(String address) {
            Log.v(TAG, "onUpdateLocalAddress: " + address);
        }

        @Override
        public void onUpdateInternetAddress(String address) {
            Log.v(TAG, "onUpdateInternetAddress: " + address);
        }
    }
}
