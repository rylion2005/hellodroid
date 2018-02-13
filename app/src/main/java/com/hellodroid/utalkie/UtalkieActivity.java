package com.hellodroid.utalkie;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.hellodroid.R;

public class UtalkieActivity extends AppCompatActivity {


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

    }

    public void refreshContacts(View view){

    }

    public void refreshAccount(View view){

    }

/* ********************************************************************************************** */

    private void initViews(){
        initFragment();
    }

    private void initFragment(){
        SessionsFragment fragment = new SessionsFragment();
        FragmentTransaction transaction =  getFragmentManager().beginTransaction();
        transaction.add(R.id.FRG_TalkieContainer, fragment, "SessionsFragment");
        transaction.commit();
    }
}
