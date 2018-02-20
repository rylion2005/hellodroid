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

    public static final int PLAY_MODE_FILE = 0;
    public static final int PLAY_MODE_STREAM = 1;

    private static MyAudioTracker mInstance;
    private final PlayingThread mPlayingThread = new PlayingThread();


/* ********************************************************************************************** */


    private MyAudioTracker(int mode){
        Log.v(TAG, "new tracker");
        mPlayingThread.setMode(mode);
    }

    public static MyAudioTracker newInstance(int mode){
        if (mInstance == null){
            mInstance = new MyAudioTracker(mode);
        }
        return mInstance;
    }

    public void setMode(int mode){
        mPlayingThread.setMode(mode);
    }

    public void play(ByteBuffer bb){
        Log.v(TAG, "play: " + bb);
        mPlayingThread.flush(bb);
        if (!mPlayingThread.isAlive()){
            mPlayingThread.start();
        }
    }

    public void play(Context context, String fileName){
        mPlayingThread.set(context, fileName);
        if (!mPlayingThread.isAlive()){
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
        private int mMode = PLAY_MODE_FILE;

        private Context mContext;
        private String mFileName = "record.pcm";
        private FileInputStream fis;

        private ByteBuffer mByteBuffer;

        @Override
        public void run() {
            Log.v(TAG, ":playing: running ...");

            init();

            while(!isInterrupted()) {
                int readBytes = 0;
                byte[] bytes = null;

                if (mMode == PLAY_MODE_FILE) {
                    try {
                        int available = fis.available();
                        Log.v(TAG, "available: " + available);
                        if (available > 0) {
                            bytes = new byte[mBufferSizeInBytes];
                            readBytes = fis.read(bytes);
                            Log.v(TAG, "read: " + readBytes);
                            Log.v(TAG, "write: " + readBytes);
                            mTrack.write(bytes, 0, readBytes);
                        } else {
                            break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                } else {
                    if(mByteBuffer.remaining() > 0) {
                        bytes = mByteBuffer.array();
                        int count = mTrack.write(bytes, 0, bytes.length);
                        mByteBuffer.position(mByteBuffer.limit());;
                        Log.v(TAG, "write/" + count + "/: " + mByteBuffer.toString());
                    }
                }
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

            mTrack = new AudioTrack(
                    AUDIO_STREAM,
                    AUDIO_SAMPLE_RATE,
                    AUDIO_CHANNEL,
                    AUDIO_ENCODING,
                    mBufferSizeInBytes,
                    AudioTrack.MODE_STREAM);

            if ( (mTrack != null)
                    && (mTrack.getState() != AudioTrack.STATE_UNINITIALIZED)
                    && (mTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) ) {
                mTrack.play();
            }

            if (mMode == 0) {
                try {
                    fis = mContext.openFileInput(mFileName);
                } catch (IOException e) {
                    fis = null;
                    //e.printStackTrace();
                }
            }
        }

        private void end(){
            if (mMode == 0) {
                try {
                    fis.close();
                } catch (IOException e) {
                    //e.printStackTrace();
                }
            }

            if ( (mTrack != null)
                    && (mTrack.getState() != AudioTrack.STATE_UNINITIALIZED)) {
                mTrack.play();
            }
        }

        private void setMode(int mode){
            mMode = mode;
        }

        private void set(Context context, String fileName){
            if (context != null){
                mContext = context;
            }

            if (fileName != null){
                mFileName = fileName;
            }
        }

        private void flush(ByteBuffer bb){
            mByteBuffer = bb.duplicate();
        }
    }
}
