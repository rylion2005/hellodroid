package com.hellodroid.Adapter;


import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.BaseAdapter;
import android.content.Context;

import java.util.ArrayList;


/*
** ********************************************************************************
**
** MyBaseAdapter
**   This is a customized base adapter which do not depend on any layout inflater.
**
** ********************************************************************************
*/
public class MyBaseAdapter extends BaseAdapter {
    private static final String TAG = "MyBaseAdapter";
    private ArrayList<ViewHolder> mItemsList;
    private Context mContext;
    private int mLayoutResId;

    public MyBaseAdapter(Context context, int resId) {
        mItemsList = new ArrayList<>();
        mContext = context;
        mLayoutResId = resId;
    }

 	@Override
    public int getCount() {
        return mItemsList.size();
    }

    @Override
    public Object getItem(int position) {
        return mItemsList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @Override
    public int getViewTypeCount() {
        return super.getViewTypeCount();
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return super.getDropDownView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        ViewHolder vh;

        if (convertView == null) {
            vh = getItemList().get(position);
            convertView = vh.getConvertView();
            convertView.setTag(vh);
        } else {
            vh = (ViewHolder) convertView.getTag();
            // replace with new data in item list
            convertView = (getItemList().get(position)).getConvertView();
        }

        return convertView;
    }

    /* ============================================================================================== */

    /*
    ** ---------------------------------------------------------------------------
    ** createViewHolderInstance
    **   create hold instance and add it to item list
    **
    ** @RETURN ViewHolder: ViewHolder instance object.
    ** @PARAM : None
    ** @RETURN: None
    **
    ** NOTES:
    **   ......
    **
    ** ---------------------------------------------------------------------------
    */
    public ViewHolder createViewHolderInstance(){
        ViewHolder vh = new ViewHolder(mContext, mLayoutResId);
        getItemList().add(vh);
        return vh;
    }

    public void clearItemList(){
        for ( ViewHolder vh : getItemList()) {
            vh.clearViewList();
        }
        getItemList().clear();
    }

    private ArrayList<ViewHolder> getItemList(){
        return mItemsList;
    }

/* ============================================================================================== */


    /*
    ** ********************************************************************************
    **
    ** ViewHolder
    **   view container which can contain any number of views as you want
    **
    ** ********************************************************************************
    */
    public class ViewHolder {
        //every holder has itself convert view instance
        private View myConvertView;

        // store all views added to this view holder
        private ArrayList<View> myViewsList = new ArrayList<>();


        public ViewHolder(){
        }

        public ViewHolder(View convertView){
            myConvertView = convertView;
        }

        public ViewHolder(Context context, int layoutResourceId){
            myConvertView = LayoutInflater.from(context).inflate(layoutResourceId, null);
        }

        public ArrayList<View> getViewList(){
            return myViewsList;
        }

        public void clearViewList(){
            myViewsList.clear();
        }

        public View getConvertView(){
            return myConvertView;
        }

        public void addView(View v){
            myViewsList.add(v);
        }

        public View getView(int id){
            View view = null;
            for (View v : getViewList()){
                if (v.getId() == id){
                    view = v;
                    break;
                }
            }
            return view;
        }

        public void setImageView(int viewId, int imageResourceId){
            ImageView v = (ImageView) getConvertView().findViewById(viewId);
            if (v != null){
                if (v instanceof ImageView){
                    v.setImageResource(imageResourceId);
                    addView(v);
                }
            }
        }

        public void setTextView(int viewId, String text) {
            TextView v = (TextView) (getConvertView().findViewById(viewId));
            if (v != null) {
                if (v instanceof TextView){
                    v.setText(text);
                    addView(v);
                }
            }
        }

        public void setView(int viewId, int visibility){
            View v = getConvertView().findViewById(viewId);
            if (v != null) {
                v.setVisibility(visibility);
                addView(v);
            }
        }
    }
}
