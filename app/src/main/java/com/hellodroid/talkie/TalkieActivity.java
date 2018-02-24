package com.hellodroid.talkie;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import java.util.List;

import com.hellodroid.R;
import com.hellodroid.activity.BaseActivity;
import com.hellodroid.lan.LanScanner;


public class TalkieActivity extends BaseActivity {
    private static final String TAG = "TalkieActivity";

    private static final int MESSAGE_ADDRESS_UPDATING = 0xA0;
    private final MyHandler mHandler = new MyHandler();

    private SessionsFragment mSessionsFragment;
    private ContactsFragment mContactsFragment;
    private ProfileFragment mProfileFragment;


/* ********************************************************************************************** */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate");
        setContentView(R.layout.activity_utalkie);
        mLanScanner.register(new AddressCallback());
        initViews();
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "onResume: ");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
    }

    public void refreshSessions(View view){
        replaceFragment(mSessionsFragment);
    }

    public void refreshContacts(View view){
        replaceFragment(mContactsFragment);
    }

    public void refreshAccount(View view){
        replaceFragment(mProfileFragment);
    }

/* ********************************************************************************************** */


/* ********************************************************************************************** */

    private void initViews(){
        initFragments();
    }

    private void initFragments(){
        mSessionsFragment = SessionsFragment.newInstance();
        mContactsFragment = ContactsFragment.newInstance();
        mProfileFragment = ProfileFragment.newInstance();
        mContactsFragment.updateContacts(mLanScanner.getNeighbours());
        //mContactsFragment.refreshContactViews();
        mProfileFragment.updateLocalAddress(mLanScanner.getMyLocalAddress());
        mProfileFragment.updateInternetAddress(mLanScanner.getMyInternetAddress());
        //mProfileFragment.refreshAddressViews();
        addFragment(mSessionsFragment, "SessionsFragment");
    }

    private void addFragment(Fragment fragment, String tag) {
        FragmentManager fm = this.getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(R.id.FRL_FragmentContainer, fragment, tag);
        ft.commit();
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.FRL_FragmentContainer, fragment);
        ft.commit();
    }


/* ********************************************************************************************** */

    class AddressCallback implements LanScanner.Callback{
        @Override
        public void onUpdateNeighbors(List<String> neighbors) {
            //mContactsFragment.updateContacts(neighbors);
            //mHandler.sendEmptyMessage(MESSAGE_ADDRESS_UPDATING);
        }

        @Override
        public void onUpdateLocalAddress(String address) {
            mProfileFragment.updateLocalAddress(address);
            mHandler.sendEmptyMessage(MESSAGE_ADDRESS_UPDATING);
        }

        @Override
        public void onUpdateInternetAddress(String address) {
            mProfileFragment.updateInternetAddress(address);
            mHandler.sendEmptyMessage(MESSAGE_ADDRESS_UPDATING);
        }

        @Override
        public void onConnectedNeighbors(List<String> connectedList) {
            mContactsFragment.updateContacts(connectedList);
            mHandler.sendEmptyMessage(MESSAGE_ADDRESS_UPDATING);
        }
    }

    class MyHandler extends Handler{

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_ADDRESS_UPDATING:
                    mProfileFragment.refreshAddressViews();
                    mContactsFragment.refreshContactViews();
                    break;
            }
        }
    }
}
