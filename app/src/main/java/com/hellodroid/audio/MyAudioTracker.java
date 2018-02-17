package com.hellodroid.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

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

    private int mMode = 0; //0: file; 1: stream
    private Context mContext;
    private String mFileName = "rdc.dat";
    private final PlayingThread mPlayingThread = new PlayingThread();


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

    public void setMode(int mode){
        mMode = mode;
    }

    private void setByteBuffer(ByteBuffer bb){
        mPlayingThread.setByteBuffer(bb);
    }

    public void play(){
        Log.v(TAG, "play: " + mPlayingThread.getState().toString());
        mPlayingThread.start();
    }

    public void play(ByteBuffer bb){
        Log.v(TAG, "play: " + mPlayingThread.getState().toString());
        mPlayingThread.setByteBuffer(bb);
        if (!mPlayingThread.isAlive()) {
            mPlayingThread.start();
        }
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
        private ByteBuffer mByteBuffer;

        public void setByteBuffer(ByteBuffer bb){
            mByteBuffer = bb;
        }

        @Override
        public void run() {
            Log.v(TAG, ":playing: running ...");

            init();
            mTrack.play();

            while(!isInterrupted()) {
                int readBytes = 0;
                byte[] bytes = null;

                if (mMode == 0) {
                    try {
                        int available = fis.available();
                        Log.v(TAG, "available: " + available);
                        if (available > 0) {
                            bytes = new byte[mBufferSizeInBytes];
                            readBytes = fis.read(bytes);
                            Log.v(TAG, "read: " + readBytes);
                        } else {
                            break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                } else {
                    bytes = mByteBuffer.array();
                    readBytes = bytes.length;
                }
                mTrack.write(bytes, 0, readBytes);
            }

            mTrack.stop();
            mTrack.release();
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
