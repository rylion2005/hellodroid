package com.hellodroid.activity;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.hellodroid.R;
import com.hellodroid.audio.AudioDemo;
import com.hellodroid.nio.NioDemo;
import com.hellodroid.talkie.TalkieActivity;
import com.hellodroid.thread.ThreadDemo;


public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /* ============================================================================================== */

    public void startTalkie(View v){
        startActivity(new Intent(this, TalkieActivity.class));
    }

    public void startAudio(View v){
        startActivity(new Intent(this, AudioDemo.class));
    }

    public void startNio(View v) {
        startActivity(new Intent(this, NioDemo.class));
    }

    public void goThread(View v) {
        startActivity(new Intent(this, ThreadDemo.class));
    }


/* ********************************************************************************************** */
}
