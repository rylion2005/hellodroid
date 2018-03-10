package com.hellodroid.talkie;

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

    public User(){}

    // convert user to json string
    public String toJsonString(){
        StringBuilder sb = new StringBuilder();
        sb.append(this.uuid);
        sb.append(",");
        sb.append(this.localip);
        return sb.toString();
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getLocalip() {
        return localip;
    }

    public void setLocalip(String localip) {
        this.localip = localip;
    }

    public String getInetip() {
        return inetip;
    }

    public void setInetip(String inetip) {
        this.inetip = inetip;
    }
}
