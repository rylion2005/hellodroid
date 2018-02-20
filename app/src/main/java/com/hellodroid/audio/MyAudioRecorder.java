package com.hellodroid.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/*
** REVISED HISTORY
**   yl7 | 18-2-16: Created
**     
*/
public class MyAudioRecorder{
    private static final String TAG = "MyAudioRecorder";

    private static MyAudioRecorder mInstance;
    private final RecordingThread mRecordThread = new RecordingThread();


/* ********************************************************************************************** */


    private MyAudioRecorder(){
        Log.v(TAG, "new recorder");
    }

    // Single instance
    public static MyAudioRecorder newInstance(){
        if (mInstance == null){
            mInstance = new MyAudioRecorder();
        }
        return mInstance;
    }


/* ********************************************************************************************** */


    public void setMode(int mode){
        mRecordThread.set(mode);
    }

    public void set(Context context, String fileName){
        mRecordThread.set(context, fileName);
    }

    public void register(Callback cb){
        mRecordThread.register(cb);
    }

    public void start(){
        mRecordThread.start();
    }

    public void stop(){
        Log.v(TAG, "stop");
        mRecordThread.interrupt();
    }

    public void pause(){
        //TODO:
    }

    public void resume(){
        //TODO:
    }


/* ********************************************************************************************** */


/* ********************************************************************************************** */


    class RecordingThread extends Thread {

        // Audio configurations
        private final static int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;
        private final static int AUDIO_SAMPLE_RATE = 44100; //44.1khz
        private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_STEREO;
        private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

        private AudioRecord mRecord;
        private int mBufferSizeInBytes;

        private int mMode = 0; // 0: file; 1: stream

        private Context mContext;
        private String mFileName = "record.pcm";
        FileOutputStream fos;

        private final List<Callback> mCallbackList = new ArrayList<>();

        @Override
        public void run() {
            Log.v(TAG, ":recording: running ......");

            init();

            ByteBuffer bb = ByteBuffer.allocate(mBufferSizeInBytes);
            byte[] bytes = new byte[mBufferSizeInBytes];

            while (!isInterrupted()) {  // thread running
                int readBytes = mRecord.read(bytes, 0, mBufferSizeInBytes);
                Log.v(TAG, "read: " + readBytes);
                if (readBytes > 0) {
                    // reset buffer status
                    bb.clear();
                    bb.rewind();
                    //Log.v(TAG, "source: " + bb.toString());

                    bb.put(bytes);
                    bb.rewind();
                    if (mMode == 0) { // save buffer to file
                        save(bb);
                    } else {          // dispatch buffer to client
                        dispatch(bb);
                    }
                }

            }

            end();

            Log.v(TAG, ":recording: exit ...");
        }

        private void init(){
            Log.v(TAG, "init");

            // calculate buffer
            mBufferSizeInBytes = AudioRecord.getMinBufferSize(
                    AUDIO_SAMPLE_RATE,
                    AUDIO_CHANNEL,
                    AUDIO_ENCODING);
            Log.v(TAG, "mBufferSizeInBytes: " + mBufferSizeInBytes);

            // create record instance
            mRecord = new AudioRecord(
                    AUDIO_INPUT,
                    AUDIO_SAMPLE_RATE,
                    AUDIO_CHANNEL,
                    AUDIO_ENCODING,
                    mBufferSizeInBytes);
            Log.v(TAG, "AudioRecord: " + mRecord.toString());

            // start recording
            if ( (mRecord != null)
                    && (mRecord.getState() != AudioRecord.STATE_UNINITIALIZED)
                    && (mRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) ) {
                mRecord.startRecording();
            }

            // open file
            if (mMode == 0) {
                try{
                    mContext.deleteFile(mFileName); // TODO: mode judgement
                    fos = mContext.openFileOutput(mFileName, Context.MODE_APPEND);
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        }

        private void end(){
            Log.v(TAG, "end");
            // close file
            try {
                fos.close();
            } catch (IOException|NullPointerException e){
                //e.printStackTrace();
            }

            // stop record
            if ( (mRecord != null)
                    && (mRecord.getState() != AudioRecord.STATE_UNINITIALIZED)
                    && (mRecord.getRecordingState() != AudioRecord.RECORDSTATE_STOPPED) ) {
                mRecord.stop();
                mRecord.release();
            }
        }


        private void set(int mode){
            mMode = mode;
        }

        private void set(Context context, String fileName){
            if (context != null){
                mContext = context;
            }

            if (fileName != null){
                mFileName = fileName;
                mContext.deleteFile(mFileName);
            }
        }

        private void register(Callback cb){
            if ( cb != null) {
                mCallbackList.add(cb);
            }
        }

        // TODO: optimize more, we should dispatch buffer to main thread
        private void dispatch(ByteBuffer bb){
            Log.v(TAG, "dispatch: " + bb);
            for (Callback cb : mCallbackList){
                cb.onBufferBytes(bb);
            }
        }

        private void save(ByteBuffer bb){
            Log.v(TAG, "save: bb=" + bb);
            try {
                fos.write(bb.array());
            } catch (IOException|NullPointerException e) {
                e.printStackTrace();
            }
        }
    }


/* ********************************************************************************************** */


    public interface Callback {
        void onBufferBytes(ByteBuffer buffer);
    }
}
