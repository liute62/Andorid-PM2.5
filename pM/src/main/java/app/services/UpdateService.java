package app.services;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import app.Entity.State;
import app.utils.ACache;
import app.utils.Const;
import app.utils.DBConstants;
import app.utils.DBHelper;
import app.utils.HttpUtil;
import app.utils.VolleyQueue;
import nl.qbusict.cupboard.QueryResultIterable;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

/**
 * Created by Jason on 2015/12/14.
 * Step 1: get all not upload
 * Step 2: get real density
 * Step 3: insert into the database with real density
 * Step 4: update the whole pm25
 * Step 5: upload the real state
 */
public class UpdateService extends Service {
    private DBHelper dbHelper;
    private SQLiteDatabase db;
    private ACache aCache;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = new DBHelper(getApplicationContext());
        db = dbHelper.getReadableDatabase();
    }

    //main process
    private void run() {
        boolean isConnected = isNetworkAvailable(this);
        String userid = aCache.getAsString(Const.Cache_User_Id);
        if (userid != null && !userid.equals("")) {
            if (isConnected) {
                synchronized (this) {
                    List<State> states = this.getAllNotUpload();
                    if (states != null) {
                        while (states.size() > 0 && isConnected) {
                            State s = states.remove(0);
                            UpdateDensity(s);
                        }
                    }
                }
            }
        }
    }

    /*
    get all not uploaded
     */
    private List<State> getAllNotUpload() {
        Cursor c = cupboard().withDatabase(db).query(State.class).getCursor();
        QueryResultIterable<State> itr = cupboard().withCursor(c).iterate(State.class);
        try{
            List<State> siList = new ArrayList<State>();
            for (State state:itr) {
                if (state.getUpload()==0) {
                    siList.add(state);
                }
            }
            return siList;
        } catch(Exception e) {
            Log.e("error","update is fail");
        }finally{
            c.close();
        }
        return null;
    }

    private void updateStateDensity(State state,String density) {
        ContentValues values = new ContentValues();
        values.put(DBConstants.DB_MetaData.STATE_DENSITY_COL, density);
        cupboard().withDatabase(db).update(State.class,values,"id = ?",state.getId()+"");
    }

    private void updateStateHasUpload(State state,int hasUpload) {
        ContentValues values = new ContentValues();
        values.put(DBConstants.DB_MetaData.STATE_HAS_UPLOAD, hasUpload);
        cupboard().withDatabase(db).update(State.class,values,"id = ?",state.getId()+"");
    }

    /*
    check the network is available
     */
    private boolean isNetworkAvailable(Context context) {
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

    /*
    update state with real density
     */
    public void UpdateDensity(State state) {
        String url = HttpUtil.Search_PM_url + "?longitude=" + state.getLongtitude() + "&latitude=" + state.getLatitude()
                + "&time_point=" + state.getTime_point();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String mDensity = String.valueOf(response.getDouble("PM25"));
                    //update density
                    updateStateDensity(state, mDensity);
                    //update total pm25 volume
                    updateTotalPM25(state,mDensity);
                    //upload state and check whether upload success
                    state.setDensity(mDensity);
                    upload(state);
                } catch (JSONException e) {
                    Log.e("error",e.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), "cannot connect to the server", Toast.LENGTH_SHORT).show();
            }
        });

        VolleyQueue.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

    /*
    update total pm2.5
     */
    private void updateTotalPM25(State state, String mDensity) {
        double breath = 0;
        Double density = new Double(Double.valueOf(mDensity)-Double.valueOf(state.getDensity()));
        Boolean mIndoor =  state.getOutdoor().equals("0")?true:false;
        Const.MotionStatus mMotionStatus = state.getStatus().equals("1")? Const.MotionStatus.STATIC:state.getStatus().equals("2")? Const.MotionStatus.WALK: Const.MotionStatus.RUN;

        if (mMotionStatus == Const.MotionStatus.STATIC) {
            breath = Const.static_breath;
        } else if (mMotionStatus == Const.MotionStatus.WALK) {
            breath = Const.walk_breath;
        } else if (mMotionStatus == Const.MotionStatus.RUN) {
            breath = Const.run_breath;
        }

        double PM25 = density*breath/60/1000;
    }

    /*
    upload data
     */
    public void upload(final State state)  {
        String url = HttpUtil.Upload_url;

        JSONObject tmp = State.toJsonobject(state, aCache.getAsString(Const.Cache_User_Id));

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, tmp,  new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                updateStateHasUpload(state, 1);
                Log.d("upload","Upload Success");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("hasUpload:", "upload failed");
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json; charset=UTF-8");
                return headers;
            }
        };
        VolleyQueue.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

}
