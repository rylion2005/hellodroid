package com.hellodroid.talkie;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.StringBuilderPrinter;

import com.google.gson.Gson;
import com.hellodroid.identity.Identity;
import com.hellodroid.network.MyNetworkReceiver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/*
**
** ${FILE}
**   ...
**
** REVISED HISTORY
**   yl7 | 18-2-27: Created
**     
*/
/* ********************************************************************************************** */
class Contacts implements ContactUpdater.Callback, MyNetworkReceiver.CallBack{
    private static final String TAG = "Contacts";
	private static final String CONTACT_FILE = "contacts.db";

    private final List<User> mUserList = new ArrayList<>();
    private final ContactUpdater mUpdater = new ContactUpdater();
    private final MyNetworkReceiver mReceiver;
    private final Identity myself;

    public Contacts(Context context){
        myself = new Identity(context);
        mReceiver = MyNetworkReceiver.getInstance(context);
        mReceiver.register(this);
        mUpdater.register(this);
        //mUpdater.broadcastMyself(myself.toString().getBytes(), myself.toString().length());
	}

    @Override
    public void onBytesReceived(byte[] bytes, int length) {
        User user = fromBytes(bytes, length);
        if (hasUuid(user.getUuid())){
            update(user);
        } else {
            mUserList.add(user);
        }
    }

    @Override
    public void onNetworkStatusChanged() {
        myself.update();
        mUpdater.broadcastMyself(myself.toString().getBytes(), myself.toString().length());
    }

    @Override
    public void onWifiConnectivity(boolean connected) {

    }

    @Override
    public void onMobileConnectivity(boolean connected) {

    }

    private User fromBytes(byte[] bytes, int length){
        User user = null;
        String buffer = new String(bytes);
        String uuid = buffer.substring(0, buffer.indexOf(','));
        String ip = buffer.substring(buffer.indexOf(',')+1, length+1);
        user.setUuid(uuid);
        user.setLocalip(ip);
        return user;
    }

    private boolean hasUuid(String uuid){
        boolean existed = false;
        for (User u: mUserList){
            if (u.getUuid().equals(uuid)){
                existed = true;
            }
        }
        return existed;
    }

    private void update(User user){
        for (User u : mUserList){
            if (u.getUuid().equals(user.getUuid())){
                u.setUuid(user.getUuid());
            }
        }
    }

    private void rebuild(List<User> users){
    }
}
