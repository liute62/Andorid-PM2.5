package app.view.widget;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.pm.R;

import org.json.JSONException;
import org.json.JSONObject;

import app.model.PMModel;
import app.utils.ACache;
import app.utils.Const;
import app.utils.HttpUtil;
import app.utils.ShortcutUtil;
import app.utils.VolleyQueue;

/**
 * Created by liuhaodong1 on 16/1/30.
 */
public class DialogGetDensity extends Dialog implements View.OnClickListener
{

    public static final String TAG = "DialogGetDensity";
    ACache aCache;
    Context mContext;
    boolean isRunning;
    PMModel pmModel;
    Double PM25Density;
    TextView mLoading;
    TextView mLati;
    TextView mLongi;
    TextView mDensity;
    Button mCancel;
    Button mSearch;
    boolean isStop;

    Runnable mRunnable = new Runnable() {

        int num = 0;
        @Override
        public void run() {
            if(!isStop) {
                if (num == 0) {
                    mLoading.setText(mContext.getResources().getString(R.string.dialog_base_loading));
                } else if (num == 1) {
                    mLoading.setText(mContext.getResources().getString(R.string.dialog_base_loading) + ".");
                } else if (num == 2) {
                    mLoading.setText(mContext.getResources().getString(R.string.dialog_base_loading) + "..");
                } else if (num >= 3) {
                    mLoading.setText(mContext.getResources().getString(R.string.dialog_base_loading) + "...");
                    num = 0;
                }
                num++;
            }
            mHandler.postDelayed(mRunnable,300);
        }

    };

    Handler mHandler = new Handler();

    public DialogGetDensity(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.widget_dialog_get_density);
        isRunning = false;
        mCancel = (Button)findViewById(R.id.get_density_back);
        mSearch = (Button)findViewById(R.id.get_density_confirm);
        mLati = (TextView)findViewById(R.id.get_density_lati);
        mLongi = (TextView)findViewById(R.id.get_density_longi);
        mDensity = (TextView)findViewById(R.id.get_density_density);
        mLoading = (TextView)findViewById(R.id.get_density_loading);
        mCancel.setOnClickListener(this);
        mSearch.setOnClickListener(this);
        init();
    }

    private void init(){
        aCache = ACache.get(mContext);
        String longiStr = aCache.getAsString(Const.Cache_Longitude);
        String latiStr = aCache.getAsString(Const.Cache_Latitude);
        String density = aCache.getAsString(Const.Cache_PM_Density);
        if(ShortcutUtil.isStringOK(latiStr)) mLati.setText(latiStr);
        if(ShortcutUtil.isStringOK(longiStr)) mLongi.setText(longiStr);
        if(ShortcutUtil.isStringOK(density)) mDensity.setText(density);
    }

    private void setStop(){
        isStop = true;
        mLoading.setText("");
    }

    private void setRun(){
        isStop = false;
    }

    private void notifyService(Double value){
        Intent intent = new Intent(Const.Action_Search_Density_ToService);
        intent.putExtra(Const.Intent_PM_Density,value);
        mContext.sendBroadcast(intent);
    }

    /**
     * Get and Update Current PM info.
     *
     * @param longitude
     * @param latitude
     */
    private void searchPMRequest(String longitude, String latitude) {
        mSearch.setEnabled(false);
        mSearch.setClickable(false);
        setRun();
        isRunning = true;
        String url = HttpUtil.Search_PM_url;
        url = url + "?longitude=" + longitude + "&latitude=" + latitude;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                isRunning = false;
                mSearch.setEnabled(true);
                mSearch.setClickable(true);
                setStop();
                try {
                    int status = response.getInt("status");
                    if(status == 1) {
                        pmModel = PMModel.parse(response.getJSONObject("data"));
                        Intent intent = new Intent(Const.Action_DB_MAIN_PMDensity);
                        intent.putExtra(Const.Intent_PM_Density, pmModel.getPm25());
                        //set current pm density for calculation
                        PM25Density = Double.valueOf(pmModel.getPm25());
                        Log.e(TAG, "searchPMRequest PM2.5 Density " + String.valueOf(PM25Density));
                        mDensity.setText(String.valueOf(PM25Density));
                        aCache.put(Const.Cache_PM_Density, PM25Density);
                        notifyService(PM25Density);
                        Toast.makeText(mContext.getApplicationContext(), Const.Info_PMDATA_Success, Toast.LENGTH_SHORT).show();
                    }else {
                        String str = response.getString("message");
                        mDensity.setText(str);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    mDensity.setText("server error");
                }
                Log.e(TAG, "searchPMRequest resp:" + response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(error != null){
                    if(error.getMessage() != null) {
                        Log.e(TAG, error.getMessage());
                        mDensity.setText(error.getMessage());
                        if (error.getMessage().trim().equals("org.json.JSONException: End of input at character 0 of")) {
                            mDensity.setText(Const.Info_No_PMDensity);
                            //Toast.makeText(mContext.getApplicationContext(),Const.Info_No_PMDensity,Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                isRunning = false;
                mSearch.setEnabled(true);
                mSearch.setClickable(true);
                setStop();
            }

        });
        VolleyQueue.getInstance(mContext.getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.get_density_back:
                DialogGetDensity.this.dismiss();
                break;
            case R.id.get_density_confirm:
                mRunnable.run();
                if(!isRunning)
                    searchPMRequest(mLongi.getText().toString(),mLati.getText().toString());
                break;
        }
    }
}
