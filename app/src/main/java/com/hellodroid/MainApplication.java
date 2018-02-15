package com.hellodroid;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;

import com.hellodroid.service.MyDaemonService;


/*
** ==========================================================================================
** MainApplication
**   This is a application class which we can do some global initialization here
**
** ==========================================================================================
*/
public class MainApplication  extends Application  {
    private static final String TAG = "MainApplication";
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "onCreate");
        init();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.v(TAG, "onLowMemory");
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Log.d(TAG, "onTrimMemory(" + level + ")");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /*
    ** ---------------------------------------------------------------------------
    ** init
    **   Global initialization
    **
    ** @PARAM : None
    ** @RETURN: None
    **
    ** NOTES:
    **   ......
    ** ---------------------------------------------------------------------------
    */
    private void init(){
        startService(new Intent(getApplicationContext(), MyDaemonService.class));
    }
}
