package com.hellodroid.talkie;

import android.content.Context;

import com.google.gson.Gson;

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
class Contacts {
    private static final String TAG = "Contacts";
	private static final String CONTACT_FILE = "contacts.db";

    private final User myself = Myself.getMyself();
    private final List<User> mUserList = new ArrayList<>();

    private final Gson mGson = new Gson();


    public Contacts(Context context){
	}

    public User getMyself(){
        return myself;
    }

    public void add(User user){
        mUserList.add(user);
    }

    public void update(int index, User user){
        //mUserList.get(index).copy(profile);
    }

    public void delete(int uuid){

        Iterator<User> iterator = mUserList.iterator();
        while(iterator.hasNext()){
            User p = iterator.next();
            //if(p.uuid.equals(uuid){
            //    iterator.remove();
            //}
        }
    }
}
