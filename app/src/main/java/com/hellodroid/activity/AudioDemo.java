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

        mRecord = MyAudioRecorder.newInstance();
        mRecord.setMode(0);
        mRecord.set(this, "audio.pcm");
        mTrack = MyAudioTracker.newInstance(MyAudioTracker.PLAY_MODE_FILE);
    }

    public void startRecord(View v){
        mRecord.start();
    }

    public void stopRecord(View v){
        mRecord.stop();
    }

    public void play(View v){
        mTrack.play(this, "audio.pcm");
    }

    public void stop(View v){
        mTrack.stop();
    }

}
