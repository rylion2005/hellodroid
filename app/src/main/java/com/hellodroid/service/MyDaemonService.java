package com.hellodroid.service;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.hellodroid.audio.MyAudioRecorder;
import com.hellodroid.audio.MyAudioTracker;
import com.hellodroid.nio.SocketChanner;

import java.nio.ByteBuffer;
import java.util.ArrayList;


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

    private SocketChanner mSocketChanner;

    private MyAudioRecorder mAudioRecord;
    private MyAudioTracker mAudioTrack;

    public MyDaemonService() {
        Log.v(TAG, "new an instance");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");

        initSocket();
        initAudio();
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
            mSocketChanner.register(cb);
        }
    }

    public void setNeighbors(ArrayList<String> neighbors){
        mSocketChanner.setNeighbors(neighbors);
    }

    public void sendText(String text){
        //mSocketChanner.sendText(text);
    }

    // Audio operations
    public void record(boolean start){
        if (start){
            //mAudioRecord.start();
        } else {
            // stop record
            ///mAudioRecord.stop();

            // stop stream
            //mSocketChanner.destroyStream();
        }
    }


/* ********************************************************************************************** */

    private void initSocket(){
        mSocketChanner = SocketChanner.newInstance();
        mSocketChanner.register(new AudioIncomingCallback());
    }

    private void initAudio(){
        mAudioRecord = MyAudioRecorder.newInstance();
        mAudioRecord.setMode(1);
        mAudioRecord.register(new AudioRecordCallback());
        mAudioTrack = MyAudioTracker.newInstance();
        mAudioRecord.setMode(1);
    }

/* ********************************************************************************************** */

    public class MyBinder extends Binder {
        public MyDaemonService getService(){
            return MyDaemonService.this;
        }
    }

    public class AudioRecordCallback implements MyAudioRecorder.Callback{
        @Override
        public void onBufferBytes(ByteBuffer buffer) {
            Log.v(TAG, "onBufferBytes: " + buffer.toString());

            // send buffer to socket
            mSocketChanner.startStreams(buffer);
        }
    }

    public class AudioIncomingCallback implements SocketChanner.Callback{
        @Override
        public void onIncomingFile(String name) {

        }

        @Override
        public void onByteBuffer(ByteBuffer buffer) {
            Log.v(TAG, "onByteBuffer: " + buffer.toString());
            // send buffer to audio track
            //mAudioTrack.play(buffer);
        }
    }
}
