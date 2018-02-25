package com.hellodroid.nio;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.hellodroid.R;

import java.nio.ByteBuffer;


public class NioDemo extends AppCompatActivity {
    private static final String TAG = "NioDemo";
    private TextView mTXVMessages;

    private final Messenger mMessenger = Messenger.newInstance();
    private final Streamer mStreamer = Streamer.newInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nio_demo);
        mTXVMessages = findViewById(R.id.TXV_Messages);

        mMessenger.register(null, new MyHandler());
    }

    public void sendText(View v){
        mMessenger.sendText("H......H");
    }

    public void sendFile(View v){

    }

    public void startStream(View v){
        ByteBuffer bb = ByteBuffer.allocate(128);
        final Object mLock = new Object();
        int index = 0;

        while (index++ < 65535) {
            String str = "[" + index + "] Stream bytes";
            synchronized (mLock) {
                bb.clear();
                //Log.d(TAG, "[" + index + "]");
                bb.put(str.getBytes());
                bb.flip();
                mStreamer.startStream(bb);
            }
        }
    }

    public void stopStream(View v){
        mStreamer.stopStream();
    }

    class MyHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            Log.v(TAG, ": " + msg.toString());
            try {
                String text = new String(msg.getData().getByteArray("ByteBuffer"));
                mTXVMessages.append(text + "\n");
            } catch (NullPointerException e) {
                Log.v(TAG, "no bytes");
            }
        }
    }
}
