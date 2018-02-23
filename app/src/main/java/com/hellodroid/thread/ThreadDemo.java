package com.hellodroid.thread;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;


import com.hellodroid.R;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class ThreadDemo extends AppCompatActivity {

    private static final String TAG = "ThreadDemo";

    Button mBTNInterrupt;
    Button mBTNBlock;

    private final InterruptThread mInterruptThread = new InterruptThread();
    private final SocketThread mSocketThread = new SocketThread();

    private boolean mInterrupt = false;
    private boolean mBlocking = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread_demo);

        mBTNBlock = findViewById(R.id.BTN_Block);
        mBTNInterrupt = findViewById(R.id.BTN_Interrupt);
    }

    public void interrupt(View view){
        Log.v(TAG, "InterruptThread: " + mInterruptThread.getState().toString());
        if (mInterrupt){
            mInterruptThread.start();
            mBTNInterrupt.setText("Interrupt Looping");
        } else {
            mInterruptThread.interrupt();
            mBTNInterrupt.setText("Start Looping");
        }
        mInterrupt = !mInterrupt;
    }

    public void block(View view){
        Log.v(TAG, "SocketThread: " + mSocketThread.getState().toString());
        if (mBlocking){
            mSocketThread.start();
            mBTNBlock.setText("Stop Socket");
        } else {
            mSocketThread.interrupt();
            mBTNBlock.setText("Start Socket");
        }
        mBlocking = !mBlocking;
    }


/* ********************************************************************************************** */


    class InterruptThread extends Thread {

        int count = 0;

        @Override
        public void run() {
            Log.v(TAG, "InterruptThread: running ");
            while (!isInterrupted()){
                count++;
                Log.v(TAG, "count: " + count);
            }

            Log.v(TAG, "InterruptThread: exit");
        }
    }

    class SocketThread extends Thread {

        @Override
        public void run() {
            Log.v(TAG, "SocketThread: running");
            try {
                SocketChannel sc = SocketChannel.open();
                while (!isInterrupted()) {
                    boolean connected = sc.connect(new InetSocketAddress("10.10.10.101", 8866));
                    Log.v(TAG, "connected: " + connected);
                    sc.close();
                }
            } catch (IOException e) {
                Log.v(TAG, "SocketThread: die! ");
                e.printStackTrace();
            }

            Log.v(TAG, "SocketThread: exit");
        }
    }

}
