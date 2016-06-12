package app.view.widget;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.example.pm.R;

import app.services.LocationServiceUtil;

/**
 * Created by liuhaodong1 on 16/1/30.
 */
public class DialogRefresh extends Dialog implements View.OnClickListener
{
    Context mContext;
    Button mRefresh;
    Button mCancel;
    TextView mLongi;
    TextView mLati;
    TextView mLoading;
    Handler mHandler;
    LocationServiceUtil locationServiceUtil;
    boolean isStop;
    boolean isRunning;
    Runnable runnable = new Runnable() {

        int num;
        @Override
        public void run() {
            if(!isStop) {
                if (num == 0) {
                    mLoading.setText(mContext.getResources().getString(R.string.dialog_base_searching));
                } else if (num == 1) {
                    mLoading.setText(mContext.getResources().getString(R.string.dialog_base_searching) + ".");
                } else if (num == 2) {
                    mLoading.setText(mContext.getResources().getString(R.string.dialog_base_searching) + "..");
                } else if (num >= 3) {
                    mLoading.setText(mContext.getResources().getString(R.string.dialog_base_searching) + "...");
                    num = 0;
                }
                num++;
            }
            handler.postDelayed(runnable,300);
        }
    };

    Handler handler = new Handler();

    public DialogRefresh(Context context,Handler parent) {
        super(context);
        isStop = false;
        isRunning = false;
        mContext = context;
        mHandler = parent;
        locationServiceUtil = LocationServiceUtil.getInstance(mContext);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.widget_dialog_refresh);
        mLongi = (TextView)findViewById(R.id.refresh_longi);
        mLati = (TextView)findViewById(R.id.refresh_lati);
        mLoading = (TextView)findViewById(R.id.refresh_loading);
        mRefresh = (Button)findViewById(R.id.refresh_refresh);
        mRefresh.setOnClickListener(this);
        mCancel = (Button)findViewById(R.id.refresh_back);
        mCancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.refresh_refresh:
                if(!isRunning)
                    getLocation();
                break;
            case R.id.refresh_back:
                DialogRefresh.this.dismiss();
                break;
        }
    }

    private void getLocation() {
        setRunning();
        isRunning = true;
        locationServiceUtil.run();
    }

    private void setRunning(){
        runnable.run();
        mRefresh.setClickable(false);
        mRefresh.setEnabled(false);
    }

    private void setStop(){
        mRefresh.setClickable(true);
        mRefresh.setEnabled(true);
    }
}
