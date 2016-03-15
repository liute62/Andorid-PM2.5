package com.example.pm;


import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;
import app.services.DBService;
import app.utils.ACache;
import app.utils.Const;
import app.utils.HttpUtil;
import app.utils.ShareUtils;
import app.utils.ShortcutUtil;
import app.utils.VolleyQueue;
import app.view.widget.DialogConfirm;
import app.view.widget.DialogNotification;
import app.view.widget.DialogPersonalState;
import app.view.widget.InfoDialog;
import app.view.widget.LoginDialog;
import app.view.widget.ModifyPwdDialog;
import app.view.widget.PullScrollView;

public class ProfileFragment extends Fragment implements
        OnClickListener, PullScrollView.OnTurnListener {

    Activity mActivity;
    ImageView mHead;
    PullScrollView mScrollView;
    TextView mName;
    TextView mUsername;
    TextView mGender;
    TextView mLogOff;
    Button mLogin;
    Button mExit;
    Button mTurnOffUpload;
    Button mClear;
    Button mRegister;
    //Button mTurnOffService;
    Button mHelp;
    Button mBluetooth;
    Button mModifyPwd;
    Button mShare;
    Button mSavingBattery;
    TextView mResetPwd;
    TextView mNotification;
    TextView mModifyAndView;
    ACache aCache;
    InfoDialog infoDialog;
    boolean infoDialogShow;

    Handler loginHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == Const.Handler_Login_Success) {
                //mLogin.setOnClickListener(null);
                //mLogin.setVisibility(View.INVISIBLE);
                checkCache();
            }
        }
    };

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == Const.Handler_Gender_Updated){
                String gender = aCache.getAsString(Const.Cache_User_Gender);
                if(gender != null){
                    if(Integer.valueOf(gender) == 1){
                        mGender.setText(mActivity.getResources().getString(R.string.profile_gender_female));
                    }else if(Integer.valueOf(gender) == 0){
                        mGender.setText(mActivity.getResources().getString(R.string.profile_gender_male));
                    }
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        aCache = ACache.get(mActivity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        mHead = (ImageView) view.findViewById(R.id.profile_background_img);
        mScrollView = (PullScrollView) view.findViewById(R.id.profile_scroll_view);
        mScrollView.setHeader(mHead);
        mName = (TextView) view.findViewById(R.id.profile_name);
        mUsername = (TextView) view.findViewById(R.id.profile_username);
        mGender = (TextView) view.findViewById(R.id.profile_gender);
        mLogin = (Button) view.findViewById(R.id.profile_login);
        mExit = (Button) view.findViewById(R.id.profile_exit);
        mTurnOffUpload = (Button) view.findViewById(R.id.profile_turnoff_upload);
        //mTurnOffService = (Button) view.findViewById(R.id.profile_turnoff_service);
        mHelp = (Button)view.findViewById(R.id.profile_help);
        mClear = (Button) view.findViewById(R.id.profile_clear_data);
        mRegister = (Button) view.findViewById(R.id.profile_rigister);
        mBluetooth = (Button) view.findViewById(R.id.profile_bluetooth);
        mModifyPwd = (Button) view.findViewById(R.id.profile_modify_password);
        mResetPwd = (TextView) view.findViewById(R.id.profile_reset_pwd);
        mShare = (Button)view.findViewById(R.id.profile_share);
        mNotification = (TextView)view.findViewById(R.id.profile_view_notification);
        mModifyAndView = (TextView)view.findViewById(R.id.profile_modify_personal_state);
        mLogOff = (TextView)view.findViewById(R.id.profile_logoff);
        mSavingBattery = (Button)view.findViewById(R.id.profile_saving_battery);
        setSizeByWidth();
        checkCache();
        setListener();
        return view;
    }

    private void checkCache() {
        String userId = aCache.getAsString(Const.Cache_User_Id);
        if (ShortcutUtil.isStringOK(userId)) {
            mLogin.setVisibility(View.INVISIBLE);
            mLogin.setOnClickListener(null);
            mLogOff.setVisibility(View.VISIBLE);
            mLogOff.setOnClickListener(this);
            mResetPwd.setVisibility(View.VISIBLE);
            mResetPwd.setOnClickListener(this);
            mUsername.setText(Const.CURRENT_USER_NAME);
            mName.setText(Const.CURRENT_USER_NICKNAME);
        } else {
            mLogin.setVisibility(View.VISIBLE);
            mLogin.setOnClickListener(this);
            mResetPwd.setVisibility(View.INVISIBLE);
            mResetPwd.setOnClickListener(null);
            mLogOff.setVisibility(View.INVISIBLE);
            mLogOff.setOnClickListener(null);
            mGender.setText(mActivity.getResources().getString(R.string.profile_gender));
            mUsername.setText(mActivity.getResources().getString(R.string.profile_username));
            mName.setText(mActivity.getResources().getString(R.string.profile_name));
        }
        String gender = aCache.getAsString(Const.Cache_User_Gender);
        if(ShortcutUtil.isStringOK(gender)){
            if (gender.equals("0")) {
                mGender.setText(mActivity.getResources().getString(R.string.profile_gender_male));
            } else if (gender.equals("1")) {
                mGender.setText(mActivity.getResources().getString(R.string.profile_gender_female));
            } else {
                mGender.setText(mActivity.getResources().getString(R.string.profile_gender));
            }
        }
        String battery = aCache.getAsString(Const.Cache_Is_Saving_Battery);
        if(ShortcutUtil.isStringOK(battery)) {
            if (battery.equals(Const.IS_SAVING_BATTERY))
                mSavingBattery.setText(mActivity.getResources().getString(R.string.profile_btn_saving_battery_off));
            else
                mSavingBattery.setText(mActivity.getResources().getString(R.string.profile_btn_saving_battery_on));
        }
    }

    private void setListener() {
        mScrollView.setOnTurnListener(this);
        mLogin.setOnClickListener(this);
        mExit.setOnClickListener(this);
        mHelp.setOnClickListener(this);
        mTurnOffUpload.setOnClickListener(this);
        mClear.setOnClickListener(this);
        mRegister.setOnClickListener(this);
        mBluetooth.setOnClickListener(this);
        mModifyPwd.setOnClickListener(this);
        mShare.setOnClickListener(this);
        mNotification.setOnClickListener(this);
        mModifyAndView.setOnClickListener(this);
        mLogOff.setOnClickListener(this);
        mSavingBattery.setOnClickListener(this);
    }

    @Override
    public void onTurn() {
        mHead.setImageResource(Const.profileImg[(int) (Math.random() * 2)]);
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.profile_modify_personal_state:
                MainActivity main1Activity = (MainActivity)mActivity;
                main1Activity.toggle();
                DialogPersonalState dialogPersonalState = new DialogPersonalState(mActivity, mHandler);
                dialogPersonalState.show();
                break;
            case R.id.profile_view_notification:
                DialogNotification dialogNotification = new DialogNotification(mActivity);
                dialogNotification.show();
                break;
            case R.id.profile_saving_battery:
                String is = aCache.getAsString(Const.Cache_Is_Saving_Battery);
                if(! ShortcutUtil.isStringOK(is))
                    aCache.put(Const.Cache_Is_Saving_Battery,Const.Not_SAVING_BATTERY);
                is = aCache.getAsString(Const.Cache_Is_Saving_Battery);
                if (is.equals(Const.Not_SAVING_BATTERY)) {
                    ((TextView) v).setText(mActivity.getResources().getString(R.string.profile_btn_saving_battery_off));
                    aCache.put(Const.Cache_Is_Saving_Battery, Const.IS_SAVING_BATTERY);
                    notifySavingBattery(true);
                } else if (is.equals(Const.IS_SAVING_BATTERY)) {
                    aCache.put(Const.Cache_Is_Saving_Battery, Const.Not_SAVING_BATTERY);
                    ((TextView) v).setText(mActivity.getResources().getString(R.string.profile_btn_saving_battery_on));
                    notifySavingBattery(true);
                }
                break;
            case R.id.profile_login:
                MainActivity main2Activity = (MainActivity)mActivity;
                main2Activity.toggle();
                LoginDialog loginDialog = new LoginDialog(mActivity, loginHandler);
                loginDialog.show();
                break;
            case R.id.profile_share:
                shareProcess();
                break;
            case R.id.profile_exit:
                exit();
                break;
            case R.id.profile_logoff:
                logOff();
                break;
//            case R.id.profile_turnoff_service:
//                if (v.getTag() == null || v.getTag().equals("on")) {
//                    v.setTag("off");
//                    intent = new Intent(mActivity, DBService.class);
//                    mActivity.stopService(intent);
//                    ((TextView) v).setText(Const.Info_Turn_On_Service);
//                    Toast.makeText(mActivity, Const.Info_Turn_Off_Service, Toast.LENGTH_SHORT).show();
//                } else if (v.getTag().equals("off")) {
//                    v.setTag("on");
//                    intent = new Intent(mActivity, DBService.class);
//                    mActivity.startService(intent);
//                    ((TextView) v).setText(Const.Info_Turn_Off_Service);
//                    Toast.makeText(mActivity, Const.Info_Turn_On_Service, Toast.LENGTH_SHORT).show();
//                }
//                break;
            case R.id.profile_help:
                DialogNotification helpDialog = new DialogNotification(mActivity);
                helpDialog.setResId(R.layout.widget_dialog_help);
                helpDialog.show();
                break;
            case R.id.profile_turnoff_upload:
                if (v.getTag() == null || v.getTag().equals("on")) {
                    v.setTag("off");
                    ((TextView) v).setText(Const.Info_Turn_On_Upload);
                    Toast.makeText(mActivity, Const.Info_Turn_Off_Upload, Toast.LENGTH_SHORT).show();
                } else if (v.getTag().equals("off")) {
                    v.setTag("on");
                    intent = new Intent(mActivity, DBService.class);
                    mActivity.startService(intent);
                    ((TextView) v).setText(Const.Info_Turn_Off_Upload);
                    Toast.makeText(mActivity, Const.Info_Turn_On_Upload, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.profile_clear_data:
                clearCache();
                if (ShortcutUtil.isServiceWork(mActivity, Const.Name_DB_Service)) {
                    intent = new Intent(mActivity, DBService.class);
                    mActivity.stopService(intent);
                }
                getActivity().finish();
                break;
            case R.id.profile_rigister:
                intent = new Intent(mActivity, RegisterActivity.class);
                startActivityForResult(intent, Const.Action_Profile_Register);
                break;
            case R.id.profile_bluetooth:
                bluetoothProcess();
                break;
            case R.id.profile_modify_password:
                if (!Const.CURRENT_ACCESS_TOKEN.equals("-1")) {
                    ModifyPwdDialog modifyPwdDialog = new ModifyPwdDialog(mActivity, mHandler);
                    modifyPwdDialog.show();
                } else {
                    Toast.makeText(mActivity.getApplicationContext(), Const.Info_Login_First, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.profile_reset_pwd:
                if (!Const.CURRENT_ACCESS_TOKEN.equals("-1")) {
                    if (!infoDialogShow) {
                        infoDialog = new InfoDialog(mActivity);
                        infoDialog.setContent(Const.Info_Reset_Confirm);
                        infoDialog.setSureClickListener(new ResetPwdListener(1, infoDialog));
                        infoDialog.setCancelClickListener(new ResetPwdListener(2, infoDialog));
                        infoDialog.show();
                    }
                } else {
                    Toast.makeText(mActivity.getApplicationContext(), Const.Info_Login_First, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Const.Action_Profile_Register) {
            if (resultCode == 1) {
                //success
                checkCache();
            }
        }
    }

    private void clearLoginCache() {
        aCache.remove(Const.Cache_User_Id);
        aCache.remove(Const.Cache_Access_Token);
        aCache.remove(Const.Cache_User_Name);
        aCache.remove(Const.Cache_User_Nickname);
        aCache.remove(Const.Cache_User_Gender);
    }

    private void clearCache() {
        aCache.remove(Const.Cache_Chart_1);
        aCache.remove(Const.Cache_Chart_2);
        aCache.remove(Const.Cache_Chart_3);
        aCache.remove(Const.Cache_Chart_4);
        aCache.remove(Const.Cache_Chart_5);
        aCache.remove(Const.Cache_Chart_6);
        aCache.remove(Const.Cache_Chart_7);
        aCache.remove(Const.Cache_Chart_7_Date);
        aCache.remove(Const.Cache_Chart_8);
        aCache.remove(Const.Cache_Chart_10);
        aCache.remove(Const.Cache_Chart_12);
        aCache.remove(Const.Cache_Chart_12_Date);
        aCache.remove(Const.Cache_PM_Density);
        aCache.remove(Const.Cache_DB_Lastime_searchDensity);
        aCache.remove(Const.Cache_City);
        aCache.remove(Const.Cache_Pause_Time);
    }

    private void exit() {
        final DialogConfirm confirm = new DialogConfirm(mActivity,
                mActivity.getResources().getString(R.string.menu_exit_confirm_title),
                mActivity.getResources().getString(R.string.menu_exit_confirm_content));
        confirm.setCancelText(mActivity.getResources().getString(R.string.menu_exit_confirm_cancel));
        confirm.setConfirmText(mActivity.getResources().getString(R.string.menu_exit_confirm_confirm));
        confirm.setCancelable(false);
        confirm.setCancelListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                confirm.dismiss();
            }
        });
        confirm.setConfirmListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mActivity, DBService.class);
                mActivity.stopService(intent);
                getActivity().finish();
            }
        });
        confirm.show();
    }

    private void logOff(){
        clearLoginCache();
        Intent intent = new Intent(mActivity, DBService.class);
        mActivity.stopService(intent);
        getActivity().finish();
    }

    private class ResetPwdListener implements OnClickListener {

        int type; // 1 sure, 2 cancel
        InfoDialog infoDialog;

        public ResetPwdListener(int type, InfoDialog infoDialog) {
            this.type = type;
            this.infoDialog = infoDialog;
        }

        @Override
        public void onClick(View view) {
            if (type == 1) {
                resetPwd();
            } else {
                infoDialog.dismiss();
                infoDialogShow = false;
            }
        }
    }

    private void resetPwd() {
        String url = HttpUtil.Reset_Pwd_url + Const.CURRENT_USER_NAME;
        JSONObject object = new JSONObject();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, object, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String status = response.getString("status");
                    if (status.equals("1")) {
                        Toast.makeText(mActivity.getApplicationContext(), Const.Info_Reset_Success, Toast.LENGTH_SHORT).show();
                        infoDialog.dismiss();
                        infoDialogShow = false;
                        logOff();
                    } else if (status.equals("-1")) {
                        Toast.makeText(mActivity.getApplicationContext(), Const.Info_Reset_Username_Fail, Toast.LENGTH_SHORT).show();
                    } else if (status.equals("0")) {
                        Toast.makeText(mActivity.getApplicationContext(), Const.Info_Reset_NoUser_Fail, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(mActivity.getApplicationContext(), Const.Info_Reset_Unknown_Fail, Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(mActivity.getApplicationContext(), Const.Info_No_Network, Toast.LENGTH_SHORT).show();
            }
        });
        VolleyQueue.getInstance(mActivity.getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

    private void bluetoothProcess(){
        
        Fragment bluetooth = new BluetoothFragment();
        mActivity.getFragmentManager()
                .beginTransaction()
                .replace(R.id.content, bluetooth)
                .commit();
       MainActivity mainActivity = (MainActivity)mActivity;
       mainActivity.toggle();
    }


    private void shareProcess(){
        ShareUtils shareUtils = new ShareUtils(mActivity);
        shareUtils.share();
    }

    private void setSizeByWidth(){
        int width = Const.CURRENT_WIDTH;
        if(width == -1) return;
        if(width <= Const.Resolution_Small){
            mLogin.setWidth(40);
            mLogin.setHeight(28);
        }
    }

    private void notifySavingBattery(boolean is){
        Intent intent = new Intent(Const.Action_Low_Battery_ToService);
        if(is) intent.putExtra(Const.Intent_Low_Battery_State,Const.IS_SAVING_BATTERY);
        else intent.putExtra(Const.Intent_Low_Battery_State,Const.Not_SAVING_BATTERY);
        mActivity.sendBroadcast(intent);
    }

}
