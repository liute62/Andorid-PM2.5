package app.view.widget;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pm.R;

import app.services.LocationService;
import app.utils.ACache;
import app.utils.Const;
import app.utils.ShortcutUtil;

/**
 * Created by Administrator on 1/18/2016.
 */
public class DialogGetLocation extends Dialog implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener,LocationService.GetTheLocation
{

    public static final String TAG = "DialogGetLocation";
    ACache aCache;
    Context mContext;
    TextView mNewLati;
    TextView mNewLongi;
    TextView mLati;
    TextView mLongi;
    Button mSave;
    Button mSearch;
    Button mCancel;
    RadioButton mBaidu;
    RadioButton mGPS;
    RadioButton mNetwork;
    LocationService locationService;
    boolean isSearching;
    boolean isRunnable;
    Handler handler = new Handler();

    Runnable runnable = new Runnable() {
        int num = 0;
        @Override
        public void run() {
            if(isRunnable) {
                if (num == 0) {
                    mSearch.setText(mContext.getString(R.string.dialog_base_searching));
                } else if (num == 1) {
                    mSearch.setText(mContext.getString(R.string.dialog_base_searching) + ".");
                } else if (num == 2) {
                    mSearch.setText(mContext.getString(R.string.dialog_base_searching) + "..");
                } else if (num == 3) {
                    mSearch.setText(mContext.getString(R.string.dialog_base_searching) + "...");
                } else {
                    num = 0;
                }
                num++;

                handler.postDelayed(runnable, 300);
            }
        }
    };

    public DialogGetLocation(Context context) {
        super(context);
        mContext = context;
        aCache = ACache.get(context);
        locationService = LocationService.getInstance(context);
        isSearching = false;
        isRunnable = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setCancelable(false);
        setContentView(R.layout.widget_dialog_get_location);
        mNewLati = (TextView)findViewById(R.id.get_location_new_lati);
        mNewLongi = (TextView)findViewById(R.id.get_location_new_longi);
        mLati = (TextView)findViewById(R.id.get_location_lati);
        mLongi = (TextView)findViewById(R.id.get_location_longi);
        mSave = (Button)findViewById(R.id.get_location_save);
        mSave.setOnClickListener(this);
        mCancel = (Button)findViewById(R.id.get_location_back);
        mCancel.setOnClickListener(this);
        mSearch = (Button)findViewById(R.id.get_location_search);
        mSearch.setOnClickListener(this);
        mBaidu =(RadioButton)findViewById(R.id.get_location_baidu);
        mBaidu.setOnCheckedChangeListener(this);
        mGPS = (RadioButton)findViewById(R.id.get_location_gps);
        mGPS.setOnCheckedChangeListener(this);
        mNetwork = (RadioButton)findViewById(R.id.get_location_network);
        mNetwork.setOnCheckedChangeListener(this);
        init();
    }

    private void init(){
        String lati = aCache.getAsString(Const.Cache_Latitude);
        String longi = aCache.getAsString(Const.Cache_Longitude);
        if(ShortcutUtil.isStringOK(lati)) mLati.setText(lati);
        if(ShortcutUtil.isStringOK(longi)) mLongi.setText(longi);
        //baiduChecked();
        GPSChecked();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.get_location_back:
                if(isSearching){
                    stop();
                    mCancel.setText(mContext.getString(R.string.str_back));
                }else {
                    DialogGetLocation.this.dismiss();
                }
                break;
            case R.id.get_location_search:
                if(!isSearching)
                    begin();
                break;
            case R.id.get_location_save:
                String lati = mNewLati.getText().toString();
                String longi = mNewLongi.getText().toString();
                if(ShortcutUtil.isStringOK(lati) && !lati.equals("0") && ShortcutUtil.isStringOK(longi) && !longi.equals("0")) {
                    aCache.put(Const.Cache_Latitude,lati);
                    aCache.put(Const.Cache_Longitude, longi);
                    Toast.makeText(mContext.getApplicationContext(),Const.Info_Location_Saved,Toast.LENGTH_SHORT).show();
                    notifyService(Double.valueOf(lati),Double.valueOf(longi));
                }
                DialogGetLocation.this.dismiss();
                break;
        }
    }

    private void notifyService(double lati,double longi){
        Intent intent = new Intent(Const.Action_Get_Location_ToService);
        intent.putExtra(Const.Intent_DB_PM_Lati,lati);
        intent.putExtra(Const.Intent_DB_PM_Longi,longi);
        mContext.sendBroadcast(intent);
    }

    private void begin(){
        isRunnable = true;
        beforeSearch();
        mCancel.setText(mContext.getString(R.string.str_stop));
        locationService.setGetTheLocationListener(this);
        isSearching = true;
        int tag = LocationService.TYPE_GPS;
        if(mGPS.isChecked())tag = LocationService.TYPE_GPS;
        else if(mNetwork.isChecked()) tag = LocationService.TYPE_NETWORK;
        else if(mBaidu.isChecked()) tag = LocationService.TYPE_BAIDU;
        Log.e(TAG,"begin tag = "+tag);
        locationService.run(tag);
        runnable.run();
        onSearch();
    }

    private void stop(){
        mSearch.setText(mContext.getString(R.string.dialog_base_begin));
        mSearch.setEnabled(true);
        mSearch.setClickable(true);
        isRunnable = false;
        isSearching = false;
        if(locationService != null)
            locationService.stop();
    }

    private void beforeSearch(){
        mNewLati.setText("0");
        mNewLongi.setText("0");
        mSearch.setText(mContext.getString(R.string.dialog_base_begin));
        mSearch.setEnabled(true);
        mSearch.setClickable(true);
    }

    private void onSearch(){
        mSearch.setText(mContext.getString(R.string.dialog_base_searching));
        mSearch.setClickable(false);
        mSearch.setEnabled(false);
    }

    private void afterSearch(){
        mSearch.setText(mContext.getString(R.string.dialog_base_begin));
        mSearch.setEnabled(true);
        mSearch.setClickable(true);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        switch (compoundButton.getId()){
            case R.id.get_location_baidu:
               Log.e(TAG,"baidu "+String.valueOf(b));
                if(b)
                  baiduChecked();
               break;
           case R.id.get_location_gps:
               Log.e(TAG,"GPS "+String.valueOf(b));
               if(b)
               GPSChecked();
               break;
           case R.id.get_location_network:
               Log.e(TAG,"network "+String.valueOf(b));
               if(b)
               networkChecked();
               break;
       }
    }

    private void baiduChecked(){
        mBaidu.setChecked(true);
        mGPS.setChecked(false);
        mNetwork.setChecked(false);
    }

    private void GPSChecked(){
        mBaidu.setChecked(false);
        mGPS.setChecked(true);
        mNetwork.setChecked(false);
    }

    private void networkChecked(){
        mBaidu.setChecked(false);
        mGPS.setChecked(false);
        mNetwork.setChecked(true);
    }

    @Override
    protected void onStop() {
        if(locationService != null)
            locationService.stop();
        super.onStop();
    }

    @Override
    public void onGetLocation(Location location) {
        if(location != null)
            mNewLati.setText(String.valueOf(location.getLatitude()));
            mNewLongi.setText(String.valueOf(location.getLongitude()));
    }

    @Override
    public void onSearchStop(Location location) {
        isSearching = false;
        isRunnable = false;
        afterSearch();
        stop();
        mCancel.setText(mContext.getString(R.string.str_back));
        if(location != null) {
            mNewLati.setText(String.valueOf(location.getLatitude()));
            mNewLongi.setText(String.valueOf(location.getLongitude()));
        }else {
            mNewLati.setText("0");
            mNewLongi.setText("0");
        }
    }
}
