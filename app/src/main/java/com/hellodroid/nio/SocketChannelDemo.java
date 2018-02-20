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
import java.util.ArrayList;

public class SocketChannelDemo extends AppCompatActivity {
    private static final String TAG = "SocketChannelDemo";
    private TextView mTXVMessages;

    private final SocketChanner mSocketChanner = SocketChanner.newInstance();
    private boolean mRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_socket_channel_demo);
        mTXVMessages = findViewById(R.id.TXV_Messages);

        /*
        ArrayList<String> ips = new ArrayList<>();
        ips.add("10.10.10.100");
        ips.add("10.10.10.101");

        mSocketChanner.setNeighbors(ips);
        */

        mSocketChanner.register(null, new SocketHandler());
    }

    public void sendText(View v){
        mSocketChanner.sendText("This is my socket strings!!!");
    }

    public void startStream(View v){

        mRunning = true;
        mSocketChanner.prepareStream();
        while (mRunning){
            ByteBuffer bb = ByteBuffer.allocate(1024);
            bb.put("ooooooooooooooooooooo".getBytes());
            bb.rewind();
            mSocketChanner.sendStream(bb);
            bb.clear();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //
            }
        }
    }

    public void stopStream(View v){
        mRunning = false;
        mSocketChanner.stopStream();
    }

    class SocketHandler extends Handler{
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
