package app.view.widget;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.nfc.tech.TagTechnology;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

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

    Activity mActivity = null;

    Context mContext;

    DataServiceUtil dataServiceUtil;

    LocationServiceUtil locationServiceUtil;

    boolean isSuccess;

    boolean isSearchDensity = false;

    boolean isSearchLocation = false;

    TextView mLati;

    TextView mLongi;

    TextView mDensity;

    TextView mLocalization;

    TextView mSearch;

    Button mSuccess;

    Button mCancel;

    Handler mHandler;

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

    private void searchDensity(){

        if(!isSearchDensity){
            double lati = dataServiceUtil.getLatitude();
            double longi = dataServiceUtil.getLongitude();
            if(lati != 0.0 && longi != 0.0) {
                isSearchDensity = true;
                mSearch.setTextColor(Color.GRAY);
                searchPMResult(String.valueOf(lati),String.valueOf(longi));
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
                    if(location != null)
                        dataServiceUtil.cacheLocation(location);
                    searchLocationFinished();
                }
            });
        }
    }

    private void searchLocationFinished(){
        isSearchLocation = false;
        mSearch.setTextColor(Color.BLUE);
        updateLocation();
        checkSuccessAvailable();
    }

    private void updateLocation(){
        if(dataServiceUtil.getLatitude() != 0.0 && dataServiceUtil.getLongitude() != 0.0){
            mLati.setText(String.valueOf(dataServiceUtil.getLatitude()));
            mLongi.setText(String.valueOf(dataServiceUtil.getLongitude()));
        }
    }

    /**
     * Get and Update Current PM info.
     *
     * @param longitude the current zone longitude
     * @param latitude  the current zone latitude
     */
    private void searchPMResult(String longitude, String latitude) {

        String url = HttpUtil.Search_PM_url;
        url = url + "?longitude=" + longitude + "&latitude=" + latitude;
        FileUtil.appendStrToFile(TAG,"searchPMResult " + url);
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
}
