package com.hellodroid.utalkie;

import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.hellodroid.R;
import com.hellodroid.adapter.MyBaseAdapter;
import com.hellodroid.lan.Scanner;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment{
    private static final String TAG = "AccountFragment";

    private OnFragmentInteractionListener mListener;

    private String mLocalAddress;
    private String mInternetAddress;


/* ********************************************************************************************** */


    public ProfileFragment() {
        // Required empty public constructor
    }

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
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
        View view = inflater.inflate(R.layout.fragment_account, container, false);
        initViews(view);
        return view;
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
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


/* ********************************************************************************************** */

    public void updateLocalAddress(String address){
        Log.v(TAG, "updateLocalAddress: " + address);
        mLocalAddress = address;
    }

    public void updateInternetAddress(String address){
        Log.v(TAG, "updateInternetAddress: " + address);
        mInternetAddress = address;
    }

/* ********************************************************************************************** */


    private void initViews(View rootView){
        TextView txvLocalAddress = rootView.findViewById(R.id.TXV_LocalAddress);
        TextView txvInternetAddress = rootView.findViewById(R.id.TXV_InternetAddress);

        txvLocalAddress.setText(mLocalAddress);
        txvInternetAddress.setText(mInternetAddress);
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
