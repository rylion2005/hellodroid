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

    private User myself;
    private final List<User> mUserList = new ArrayList<>();

    public Contacts(){}

    public User getMyself(){
        return myself;
    }

    public User newUser(){
        return new User();
    }

    public void add(User profile){
        mUserList.add(profile);
    }

    public void update(int index, User profile){
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

    private void genMyself(){
        // Gen uuid
    }
}
