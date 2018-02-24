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

import javax.security.auth.login.LoginException;

/*
**
** ${FILE}
**   ...
**
** REVISED HISTORY
**   yl7 | 18-2-16: Created
**     
*/
public class Tracker {
    private static final String TAG = "Tracker";
    public static final int PLAY_FILE = 0xA1;
    public static final int PLAY_STREAM = 0xA2;

    private static Tracker mInstance;
    private final PlayRunnable mPlayRunnable = new PlayRunnable();
    private int mWhat = -1;


/* ********************************************************************************************** */


    private Tracker(){
        Log.v(TAG, "new tracker");
    }

    public static Tracker newInstance(){
        if (mInstance == null){
            mInstance = new Tracker();
        }
        return mInstance;
    }

    public void configure(int what){
        mWhat = what;
    }

    public void configure(Context context, String name){
        mPlayRunnable.configure(context, name);
    }

    public void play(ByteBuffer bb){
        Log.v(TAG, "play: " + bb);
        mPlayRunnable.flush(bb);
        if (mPlayRunnable.getStopSignal()) {
            Thread t = new Thread(mPlayRunnable);
            t.start();
        }
    }

    public void play(){
        Thread t = new Thread(mPlayRunnable);
        t.start();
    }

    public void stop(){
        mPlayRunnable.setStopSignal();
    }


/* ********************************************************************************************** */


    class PlayRunnable implements Runnable {
        private final static int AUDIO_STREAM = AudioManager.STREAM_MUSIC;
        private final static int AUDIO_SAMPLE_RATE = 44100; //44.1khz
        private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_OUT_STEREO;
        private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

        private AudioTrack mTrack;
        private volatile boolean mStopSignal;
        private int mBufferSizeInBytes;
        private FileInputStream fis;
        private ByteBuffer mByteBuffer;

        private void configure(Context context, String fileName){
            try{
                fis = context.openFileInput(fileName);
            } catch (IOException|NullPointerException e){
                e.printStackTrace();
                assert (fis==null);
            }
        }

        private void setStopSignal(){
            mStopSignal = true;
        }

        private boolean getStopSignal(){
            return mStopSignal;
        }

        private void flush(ByteBuffer bb){
            mByteBuffer = bb;
        }

        @Override
        public void run() {
            Log.v(TAG, ":playing: running ...");

            preparePlaying();
            byte[] bytes = new byte[mBufferSizeInBytes];
            try {
                while (!mStopSignal) {
                    if (mWhat == PLAY_STREAM){ //play stream
                        if (mByteBuffer.hasRemaining()) {
                            int count = mTrack.write(mByteBuffer, mBufferSizeInBytes, AudioTrack.WRITE_BLOCKING);
                        }
                    } else { // play file
                        int count = fis.read(bytes);
                        if (count > 0){
                            Log.d(TAG, "playing: " + count);
                            mTrack.write(bytes, 0, count);
                        } else { // stream tail
                            break;
                        }
                    }
                }

            } catch (IOException|NullPointerException e) {
                Log.e(TAG, "play thread die !");
                e.printStackTrace();
            }

            endPlaying();
            Log.v(TAG, ":playing: exit");
        }

        private void preparePlaying(){
            Log.v(TAG, "prepare playing");
            mStopSignal = false;

            mBufferSizeInBytes = AudioTrack.getMinBufferSize(
                    AUDIO_SAMPLE_RATE,
                    AUDIO_CHANNEL,
                    AUDIO_ENCODING);
            Log.v(TAG, "Min Buffer Size: " + mBufferSizeInBytes);
            try {
                mTrack = new AudioTrack(
                        AUDIO_STREAM,
                        AUDIO_SAMPLE_RATE,
                        AUDIO_CHANNEL,
                        AUDIO_ENCODING,
                        mBufferSizeInBytes,
                        AudioTrack.MODE_STREAM);
                mTrack.play();
            } catch (IllegalArgumentException|IllegalStateException e) {
                e.printStackTrace();
            }
        }

        private void endPlaying(){
            Log.v(TAG, "end playing");

            try {
                fis.close();
                mTrack.stop();
            } catch (IOException|IllegalStateException|NullPointerException e) {
                //
            }

            fis = null;
            mStopSignal = true;
            mBufferSizeInBytes = -1;
            mByteBuffer = null;
        }
    }
}
