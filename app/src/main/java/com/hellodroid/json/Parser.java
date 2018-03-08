package com.hellodroid.json;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/*
**
** ${FILE}
**   ...
**
** REVISED HISTORY
**   yl7 | 18-2-27: Created
**     
*/
public class Parser {

    private Parser(){}

    // var returnCitySN = {"cip": "101.81.72.109", "cid": "310000", "cname": "上海市"};
    public static Object parse(String objects, String key){
        Object cip = null;
        try {
            JSONObject ob = new JSONObject(objects);
            //cip = ob.get("cip");
            //Object cid = (String) city.get("cid");
            //Object cname = (String) city.get("cname");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return cip;
    }
}
