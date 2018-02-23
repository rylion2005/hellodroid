package com.hellodroid.talkie;


import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.hellodroid.R;
import com.hellodroid.activity.BaseActivity;
import com.hellodroid.adapter.MyBaseAdapter;
import com.hellodroid.nio.SocketChanner;

import java.nio.ByteBuffer;

public class ChatActivity extends BaseActivity implements View.OnClickListener, AdapterView.OnItemClickListener , TextWatcher {
    private static final String TAG = "ChatActivity";

    private ImageView mIMVType;
    private EditText mEDTText;
    private TextView mTXVTalk;
    private ImageView mIMVMore;
    private Button mBTNAction;

    private MyBaseAdapter mAdapter;

    private boolean mMessageModeText = true;

    private static boolean mTalkStop = false;

/* ********************************************************************************************** */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initViews();
        init();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.IMV_MessageMode:
                mMessageModeText = !mMessageModeText;
                switchMessageMode(mMessageModeText);
                break;

            case R.id.TXV_Talk:
                mTalkStop = !mTalkStop;
                myDaemonService.record(mTalkStop);
                break;

            case R.id.EDT_Input:

                break;
            case R.id.IMV_More:
                break;
            case R.id.BTN_Action:
                // Show in list view
                showTextMessage(false, mEDTText.getText().toString());

                // Send message
                myDaemonService.sendText(mEDTText.getText().toString());
                switchAction(false);
                break;
            default:
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        switchAction(true);
    }




/* ********************************************************************************************** */


    private void init() {
        mySocketCallback = new SocketMessageCallback();
    }

    private void initViews() {
        mIMVType = findViewById(R.id.IMV_MessageMode);
        mEDTText = findViewById(R.id.EDT_Input);
        mTXVTalk = findViewById(R.id.TXV_Talk);
        mIMVMore = findViewById(R.id.IMV_More);
        mBTNAction = findViewById(R.id.BTN_Action);

        mIMVType.setOnClickListener(this);
        mEDTText.setOnClickListener(this);
        mTXVTalk.setOnClickListener(this);
        mIMVMore.setOnClickListener(this);
        mBTNAction.setOnClickListener(this);

        mEDTText.setFocusable(false);
        mEDTText.addTextChangedListener(this);

        initListView();
    }

    private void initListView() {
        mAdapter = MyBaseAdapter.newInstance(this, R.layout.chat_message_list);
        ListView lsv = findViewById(R.id.LSV_Messages);
        lsv.setAdapter(mAdapter);
        lsv.setOnItemClickListener(this);
    }

    private void showTextMessage(boolean isIncomingMessage, String text) {

        MyBaseAdapter.ViewHolder vh = mAdapter.createHolder();
        TextView tv = vh.getConvertView().findViewById(R.id.TXV_TextMessage);
        if (isIncomingMessage) { // set text gravity
            tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        } else {
            tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        }
        vh.setTextView(R.id.TXV_TextMessage, text);
        mAdapter.notifyDataSetChanged();
    }

    private void switchAction(boolean isButton) {
        // change action button
        if (isButton) {
            mBTNAction.setVisibility(View.VISIBLE);
            mIMVMore.setVisibility(View.GONE);
        } else {
            mIMVMore.setVisibility(View.VISIBLE);
            mBTNAction.setVisibility(View.GONE);
        }
    }

    private void switchMessageMode(boolean isTextMode) {
        if (isTextMode) {
            mIMVType.setImageResource(R.mipmap.chat_voice);
            mTXVTalk.setVisibility(View.GONE);

            mEDTText.setVisibility(View.VISIBLE);
            mEDTText.setFocusable(true);
            mEDTText.setFocusableInTouchMode(true);
            mEDTText.requestFocus();
            mEDTText.setShowSoftInputOnFocus(true);
        } else {
            mIMVType.setImageResource(R.mipmap.chat_text);
            mEDTText.setVisibility(View.GONE);
            mTXVTalk.setVisibility(View.VISIBLE);
        }
    }


/* ********************************************************************************************** */

    class SocketMessageCallback implements SocketChanner.Callback {

        @Override
        public void onIncomingFile(String name) {

        }

        @Override
        public void onByteBuffer(ByteBuffer buffer) {
            //showTextMessage(true, text);
        }
    }
}
