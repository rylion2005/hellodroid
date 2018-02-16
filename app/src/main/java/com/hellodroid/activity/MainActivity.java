package com.hellodroid.activity;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.hellodroid.R;
import com.hellodroid.talkie.TalkieActivity;



public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";

    private TextView mTXVContents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTXVContents = findViewById(R.id.TXV_TextContents);
        mTXVContents.setMovementMethod(null);

        init();
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

/* ============================================================================================== */

    private void init(){
    }


/* ********************************************************************************************** */
}
