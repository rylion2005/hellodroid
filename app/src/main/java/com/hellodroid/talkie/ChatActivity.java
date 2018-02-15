package com.hellodroid.talkie;


import android.os.Bundle;
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

public class ChatActivity extends BaseActivity implements View.OnClickListener, AdapterView.OnItemClickListener{
    private static final String TAG = "ChatActivity";

    private ImageView mIMVType;
    private EditText mEDTText;
    private TextView mTXVTalk;
    private ImageView mIMVMore;
    private Button mBTNAction;

    private MyBaseAdapter mAdapter;


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
            case R.id.IMV_MessageType:
                break;
            case R.id.EDT_Input:
                // get focus and popup keyboard
                v.setFocusable(true);
                v.setFocusableInTouchMode(true);
                v.requestFocus();
                v.setVisibility(View.VISIBLE);
                ((EditText)v).setShowSoftInputOnFocus(true);

                // change action button
                mBTNAction.setVisibility(View.VISIBLE);
                mIMVMore.setVisibility(View.GONE);
                break;
            case R.id.IMV_More:
                break;
            case R.id.BTN_Action:
                // Show in list view
                showTextMessage(false, mEDTText.getText().toString());

                // Send message
                myDaemonService.sendText(mEDTText.getText().toString());
                break;
            default:
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }



/* ********************************************************************************************** */

    private void init(){
        mySocketCallback = new SocketMessageCallback();
    }

    private void initViews(){
        mIMVType = findViewById(R.id.IMV_MessageType);
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

        initListView();
    }

    private void initListView(){
        mAdapter = MyBaseAdapter.newInstance(this, R.layout.chat_message_list);
        ListView lsv = findViewById(R.id.LSV_Messages);
        lsv.setAdapter(mAdapter);
        lsv.setOnItemClickListener(this);
    }

    private void showTextMessage(boolean isIncomingMessage, String text){

        MyBaseAdapter.ViewHolder vh = mAdapter.createHolder();
        TextView tv = vh.getConvertView().findViewById(R.id.TXV_TextMessage);
        if (isIncomingMessage) { // set text gravity
            tv.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);
        } else {
            tv.setGravity(Gravity.CENTER_VERTICAL|Gravity.END);
        }
        vh.setTextView(R.id.TXV_TextMessage, text);
        mAdapter.notifyDataSetChanged();
    }


/* ********************************************************************************************** */

    class SocketMessageCallback implements SocketChanner.Callback{
        @Override
        public void onTextMessageArrived(String text) {
            Log.v(TAG, "onTextMessageArrived: " + text);
            showTextMessage(true, text);
        }
    }
}
