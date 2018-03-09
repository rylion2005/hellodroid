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
public class User {
    private String uuid;    // auto generate for devices
    private String imei;    // gsm/tdscdma/wcdma
    private String meid;    // cdma
    private String serialno;
    private String wfmacaddress;
    private String btmacaddress;

	// show to user
    private String name;
    private String nickName;
    private int status;
    private String localip;
    private String inetip;
}
