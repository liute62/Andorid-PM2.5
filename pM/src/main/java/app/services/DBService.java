package app.services;

import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import app.Entity.State;
import app.utils.Const;
import app.utils.DBHelper;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

/**
 * Created by liuhaodong1 on 15/11/10.
 */
public class DBService extends Service
{
    private DBHelper dbHelper;
    private SQLiteDatabase db;

    private Handler DBHandler = new Handler();
    private Runnable DBRunnable = new Runnable() {
        @Override
        public void run() {
            addPM25();
            DBHandler.postDelayed(DBRunnable, Const.DB_TIME_INTERVAL);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Initial();
    }

    private void Initial(){
        dbHelper = new DBHelper(getApplicationContext());
        db = dbHelper.getReadableDatabase();
    }

    private void addPM25() {
        Double VENTILATION_VOLUME = Const.CURRENT_VENTILATION_VOLUME;
        Double PM25 = Double.valueOf(Const.CURRENT_PM_MODEL.getPm25());
        Double breath = 0.0;
        Double density = Double.valueOf(Const.CURRENT_PM_MODEL.getPm25());
        if (Const.CURRENT_INDOOR) {
            density /= 3;
        }
        if (Const.CURRENT_STATUS == Const.MotionStatus.STATIC) {
            breath = Const.static_breath;
        } else if (Const.CURRENT_STATUS == Const.MotionStatus.WALK) {
            breath = Const.walk_breath;
        } else if (Const.CURRENT_STATUS == Const.MotionStatus.RUN) {
            breath = Const.run_breath;
        }

        VENTILATION_VOLUME += breath;
        PM25 += density*breath;

        State state = new State("0", Long.toString(System.currentTimeMillis()),
                String.valueOf(Const.CURRENT_LONGITUDE),
                String.valueOf(Const.CURRENT_LATITUDE),
                Const.CURRENT_INDOOR? "1":"0",
                Const.CURRENT_STATUS == Const.MotionStatus.STATIC? "1" : Const.CURRENT_STATUS == Const.MotionStatus.WALK? "2" : "3",
                Integer.toString(Const.CURRENT_STEPS_NUM), "12", VENTILATION_VOLUME.toString(), density.toString(), PM25.toString(), "1");
        insertState(state);
       //upload(state);
    }

    /**
     * DB Operations
     * @param state
     */
    private void insertState(State state) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        cupboard().withDatabase(db).put(state);
    }
}
