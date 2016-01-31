package app.services;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.ArrayMap;
import android.util.Log;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;
import com.baidu.location.Poi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author haodong
 */
public class LocationService implements LocationListener
{

    public static final String TAG = "LocationService";
    public static LocationService instance;
    Location mLastLocation = null;
    Context mContext;
    String provider = null;
    public final String[] providers = {LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.NETWORK_PROVIDER};
    LocationManager mLocationManager;
    LocationQueue locationQueue;
    Long timeInterval;

    /**
     * Baidu Map
     */
    LocationClient locationClient;
    BDLocationListener bdLocationListener = new MyLocationListener();

    private void onCreate(){
        locationClient = new LocationClient(mContext.getApplicationContext());
        locationClient.registerLocationListener(bdLocationListener);
    }

    private void initLocation(){
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationMode.Hight_Accuracy);
        option.setCoorType("bd09ll");
        int span=1000;
        option.setScanSpan(span);
        option.setIsNeedAddress(true);
        option.setOpenGps(true);
        option.setLocationNotify(true);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        option.setIsNeedLocationDescribe(false);
        option.setIsNeedLocationPoiList(false);
        option.setIgnoreKillProcess(false);
        option.SetIgnoreCacheException(false);
        option.setEnableSimulateGps(false);
        locationClient.setLocOption(option);
    }

    public class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            //Receive Location
            StringBuffer sb = new StringBuffer(256);
            sb.append("time : ");
            sb.append(location.getTime());
            sb.append("\nerror code : ");
            sb.append(location.getLocType());
            sb.append("\nlatitude : ");
            sb.append(location.getLatitude());
            sb.append("\nlontitude : ");
            sb.append(location.getLongitude());
            sb.append("\nradius : ");
            sb.append(location.getRadius());
            if (location.getLocType() == BDLocation.TypeGpsLocation) {// GPS定位结果
                sb.append("\nspeed : ");
                sb.append(location.getSpeed());// 单位：公里每小时
                sb.append("\nsatellite : ");
                sb.append(location.getSatelliteNumber());
                sb.append("\nheight : ");
                sb.append(location.getAltitude());// 单位：米
                sb.append("\ndirection : ");
                sb.append(location.getDirection());// 单位度
                sb.append("\naddr : ");
                sb.append(location.getAddrStr());
                sb.append("\ndescribe : ");
                sb.append("gps定位成功");

            } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {// 网络定位结果
                sb.append("\naddr : ");
                sb.append(location.getAddrStr());
                //运营商信息
                sb.append("\noperationers : ");
                sb.append(location.getOperators());
                sb.append("\ndescribe : ");
                sb.append("网络定位成功");
            } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {// 离线定位结果
                sb.append("\ndescribe : ");
                sb.append("离线定位成功，离线定位结果也是有效的");
            } else if (location.getLocType() == BDLocation.TypeServerError) {
                sb.append("\ndescribe : ");
                sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
            } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
                sb.append("\ndescribe : ");
                sb.append("网络不同导致定位失败，请检查网络是否通畅");
            } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
                sb.append("\ndescribe : ");
                sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
            }
            sb.append("\nlocationdescribe : ");
            sb.append(location.getLocationDescribe());// 位置语义化信息
            List<Poi> list = location.getPoiList();// POI数据
            if (list != null) {
                sb.append("\npoilist size = : ");
                sb.append(list.size());
                for (Poi p : list) {
                    sb.append("\npoi= : ");
                    sb.append(p.getId() + " " + p.getName() + " " + p.getRank());
                }
            }
            Log.e("BaiduLocationApiDem", sb.toString());
        }
    }

    public static LocationService getInstance(Context context){
        if(instance == null)
            instance = new LocationService(context);
        return instance;
    }

    private LocationService(Context context) {
        mContext = context;
        locationQueue = new LocationQueue();
    }

    public Location getLastKnownLocation() {
        init();
        mLastLocation = mLocationManager.getLastKnownLocation(provider);
        return mLastLocation;
    }

    private void init(){
        mLocationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
        if(provider == null) setDefaultProvider();
        if(provider != null) {
            Log.e(TAG,"Using provider "+provider);
            mLocationManager.requestLocationUpdates(provider, 0, 0, this);
        }
    }

    private void setDefaultProvider(){
        for (int i = 0; i != providers.length; i++){
            if(mLocationManager.isProviderEnabled(providers[i])){
                provider = providers[i];
            }
        }
    }

    Runnable mRun = new Runnable() {
        int num = 0;
        @Override
        public void run() {

            if(num == 1){
                Log.d(TAG,"It is time to stop");
                stop();
            }
            num++;
            mHandler.postDelayed(mRun,timeInterval);
        }
    };

    Handler mHandler = new Handler();

    public void run(Long minTime){
        //init();
        //timeInterval = minTime;
        //mRun.run();
        onCreate();
        initLocation();
    }

    public void stop(){
        mLocationManager.removeUpdates(this);
        //mHandler.removeCallbacks(mRun);
    }

    @Override
    public void onLocationChanged(Location location) {
        if(location != null){
            Log.e(TAG,location.getLatitude()+" "+location.getLongitude()+" "+location.getSpeed()+" "
            +location.getAltitude()+" "+location.getProvider());
            locationQueue.add(location);
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    public interface getTheLocation{

        void onGetLocation(Location location);
    }

    /**
     * A queue to collect the location and to
     *
     *
     *
     */
    private class LocationQueue extends ArrayList{

        private int threshed = 10;

        public void setThreshed(int t){
            threshed = t;
        }

        public int getThreshed(){
            return threshed;
        }

        /**
         * keep the size of queue below to t
         * @param object
         * @return
         */
        @Override
        public boolean add(Object object) {

            if(object instanceof Location){
                if(size() >  threshed){
                    remove(0);
                }
            }else {
                return false;
            }
            return super.add(object);
        }

        public void getCommonLocation(){
            Map<Double,Integer> latis = new HashMap<>();
            Map<Double,Integer> longis = new HashMap<>();
            for(int i = 0; i != size(); i++){
                Location location = (Location) get(i);
                double lati = location.getLatitude();
                double longi = location.getLongitude();
                if(latis.containsKey(lati)){
                    int num = latis.get(lati);
                    latis.put(lati,num++);
                }else {
                    latis.put(lati,1);
                }
                if(longis.containsKey(longi)){
                    int num = longis.get(longi);
                    longis.put(longi,num++);
                }else {
                    longis.put(longi,1);
                }
            }


        }
    }
}
