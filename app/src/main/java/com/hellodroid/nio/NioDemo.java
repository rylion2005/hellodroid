package com.hellodroid.nio;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.hellodroid.R;

import java.nio.ByteBuffer;


public class NioDemo extends AppCompatActivity {
    private static final String TAG = "NioDemo";
    private TextView mTXVMessages;
    private Button mBTNStream;

    private final Messenger mMessenger = Messenger.newInstance();
    private final Streamer mStreamer = Streamer.newInstance();

    private volatile boolean mStreamRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nio_demo);
        mTXVMessages = findViewById(R.id.TXV_Messages);
        mBTNStream = findViewById(R.id.BTN_Stream);
        mBTNStream.setBackgroundColor(0xFFFFD700);

        mMessenger.register(null, new MyHandler());
    }

    public void sendText(View v){
        mMessenger.sendText("H......H");
    }

    public void sendFile(View v){

    }

    public void startStream(View v){
        mStreamRunning = true;
        mBTNStream.setBackgroundColor(0xFF1C86EE);
        new Thread( new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Stream: sending ...");
                    ByteBuffer bb = ByteBuffer.allocate(128);
                    final Object mLock = new Object();
                    int index = 0;
                    while(mStreamRunning){
                        String str = "[" + index++ + "] Stream bytes";
                        synchronized (mLock) {
                            bb.clear();
                            Log.d(TAG, "[" + str + "]");
                            bb.put(str.getBytes());
                            bb.flip();
                            mStreamer.startStream(bb);
                        }
                    }
                    Log.d(TAG,"Stream is stopped !");
                }
            }
        ).start();
    }

    public void stopStream(View v){
        Log.d(TAG, "stopStream");
        mStreamRunning = false;
        mBTNStream.setBackgroundColor(0xFF708090);
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
