package com.hellodroid.talkie;

/*
**
** ${FILE}
**   ...
**
** REVISED HISTORY
**   yl7 | 18-2-27: Created
**     
*/

import android.telephony.TelephonyManager;

import com.google.gson.Gson;

import java.util.UUID;

/*
< uses-permission android:name="android.permission.READ_PHONE_STATE" />

*/
public class Myself {
	private static User myself;

	private boolean mInitCompleted = false;
	private TelephonyManager mTelephony;

	private final Gson mGson = new Gson();

	private void Myself(){
		init();
	}

	public static User getMyself(){
		if (myself == null){
			myself = new User();
		}
		return myself;
	}

	private void init(){
        Gson gson = new Gson();
		 myself.setUuid(UUID.randomUUID().toString());
	}

	
	class IdentityRunnable implements Runnable {
		@Override
		public void run() {

		}
	}
}
