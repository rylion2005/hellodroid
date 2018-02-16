package com.hellodroid.audio;

import android.content.Context;
import android.media.AudioFocusRequest;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioRecordingConfiguration;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/*
**
** ${FILE}
**   ...
**
** REVISED HISTORY
**   yl7 | 18-2-16: Created
**     
*/
public class MyAudioRecorder extends Handler{
    private static final String TAG = "MyAudioRecorder";

    // Internal message
    private final static int INTERNAL_MESSAGE_READ_BYTES = 0xBB;

    // Audio configurations
    private final static int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;
    private final static int AUDIO_SAMPLE_RATE = 44100; //44.1khz
    private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private static MyAudioRecorder mInstance;

    private Thread mRecordThread;
    private Context mContext;

    private AudioRecord mRecord;
    //private AudioRecordingConfiguration mRecordConfig;
    //private AudioManager.AudioRecordingCallback mRecordCallback;
    //private AudioFocusRequest mAudioFocusRequest;

    private int mRecordMode = 0;
    private int mBufferSizeInBytes;
    private String mFileName = "rdc.data";

    // internal audio recording data queue
    private final List<ByteBuffer> mBufferQueue = new ArrayList<>();

    // Clients who wants to byte buffer
    private final List<Callback> mCallbackList = new ArrayList<>();



/* ********************************************************************************************** */


    private MyAudioRecorder(Context context){
        Log.v(TAG, "new recorder");
        init(context);
    }

    // Single instance
    public static MyAudioRecorder newInstance(Context context){
        if (mInstance == null){
            mInstance = new MyAudioRecorder(context);
        }
        return mInstance;
    }

    @Override
    public void handleMessage(Message msg) {
        Log.v(TAG, "handleMessage: " + msg.what);
        switch (msg.what){
            case INTERNAL_MESSAGE_READ_BYTES:
                synchronized (mBufferQueue){
                    for (ByteBuffer bb : mBufferQueue){
                        Log.v(TAG, "record mode: " + mRecordMode);
                        if (mRecordMode == 0){ //store into file
                            writeFile(bb);
                            bb.clear();
                        } else { // delivery stream to client
                            for (Callback cb : mCallbackList){
                                cb.onBufferBytes(bb);
                            }
                        }
                    }
                    mBufferQueue.clear();
                }
                break;
            default:
                break;
        }
    }


/* ********************************************************************************************** */

    /*
    ** ---------------------------------------------------------------------------
    ** setRecordMode
    **   set record mode
    **
    ** @PARAM mode, IN:
    **   0: record to file;
    **   1: record to buffer
    **
    ** @PARAM cb, IN:
    **   mode=1, callback is provided or null
    **
    ** @PARAM filename, IN:
    **   mode=0, filename is MUST, or null when mode=1
    **
    ** @RETURN: None
    **
    ** NOTES:
    **   ......
    **
    ** ---------------------------------------------------------------------------
    */
    public void setRecordMode(int mode, Callback cb, String fileName){
        mRecordMode = mode;

        if (cb != null) {
            mCallbackList.add(cb);
        }

        if (fileName != null){
            mFileName = fileName;
        }
    }

    public void startRecord(){
        Log.v(TAG, "Start record audio");
        mRecord.startRecording();
        startThread();
    }

    public void pauseRecord(){
    }

    public void resumeRecord(){

    }

    public void stopRecord(){
        Log.v(TAG, "stop record audio");
        stopThread();
        mRecord.stop();
        mRecord.release();
    }


/* ********************************************************************************************** */

    private void init(Context context){
        mContext = context;

        mBufferSizeInBytes = AudioRecord.getMinBufferSize(
                AUDIO_SAMPLE_RATE,
                AUDIO_CHANNEL,
                AUDIO_ENCODING);

        mRecord = new AudioRecord(
                AUDIO_INPUT,
                AUDIO_SAMPLE_RATE,
                AUDIO_CHANNEL,
                AUDIO_ENCODING,
                mBufferSizeInBytes);

        mRecordThread = new Thread(new RecordingRunnable());
    }

    private void writeFile(ByteBuffer bb){
        Log.v(TAG, "writeFile: ByteBuffer=" + bb);
        if ( (bb == null) || (bb.limit() == 0)){
            Log.v(TAG, "ByteBuffer has nothing !!!");
            return;
        }

        try {
            FileOutputStream fos = mContext.openFileOutput(mFileName, Context.MODE_APPEND);
            fos.write(bb.array());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startThread(){
        Log.v(TAG, "start thread");
        if ( !mRecordThread.isAlive() ){
            mRecordThread.start();
        }
    }

    private void stopThread(){
        Log.v(TAG, "stop thread: " + mRecordThread.getState().toString());
        //if (!mRecordThread.isInterrupted()){
            mRecordThread.interrupt();
        //}
        Log.v(TAG, "Thread: " + mRecordThread.toString());
    }


/* ********************************************************************************************** */


    class RecordingRunnable implements Runnable {
        @Override
        public void run() {
            Log.v(TAG, ":Record: running ......");
            while (true) {
                ByteBuffer bb = ByteBuffer.allocate(mBufferSizeInBytes);
                int readBytes = mRecord.read(bb, mBufferSizeInBytes);
                Log.v(TAG, "bbb: " + bb);
                synchronized (mBufferQueue) {
                    mBufferQueue.add(bb);
                }
                sendEmptyMessage(INTERNAL_MESSAGE_READ_BYTES);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.v(TAG, "Sleep Exception");
                    //e. printStackTrace();
                    break;
                }
            }
        }
    }


/* ********************************************************************************************** */


    public interface Callback {
        void onBufferBytes(ByteBuffer bb);
    }
}
