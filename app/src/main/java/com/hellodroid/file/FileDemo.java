package com.hellodroid.file;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.hellodroid.R;
import com.hellodroid.activity.BaseActivity;

public class FileDemo extends BaseActivity {
    private static final String TAG = "FileDemo";
    private static final int REQUEST_SELECT_FILE = 0xDD;

    private TextView mTXVInformation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_demo);

        mTXVInformation = findViewById(R.id.TXV_Information);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case REQUEST_SELECT_FILE:
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = data.getData();
                    String fileName = Utils.getFileName(this, uri);
                    String path = Utils.getPath(this, uri);
                    String pathname = Utils.getFileWithPath(this, uri);
                    mTXVInformation.append("name: " + fileName + "\n");
                    mTXVInformation.append("path: " + path + "\n");
                    mTXVInformation.append("pathname: " + pathname + "\n");
                } else {
                    //TODO:
                }
                break;

            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void selectFile(View v){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // all type
        //intent.setType(“image/*”);
        //intent.setType(“audio/*”);
        //intent.setType(“video/*”);
        //intent.setType(“video/*;image/*”);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_SELECT_FILE);
    }
}
