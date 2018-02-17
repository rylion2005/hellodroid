package com.hellodroid.activity;

import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.hellodroid.R;
import com.hellodroid.audio.MyAudioRecorder;
import com.hellodroid.audio.MyAudioTracker;

public class AudioDemo extends BaseActivity {

    MyAudioTracker mTrack;
    MyAudioRecorder mRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_demo);

        mRecord = MyAudioRecorder.newInstance(this);
        mRecord.setRecordMode(0, null, "rdc.dat");

        mTrack = MyAudioTracker.newInstance(this);
    }

    public void startRecord(View v){
        mRecord.startRecord();
    }

    public void stopRecord(View v){
        mRecord.stopRecord();
    }

    public void play(View v){
        mTrack.play("rdc.dat");
    }

    public void stop(View v){
        mTrack.stop();
    }

}
