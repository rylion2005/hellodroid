package com.hellodroid.talkie;

import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hellodroid.R;

public class ProfileFragment extends Fragment{
    private static final String TAG = "AccountFragment";

    TextView mTXVLocalAddress;
    TextView mTXVInternetAddress;

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
        View view = inflater.inflate(R.layout.fragment_account, container, false);
        initViews(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
        refreshAddressViews();
    }

    public void updateInternetAddress(String address){
        Log.v(TAG, "updateInternetAddress: " + address);
        mInternetAddress = address;
        refreshAddressViews();
    }

/* ********************************************************************************************** */


    private void initViews(View rootView){
        mTXVLocalAddress = rootView.findViewById(R.id.TXV_LocalAddress);
        mTXVInternetAddress = rootView.findViewById(R.id.TXV_InternetAddress);

        mTXVLocalAddress.setText(mLocalAddress);
        mTXVInternetAddress.setText(mInternetAddress);
    }

    private void refreshAddressViews(){
        if ((mTXVLocalAddress == null || (mTXVInternetAddress == null))){
            return;
        }

        mTXVLocalAddress.setText(mLocalAddress);
        mTXVInternetAddress.setText(mInternetAddress);
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
