package com.hellodroid.activity;


import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.hellodroid.R;
import com.hellodroid.lan.Scanner;
import com.hellodroid.socket.SocketListener;


public class MainActivity extends AppCompatActivity {

    private TextView mTXVContents;

    private final SocketListener sl = new SocketListener(this);
    private final InCallback mCallBack = new InCallback();

    private Scanner sc = new Scanner();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTXVContents = findViewById(R.id.TXV_TextContents);
    }

/* ============================================================================================== */

    public void scanLan(View v){
        mTXVContents.append(sc.getMyLocalAddress() + " | " + sc.getNetworkAddress() + "\n");
    }


/* ============================================================================================== */


    public void becomeAsServer(View v){
        sl.becomeAsServer();
        sl.registerCallback(mCallBack);
    }

    public void becomeAsClient(View v){
        sl.becomeAsClient();
    }

    class InCallback implements SocketListener.IncomingCallBack{
        @Override
        public void onMessageReceived(String text) {
            //mTXVContents.append("\n" + text);
        }

        @Override
        public void onFileReceived(String fileName) {

        }
    }
}
