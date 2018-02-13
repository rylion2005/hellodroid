package com.hellodroid.file;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

/*
**
** ${FILE}
**   ...
**
** REVISED HISTORY
**   yl7 | 18-2-13: Created
**     
*/
public class Utils {
    private static final String TAG = "Utils";

    private static Context mContext;

    public Utils(Context context){
        mContext = context;
    }

    public static String getFileName(Context context, final Uri uri){
        String path = getFileWithPath(context, uri);
        String name = path.substring(path.lastIndexOf("/") + 1, path.length());
        return name;
    }

    public static String getPath(Context context, final Uri uri){
        String path = getFileWithPath(context, uri);
        String name = path.substring(0, path.lastIndexOf("/"));
        return name;
    }

    public static String getFileWithPath(Context context, final Uri uri) {
        if ( null == uri ) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if ( scheme == null )
            data = uri.getPath();
        else if ( ContentResolver.SCHEME_FILE.equals( scheme ) ) {
            data = uri.getPath();
        } else if ( ContentResolver.SCHEME_CONTENT.equals( scheme ) ) {
            Cursor cursor = context.getContentResolver().query(
                    uri,
                    new String[] { MediaStore.Images.ImageColumns.DATA },
                    null,
                    null,
                    null );

            if ( null != cursor ) {
                if ( cursor.moveToFirst() ) {
                    int index = cursor.getColumnIndex( MediaStore.Images.ImageColumns.DATA );
                    if ( index > -1 ) {
                        data = cursor.getString( index );
                    }
                }
                cursor.close();
            }
        }
        return data;
    }
}
