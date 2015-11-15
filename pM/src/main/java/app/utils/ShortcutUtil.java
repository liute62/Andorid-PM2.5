package app.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Parcelable;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by liuhaodong1 on 15/11/10.
 */
public class ShortcutUtil {

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

    public static String refFormatNowDate(long currentTimeMillis) {
        Date nowTime = new Date(currentTimeMillis);
        SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss E");
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
     * @param ugNumber the ug number
     * @param scale
     * @return
     */
    public static String ugToMg(Double ugNumber,int scale){
        BigDecimal bd = new BigDecimal(ugNumber / 1000);
        BigDecimal setScale = bd.setScale(scale, bd.ROUND_DOWN);
        return String.valueOf(setScale.doubleValue());
    }
}
