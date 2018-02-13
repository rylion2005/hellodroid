package com.hellodroid.utalkie;

import android.content.Context;
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

import java.util.List;


public class SessionsFragment extends Fragment implements AdapterView.OnItemClickListener {
    private static final String TAG = "SessionsFragment";
    // TODO: Customize parameter argument names

    private OnListFragmentInteractionListener mListener;
    private MyBaseAdapter mAdapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SessionsFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static SessionsFragment newInstance() {
        SessionsFragment fragment = new SessionsFragment();
        //Bundle args = new Bundle();
        //args.putInt(ARG_COLUMN_COUNT, columnCount);
        //fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_session_list, container, false);
        initViews(view);
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

/* ********************************************************************************************** */

    private void initViews(View rootView){
        //initSessions();
        initListViews(rootView);

    }

    private void initListViews(View rootView){
        mAdapter = new MyBaseAdapter(this.getActivity(), R.layout.fragment_session_list);

        ListView lsv = rootView.findViewById(R.id.LSV_Sessions);
        lsv.setAdapter(mAdapter);
        lsv.setOnItemClickListener(this);
        reloadListView();
    }

    private void reloadListView(){
        Log.v(TAG, "reloadListView()");

        mAdapter.clearItemList();

        MyBaseAdapter.ViewHolder vh = mAdapter.createViewHolderInstance();
        vh.setImageView(R.id.IMV_SessionLogo, R.mipmap.ic_session);
        vh.setTextView(R.id.TXV_SessionOriginator, "AAA");
        vh.setTextView(R.id.TXV_LastMessage, "MessageAAAAA.....");
        vh.setTextView(R.id.TXV_LastMessageTime, "16:43");

        MyBaseAdapter.ViewHolder vh2 = mAdapter.createViewHolderInstance();
        vh2.setImageView(R.id.IMV_SessionLogo, R.mipmap.ic_session);
        vh2.setTextView(R.id.TXV_SessionOriginator, "BBB");
        vh2.setTextView(R.id.TXV_LastMessage, "MessageBBBBBBBBBBBBB.....");
        vh2.setTextView(R.id.TXV_LastMessageTime, "20:07");

        MyBaseAdapter.ViewHolder vh3 = mAdapter.createViewHolderInstance();
        vh3.setImageView(R.id.IMV_SessionLogo, R.mipmap.ic_session);
        vh3.setTextView(R.id.TXV_SessionOriginator, "CCCCCC");
        vh3.setTextView(R.id.TXV_LastMessage, "MessageCCCCCCCCC.....");
        vh3.setTextView(R.id.TXV_LastMessageTime, "22:15");

        mAdapter.notifyDataSetChanged();
    }

/* ********************************************************************************************** */


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction();
    }
}
