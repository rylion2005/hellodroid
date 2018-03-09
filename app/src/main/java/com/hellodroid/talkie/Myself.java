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

import java.util.UUID;

/*
< uses-permission android:name="android.permission.READ_PHONE_STATE" />

*/
public class Myself {
	private static User myself;
	private boolean mInitCompleted = false;
	private TelephonyManager mTelephony;

	private void Myself(){
		init();
	}

	public Myself getMyself(){
		if (myself == null){
			myself = new Myself();
		}
		return myself;
	}

	private init(){

		 myself.setUuid(UUID.randomUUID().toString());
	}

	
	class IdentityRunnable implements Runnable {
        @override
		public void run(){
			
		}
    }
}
