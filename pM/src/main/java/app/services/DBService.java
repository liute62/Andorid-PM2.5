package app.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by liuhaodong1 on 15/11/10.
 */
public class DBService extends Service
{
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
