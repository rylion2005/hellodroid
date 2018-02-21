package com.hellodroid.activity;


import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.hellodroid.R;
import com.hellodroid.audio.MyAudioRecorder;
import com.hellodroid.audio.MyAudioTracker;

public class AudioDemo extends BaseActivity {

    Button mBTNRecord;
    Button mBTNPlay;

    MyAudioTracker mTrack;
    MyAudioRecorder mRecord;

    private boolean mRecording = false;
    private boolean mPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_demo);

        mBTNRecord = findViewById(R.id.BTN_RecordAudio);
        mBTNPlay = findViewById(R.id.BTN_PlayAudio);


        mRecord = MyAudioRecorder.newInstance();
        mRecord.setMode(0);
        mRecord.set(this, "audio.pcm");
        mTrack = MyAudioTracker.newInstance();
        //mTrack.setPlayMode(MyAudioTracker.PLAY_MODE_FILE);
    }

    public void record(View v){
        mRecording = !mRecording;
        if (mRecording){
            mBTNRecord.setText("Stop Recording");
            mRecord.start();
        } else {
            mBTNRecord.setText("Start Record");
            mRecord.stop();
        }
    }


    public void play(View v){
        if (!mPlaying){
            if (mTrack.openFile(this, "audio.pcm")) {
                mTrack.setPlayMode(MyAudioTracker.PLAY_MODE_FILE);
                mTrack.play();
                mBTNPlay.setText("Stop Playing");
                mPlaying = true;
            } else {
                Toast.makeText(this, "No audio file", Toast.LENGTH_SHORT);
            }
        } else {
            mBTNPlay.setText("Start Play");
            mTrack.stop();
            mPlaying = false;
        }
    }
}
