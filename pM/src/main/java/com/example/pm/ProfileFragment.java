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
import app.utils.ShortcutUtil;
import app.utils.VolleyQueue;
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
    Button mLogin;
    Button mLogout;
    Button mTurnOffUpload;
    Button mClear;
    Button mRegister;
    Button mTurnOffService;
    Button mBluetooth;
    Button mModifyPwd;
    TextView mResetPwd;
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

    Handler modifyPwdHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

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
        mLogout = (Button) view.findViewById(R.id.profile_logout);
        mTurnOffUpload = (Button) view.findViewById(R.id.profile_turnoff_upload);
        mTurnOffService = (Button) view.findViewById(R.id.profile_turnoff_service);
        mClear = (Button) view.findViewById(R.id.profile_clear_data);
        mRegister = (Button) view.findViewById(R.id.profile_rigister);
        mBluetooth = (Button) view.findViewById(R.id.profile_bluetooth);
        mModifyPwd = (Button) view.findViewById(R.id.profile_modify_password);
        mResetPwd = (TextView) view.findViewById(R.id.profile_reset_pwd);
        checkCache();
        setListener();
        return view;
    }

    private void checkCache() {
        String userId = aCache.getAsString(Const.Cache_User_Id);
        if (ShortcutUtil.isStringOK(userId)) {
            mLogin.setVisibility(View.INVISIBLE);
            mLogin.setOnClickListener(null);
            mResetPwd.setVisibility(View.VISIBLE);
            mResetPwd.setOnClickListener(this);
            if (Const.CURRENT_USER_GENDER.equals("1")) {
                mGender.setText("男");
            } else if (Const.CURRENT_USER_GENDER.equals("2")) {
                mGender.setText("女");
            } else {
                mGender.setText("Gender");
            }
            mUsername.setText(Const.CURRENT_USER_NAME);
            mName.setText(Const.CURRENT_USER_NICKNAME);
        } else {
            mLogin.setVisibility(View.VISIBLE);
            mLogin.setOnClickListener(this);
            mResetPwd.setVisibility(View.INVISIBLE);
            mResetPwd.setOnClickListener(null);
            mGender.setText("Gender");
            mUsername.setText("Username");
            mName.setText("Name");
        }
    }

    private void setListener() {
        mScrollView.setOnTurnListener(this);
        mLogin.setOnClickListener(this);
        mLogout.setOnClickListener(this);
        mTurnOffService.setOnClickListener(this);
        mTurnOffUpload.setOnClickListener(this);
        mClear.setOnClickListener(this);
        mRegister.setOnClickListener(this);
        mBluetooth.setOnClickListener(this);
        mModifyPwd.setOnClickListener(this);
    }

    @Override
    public void onTurn() {
        mHead.setImageResource(Const.profileImg[(int) (Math.random() * 2)]);
    }

    @Override
    public void onClick(View v) {
        Intent intent = null;
        switch (v.getId()) {
            case R.id.profile_login:
                LoginDialog loginDialog = new LoginDialog(getActivity(), loginHandler);
                loginDialog.show();
                break;
            case R.id.profile_logout:
                logOff();
                break;
            case R.id.profile_turnoff_service:
                if (v.getTag() == null || v.getTag().equals("on")) {
                    v.setTag("off");
                    intent = new Intent(mActivity, DBService.class);
                    mActivity.stopService(intent);
                    ((TextView) v).setText(Const.Info_Turn_On_Service);
                    Toast.makeText(mActivity, Const.Info_Turn_Off_Service, Toast.LENGTH_SHORT).show();
                } else if (v.getTag().equals("off")) {
                    v.setTag("on");
                    intent = new Intent(mActivity, DBService.class);
                    mActivity.startService(intent);
                    ((TextView) v).setText(Const.Info_Turn_Off_Service);
                    Toast.makeText(mActivity, Const.Info_Turn_On_Service, Toast.LENGTH_SHORT).show();
                }
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
                getActivity().finish();
                if (ShortcutUtil.isServiceWork(mActivity, Const.Name_DB_Service)) {
                    intent = new Intent(mActivity, DBService.class);
                    mActivity.stopService(intent);
                }
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
                    ModifyPwdDialog modifyPwdDialog = new ModifyPwdDialog(mActivity, modifyPwdHandler);
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
    }

    private void logOff() {
        clearLoginCache();
        //turn off the running service
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

}
