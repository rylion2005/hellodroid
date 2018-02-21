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
public class MyAudioTracker {
    private static final String TAG = "MyAudioTracker";

    public static final int PLAY_MODE_FILE = 0;
    public static final int PLAY_MODE_STREAM = 1;

    private static MyAudioTracker mInstance;
    private final PlayingThread mPlayingThread = new PlayingThread();


/* ********************************************************************************************** */


    private MyAudioTracker(){
        Log.v(TAG, "new tracker");
    }

    public static MyAudioTracker newInstance(){
        if (mInstance == null){
            mInstance = new MyAudioTracker();
        }
        return mInstance;
    }

    public void setPlayMode(int mode){
        mPlayingThread.setPlayMode(mode);
    }

    public boolean openFile(Context context, String name){
        return mPlayingThread.openInputStream(context, name);
    }

    public void play(ByteBuffer bb){
        Log.v(TAG, "play: " + bb);
        mPlayingThread.flush(bb);
        if (!mPlayingThread.isAlive()){
            mPlayingThread.start();
        }
    }

    public void play(){
        if (!mPlayingThread.isAlive()) {
            mPlayingThread.start();
        }
    }

    public void stop(){
        mPlayingThread.interrupt();
    }


/* ********************************************************************************************** */


    class PlayingThread extends Thread {
        // Audio configurations
        private final static int AUDIO_STREAM = AudioManager.STREAM_MUSIC;
        private final static int AUDIO_SAMPLE_RATE = 44100; //44.1khz
        private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_OUT_STEREO;
        private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

        private AudioTrack mTrack;
        private int mBufferSizeInBytes;
        private int mMode = PLAY_MODE_FILE;
        private FileInputStream fis;
        private ByteBuffer mByteBuffer;

        @Override
        public void run() {
            Log.v(TAG, ":playing: running ...");

            init();

            try {
                if (mMode == PLAY_MODE_FILE) {
                    while (!isInterrupted()) {
                        byte[] bytes = new byte[mBufferSizeInBytes];
                        int read = fis.read(bytes);
                        Log.d(TAG, "read: " + read);
                        if (read > 0){
                            mTrack.write(bytes, 0, read);
                        }
                    }
                } else {
                    while (!isInterrupted()) {
                        if (mByteBuffer.remaining() > 0) {
                            int count = mTrack.write(mByteBuffer.array(), 0, mByteBuffer.array().length);
                            //mByteBuffer.position(mByteBuffer.limit());
                            Log.v(TAG, "write/" + count + "/: " + mByteBuffer.toString());
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "play thread die !");
                e.printStackTrace();
            }

            end();
            Log.v(TAG, ":playing: exit");
        }

        private void init(){
            Log.v(TAG, "init");

            mBufferSizeInBytes = AudioTrack.getMinBufferSize(
                    AUDIO_SAMPLE_RATE,
                    AUDIO_CHANNEL,
                    AUDIO_ENCODING);

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

        private void end(){
            try {
                fis.close();
            } catch (IOException e) {
                //e.printStackTrace();
            }

            if ( (mTrack != null)
                    && (mTrack.getState() != AudioTrack.STATE_UNINITIALIZED)) {
                mTrack.stop();
            }
        }

        private void setPlayMode(int mode){
            mMode = mode;
        }

        private boolean openInputStream(Context context, String fileName){
            boolean result = false;
            try {
                fis = context.openFileInput(fileName);
                result = true;
            } catch (IOException e) {
                fis = null;
                e.printStackTrace();
            }
            return result;
        }

        private void flush(ByteBuffer bb){
            mByteBuffer = bb;
        }
    }
}
