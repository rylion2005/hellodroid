package com.hellodroid.activity;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.hellodroid.R;
import com.hellodroid.socket.SocketListener;


public class MainActivity extends AppCompatActivity {

    private final SocketListener sl = new SocketListener(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sl.registerHandler(new MyHandler());
    }


    public void becomeAsServer(View v){
        sl.becomeAsServer();
    }

    public void becomeAsClient(View v){
        sl.becomeAsClient();
    }

    class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

        }
    }
}
