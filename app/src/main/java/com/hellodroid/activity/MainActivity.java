package com.hellodroid.activity;


import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.hellodroid.R;
import com.hellodroid.network.MyNetworkReceiver;
import com.hellodroid.lan.Scanner;
import com.hellodroid.nio.SocketChanner;
import com.hellodroid.socket.SocketListener;

import java.nio.ByteBuffer;


public class MainActivity extends AppCompatActivity {

    private TextView mTXVContents;

    private final SocketListener sl = new SocketListener(this);
    private final SocketListener.CallBack mCallBack = new SocketCallback();

    private Scanner sc = new Scanner();

    private MyNetworkReceiver myReceiver = MyNetworkReceiver.getInstance(this);
    private MyNetworkReceiver.CallBack mNetworkNotification = new NetworkNotification();

    private SocketChanner mSocketChanner = new SocketChanner();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTXVContents = findViewById(R.id.TXV_TextContents);
        mTXVContents.setMovementMethod(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        myReceiver.register(mNetworkNotification);
    }

    @Override
    protected void onPause() {
        super.onPause();
        myReceiver.unregister();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

/* ============================================================================================== */

    public void scanLan(View v){
        //mTXVContents.append(sc.getMyLocalAddress() + " | " + sc.getNetworkAddress() + "\n");
        mTXVContents.setText("");
        for (String ip : sc.getNeighbours()){
            if (ip != null) {
                mTXVContents.append(ip + "\n");
            }
        }
    }


/* ============================================================================================== */


    public void becomeAsServer(View v){
        //sl.becomeAsServer();
        //sl.registerCallback(mCallBack);
        mSocketChanner.setNeighbors(sc.getNeighbours());
        mSocketChanner.start();
        mSocketChanner.registerCallback(mChcb);
    }

    public void becomeAsClient(View v){
        //sl.becomeAsClient();
        mSocketChanner.sendText("!!!From Client!!!");
    }

    class SocketCallback implements SocketListener.CallBack{
        @Override
        public void onMessageReceived(String text) {
            //mTXVContents.append("\n" + text);
        }

        @Override
        public void onFileReceived(String fileName) {

        }
    }

/* ============================================================================================== */

    public class NetworkNotification implements MyNetworkReceiver.CallBack {
        @Override
        public void onWifiConnectivity(boolean connected) {

        }

        @Override
        public void onMobileConnectivity(boolean connected) {

        }
    }

    private SocketChanner.ChannelCallback mChcb = new SocketChanner.ChannelCallback() {
        @Override
        public void onReadEvent(ByteBuffer bb) {
            //mTXVContents.append(bb.toString());
        }
    };

}
