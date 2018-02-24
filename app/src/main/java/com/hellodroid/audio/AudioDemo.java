package com.hellodroid.audio;


import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.hellodroid.R;
import com.hellodroid.activity.BaseActivity;


public class AudioDemo extends BaseActivity {

    private Button mBTNRecord;
    private Button mBTNPlay;

    private final Tracker mTrack = Tracker.newInstance();
    private final Recorder mRecord = Recorder.newInstance();
    private boolean mRecording = false;
    private boolean mPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_demo);

        mBTNRecord = findViewById(R.id.BTN_RecordAudio);
        mBTNPlay = findViewById(R.id.BTN_PlayAudio);

        mRecord.configure(Recorder.RECORD_TO_FILE);
        mRecord.configure(this, "audio.pcm");
        mRecord.configure(Tracker.PLAY_FILE);
        mTrack.configure(this, "audio.pcm");
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
        mPlaying = !mPlaying;
        if (mPlaying){
            mBTNPlay.setText("Stop Playing");
            mTrack.play();
        } else {
            mBTNPlay.setText("Start Play");
            mTrack.stop();
        }
    }
}
