package com.example.pm;


import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import app.utils.ACache;
import app.utils.Const;
import app.view.widget.LoginDialog;
import app.view.widget.PullScrollView;

public class ProfileFragment extends Fragment implements
        OnClickListener, PullScrollView.OnTurnListener {

    ImageView mHead;
    PullScrollView mScrollView;
    Button mLogin;
    Button mLogout;
    ACache aCache;

    Handler loginHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == Const.Handler_Login_Success){
                mLogin.setVisibility(View.INVISIBLE);
            }
        }
    };
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        aCache = ACache.get(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        mHead = (ImageView) view.findViewById(R.id.profile_background_img);
        mScrollView = (PullScrollView) view.findViewById(R.id.profile_scroll_view);
        mScrollView.setHeader(mHead);
        mLogin = (Button)view.findViewById(R.id.profile_login);
        mLogout = (Button)view.findViewById(R.id.profile_logout);
        checkCache();
        setListener();
        return view;
    }

    private void checkCache(){
        String userId = aCache.getAsString(Const.Cache_User_Id);
        if (userId != null && !userId.equals("")){
            mLogin.setVisibility(View.INVISIBLE);
        }
    }

    private void setListener(){
        mScrollView.setOnTurnListener(this);
        mLogin.setOnClickListener(this);
        mLogout.setOnClickListener(this);
    }
    @Override
    public void onTurn() {
        // TODO Auto-generated method stub
        mHead.setImageResource(R.drawable.beijing);
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch (v.getId()){
            case R.id.profile_login:
                LoginDialog loginDialog = new LoginDialog(getActivity(),loginHandler);
                loginDialog.show();
                break;
            case R.id.profile_logout:
                aCache.remove(Const.Cache_User_Id);
                aCache.remove(Const.Cache_Access_Token);
                getActivity().finish();
                break;
        }
    }
}
