package com.hellodroid.identity;


import android.content.Context;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.gson.Gson;

/*
**
** ${FILE}
**   ...
**
** REVISED HISTORY
**   yl7 | 18-2-27: Created
**     
*/
public class Identity {

	private static final String UUID_FILE = "uuid.id";

    private String uuid;
    private String imei;
    private String meid;
    private String serialno;
    private String wfmacaddress;
    private String btmacaddress;
    private String localip;
    private String inetip;

	private Context mContext;
	private Callback mCallback;

    public Identity(Context context){
		mContext = context;
        update();
	}

	private void register(Callback cb){
		if (cb != null){
			mCallback = cb;
		}
	}

	public void update(){
		uuid = Utils.getUuid(mContext, UUID_FILE);
		localip = Utils.getLocalAddress();
		requestInetAddress();
	}


    public String getUuid() {
		return uuid;
    }

    public String getImei() {
        return imei;
    }

    public String getMeid() {
        return meid;
    }

    public void setMeid(String meid) {
        this.meid = meid;
    }

    public String getSerialno() {
        return serialno;
    }

    public void setSerialno(String serialno) {
        this.serialno = serialno;
    }

    public String getWfmacaddress() {
        return wfmacaddress;
    }

    public void setWfmacaddress(String wfmacaddress) {
        this.wfmacaddress = wfmacaddress;
    }

    public String getBtmacaddress() {
        return btmacaddress;
    }

    public void setBtmacaddress(String btmacaddress) {
        this.btmacaddress = btmacaddress;
    }

    public String getLocalip() {     
        return localip;
    }

    public String getInetip() {
        return inetip;
    }

    public void setInetip(String inetip) {
        this.inetip = inetip;
    }

	private void requestInetAddress(){
		(new Thread(new MyRunnable())).start();
	}


	class MyRunnable implements Runnable {
		@Override
		public void run(){
			inetip = Utils.getInternetAddress();
			if (mCallback != null){
				mCallback.onInetAddress(inetip);
			}
		}
	}

	interface Callback {
		void onInetAddress(String inet);
	}
}
