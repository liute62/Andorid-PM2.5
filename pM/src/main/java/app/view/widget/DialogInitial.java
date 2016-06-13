package app.view.widget;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.pm.R;

import org.json.JSONException;
import org.json.JSONObject;

import app.model.PMModel;
import app.services.DataServiceUtil;
import app.services.LocationServiceUtil;
import app.services.NotifyServiceUtil;
import app.utils.Const;
import app.utils.FileUtil;
import app.utils.HttpUtil;
import app.utils.ShortcutUtil;
import app.utils.VolleyQueue;

/**
 * Created by liuhaodong1 on 16/6/13.
 */
public class DialogInitial extends Dialog implements View.OnClickListener{

    public static final String TAG = "DialogInitial";

    private Activity mActivity = null;

    private Context mContext;

    private DataServiceUtil dataServiceUtil;

    private LocationServiceUtil locationServiceUtil;

    private boolean isSuccess;

    private boolean isSearchDensity = false;

    private boolean isSearchLocation = false;

    private TextView mLati;

    private TextView mLongi;

    private TextView mDensity;

    private TextView mLocalization;

    private TextView mSearch;

    private Button mSuccess;

    private Button mCancel;

    private Handler mHandler;

    public DialogInitial(Context context,Handler handler) {
        super(context);
        mHandler = handler;
        mContext = context;
        dataServiceUtil = DataServiceUtil.getInstance(context);
        locationServiceUtil = LocationServiceUtil.getInstance(context);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setCancelable(false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.widget_dialog_initial);

        mSearch = (TextView)findViewById(R.id.initial_search_density);
        mLocalization = (TextView)findViewById(R.id.initial_search_location);
        mSuccess = (Button)findViewById(R.id.initial_success);
        mCancel = (Button) findViewById(R.id.initial_back);
        mLati = (TextView)findViewById(R.id.initial_lati);
        mLongi = (TextView)findViewById(R.id.initial_longi);
        mDensity = (TextView)findViewById(R.id.initial_density);

        mSearch.setOnClickListener(this);
        mLocalization.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mCancel.setOnClickListener(this);

        mDensity.setText(""+dataServiceUtil.getPM25Density());
        mLongi.setText(""+dataServiceUtil.getLongitude());
        mLati.setText(""+dataServiceUtil.getLatitude());

        checkSuccessAvailable();
    }

    private void checkSuccessAvailable(){
        if(ShortcutUtil.isInitialized(dataServiceUtil)){
            mSuccess.setEnabled(true);
        }else {
            mSuccess.setEnabled(false);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.initial_search_density:
                searchDensity();
                break;
            case R.id.initial_search_location:
                searchLocation();
                break;
            case R.id.initial_success:
                isSuccess = true;
                mHandler.sendEmptyMessage(Const.Handler_Initial_Success);
                DialogInitial.this.dismiss();
                break;
            case R.id.initial_back:
                DialogInitial.this.dismiss();
                break;
        }
    }

    private void searchDensity(){

        if(!isSearchDensity){
            double lati = dataServiceUtil.getLatitude();
            double longi = dataServiceUtil.getLongitude();
            if(lati != 0.0 && longi != 0.0) {
                isSearchDensity = true;
                mSearch.setTextColor(Color.GRAY);
                searchPMResult(String.valueOf(longi),String.valueOf(lati));
            }
        }
    }

    private void searchDensityFinished(){
        isSearchDensity = false;
        mSearch.setTextColor(Color.BLUE);
        updateDensity();
        checkSuccessAvailable();
    }

    private void updateDensity(){

        if(dataServiceUtil.getPM25Density() != -1){
            mDensity.setText(String.valueOf(dataServiceUtil.getPM25Density()));
        }
    }

    private void searchLocation(){

        if(!isSearchLocation){
            isSearchLocation = true;
            mLocalization.setTextColor(Color.GRAY);
            locationServiceUtil.setGetTheLocationListener(new LocationServiceUtil.GetTheLocation() {
                @Override
                public void onGetLocation(Location location) {

                }

                @Override
                public void onSearchStop(Location location) {
                    if (location != null)
                        dataServiceUtil.cacheLocation(location);
                    else  Toast.makeText(mContext,Const.Info_Failed_Location,Toast.LENGTH_SHORT).show();
                    searchLocationFinished();
                }
            });
            locationServiceUtil.run(LocationServiceUtil.TYPE_BAIDU);
        }
    }

    private void searchLocationFinished(){
        isSearchLocation = false;
        mLocalization.setTextColor(Color.BLUE);
        updateLocation();
        checkSuccessAvailable();
    }

    private void updateLocation(){
        if(dataServiceUtil.getLatitude() != 0.0 && dataServiceUtil.getLongitude() != 0.0){
            mLati.setText(String.valueOf(dataServiceUtil.getLatitude()));
            mLongi.setText(String.valueOf(dataServiceUtil.getLongitude()));
        }
    }

    private void searchPMResult(String longitude, String latitude) {

        String url = HttpUtil.Search_PM_url;
        url = url + "?longitude=" + longitude + "&latitude=" + latitude;
        FileUtil.appendStrToFile(TAG,"searchPMResult " + url);
        Log.e(TAG,url);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    int status = response.getInt("status");
                    if (status == 1) {
                        PMModel pmModel = PMModel.parse(response.getJSONObject("data"));
                        NotifyServiceUtil.notifyDensityChanged(pmModel.getPm25());
                        double PM25Density = Double.valueOf(pmModel.getPm25());
                        int PM25Source = pmModel.getSource();
                        dataServiceUtil.cachePMResult(PM25Density, PM25Source);
                        FileUtil.appendStrToFile(TAG,"3.search pm density success, density: " + PM25Density);
                    } else {
                        Toast.makeText(mContext,Const.Info_Failed_PMDensity,Toast.LENGTH_SHORT).show();
                        FileUtil.appendErrorToFile(-1,"search pm density failed, status != 1");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                searchDensityFinished();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                searchDensityFinished();
                Toast.makeText(mContext,Const.Info_Failed_PMDensity,Toast.LENGTH_SHORT).show();
                FileUtil.appendErrorToFile(-1, "search pm density failed " + error.getMessage() + " " + error);
            }

        });
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                Const.Default_Timeout,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        VolleyQueue.getInstance(mContext.getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

    public void setActivity(Activity activity){
        this.mActivity = activity;
    }

    @Override
    protected void onStop() {
        if(!isSuccess && mActivity != null){
            mActivity.finish();
        }
        super.onStop();
    }
}
