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
public class Recorder{
    private static final String TAG = "Recorder";

    public static final int RECORD_TO_FILE = 0xA1;
    public static final int RECORD_TO_STREAM = 0xA2;

    private static Recorder mInstance;

    private int mWhere = -1;
    private final RecordRunnable mRecordRunnable = new RecordRunnable();


/* ********************************************************************************************** */


    private Recorder(){
        Log.v(TAG, "new recorder");
    }

    // Single instance
    public static Recorder newInstance(){
        if (mInstance == null){
            mInstance = new Recorder();
        }
        return mInstance;
    }

    public void configure(int where){
        mWhere = where;
    }

    public void configure(Context context, String fileName){
        mRecordRunnable.configure(context, fileName);
    }

    public void register(Callback cb){
        mRecordRunnable.register(cb);
    }

    public void start(){
        Thread t = new Thread(mRecordRunnable);
        t.start();
    }

    public void stop(){
        mRecordRunnable.setStopSignal();
    }


/* ********************************************************************************************** */


    class RecordRunnable implements Runnable {

        private final static int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;
        private final static int AUDIO_SAMPLE_RATE = 44100; //44.1khz
        private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_STEREO;
        private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

        private AudioRecord mRecord;
        private final List<Callback> mCallbackList = new ArrayList<>();

        private volatile boolean mStopSignal;
        private ByteBuffer mBuffer;
        private int mBufferSizeInBytes;
        private FileOutputStream fos;


        private RecordRunnable(){
        }

        private void configure(Context context, String fileName){
            try{
                context.deleteFile(fileName);
                fos = context.openFileOutput(fileName, Context.MODE_APPEND);
            } catch (IOException|NullPointerException e){
                e.printStackTrace();
            }
        }

        private void register(Callback cb){
            if ( cb != null) {
                mCallbackList.add(cb);
            }
        }

        private void setStopSignal(){
            mStopSignal = true;
        }

        @Override
        public void run() {
            Log.v(TAG, ":recording: running ......");

            prepareRecording();
            byte[] buf = new byte[mBufferSizeInBytes];
            while (!mStopSignal) {  // thread running
                int count = mRecord.read(buf, 0, mBufferSizeInBytes);
                if (count > 0) {
                    if (mWhere == RECORD_TO_FILE) {
                        save(buf, 0, count);
                    } else {
                        mBuffer.clear();
                        mBuffer.put(buf);
                        mBuffer.flip();
                        dispatch(mBuffer);
                    }
                }
            }
            endRecording();
            Log.v(TAG, ":recording: exit ...");
        }

        private void prepareRecording(){
            Log.v(TAG, "prepare recording");
            mStopSignal = false;

            try {
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

                Log.v(TAG, "Record state: " + mRecord.getState());
                mRecord.startRecording();

                mBuffer = ByteBuffer.allocate(mBufferSizeInBytes);
            } catch (IllegalStateException|NullPointerException e) {
                Log.v(TAG, "prepare exception");
                e.printStackTrace();
            }
        }

        private void endRecording(){
            Log.v(TAG, "end recording");

            try{
                fos.close();
                mRecord.stop();
                mRecord.release();
            } catch (IllegalStateException|IOException|NullPointerException e){
                // nothing
            }

            fos = null;
            mStopSignal = true;
            mBuffer = null;
            mBufferSizeInBytes = -1;
            mRecord = null;
        }

        private void dispatch(ByteBuffer bb){
            Log.v(TAG, "dispatch: " + bb);
            for (Callback cb : mCallbackList){
                cb.onByteStream(bb);
            }
        }

        private void save(byte[] bytes, int offset, int length){
            Log.v(TAG, "save: " + length);
            try {
                fos.write(bytes, offset, length);
            } catch (IOException|NullPointerException e) {
                e.printStackTrace();
            }
        }
    }


/* ********************************************************************************************** */


    public interface Callback {
        void onByteStream(ByteBuffer frameBytes);
    }
}
