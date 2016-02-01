package app.view.widget;

import android.app.Dialog;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.example.pm.R;

import java.security.cert.LDAPCertStoreParameters;

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

    // TODO: 1/18/2016 enable user to get GPS by network and GPS provider manually
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
                    mSearch.setText(mContext.getString(R.string.dialog_base_searching)+".");
                } else if (num == 2) {
                    mSearch.setText(mContext.getString(R.string.dialog_base_searching)+"..");
                } else if(num == 3){
                    mSearch.setText(mContext.getString(R.string.dialog_base_searching)+"...");
                }else {
                    num = 0;
                }
                num++;
            }
            handler.postDelayed(runnable,300);
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
        baiduChecked();

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.get_location_back:
                DialogGetLocation.this.dismiss();
                break;
            case R.id.get_location_search:
                if(!isSearching)
                    begin();
                break;
            case R.id.get_location_save:
                String lati = mNewLati.getText().toString();
                String longi = mNewLongi.getText().toString();
                if(ShortcutUtil.isStringOK(lati) && !lati.equals("0")) aCache.put(Const.Cache_Latitude,lati);
                if(ShortcutUtil.isStringOK(longi) && !longi.equals("0")) aCache.put(Const.Cache_Longitude,longi);
                DialogGetLocation.this.dismiss();
                break;
        }
    }

    private void begin(){
        runnable.run();
        onSearch();
        isSearching = true;
        int tag = LocationService.TAG_GPS;
        if(mGPS.isChecked())tag = LocationService.TAG_GPS;
        if(mNetwork.isChecked()) tag = LocationService.TAG_NETWORK;
        Log.e(TAG,"begin tag = "+tag);
        locationService.run(tag);
    }

    private void onSearch(){
        mSearch.setClickable(false);
        mSearch.setEnabled(false);
    }

    private void afterSearch(){
        mSearch.setText(mContext.getString(R.string.dialog_base_searching));
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
    public void onGetLocation(Location location) {
        isSearching = false;
        isRunnable = false;
        afterSearch();
    }
}
