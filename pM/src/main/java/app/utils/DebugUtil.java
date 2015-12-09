package app.utils;

import android.util.Log;

import java.util.Map;

import javax.security.auth.login.LoginException;

/**
 * Created by Administrator on 12/9/2015.
 */
public class DebugUtil {

    public static void print(String TAG,String Info){
        Log.e(TAG,Info);
    }

    public static void printMap(String TAG,Map map){
        print(TAG,"-------------map-------------");
        for (Object key : map.keySet()){
            print(String.valueOf(key),String.valueOf(map.get(key)));
        }
        print(TAG,"-------------map-------------");
    }
}
