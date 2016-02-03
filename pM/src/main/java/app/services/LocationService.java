package app.services;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;
import com.baidu.location.Poi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import app.utils.FileUtil;

/**
 * @author haodong
 */
public class LocationService implements LocationListener,GpsStatus.Listener
{

    public static final String TAG = "LocationService";

    public static final int TYPE_BAIDU = 0;

    public static final int TYPE_GPS = 1;

    public static final int TYPE_NETWORK = 2;

    public static final int Outdoor = 1;

    public static final int Indoor = 0;

    public static LocationService instance;

    GetTheLocation getTheLocation = null;

    Location mLastLocation = null;
    Context mContext;
    String provider = null;
    public final String[] providers = {LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.NETWORK_PROVIDER};
    LocationManager mLocationManager;
    LocationQueue locationQueue;
    Long timeInterval;
    int localization_type;

    long runBeginTime;

    long runMiddleTime;

    private long runTimePeriod = 1000 * 10;

    boolean isRunning;

    /**
     * Baidu Map
     */
    LocationClient locationClient;
    BDLocationListener bdLocationListener = new MyLocationListener();

    boolean isGpsAvailable;

    boolean isWifiAvailable;

    public static LocationService getInstance(Context context){
        if(instance == null)
            instance = new LocationService(context);
        return instance;
    }

    private LocationService(Context context) {
        isGpsAvailable = false;
        isWifiAvailable = false;
        isRunning = false;
        mContext = context.getApplicationContext();
        locationQueue = new LocationQueue();
        setDefaultTag();
        //initMethodByType(localization_type);
    }

    public void setGetTheLocationListener(GetTheLocation getTheLocation){
        this.getTheLocation = getTheLocation;
    }

    private void setDefaultTag(){
        localization_type = TYPE_GPS;
    }

    public void run(){
        initMethodByType(localization_type);
        runMethodByType(localization_type);
    }

    public void run(int type){
        isRunning = true;
        if(type == TYPE_BAIDU || type == TYPE_GPS || type == TYPE_NETWORK)
            localization_type = type;
        initMethodByType(localization_type);
        runMethodByType(localization_type);
    }

    public void stop(){
        if(isRunning)
          stopMethodByType(localization_type);
        isRunning = false;
    }

    private void initMethodByType(int type){
        switch (type){
            case TYPE_BAIDU:
                 baiduInit();
                break;
            case TYPE_GPS:
                deviceInit(TYPE_GPS);
                break;
            case TYPE_NETWORK:
                deviceInit(TYPE_NETWORK);
                break;
        }
    }

    private void runMethodByType(int type){
        switch (type){
            case TYPE_BAIDU:
                baiduRun();
                break;
            case TYPE_GPS:
            case TYPE_NETWORK:
                deviceRun();
                break;
        }
    }

    private void stopMethodByType(int type){
        switch (type){
            case TYPE_BAIDU:
                baiduStop();
                break;
            case TYPE_GPS:
            case TYPE_NETWORK:
                deviceStop();
                break;
        }
    }

    /**
     * For Baidu Method
     */
    private void baiduInit(){
        baiduCreate();
        baiduInitLoc();
    }

    private void baiduRun(){
        locationClient.start();
    }

    private void baiduStop(){
        locationClient.stop();
    }

    private void baiduCreate(){
        locationClient = new LocationClient(mContext.getApplicationContext());
        locationClient.registerLocationListener(bdLocationListener);
    }

    private void baiduInitLoc(){
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

    @Override
    public void onGpsStatusChanged(int i) {

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


    /**
     * For GPS / NETWORK Method
     * @return
     */
    public Location getLastKnownLocation() {
        initGPS();
        mLastLocation = mLocationManager.getLastKnownLocation(provider);
        if(mLastLocation != null){
            Log.e(TAG,"getLastKnownLocation gps == NULL");
            return mLastLocation;
        }
        initNetwork();
        mLastLocation = mLocationManager.getLastKnownLocation(provider);
        if(mLastLocation == null) Log.e(TAG,"getLastKnownLocation Network == NULL");
        return mLastLocation;
    }

    public int getIndoorOutdoor(){
        isWifiAvailable = isWifiAvailable();
        if(isWifiAvailable || !isGpsAvailable){
            return Indoor;
        }
        return Outdoor;
    }

    String lastTimeSSID = "lastTimeSSID";
    private boolean isWifiAvailable(){
        boolean isSuccessConnected = false;
        WifiManager wifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        if(wifiManager != null){
            int wifiState = wifiManager.getWifiState();
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String id = wifiInfo.getSSID();
            //Log.e(TAG,"wifiInfo "+wifiInfo.getSSID());
            if(id != null && !id.equals("0x") && !id.equals("<unknown ssid>")) {
                if(!lastTimeSSID.equals(id)){
                    lastTimeSSID = id;
                    FileUtil.appendStrToFile(-1,"LocationService wifiInfo "+id);
                }
                isSuccessConnected = true;
            }

        }
        return isSuccessConnected;
    }

    private void initGPS(){
        mLocationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
        provider = providers[0];
    }

    private void initNetwork(){
        mLocationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
        provider = providers[2];
    }

    private void setDefaultProvider(){
        for (int i = 0; i != providers.length; i++){
            if(mLocationManager.isProviderEnabled(providers[i])){
                provider = providers[i];
            }
        }
    }

    private void deviceInit(int type){
        if(type == TYPE_GPS) initGPS();
        if(type == TYPE_NETWORK) initNetwork();
    }

    private void deviceRun(){
        if(provider != null) {
            Log.e(TAG,"deviceRun: Using provider "+provider);
            runBeginTime = System.currentTimeMillis();
            if(mLocationManager.isProviderEnabled(provider)) {
                mLocationManager.requestLocationUpdates(provider, 0, 0, this);
                mLocationManager.addGpsStatusListener(this);
            }else {
                Log.e(TAG,"provider = "+provider+" is not enabled");
                stop();
            }
        }
    }

    private void deviceStop(){
        mLocationManager.removeUpdates(this);
        mLocationManager.removeGpsStatusListener(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        if(location != null){
            Log.e(TAG,"onLocationChanged: "+location.getLatitude()+" "+location.getProvider());
            locationQueue.add(location);
            getTheLocation.onGetLocation(location);
            if(locationQueue.isFull()){
                stop();
                FileUtil.appendStrToFile(-1,provider+" get location queue in location service"+locationQueue.toString());
                getTheLocation.onSearchStop(locationQueue.getCommonLocation());
            }
        }else {
            runMiddleTime = System.currentTimeMillis();
            if(runMiddleTime - runBeginTime > runTimePeriod){
                runBeginTime = 0;
                runMiddleTime = 0;
                stop();
                FileUtil.appendStrToFile(-1,"failed to get the location in location service");
            }
            Log.e(TAG,"onLocationChanged provider = "+provider+" null");
        }
    }

    @Override
    public void onStatusChanged(String s, int event, Bundle bundle) {
        if(mLocationManager == null) mLocationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
        GpsStatus status = mLocationManager.getGpsStatus(null);
        if(event == GpsStatus.GPS_EVENT_SATELLITE_STATUS){
                Iterable<GpsSatellite> allgps = status.getSatellites();
                Iterator<GpsSatellite> items = allgps.iterator();
                int i = 0;
                int ii = 0;
                while (items.hasNext())
                {
                    GpsSatellite tmp = (GpsSatellite) items.next();
                    if (tmp.usedInFix())
                        ii++;
                    i++;
                }
                if(ii > 4){
                    isGpsAvailable = true;
                }else {
                    isGpsAvailable = false;
                }
               Log.e(TAG,"GPS_EVENT_SATELLITE_STATUS i "+String.valueOf(i)+" ii"+String.valueOf(ii));
            } else if (event == GpsStatus.GPS_EVENT_STARTED) {
                Iterable<GpsSatellite> allgps = status.getSatellites();
                Iterator<GpsSatellite> items = allgps.iterator();
                int i = 0;
                int ii = 0;
                while (items.hasNext())
                {
                    GpsSatellite tmp = (GpsSatellite) items.next();
                    if (tmp.usedInFix())
                        ii++;
                    i++;
                }
                if(ii > 4){
                    isGpsAvailable = true;
                }else {
                    isGpsAvailable = false;
                }
                Log.e(TAG,"GPS_EVENT_STARTED started i "+String.valueOf(i)+" ii"+String.valueOf(ii));
        }
    }

    @Override
    public void onProviderEnabled(String s) {
        Log.e(TAG,"onProviderEnabled "+s);

    }

    @Override
    public void onProviderDisabled(String s) {
        Log.e(TAG,"onProviderDisabled "+s);
    }

    /**
     * onGetLocation
     *
     * onSearchStop
     */
    public interface GetTheLocation{

        void onGetLocation(Location location);

        void onSearchStop(Location location);
    }

    /**
     * A queue to collect locations and to get the most possible location from collections.
     */
    private class LocationQueue extends ArrayList{

        private int threshed = 1;

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

        public boolean isFull(){
            return size()>threshed;
        }

        @Override
        public Location get(int index) {
            return (Location)super.get(index);
        }

        @Override
        public String toString() {
            String str = "";
            for (int i = 0; i != size(); i++){
               str += i+" "+String.valueOf(get(i).getLongitude())+" "+String.valueOf(get(i).getLatitude());
            }
            return str;
        }

        public Location getCommonLocation(){
            Location result = null;
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
            //todo find a way to select the most possible location
            if(size() > 1) result = get(size()-1);
            return result;
        }
    }
}
