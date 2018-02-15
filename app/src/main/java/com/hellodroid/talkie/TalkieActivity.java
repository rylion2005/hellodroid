package com.hellodroid.talkie;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import java.util.ArrayList;
import com.hellodroid.R;
import com.hellodroid.activity.BaseActivity;
import com.hellodroid.lan.Scanner;


public class TalkieActivity extends BaseActivity {
    private static final String TAG = "TalkieActivity";

    private SessionsFragment mSessionsFragment;
    private ContactsFragment mContactsFragment;
    private ProfileFragment mProfileFragment;


/* ********************************************************************************************** */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate");
        setContentView(R.layout.activity_utalkie);

        // Lan Scanner
        mLanScannerCallback = new AddressUpdateCallback();
        mLanScanner.register(mLanScannerCallback);

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
        mProfileFragment.updateLocalAddress(mLanScanner.getMyLocalAddress());
        mProfileFragment.updateInternetAddress(mLanScanner.getMyInternetAddress());
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

    class AddressUpdateCallback implements Scanner.Callback {
        @Override
        public void onUpdateNeighbors(ArrayList<String> neighbors) {
            Log.v(TAG, "onUpdateNeighbors: " + neighbors.size());
            mContactsFragment.updateContacts(neighbors);
            myDaemonService.setNeighbors(neighbors);
        }

        @Override
        public void onUpdateLocalAddress(String address) {
            Log.v(TAG, "onUpdateLocalAddress: " + address);
            mProfileFragment.updateLocalAddress(address);
        }

        @Override
        public void onUpdateInternetAddress(String address) {
            Log.v(TAG, "onUpdateInternetAddress: " + address);
            mProfileFragment.updateInternetAddress(address);
        }
    }
}
