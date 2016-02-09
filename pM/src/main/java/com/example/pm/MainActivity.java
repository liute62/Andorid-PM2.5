package com.example.pm;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivity;

import app.services.UpdateService;
import app.utils.ACache;
import app.utils.Const;
import app.utils.FileUtil;
import app.utils.ShortcutUtil;

public class MainActivity extends SlidingActivity {

    public static final String TAG = "MainActivity";
    ACache aCache;
    Fragment newFragment;
    int offset = 20;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Display d = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int width = d.getWidth();
        int height = d.getHeight();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        float density = displayMetrics.density;
        int densityDPI = displayMetrics.densityDpi;
        offset = getOffsetByResolution(width);
        MyInitial();
        FileUtil.appendStrToFile(-1,"screen width: "+String.valueOf(width)+" "+"height: "+String.valueOf(height)
        +" "+"density: "+String.valueOf(density)+" densityDPI "+String.valueOf(densityDPI));
        Log.e(TAG,"screen width: "+String.valueOf(width)+" "+"height: "+String.valueOf(height));
    }

    /**
     * for 540 X 960 case 540
     * for 720 X 1280 case 720
     * for 1080 X 1920 case 1080
     * for 1440 X 2560 case 1440
     * @param width
     * @return
     */
    private int getOffsetByResolution(int width){
        int result = 20;
        switch(width){
            case 540:
                result = 80;
                break;
            case 720:
                result = 100;
                break;
            case 1080:
                result = 160;
                break;
            case 1440:
                result = 220;
                break;
        }
        return result;
    }

    private void MyInitial() {
        newFragment = new MainFragment();
        getFragmentManager().
                beginTransaction().
                replace(R.id.content, newFragment).
                commit();
        // set the Behind View
        setBehindContentView(R.layout.fragment_profile);
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        Fragment profileFragment = new ProfileFragment();
        fragmentTransaction.replace(R.id.profile_fragment, profileFragment);
        fragmentTransaction.commit();

        // customize the SlidingMenu
        SlidingMenu sm = getSlidingMenu();
        sm.setShadowWidth(40);
        sm.setShadowDrawable(R.drawable.shadow);
        //setBehindOffset()为设置SlidingMenu打开后，右边留下的宽度。可以把这个值写在dimens里面去:60dp
        sm.setBehindOffset(offset);
        sm.setFadeDegree(0.35f);
        //sm.setAboveOffset(20);
        //设置slding menu的几种手势模式
        //TOUCHMODE_FULLSCREEN 全屏模式，在content页面中，滑动，可以打开sliding menu
        //TOUCHMODE_MARGIN 边缘模式，在content页面中，如果想打开slding ,你需要在屏幕边缘滑动才可以打开slding menu
        //TOUCHMODE_NONE 自然是不能通过手势打开啦
        sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);

        //使用左上方icon可点，这样在onOptionsItemSelected里面才可以监听到R.id.home
        //getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     *
     */
    public void switchContent(Fragment fragment) {
        newFragment = fragment;
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.content, fragment)
                .commit();
        getSlidingMenu().showContent();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public boolean stopService(Intent name) {
        return super.stopService(name);
    }


}
