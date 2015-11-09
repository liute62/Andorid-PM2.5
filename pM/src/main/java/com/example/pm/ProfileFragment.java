package com.example.pm;


import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import app.view.widget.PullScrollView;

public class ProfileFragment extends Fragment implements
        OnClickListener, PullScrollView.OnTurnListener {

    ImageView mHead;
    PullScrollView mScrollView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        mHead = (ImageView) view.findViewById(R.id.profile_background_img);
        mScrollView = (PullScrollView) view.findViewById(R.id.profile_scroll_view);
        mScrollView.setHeader(mHead);
        mScrollView.setOnTurnListener(this);
        return view;
    }

    @Override
    public void onTurn() {
        // TODO Auto-generated method stub
        mHead.setImageResource(R.drawable.beijing);
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub

    }
}
