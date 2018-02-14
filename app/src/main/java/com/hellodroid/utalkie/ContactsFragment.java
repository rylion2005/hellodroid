package com.hellodroid.utalkie;

import android.content.Context;
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
import com.hellodroid.lan.Scanner;

import java.util.ArrayList;
import java.util.List;

public class ContactsFragment extends Fragment implements AdapterView.OnItemClickListener{
    private static final String TAG = "ContactsFragment";

    private OnFragmentInteractionListener mListener;
    private MyBaseAdapter mAdapter;
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
        /*
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        */
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
        mListener = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }


/* ********************************************************************************************** */

    public void updateContacts(ArrayList<String> addressList){
        Log.v(TAG, "updateContacts: " + addressList.size());
        reloadContacts(addressList);
        refreshListView();
    }


/* ********************************************************************************************** */


    private void initViews(View rootView){
        initListViews(rootView);
    }

    private void initListViews(View rootView){
        mAdapter = new MyBaseAdapter(this.getActivity(), R.layout.fragment_contact_list);

        ListView lsv = rootView.findViewById(R.id.LSV_Contacts);
        lsv.setAdapter(mAdapter);
        lsv.setOnItemClickListener(this);

        refreshListView();
    }

    private void refreshListView(){
        Log.v(TAG, "refreshListView()");
        if ((mAdapter == null)){
            return;
        }

        mAdapter.clearItemList();
        for ( String ip : mAddressList ) {
            MyBaseAdapter.ViewHolder vh = mAdapter.createViewHolderInstance();
            vh.setImageView(R.id.IMV_ContactLogo, R.mipmap.ic_contact);
            vh.setTextView(R.id.TXV_IpAddress, ip);
        }
        mAdapter.notifyDataSetChanged();
    }

    private void reloadContacts(ArrayList<String> addressList){
        mAddressList.clear();
        mAddressList.addAll(addressList);
    }


/* ********************************************************************************************** */


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
