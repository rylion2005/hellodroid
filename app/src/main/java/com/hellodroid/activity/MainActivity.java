package com.hellodroid.activity;


import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.hellodroid.R;
import com.hellodroid.file.Utils;
import com.hellodroid.network.MyNetworkReceiver;
import com.hellodroid.lan.Scanner;
import com.hellodroid.nio.SocketChanner;
import com.hellodroid.socket.SocketListener;
import com.hellodroid.utalkie.UtalkieActivity;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import static android.app.Activity.RESULT_OK;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private TextView mTXVContents;

    private SocketListener mSocketListener;
    private MyNetworkReceiver myReceiver;
    private Scanner mLanScanner;
    private SocketChanner mSocketChanner;

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
        myReceiver.register(new NetworkCallback());
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v(TAG, "onActivityResult: " + requestCode + ", " + resultCode);
        switch (requestCode) {
            case 0xCE:
                if ((resultCode == RESULT_OK) && (data != null)){
                    Uri uri = data.getData();
                    Log.v(TAG, "uri: " + uri.toString());
                    String pathname = Utils.getFileWithPath(this, uri);
                    String path = Utils.getPath(this, uri);
                    String name = Utils.getFileName(this, uri);
                    Log.v(TAG, "PathName: " + pathname);
                    Log.v(TAG, "Path: " + path);
                    Log.v(TAG, "Name: " + name);
                    mSocketChanner.sendFile(pathname);
                }
                break;

            default:
                break;
        }
    }

    /* ============================================================================================== */

    public void becomeAsServer(View v){
        //sl.becomeAsServer();
        //sl.registerCallback(mCallBack);

        // socket channer
        mSocketChanner.setNeighbors(mLanScanner.getNeighbours());
        mSocketChanner.start();
        mSocketChanner.registerCallback(new ChannerCallback());
    }

    public void startUTalkie(View v){
        startActivity(new Intent(this, UtalkieActivity.class));
    }

    public void selectFile(View v){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        //intent.setType(“image/*”);//选择图片
        //intent.setType(“audio/*”); //选择音频
        //intent.setType(“video/*”); //选择视频 （mp4 3gp 是android支持的视频格式）
        //intent.setType(“video/*;image/*”);//同时选择视频和图片
        intent.setType("*/*"); //设置类型，我这里是任意类型，任意后缀的可以这样写。
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent,0xCE);
    }

    public void scanLan(View v){
        mTXVContents.setText("");
        for (String ip : mLanScanner.getNeighbours()){
            if (ip != null) {
                mTXVContents.append(ip + "\n");
            }
        }
    }


/* ============================================================================================== */

    private void init(){
        mSocketListener = new SocketListener(this);
        //mListenerCallBack = new SocketCallback();

        mLanScanner = new Scanner(new NeighborUpdateCallback());

        myReceiver = MyNetworkReceiver.getInstance(this);

        mSocketChanner = new SocketChanner(this);
    }


/* ********************************************************************************************** */

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

    public class NetworkCallback implements MyNetworkReceiver.CallBack {
        @Override
        public void onWifiConnectivity(boolean connected) {

        }

        @Override
        public void onMobileConnectivity(boolean connected) {

        }
    }

/* ********************************************************************************************** */


    class ChannerCallback implements SocketChanner.ChannelCallback {

        @Override
        public void onReadEvent(ByteBuffer bb) {

        }

        @Override
        public void onTextMessageArrived(String text) {
            mTXVContents.append(text + "\n");
        }
    };

/* ********************************************************************************************** */

    class NeighborUpdateCallback implements Scanner.Callback {
        @Override
        public void onNewNeighbors(ArrayList<String> neighbors) {
            mSocketChanner.setNeighbors(neighbors);
        }
    }
}
