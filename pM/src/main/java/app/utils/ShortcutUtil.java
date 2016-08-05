package app.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import app.services.DataServiceUtil;

/**
 * Created by liuhaodong1 on 15/11/10.
 */
public class ShortcutUtil {

    public static final String TAG = "ShortcutUtil";

    public static int judgeInput(String user, String pass) {

        if (user == null || pass == null || user.isEmpty() || pass.isEmpty()
                || user.trim().equals("") || pass.trim().equals("")) {
            return 0;
        } else {
            int judge = 0;
            char[] temp = user.toCharArray();
            for (int i = 0; i < temp.length; i++) {
                if (temp[i] == ' ') {
                    judge = 1;
                    break;
                }
            }
            if (judge == 0) {
                judge = 0;
                temp = pass.toCharArray();
                for (int i = 0; i < temp.length; i++) {
                    if (temp[i] == ' ') {
                        judge = 1;
                        break;
                    }
                }
            }
            if (judge == 1) {
                return 2;
            }
        }

        if (user.length() < 8 || pass.length() < 8) {
            return 1;
        }
        return 3;
    }

    public static boolean isWeightInputCorrect(String content){
        if(content == null) return false;
        if(content.trim().equals("")) return false;
        Integer weight = 0;
        try{
            weight = Integer.valueOf(content);
        }catch (Exception e){
            return false;
        }
        if(weight <= 0 || weight >= 200){
            return false;
        }
        return true;
    }

    public static boolean isServiceWork(Context mContext, String serviceName) {
        boolean isWork = false;
        ActivityManager myAM = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> myList = myAM.getRunningServices(40);
        if (myList.size() <= 0) {
            return false;
        }
        for (int i = 0; i < myList.size(); i++) {
            String mName = myList.get(i).service.getClassName().toString();
            if (mName.equals(serviceName)) {
                isWork = true;
                break;
            }
        }
        return isWork;
    }

    /*
   check the availabilty of the network
    */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
        } else {
            NetworkInfo[] info = cm.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static String refFormatNowDate(long currentTimeMillis) {
        Date nowTime = new Date(currentTimeMillis);
        SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss E");
        String retStrFormatNowDate = sdFormatter.format(nowTime);
        return retStrFormatNowDate;
    }

    public static String refFormatDateAndTime(long currentTimeMillis) {
        Date nowTime = new Date(currentTimeMillis);
        SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String retStrFormatNowDate = sdFormatter.format(nowTime);
        return retStrFormatNowDate;
    }

    public static String refFormatDateAndTimeInHour(long currentTimeMillis) {
        String retStrFormatNowDate = refFormatDateAndTime(currentTimeMillis);
        int hour = Integer.valueOf(retStrFormatNowDate.substring(11, 13));
        int minute = Integer.valueOf(retStrFormatNowDate.substring(14, 16));
        if (minute<16) {
            hour-=2;
        } else {
            hour-=1;
        }
        if (hour<10) {
            return retStrFormatNowDate.substring(0,11)+"0"+hour+":00:00";
        } else {
            return retStrFormatNowDate.substring(0,11)+hour+":00:00";
        }
    }

    public static String refFormatOnlyDate(long currentTimeMillis) {
        Date nowTime = new Date(currentTimeMillis);
        SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd");
        String retStrFormatNowDate = sdFormatter.format(nowTime);
        return retStrFormatNowDate;
    }

    public static String getAppVersionName(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            return pi.versionName;
        } catch (Exception e) {
            return "";
        }
    }

    public static String getAppVersionCode(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            return String.valueOf(pi.versionCode);
        } catch (Exception e) {
            e.printStackTrace();
            return "0";
        }
    }

    public static void createShortCut(Activity act, int iconResId,
                                      int appnameResId) {

        Intent shortcutintent = new Intent(
                "com.android.launcher.action.INSTALL_SHORTCUT");
        shortcutintent.putExtra("duplicate", false);
        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
                act.getString(appnameResId));
        Parcelable icon = Intent.ShortcutIconResource.fromContext(
                act.getApplicationContext(), iconResId);
        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_INTENT,
                new Intent(act.getApplicationContext(), act.getClass()));
        act.sendBroadcast(shortcutintent);
    }

    /**
     * transfer the ug scale to mg scale
     *
     * @param ugNumber the ug number
     * @param scale
     * @return
     */
    public static String ugToMg(Double ugNumber, int scale) {
        BigDecimal bd = new BigDecimal(ugNumber / 1000);
        BigDecimal setScale = bd.setScale(scale, bd.ROUND_DOWN);
        return String.valueOf(setScale.doubleValue());
    }

    public static String ugScale(double number, int scale) {
        if (String.valueOf(number).equals("NaN")) {
            number = 0.0;
        }
        BigDecimal bd = new BigDecimal(number);
        BigDecimal setScale = bd.setScale(scale, bd.ROUND_DOWN);
        return String.valueOf(setScale.doubleValue());
    }

    /**
     * A method to transform current time to a point of a day
     * Ex. xxxxYxxMxxD 22:31 returns 22*1+1 = 43
     *
     * @param currentTime
     * @return
     */
    public static int timeToPointOfDay(long currentTime) {
        String date = refFormatNowDate(currentTime);
        int tmp1 = 0, tmp2 = -1, tmp3 = 0;
//        Log.e("date",date);
        for (int i = 0; i != date.length(); i++) {
            if (date.charAt(i) == '-') {
                tmp1 = i;
            }
            if (date.charAt(i) == ':') {
                if (tmp2 == -1) {
                    tmp2 = i;
                } else {
                    tmp3 = i;
                }
            }
        }
        String hour = date.substring(tmp1 + 4, tmp2);
        String min = date.substring(tmp2 + 1, tmp3);
        int add = 0;
        if (Integer.valueOf(min) >= 30) {
            add = 1;
        } else {
            add = 0;
        }
        return Integer.valueOf(hour) * 2 + add;
    }

    /**
     * A method to transform current time to a point of last two hours
     *
     * @param startTime
     * @param currentTime
     * @return
     */
    public static int timeToPointOfTwoHour(long startTime, long currentTime) {
        long diff = currentTime - startTime;
        long twoHour = 2 * 60 * 60 * 1000; //2 hour * 60min * 60 sec * 1000 ms
        double mul = Double.valueOf(diff) / Double.valueOf(twoHour);
        int sec = 60 * 2 / 5;   //2 hour and 5 min a section
        int index = (int) (mul * sec);
        return index;
    }

    public static int timeToPointOfWeek() {
        return 1;
    }

    public static float sumOfArrayNum(Object[] array) {
        int num = array.length;
        if (num == 0) return 0;
        float sum = 0;
        for (int i = 0; i != num; i++) {
            sum += (Float) array[0];
        }
        return sum;
    }

    public static float avgOfArrayNum(Object[] array) {

        return sumOfArrayNum(array) / array.length;
    }

    public static boolean isStringOK(String str) {
        if (str == null || str.trim().equals("")) {
            return false;
        }
        return true;
    }

    public static Float getMaxValueFromMap(Map<Integer, Float> map) {
        Float max = 0.0f;
        for (int i = 0; i != map.keySet().size(); i++) {
            Float tmp = (Float) map.get(i);
            if (tmp > max) {
                max = tmp;
            }
            ;
        }
        return max;
    }

    public static Float getMinusFromMaxToMin(ArrayList<Float> data) {
        if (data.size() == 0) return 0f;
        Float max = data.get(0);
        Float min = data.get(0);
        Float tmp = 0f;
        for (int i = 0; i != data.size(); i++) {
            tmp = data.get(i);
            if (tmp < min) min = tmp;
            if (tmp > max) max = tmp;
        }
        return max - min;
    }

    public static int getMaxIndexFromMap(Map<Integer,Float> map){
        if (map.size() == 0) return 0;
        int max = 0;
        for (Integer key : map.keySet()){
            if(key > max){
                max = key;
            }
        }
        return max;
    }

    public static int getMinIndexFromMap(Map<Integer,Float> map){
        if (map.size() == 0) return 0;
        int min = Integer.MAX_VALUE;
        for (Integer key : map.keySet()){
            if(key < min){
                min = key;
            }
        }
        return min;
    }

    public static int findMaxKeyDistance(Map<Integer,Float> map){
        if (map.size() == 0) return 0;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (Integer key:map.keySet()){
            if(key < min){
                min = key;
            }if(key > max){
                max = key;
            }
        }
        return max - min;
    }

    /**
     *
     * @param lati1
     * @param lati2
     * @param longi1
     * @param longi2
     * @return
     */
    public static boolean isLocationChangeEnough(double lati1,double lati2,double longi1,double longi2){
        if(Math.abs(lati1 - lati2) > 0.0005){
            return true;
        }else if(Math.abs(longi1 - longi2) > 0.0005){
            return true;
        }
        return false;
    }

    public static boolean isDataLost(Map<Integer,Float> map){
        if(map.size() <= 1) return false;
        int total = findMaxKeyDistance(map) + 1;
        ///Log.v(TAG,"total "+String.valueOf(total));
        int num = 0;
        for(Integer key : map.keySet()){
            num++;
            //Log.v(TAG,"key "+String.valueOf(key));
        }
        if(total == num)
            return false;
        else return true;
    }

    public static void setDataLost(String keyForSave,Map<Integer,Float> map){
        if(isDataLost(map)){
            //Log.v("setDataLost", keyForSave + "true");
        }else {
            //Log.v("setDataLost",keyForSave+"false");
        }
    }

    public static double calStaticBreath(int weight){
        float a = 7.8f;
        int num = 13;
        double result = weight * a * num / 1000;
        String resultStr = String.valueOf(result);
        if(resultStr.length() > 5)
            resultStr = resultStr.substring(0,4);
        return Double.valueOf(resultStr);
    }

    public static double calStaticBreath(String weightStr){
        int weight = 0;
        try {
            weight = Integer.valueOf(weightStr);
        }catch (Exception e){
            FileUtil.appendErrorToFile(TAG,"calStaticBreath parsing error1 "+
            weightStr);
            return 0.0f;
        }
        float a = 7.8f;
        int num = 13;
        double result = weight * a * num / 1000;
        String resultStr = String.valueOf(result);
        try {
            if(resultStr.length() > 5)
                resultStr = resultStr.substring(0,4);
            return Double.valueOf(resultStr);
        }catch (Exception e){
            FileUtil.appendErrorToFile(TAG,"calStaticBreath parsing error2 "+
                    resultStr);
        }
        return 0.0;
    }

    public static String subStrLocation(String location){
        String locationStr = location;
        if(location.length() >= 8)
            locationStr.substring(0,7);
        return locationStr;
    }

    public static String subStrLocation(String location,int cut){
        String locationStr = location;
        if(location.length() >= cut)
            locationStr.substring(0,cut - 1);
        return locationStr;
    }

    public static Bitmap getNormalScreenShot(View rootView) {
        FileUtil.makeTmpDir();
        String fname = FileUtil.tmp_path + refFormatNowDate(System.currentTimeMillis()) + ".png";
        rootView.setDrawingCacheEnabled(true);
        rootView.buildDrawingCache();
        Bitmap bitmap = rootView.getDrawingCache();
        if (bitmap != null) {
            try {
                FileOutputStream out = new FileOutputStream(fname);
                bitmap.compress(Bitmap.CompressFormat.PNG,100,out);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }else {
            Log.e(TAG,"getNormalScreenShot bitmap == null");
        }
        return bitmap;
    }

    public static void removeNormalScreenShots() {
       File dir = new File(FileUtil.tmp_path);
       if(dir.exists() && dir.isDirectory()){
           String[] children = dir.list();
           for (int i = 0; i < children.length; i++)
           {
               new File(dir, children[i]).delete();
           }
       }
    }

    public static  boolean isInitialized(DataServiceUtil dataServiceUtil){

        Double lati = dataServiceUtil.getLatitudeFromCache();
        Double longi = dataServiceUtil.getLongitudeFromCache();
        if(lati == 0.0 || longi == 0.0) return false;
        double density = dataServiceUtil.getPM25Density();
        if(density == -1) return false;
        return true;
    }
}
