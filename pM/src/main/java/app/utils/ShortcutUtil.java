package app.utils;

import android.app.ActivityManager;
import android.content.Context;

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
}
