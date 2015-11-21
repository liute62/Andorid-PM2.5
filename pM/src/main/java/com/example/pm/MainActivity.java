package com.example.pm;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivity;

public class MainActivity extends SlidingActivity {

    Fragment newFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MyInitial();
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
        sm.setShadowWidth(50);
        sm.setShadowDrawable(R.drawable.shadow);
        //setBehindOffset()为设置SlidingMenu打开后，右边留下的宽度。可以把这个值写在dimens里面去:60dp
        sm.setBehindOffset(60);
        sm.setFadeDegree(0.35f);
        //设置slding menu的几种手势模式
        //TOUCHMODE_FULLSCREEN 全屏模式，在content页面中，滑动，可以打开sliding menu
        //TOUCHMODE_MARGIN 边缘模式，在content页面中，如果想打开slding ,你需要在屏幕边缘滑动才可以打开slding menu
        //TOUCHMODE_NONE 自然是不能通过手势打开啦
        sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);

        //使用左上方icon可点，这样在onOptionsItemSelected里面才可以监听到R.id.home
        //getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     * 各个fragment间的切换
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean stopService(Intent name) {
        return super.stopService(name);
    }
}
