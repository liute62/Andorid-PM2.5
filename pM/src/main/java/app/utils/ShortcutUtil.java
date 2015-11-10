package app.utils;

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
}
