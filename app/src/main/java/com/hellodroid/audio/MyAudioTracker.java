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

    private Context mContext;
    private String mFileName = "rdc.dat";
    private final Thread mPlayingThread = new PlayingThread();


/* ********************************************************************************************** */


    private MyAudioTracker(Context context){
        Log.v(TAG, "new tracker");
        mContext = context;
    }

    public static MyAudioTracker newInstance(Context context){
        if (mInstance == null){
            mInstance = new MyAudioTracker(context);
        }
        return mInstance;
    }

    public void play(){
        Log.v(TAG, "play: " + mPlayingThread.getState().toString());
        mPlayingThread.start();
    }

    public void stop(){
        Log.v(TAG, "stop: " + mPlayingThread.getState().toString());
        mPlayingThread.interrupt();
    }


/* ********************************************************************************************** */


    class PlayingThread extends Thread {
        // Audio configurations
        private final static int AUDIO_STREAM = AudioManager.STREAM_MUSIC;
        private final static int AUDIO_SAMPLE_RATE = 44100; //44.1khz
        private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_OUT_MONO;
        private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

        private AudioTrack mTrack;
        private int mBufferSizeInBytes;

        private FileInputStream fis;

        @Override
        public void run() {
            Log.v(TAG, ":playing: running ...");

            init();

            while(!isInterrupted()) {
                try {
                    mTrack.play();

                    int available;
                    do {
                        available = fis.available();
                        Log.v(TAG, "available: " + available);
                        byte[] bytes = new byte[mBufferSizeInBytes];
                        int readBytes = fis.read(bytes);
                        Log.v(TAG, "read: " + readBytes);
                        mTrack.write(bytes, 0, readBytes);
                    } while ((available > 0) && (!isInterrupted()));
                    mTrack.stop();
                    mTrack.release();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }

            close();
            Log.v(TAG, ":playing: exit");
        }

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

            open();
        }

        private void open(){
            try {
                fis = mContext.openFileInput(mFileName);
            } catch (IOException e) {
                fis = null;
                e.printStackTrace();
            }
        }

        private void close(){
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
