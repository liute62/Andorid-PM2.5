package app.view.widget;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.pm.R;

import org.json.JSONException;
import org.json.JSONObject;

import app.utils.ACache;
import app.utils.Const;
import app.utils.HttpUtil;
import app.utils.ShortcutUtil;
import app.utils.VolleyQueue;

/**
 * Created by Administrator on 1/18/2016.
 */
public class DialogGetCity extends Dialog implements View.OnClickListener{

    public static final String loadingText = "正在搜索";
    public static final String successText = "搜索成功";

    Context mContext;
    Handler mHandler;
    TextView mLoading;
    TextView mCity;
    TextView mLati;
    TextView mLongi;
    Button mConfirm;
    Button mBack;
    ACache aCache;
    boolean isSearchTaskRun;
    boolean isSuccess;
    boolean isRunnableRun;
    String longiStr;
    String latiStr;

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what == Const.Handler_City_Name){
                isRunnableRun = false;
                mCity.setText((String)msg.obj);
                mLoading.setText(successText);
            }
        }
    };

    Runnable background = new Runnable() {

        int num = 0;
        @Override
        public void run() {
            if (isRunnableRun) {
               if (!isSearchTaskRun && !isSuccess) {
                    if (ShortcutUtil.isStringOK(latiStr) && ShortcutUtil.isStringOK(longiStr))
                        searchCityRequest(latiStr, longiStr);
                }
                if (num == 0) {
                    mLoading.setText(loadingText);
                } else if (num == 1) {
                    mLoading.setText(loadingText + ".");
                } else if (num == 2) {
                    mLoading.setText(loadingText + "..");
                } else {
                    mLoading.setText(loadingText + "...");
                    num = -1;
                }
                num++;
                handler.postDelayed(this, 300);
            }
        }
    };

    public DialogGetCity(Context context, Handler parent) {
        super(context);
        mContext = context;
        mHandler = parent;
        isSearchTaskRun = false;
        isSuccess = false;
        isRunnableRun = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setCancelable(false);
        setContentView(R.layout.widget_dialog_get_city);
        mLoading = (TextView)findViewById(R.id.localization_loading);
        mConfirm = (Button)findViewById(R.id.localization_confirm);
        mConfirm.setOnClickListener(this);
        mBack = (Button)findViewById(R.id.localization_back);
        mBack.setOnClickListener(this);
        mLati = (TextView)findViewById(R.id.localization_lati);
        mLongi = (TextView)findViewById(R.id.localization_longi);
        mCity = (TextView)findViewById(R.id.localization_city_name);
        aCache = ACache.get(mContext);
        longiStr = aCache.getAsString(Const.Cache_Longitude);
        latiStr = aCache.getAsString(Const.Cache_Latitude);
        if(ShortcutUtil.isStringOK(latiStr)) mLati.setText(latiStr);
        if(ShortcutUtil.isStringOK(longiStr)) mLongi.setText(longiStr);
        background.run();
    }



    /**
     * Get and Update Current City Name.
     *
     * @param lati  latitude
     * @param Longi longitude
     */
    private void searchCityRequest(String lati, final String Longi) {
        isSearchTaskRun = true;
        String url = HttpUtil.SearchCity_url;
        url = url + "&location=" + lati + "," + Longi + "&ak=" + Const.APP_MAP_KEY;
        //Log.e("url", url);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                isSearchTaskRun = false;
                isSuccess = true;
                //Log.e("searchCityRequest",response.toString());
                try {
                    //Log.e("searchCityRequest 1",response.toString());
                    JSONObject result = response.getJSONObject("result");
                    //Log.e("searchCityRequest resul",result.toString());
                    JSONObject component = result.getJSONObject("addressComponent");
                    //Log.e("searchCityRequest comp",component.toString());
                    String cityName = component.getString("city");
                    // Log.e("searchCityRequest city",cityName);
                    if (cityName != null && !cityName.trim().equals("")) {
                        Message msg = new Message();
                        msg.what = Const.Handler_City_Name;
                        msg.obj = cityName;
                        handler.sendMessage(msg);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                isSearchTaskRun = false;
                isSuccess = false;
            }

        });
        VolleyQueue.getInstance(mContext.getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.localization_back:
                this.dismiss();
                break;
            case R.id.localization_confirm:
                String str = mCity.getText().toString();
                if(ShortcutUtil.isStringOK(str) && !str.equals("null")) {
                    Message message = new Message();
                    message.obj = str;
                    message.what = Const.Handler_City_Name;
                    mHandler.sendMessage(message);
                }
                this.dismiss();
                break;
        }
    }
}
