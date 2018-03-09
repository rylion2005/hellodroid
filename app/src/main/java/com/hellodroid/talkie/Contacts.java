package com.hellodroid.talkie;

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

	private static final String[] KEYS = {
		"uuid","imei","meid","serialno","wfmac","btmac","name","nickname","localip","inetip","status"
	};

	private static final String CONTACT_FILE = "contacts.db";

	private static Jsoner mJsoner;

    private final User myself = Myself.getMyself();
    private final List<User> mUserList = new ArrayList<>();


    public Contacts(Context context){
		if (mJsoner == null){
        	mJsoner = new Jsoner(context, CONTACT_FILE, KEYS);
			mUserList = mJsoner.read();
		}
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
