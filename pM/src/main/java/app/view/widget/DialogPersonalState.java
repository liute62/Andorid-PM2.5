package app.view.widget;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pm.DataResultActivity;
import com.example.pm.R;

import app.services.LocationService;
import app.utils.ACache;
import app.utils.StableCache;
import app.utils.Const;
import app.utils.ShortcutUtil;

/**
 * Created by Administrator on 1/11/2016.
 */
public class DialogPersonalState extends Dialog implements View.OnClickListener{

    public DialogPersonalState(Context context,Handler parent) {
        super(context);
        mContext = context;
        this.mHandler = parent;
    }
    Handler mHandler;
    ACache aCache;
    StableCache stableCache;
    Context mContext;
    TextView mSaveWeight;
    EditText mWeight;
    TextView mLongitude;
    TextView mLatitude;
    Button mBack;
    Button mLocalization;
    RadioButton mIndoor;
    RadioButton mOutdoor;
    Button mDataResult;
    TextView mGPSNum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCanceledOnTouchOutside(false);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.widget_dialog_personal_state);
        mSaveWeight = (TextView)findViewById(R.id.personal_state_weight_save);
        mSaveWeight.setOnClickListener(this);
        mWeight = (EditText)findViewById(R.id.personal_state_weight);
        mLongitude = (TextView)findViewById(R.id.personal_state_longi);
        mLatitude = (TextView)findViewById(R.id.personal_state_lati);
        mIndoor = (RadioButton)findViewById(R.id.personal_state_indoor);
        mIndoor.setOnClickListener(this);
        mOutdoor = (RadioButton)findViewById(R.id.personal_state_outdoor);
        mOutdoor.setOnClickListener(this);
        mBack = (Button)findViewById(R.id.personal_state_btn);
        mBack.setOnClickListener(this);
        mDataResult = (Button)findViewById(R.id.personal_state_today);
        mDataResult.setOnClickListener(this);
        mLocalization = (Button)findViewById(R.id.personal_state_get_location);
        mLocalization.setOnClickListener(this);
        mGPSNum = (TextView)findViewById(R.id.personal_state_gps_num);
        loadData();
    }

    private void loadData(){
        aCache = ACache.get(mContext.getApplicationContext());
        stableCache = StableCache.getInstance(mContext);
        String lati = aCache.getAsString(Const.Cache_Latitude);
        String longi = aCache.getAsString(Const.Cache_Longitude);
        String weight = stableCache.getAsString(Const.Cache_User_Weight);
        String inOut = aCache.getAsString(Const.Cache_Indoor_Outdoor);
        String gps = stableCache.getAsString(Const.Cache_GPS_SATE_NUM);
        if(lati != null) mLatitude.setText(lati);
        if(longi != null) mLongitude.setText(longi);
        if(weight != null) mWeight.setText(weight);
        if(inOut != null) setLocation(inOut);
        if(gps != null) mGPSNum.setText(gps);
    }

    private void setLocation(String state){
        Integer inOut = Integer.valueOf(state);
        if(inOut == LocationService.Indoor){
            mIndoor.setChecked(true);
            mOutdoor.setChecked(false);
        }else if(inOut == LocationService.Outdoor){
            mIndoor.setChecked(false);
            mOutdoor.setChecked(true);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.personal_state_today:
                Intent intent = new Intent(mContext, DataResultActivity.class);
                mContext.startActivity(intent);
                break;
            case R.id.personal_state_weight_save:
                String content = mWeight.getText().toString();
                if(ShortcutUtil.isWeightInputCorrect(content)){
                    Toast.makeText(mContext.getApplicationContext(),Const.Info_Input_Weight_Saved,Toast.LENGTH_SHORT).show();
                    stableCache.put(Const.Cache_User_Weight, content);
                    ShortcutUtil.calStaticBreath(Integer.valueOf(content));
                }else {
                    Toast.makeText(mContext.getApplicationContext(),Const.Info_Input_Weight_Error,Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.personal_state_btn:
                DialogPersonalState.this.dismiss();
                break;
            case R.id.personal_state_indoor:
                mIndoor.setChecked(true);
                mOutdoor.setChecked(false);
                aCache.put(Const.Cache_Indoor_Outdoor,String.valueOf(LocationService.Indoor));
                break;
            case R.id.personal_state_outdoor:
                mIndoor.setChecked(false);
                mOutdoor.setChecked(true);
                aCache.put(Const.Cache_Indoor_Outdoor,String.valueOf(LocationService.Outdoor));
                break;
            case R.id.personal_state_get_location:
                DialogGetLocation getLocation = new DialogGetLocation(mContext);
                getLocation.show();
                DialogPersonalState.this.dismiss();
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
