package app.services;

import android.content.Context;
import android.content.Intent;

import app.utils.Const;

/**
 * Created by liuhaodong1 on 16/6/12.
 *
 */
public class NotifyServiceUtil {


    public static void notifyLocationChanged(Context context,double latitude,double longitude){
        Intent intentText = new Intent(Const.Action_DB_MAIN_Location);
        intentText.putExtra(Const.Intent_DB_PM_Lati, String.valueOf(latitude));
        intentText.putExtra(Const.Intent_DB_PM_Longi, String.valueOf(longitude));
        context.sendBroadcast(intentText);
    }

    public static void notifyDensityChanged(Context context,String density){
        Intent intent = new Intent(Const.Action_DB_MAIN_PMDensity);
        intent.putExtra(Const.Intent_PM_Density, density);
        context.sendBroadcast(intent);
    }

    /**
     *
     * @param context
     * @param state
     */
    public static void notifyPMSearchState(Context context,int state){
        Intent intent = new Intent(Const.Action_DB_Running_State);
        intent.putExtra(Const.Intent_DB_Run_State, state);
        context.sendBroadcast(intent);
    }

    public static void notifyCityChanged(Context context,double latitude,double longitude){
        Intent intentText = new Intent(Const.Action_DB_MAIN_Location);
        intentText.putExtra(Const.Intent_DB_PM_Lati, String.valueOf(latitude));
        intentText.putExtra(Const.Intent_DB_PM_Longi, String.valueOf(longitude));
        intentText.putExtra(Const.Intent_DB_City_Ref, 1);
        context.sendBroadcast(intentText);
    }

    public static void notifyRefreshChart(Context context){
        Intent intent = new Intent(Const.Action_Refresh_Chart_ToService);
        context.sendBroadcast(intent);
    }
}
