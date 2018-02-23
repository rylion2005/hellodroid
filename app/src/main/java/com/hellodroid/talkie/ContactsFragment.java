package com.hellodroid.talkie;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import com.hellodroid.R;
import com.hellodroid.adapter.MyBaseAdapter;
import java.util.ArrayList;
import java.util.List;

public class ContactsFragment extends Fragment implements AdapterView.OnItemClickListener{
    private static final String TAG = "ContactsFragment";

    private  MyBaseAdapter mAdapter;
    private final List<String> mAddressList = new ArrayList<>();


/* ********************************************************************************************** */


    public ContactsFragment() {
        // Required empty public constructor
    }

    public static ContactsFragment newInstance() {
        ContactsFragment fragment = new ContactsFragment();
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.v(TAG, "onAttach");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);
        initViews(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "onResume");
        refreshContactViews();
    }

    @Override
    public void onPause() {
        Log.v(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        Log.v(TAG, "onDetach");
        super.onDetach();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.v(TAG, "onItemClick: " + position);
        startActivity(new Intent(this.getActivity(), ChatActivity.class));
    }


/* ********************************************************************************************** */

    public void updateContacts(List<String> addressList){
        synchronized (mAddressList) {
            mAddressList.clear();
            mAddressList.addAll(addressList);
        }
    }

    public void refreshContactViews(){
        Log.v(TAG, "refreshContactViews: " + mAddressList.size());
        if ((mAdapter == null)){
            return;
        }

        synchronized (mAdapter.getLock()) {
            mAdapter.clearItemList();
        }

        synchronized (mAddressList) {
            for (String ip : mAddressList) {
                MyBaseAdapter.ViewHolder vh = mAdapter.createHolder();
                vh.setImageView(R.id.IMV_ContactLogo, R.mipmap.ic_contact);
                vh.setTextView(R.id.TXV_IpAddress, ip);
            }
        }
        mAdapter.notifyDataSetChanged();
    }


/* ********************************************************************************************** */


    private void initViews(View rootView){
        initListViews(rootView);
    }

    private void initListViews(View rootView){
        mAdapter = MyBaseAdapter.newInstance(this.getActivity(), R.layout.fragment_contact_list);

        ListView lsv = rootView.findViewById(R.id.LSV_Contacts);
        lsv.setAdapter(mAdapter);
        lsv.setOnItemClickListener(this);

        //refreshListView();
    }
}
