package com.hellodroid.utalkie;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.hellodroid.R;
import com.hellodroid.lan.Scanner;

import java.util.ArrayList;

public class UtalkieActivity extends AppCompatActivity {
    private static final String TAG = "UtalkieActivity";

    private SessionsFragment mSessionsFragment;
    private ContactsFragment mContactsFragment;
    private ProfileFragment mProfileFragment;

    private final Scanner mLanScanner = Scanner.newInstance(new AddressUpdateCallback());

/* ********************************************************************************************** */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_utalkie);
        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
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
            mContactsFragment.updateContacts(neighbors);
        }

        @Override
        public void onUpdateLocalAddress(String address) {
            mProfileFragment.updateLocalAddress(address);
        }

        @Override
        public void onUpdateInternetAddress(String address) {
            mProfileFragment.updateInternetAddress(address);
        }
    }
}
