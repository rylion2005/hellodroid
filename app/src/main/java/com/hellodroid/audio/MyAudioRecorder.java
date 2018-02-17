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
import java.io.FileInputStream;
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
                        for (Callback cb : mCallbackList){
                            cb.onBufferBytes(bb);
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
    public void setMode(int mode){
        mRecordMode = mode;
    }

    public void register(Callback cb){
        if (cb != null) {
            mCallbackList.add(cb);
        }
    }

    public void setFile(String name){
        if (name != null){
            mFileName = name;
        }
    }

    public void startRecord(){
        Log.v(TAG, "start");
        if ( (mRecord != null)
                && (mRecord.getState() != AudioRecord.STATE_UNINITIALIZED)
                && (mRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) ) {
            mRecord.startRecording();
        }

        mRecordThread.start();
    }

    public void pauseRecord(){
    }

    public void resumeRecord(){

    }

    public void stopRecord(){
        Log.v(TAG, "stop");
        mRecordThread.interrupt();
        if ( (mRecord != null)
             && (mRecord.getState() != AudioRecord.STATE_UNINITIALIZED)
             && (mRecord.getRecordingState() != AudioRecord.RECORDSTATE_STOPPED) ) {
            mRecord.stop();
            mRecord.release();
        }
    }


/* ********************************************************************************************** */

    private void init(Context context){
        Log.v(TAG, "init");
        mContext = context;

        mBufferSizeInBytes = AudioRecord.getMinBufferSize(
                AUDIO_SAMPLE_RATE,
                AUDIO_CHANNEL,
                AUDIO_ENCODING);
        Log.v(TAG, "mBufferSizeInBytes: " + mBufferSizeInBytes);
        mRecord = new AudioRecord(
                AUDIO_INPUT,
                AUDIO_SAMPLE_RATE,
                AUDIO_CHANNEL,
                AUDIO_ENCODING,
                mBufferSizeInBytes);
        Log.v(TAG, "AudioRecord: " + mRecord.toString());
        mRecordThread = new RecordingThread();
    }

/* ********************************************************************************************** */


    class RecordingThread extends Thread {
        FileOutputStream fos;

        @Override
        public void run() {
            Log.v(TAG, ":recording: running ......");

            if (mRecordMode == 0) {
                open();
            }

            ByteBuffer bb = ByteBuffer.allocate(mBufferSizeInBytes);
            byte[] bytes = new byte[mBufferSizeInBytes];

            while (!isInterrupted()) {
                int readBytes = mRecord.read(bytes, 0, mBufferSizeInBytes);
                Log.v(TAG, "read: " + readBytes);
                if (readBytes > 0) {
                    bb.put(bytes);
                    bb.rewind();
                    if (mRecordMode == 0) {
                        save(bb);
                    } else {
                        dispatch(bb);
                    }
                }
                bb.rewind();
                bb.clear();
            }

            if (mRecordMode == 0) {
                close();
            }

            Log.v(TAG, ":recording: exit ...");
        }

        private void dispatch(ByteBuffer bb){
            Log.v(TAG, "dispatch: " + bb);
            for (Callback cb : mCallbackList){
                cb.onBufferBytes(bb);
            }
            /*
            synchronized (mBufferQueue) {
                mBufferQueue.add(bb.duplicate());
            }
            Log.v(TAG, "sendEmptyMessage: " + INTERNAL_MESSAGE_READ_BYTES);
            sendEmptyMessage(INTERNAL_MESSAGE_READ_BYTES);
            */
        }

        private void open(){
            Log.v(TAG, "open: " + mFileName);
            try{
                mContext.deleteFile(mFileName);
                fos = mContext.openFileOutput(mFileName, Context.MODE_APPEND);
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        private void save(ByteBuffer bb){
            Log.v(TAG, "save: bb=" + bb);
            try {
                fos.write(bb.array());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void close(){
            Log.v(TAG, "close");
            try {
                fos.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }


/* ********************************************************************************************** */


    public interface Callback {
        void onBufferBytes(ByteBuffer bb);
    }
}
