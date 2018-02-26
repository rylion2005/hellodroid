package com.hellodroid.json;

import android.content.Context;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import org.json.JSONException;
import org.json.JSONObject;


public class Jsoner {
    public static final String TAG = "Jsoner";

    private Context mContext;
    private String mPath;
    private String mFileName;
    private String[] mKeys;
    private Object mLock;

    public Jsoner(Context context, String file, String[] keys) {
        mContext = context;
        mFileName = file;
        mKeys = keys;
        mLock = new Object();
    }

    public Jsoner(){

    }

    public void configure(Context context, String fileName){
        mContext = context;
        mFileName = fileName;
    }

    public void configure(String path, String name){
        mPath = path;
        mFileName = name;
    }
    public String getFileName(){
        return mFileName;
    }

    public String[] getKeys(){
        return mKeys;
    }

    private boolean hasKey(String Key){
        boolean result = false;
        return result;
    }

    public Object get(String key){
        Object obj = null;
        return obj;
    }

    public boolean isFileExisted() {
        boolean existed = false;

        try {
            FileInputStream fis = mContext.openFileInput(mFileName);
            if(fis.available() > 0){
                existed = true;
            }
            fis.close();
        } catch (IOException|NullPointerException e) {
            //
        }
        return existed;
    }

    public void deleteFile() {
        mContext.deleteFile(mFileName);
    }

    public void add(JSONObject object){
        ArrayList<JSONObject> objList = null;

        if(object == null){
            return;
        }

        if(object.length() == 0){
            return;
        }

        objList = read();
        if(objList == null){
            objList = new ArrayList<JSONObject>();
        }

        objList.add(object);
        rebuild(objList);
    }

    public void remove(int position){
        ArrayList<JSONObject> objList = read();
        if((objList == null) || (objList.size() == 0)){
            return;
        }

        if((position < 0) || (position > objList.size())){
            Log.e(TAG, "position is not in range");
            return;
        }

        objList.remove(position);
        rebuild(objList);
    }

    public boolean write(JSONObject object, String key){
        boolean result = false;

        try{
            ArrayList<JSONObject> objectList = read();
            if((objectList == null) || (objectList.size() == 0)){
                Log.e(TAG, "Empty file");
                return false;
            }

            if(!object.has(key)){
                Log.e(TAG, "key not found");
                return false;
            }

            for(int k = 0; k < objectList.size(); k++){
                if(objectList.get(k).has(key)){
                    if(objectList.get(k).getString(key).equals(object.getString(key))){
                        objectList.set(k, object);
                        Log.e(TAG, "Over write !!! ");
                        result = true;
                        break;
                    }
                }
            }

            if(result){
                rebuild(objectList);
            }
        }catch (NullPointerException|JSONException e){
            e.printStackTrace();
        }

        return result;
    }

    public void rebuild(ArrayList<JSONObject> objList) {
        synchronized (mLock) {
            try {
                if (isFileExisted()) {
                    mContext.deleteFile(mFileName);
                }

                if((objList == null) || (objList.size() == 0)){
                    return;
                }

                if ((mKeys == null)||(mKeys.length == 0)) {
                    return;
                }

                FileOutputStream fos = mContext.openFileOutput(mFileName, Context.MODE_APPEND);
                JsonWriter jw = new JsonWriter(new OutputStreamWriter(fos, "UTF-8"));
                jw.beginArray();
                for (int i = 0; i < objList.size(); i++) {
                    jw.beginObject();
                    for (int j = 0; j < mKeys.length; j++) {
                        try {
                            if (objList.get(i).getString(mKeys[j]) != null) {
                                jw.name(mKeys[j]).value(objList.get(i).getString(mKeys[j]));
                            }
                        }catch (JSONException e){
                            //Log.e(TAG, "ignored keys: " +  mKeys[j]);
                        }
                    }
                    jw.endObject();
                }
                jw.endArray();
                jw.close();
                fos.close();
            } catch (IOException|NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    public ArrayList<JSONObject> read() {
        ArrayList<JSONObject> objList = null;

        if (!isFileExisted()) {
            return null;
        }

        try {
            FileInputStream fis = mContext.openFileInput(mFileName);
            if (fis.available() <= 0) {
                Log.e(TAG, "0 byte");
                return null;
            }
            JsonReader jr = new JsonReader(new InputStreamReader(fis, "UTF-8"));
            jr.beginArray();
            objList = new ArrayList<JSONObject>();
            while (jr.hasNext()) {
                jr.beginObject();
                JSONObject obj = new JSONObject();
                while (jr.hasNext()) {
                    obj.put(jr.nextName(), jr.nextString());
                }
                jr.endObject();
                objList.add(obj);
            }
            jr.endArray();
            jr.close();
        } catch (JSONException|IOException e) {
            e.printStackTrace();
        }
        return objList;
    }

    public void append(byte[] buffer){

        synchronized (mLock) {
            if (mFileName == null) {
                Log.e(TAG, "Null file");
                return;
            }

            try {
                FileOutputStream fos = mContext.openFileOutput(mFileName, Context.MODE_APPEND);
                fos.write(buffer, 0, buffer.length);
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

/* ********************************************************************************************** */

    public FileInputStream openInput(){
        FileInputStream fis = null;
        return fis;
    }

    public FileOutputStream openOut(){
        FileOutputStream fos = null;
        return fos;
    }
}



