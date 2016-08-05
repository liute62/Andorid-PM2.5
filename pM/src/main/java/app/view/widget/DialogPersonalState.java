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

import app.services.DataServiceUtil;
import app.services.LocationServiceUtil;
import app.utils.StableCache;
import app.utils.Const;
import app.utils.ShortcutUtil;

/**
 * Created by haodong on 1/11/2016.
 */
public class DialogPersonalState extends Dialog implements View.OnClickListener{

    private Handler mHandler;
    private DataServiceUtil dataServiceUtil;
    private StableCache stableCache;
    private Context mContext;

    private TextView mSaveWeight;
    private EditText mWeight;
    private TextView mLongitude;
    private TextView mLatitude;
    private Button mBack;
    private Button mLocalization;
    private RadioButton mIndoor;
    private RadioButton mOutdoor;
    private Button mDataResult;
    private TextView mGPSNum;

    public DialogPersonalState(Context context,Handler parent) {
        super(context);
        mContext = context;
        this.mHandler = parent;
    }

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

        dataServiceUtil = DataServiceUtil.getInstance(mContext);

        stableCache = StableCache.getInstance(mContext);
        String weight = stableCache.getAsString(Const.Cache_User_Weight);
        String gps = stableCache.getAsString(Const.Cache_GPS_SATE_NUM);

        mLatitude.setText(String.valueOf(dataServiceUtil.getLatitudeFromCache()));
        mLongitude.setText(String.valueOf(dataServiceUtil.getLongitudeFromCache()));
        if(weight != null) mWeight.setText(weight);
        setLocation(dataServiceUtil.getInOutDoorFromCache());
        if(gps != null) mGPSNum.setText(gps);
    }

    private void setLocation(int state){

        if(state == LocationServiceUtil.Indoor){
            mIndoor.setChecked(true);
            mOutdoor.setChecked(false);
        }else if(state == LocationServiceUtil.Outdoor){
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
                dataServiceUtil.cacheInOutdoor(LocationServiceUtil.Indoor);
                break;
            case R.id.personal_state_outdoor:
                mIndoor.setChecked(false);
                mOutdoor.setChecked(true);
                dataServiceUtil.cacheInOutdoor(LocationServiceUtil.Outdoor);
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
