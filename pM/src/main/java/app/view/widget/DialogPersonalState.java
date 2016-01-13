package app.view.widget;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pm.DataResultActivity;
import com.example.pm.R;

import app.utils.ACache;
import app.utils.Const;
import app.utils.ShortcutUtil;

/**
 * Created by Administrator on 1/11/2016.
 */
public class DialogPersonalState extends Dialog implements View.OnClickListener{

    private static DialogPersonalState instance = null;

    public static DialogPersonalState getInstance(Context context,Handler parent){
        if(instance == null){
            instance = new DialogPersonalState(context,parent);
        }
        return instance;
    }

    private DialogPersonalState(Context context,Handler parent) {
        super(context);
        mContext = context;
        this.mHandler = parent;
    }

    Handler mHandler;
    ACache aCache;
    Context mContext;
    TextView mSaveWeight;
    EditText mWeight;
    TextView mLongitude;
    TextView mLatitude;
    Button mBack;
    RadioButton mMale;
    RadioButton mFemale;
    Button mDataResult;

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
        mMale = (RadioButton)findViewById(R.id.personal_state_male);
        mMale.setOnClickListener(this);
        mFemale = (RadioButton)findViewById(R.id.personal_state_female);
        mFemale.setOnClickListener(this);
        mBack = (Button)findViewById(R.id.personal_state_btn);
        mBack.setOnClickListener(this);
        mDataResult = (Button)findViewById(R.id.personal_state_today);
        mDataResult.setOnClickListener(this);
        loadData();
    }

    private void loadData(){
        aCache = ACache.get(mContext.getApplicationContext());
        String lati = aCache.getAsString(Const.Cache_Latitude);
        String longi = aCache.getAsString(Const.Cache_Longitude);
        String weight = aCache.getAsString(Const.Cache_User_Weight);
        String gender = aCache.getAsString(Const.Cache_User_Gender);
        if(lati != null) mLatitude.setText(lati);
        if(longi != null) mLongitude.setText(longi);
        if(weight != null) mWeight.setText(weight);
        if(gender != null) setGender(gender);
    }

    private void setGender(String gender){
        Log.e("Gender",gender);
        Integer sex = Integer.valueOf(gender);
        if(sex == 0){
            mMale.setChecked(true);
            mFemale.setChecked(false);
        }else if(sex == 1){
            mMale.setChecked(false);
            mFemale.setChecked(true);
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
                    aCache.put(Const.Cache_User_Weight, content);
                    ShortcutUtil.calStaticBreath(Integer.valueOf(content));
                }else {
                    Toast.makeText(mContext.getApplicationContext(),Const.Info_Input_Weight_Error,Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.personal_state_btn:
                DialogPersonalState.this.dismiss();
                break;
            case R.id.personal_state_male:
                mMale.setChecked(true);
                mFemale.setChecked(false);
                aCache.put(Const.Cache_User_Gender,"0");
                break;
            case R.id.personal_state_female:
                mMale.setChecked(false);
                mFemale.setChecked(true);
                aCache.put(Const.Cache_User_Gender,"1");
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        genderUpdating();
    }

    private void genderUpdating(){
        mHandler.sendEmptyMessage(Const.Handler_Gender_Updated);
    }
}
