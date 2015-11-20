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

import app.bluetooth.BluetoothActivity;
import app.services.DBService;
import app.utils.ACache;
import app.utils.Const;
import app.view.widget.LoginDialog;
import app.view.widget.ModifyPwdDialog;
import app.view.widget.PullScrollView;

public class ProfileFragment extends Fragment implements
        OnClickListener, PullScrollView.OnTurnListener {

    Activity mActivity;
    ImageView mHead;
    PullScrollView mScrollView;
    Button mLogin;
    Button mLogout;
    Button mTurnOffUpload;
    Button mClear;
    Button mRegister;
    Button mTurnOffService;
    Button mBluetooth;
    Button mModifyPwd;
    ACache aCache;

    Handler loginHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == Const.Handler_Login_Success) {
                mLogin.setOnClickListener(null);
                mLogin.setText("退出登陆");
            }
        }
    };

    Handler modifyPwdHandler = new Handler(){
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
        // TODO Auto-generated method stub
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        mHead = (ImageView) view.findViewById(R.id.profile_background_img);
        mScrollView = (PullScrollView) view.findViewById(R.id.profile_scroll_view);
        mScrollView.setHeader(mHead);
        mLogin = (Button) view.findViewById(R.id.profile_login);
        mLogout = (Button) view.findViewById(R.id.profile_logout);
        mTurnOffUpload = (Button) view.findViewById(R.id.profile_turnoff_upload);
        mTurnOffService = (Button) view.findViewById(R.id.profile_turnoff_service);
        mClear = (Button) view.findViewById(R.id.profile_clear_data);
        mRegister = (Button) view.findViewById(R.id.profile_rigister);
        mBluetooth = (Button)view.findViewById(R.id.profile_bluetooth);
        mModifyPwd = (Button)view.findViewById(R.id.profile_modify_password);
        checkCache();
        setListener();
        return view;
    }

    private void checkCache() {
        String userId = aCache.getAsString(Const.Cache_User_Id);
        if (userId != null && !userId.equals("")) {
            mLogin.setVisibility(View.INVISIBLE);
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
        // TODO Auto-generated method stub
        mHead.setImageResource(R.drawable.beijing);
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch (v.getId()) {
            case R.id.profile_login:
                LoginDialog loginDialog = new LoginDialog(getActivity(), loginHandler);
                loginDialog.show();
                break;
            case R.id.profile_logout:
                clearCache();
                getActivity().finish();
                break;
            case R.id.profile_turnoff_service:
                if (v.getTag() == null || v.getTag().equals("on")) {
                    v.setTag("off");
                    Intent intent = new Intent(mActivity, DBService.class);
                    mActivity.stopService(intent);
                    ((TextView) v).setText(Const.Info_Turn_On_Service);
                    Toast.makeText(mActivity, Const.Info_Turn_Off_Service, Toast.LENGTH_SHORT).show();
                } else if (v.getTag().equals("off")) {
                    v.setTag("on");
                    Intent intent = new Intent(mActivity, DBService.class);
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
                    Intent intent = new Intent(mActivity, DBService.class);
                    mActivity.startService(intent);
                    ((TextView) v).setText(Const.Info_Turn_Off_Upload);
                    Toast.makeText(mActivity, Const.Info_Turn_On_Upload, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.profile_clear_data:
                break;
            case R.id.profile_rigister:
                Intent intent = new Intent(mActivity,RegisterActivity.class);
                startActivity(intent);
                break;
            case R.id.profile_bluetooth:
                Intent intent1 = new Intent(mActivity,BluetoothActivity.class);
                startActivity(intent1);
                break;
            case R.id.profile_modify_password:
                if(! Const.CURRENT_ACCESS_TOKEN.equals("-1")){
                    ModifyPwdDialog modifyPwdDialog = new ModifyPwdDialog(mActivity,modifyPwdHandler);
                    modifyPwdDialog.show();
                }else {
                    Toast.makeText(mActivity.getApplicationContext(),Const.Info_Login_First,Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void clearCache() {
        aCache.remove(Const.Cache_User_Id);
        aCache.remove(Const.Cache_Access_Token);
    }
}
