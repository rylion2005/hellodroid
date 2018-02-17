package com.hellodroid.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;

/*
**
** ${FILE}
**   ...
**
** REVISED HISTORY
**   yl7 | 18-2-16: Created
**     
*/
public class MyAudioTracker {
    private static final String TAG = "MyAudioTracker";

    private static MyAudioTracker mInstance;

    // Audio configurations
    private final static int AUDIO_STREAM = AudioManager.STREAM_MUSIC;
    private final static int AUDIO_SAMPLE_RATE = 44100; //44.1khz
    private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_OUT_MONO;
    private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private Context mContext;
    private AudioTrack mTrack;
    private int mBufferSizeInBytes;
    private String mFileName = "rdc.dat";

    private Thread mPlayThread;


/* ********************************************************************************************** */


    private MyAudioTracker(Context context){
        Log.v(TAG, "new tracker");
        mContext = context;
        init();
    }

    public static MyAudioTracker newInstance(Context context){
        if (mInstance == null){
            mInstance = new MyAudioTracker(context);
        }
        return mInstance;
    }

    public void play(){
        Log.v(TAG, "play");
        mPlayThread.start();
    }

    public void play(String filename){
        Log.v(TAG, "play: " + filename);
        mFileName = filename;
        mPlayThread.start();
    }

    public void stop(){
        mPlayThread.interrupt();
        mTrack.stop();
        mTrack.release();
    }


/* ********************************************************************************************** */

    private void init(){
        mBufferSizeInBytes = AudioTrack.getMinBufferSize(
                AUDIO_SAMPLE_RATE,
                AUDIO_CHANNEL,
                AUDIO_ENCODING);

        mTrack = new AudioTrack(
                AUDIO_STREAM,
                AUDIO_SAMPLE_RATE,
                AUDIO_CHANNEL,
                AUDIO_ENCODING,
                mBufferSizeInBytes,
                AudioTrack.MODE_STREAM);

        mPlayThread = new Thread(new PlayRunnable());
    }


    class PlayRunnable implements Runnable {
        @Override
        public void run() {
            Log.v(TAG, "Play: running ......");
            try {
                mTrack.play();
                FileInputStream fileInputStream = mContext.openFileInput(mFileName);
                int available;
                do{
                    available = fileInputStream.available();
                    Log.v(TAG, "available: " + available);
                    byte[] bytes = new byte[mBufferSizeInBytes];
                    int readBytes = fileInputStream.read(bytes);
                    Log.v(TAG, "read: " + readBytes);
                    mTrack.write(bytes, 0, readBytes);
                } while (available > 0);
                mTrack.stop();
                mTrack.release();
            } catch (IOException e) {
                //e.printStackTrace();
            }
            Log.v(TAG, ":Play: thread exit !");
        }
    }
}
