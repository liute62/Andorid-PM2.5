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
import android.util.Log;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import app.utils.CacheUtil;
import app.utils.Const;
import app.utils.FileUtil;

/**
 * This class aims to contain all the necessary function for localization.
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

    public final String[] providers = {LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.NETWORK_PROVIDER};

    public static LocationService instance;

    /**
     * The call-back listener for getting the filtered location
     */
    private GetTheLocation getTheLocation = null;

    /**
     * The last knowable location for quick access
     */
    private Location mLastLocation = null;

    private Context mContext;

    private String provider = null;

    private LocationManager mLocationManager;

    /**
     * A queue for storing location,
     */
    LocationQueue locationQueue;

    private int localization_type;

    private long runBeginTime;

    private static final long runTimePeriod = 1000 * 10;

    private boolean isRunning;

    /**
     * Class for Baidu Map library
     */
    private LocationClient locationClient;

    private BDLocationListener bdLocationListener = new MyLocationListener();

    /**
     * Using for determining indoor or outdoor situation
     * param isGpsAvailable to see whether gps signal is enough strong
     * param isWifiAvailable to see whether wifi is connected
     */
    private boolean isGpsAvailable;

    private boolean isWifiAvailable;

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
    }

    private void setDefaultTag(){
        localization_type = TYPE_BAIDU;
    }

    public void setGetTheLocationListener(GetTheLocation getTheLocation){
        this.getTheLocation = getTheLocation;
    }

    public void run(){
        isRunning = true;
        initMethodByType(localization_type);
        runMethodByType(localization_type);
    }

    public void run(int type){
        if(type == TYPE_BAIDU || type == TYPE_GPS || type == TYPE_NETWORK)
            localization_type = type;
        else return;
        isRunning = true;
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
            default:
                Log.e(TAG, "initMethodByType type error");
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
        baiduInitOption();
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

    private void baiduInitOption(){
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationMode.Hight_Accuracy);
        option.setCoorType("bd09ll");
        int span=1000;
        option.setScanSpan(span);
        option.setIsNeedAddress(true);
        option.setOpenGps(true);
        option.setLocationNotify(false); //可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
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
//            StringBuffer sb = new StringBuffer(256);
//            sb.append("time : ");
//            sb.append(location.getTime());
//            sb.append("\nerror code : ");
//            sb.append(location.getLocType());
//            sb.append("\nlatitude : ");
//            sb.append(location.getLatitude());
//            sb.append("\nlontitude : ");
//            sb.append(location.getLongitude());
//            sb.append("\nradius : ");
//            sb.append(location.getRadius());
            if (location.getLocType() == BDLocation.TypeGpsLocation) {// GPS
//                sb.append("\nspeed : ");
//                sb.append(location.getSpeed());//  km/h
//                sb.append("\nsatellite : ");
//                sb.append(location.getSatelliteNumber());
//                sb.append("\nheight : ");
//                sb.append(location.getAltitude());// m
//                sb.append("\ndirection : ");
//                sb.append(location.getDirection());// degree
//                sb.append("\naddr : ");
//                sb.append(location.getAddrStr());
//                sb.append("\ndescribe : ");
//                sb.append("gps localization success");
                Location locationGPS = new Location(LocationManager.GPS_PROVIDER);
                locationGPS.setLongitude(location.getLongitude());
                locationGPS.setLatitude(location.getLatitude());
                isGpsAvailable = true;
                getLocation(locationGPS);
            } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {// network localization result
//                sb.append("\naddr : ");
//                sb.append(location.getAddrStr());
//                //phone operations info
//                sb.append("\noperationers : ");
//                sb.append(location.getOperators());
//                sb.append("\ndescribe : ");
//                sb.append("network localization success");
                Location locationNetwork = new Location(LocationManager.NETWORK_PROVIDER);
                locationNetwork.setLongitude(location.getLongitude());
                locationNetwork.setLatitude(location.getLatitude());
                getLocation(locationNetwork);
                isGpsAvailable = false;
            } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {// offline localization result
//                sb.append("\ndescribe : ");
//                sb.append("offline localization success");
                Location locationPassive = new Location(LocationManager.PASSIVE_PROVIDER);
                locationPassive.setLongitude(location.getLongitude());
                locationPassive.setLatitude(location.getLatitude());
                getLocation(locationPassive);
            } else if (location.getLocType() == BDLocation.TypeServerError) {
                //sb.append("\ndescribe : ");
                FileUtil.appendErrorToFile(Const.code_file_baidu_exception1, "failed due to server, please send IMEI code and localization time to loc-bugs@baidu.com, someone will find the reason.");
                getLocation(null);
                //sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
            } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
                //sb.append("\ndescribe : ");
                FileUtil.appendErrorToFile(Const.code_file_baidu_exception2, "failed due to bad network, please check if network is enable.");
                getLocation(null);
                //sb.append("网络不同导致定位失败，请检查网络是否通畅");
            } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
                FileUtil.appendErrorToFile(Const.code_file_baidu_exception3,
                        "unable to get the location by criteria，most of time due to the mobile," +
                                "especially when mobile is in the air mode, so try to restart it.");
                getLocation(null);
//                sb.append("\ndescribe : ");
//                sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
            }
//            sb.append("\nlocationdescribe : ");
//            sb.append(location.getLocationDescribe());// 位置语义化信息
//            List<Poi> list = location.getPoiList();// POI data
//            if (list != null) {
//                sb.append("\npoilist size = : ");
//                sb.append(list.size());
//                for (Poi p : list) {
//                    sb.append("\npoi= : ");
//                    sb.append(p.getId() + " " + p.getName() + " " + p.getRank());
//                }
//            }
//            Log.e("BaiduLocationApiDem", sb.toString());
        }
    }


    /**
     * For GPS / NETWORK Method
     * @return the last knowable location by GPS(1st) or Network(2nd)
     */
    public Location getLastKnownLocation() {
        initGPS();
        try {
            mLastLocation = mLocationManager.getLastKnownLocation(provider);
            if(mLastLocation != null){
                FileUtil.appendStrToFile(Const.code_get_lastlocation_gps_notnull, "getLastKnownLocation gps != NULL");
                return mLastLocation;
            }
            initNetwork();
            mLastLocation = mLocationManager.getLastKnownLocation(provider);
            if(mLastLocation != null)
                FileUtil.appendStrToFile(Const.code_get_last_location_network_notnull, "getLastKnownLocation network != NULL");
        }catch (SecurityException e){
            e.printStackTrace();
        }
        if(mLastLocation == null) FileUtil.appendStrToFile(Const.code_get_last_location_isnull, "getLastKnownLocation == NULL");
        return mLastLocation;
    }

    public int getIndoorOutdoor(){
        isWifiAvailable = isWifiAvailable();
        openGpsAvailable();
        if(isWifiAvailable || !isGpsAvailable){
            return Indoor;
        }
        return Outdoor;
    }

    private String lastTimeSSID = "lastTimeSSID";
    private boolean isWifiAvailable(){
        boolean isSuccessConnected = false;
        WifiManager wifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        if(wifiManager != null){
            int wifiState = wifiManager.getWifiState();
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String id = wifiInfo.getSSID();
            if(id != null && !id.equals("0x") && !id.equals("<unknown ssid>")) {
                if(!lastTimeSSID.equals(id)){
                    lastTimeSSID = id;
                    FileUtil.appendStrToFile(Const.code_file_wifi_info,"LocationService wifiInfo "+id + "wifistate "+wifiState);
                }
                isSuccessConnected = true;
            }
        }
        return isSuccessConnected;
    }

    private void openGpsAvailable(){
        try {
            if(mLocationManager == null)
                mLocationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
            mLocationManager.addGpsStatusListener(this);
        }catch (SecurityException e){

        }
    }

    private void initGPS(){
        mLocationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
        provider = providers[0];
    }

    private void initNetwork(){
        mLocationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
        provider = providers[2];
    }

    private void deviceInit(int type){
        if(type == TYPE_GPS) initGPS();
        if(type == TYPE_NETWORK) initNetwork();
    }

    private void deviceRun(){
        if(provider != null) {
            runBeginTime = System.currentTimeMillis();
            if(mLocationManager.isProviderEnabled(provider)) {
                try {
                    mLocationManager.requestLocationUpdates(provider, 0, 0, this);
                    mLocationManager.addGpsStatusListener(this);
                }catch (SecurityException e){
                   e.printStackTrace();
                }
            }
        }
    }

    private void deviceStop(){
        try {
            mLocationManager.removeUpdates(this);
            mLocationManager.removeGpsStatusListener(this);
        }catch (SecurityException e){
            e.printStackTrace();
        }
    }

    private void getLocation(Location location){
        if(location == null) {
            locationQueue.addNull();
        }else {
            locationQueue.add(location);
            getTheLocation.onGetLocation(location);
        }
        if(locationQueue.isFull()){
            stop();
            getTheLocation.onSearchStop(locationQueue.getCommonLocation());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        long runMiddleTime;
        if (location != null) {
            Log.e(TAG, "onLocationChanged: " + location.getLatitude()+" "+location.getProvider());
            getLocation(location);
        }else {
            runMiddleTime = System.currentTimeMillis();
            if(runMiddleTime - runBeginTime > runTimePeriod){
                runBeginTime = 0;
                stop();
                FileUtil.appendErrorToFile(Const.code_get_location_failed, "failed to get the location in location service, running for " + String.valueOf(runTimePeriod) + " (ms)");
            }
            Log.e(TAG,"onLocationChanged provider = "+provider+" null");
        }
    }

    private static int GPS_Num_Thred = 8;
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
                    GpsSatellite tmp = items.next();
                    if (tmp.usedInFix())
                        ii++;
                    i++;
                }
                CacheUtil.getInstance(mContext).put(Const.Cache_GPS_SATE_NUM,ii);
                if(ii > GPS_Num_Thred) isGpsAvailable = true;
                else  isGpsAvailable = false;
            Log.e(TAG,"GPS_EVENT_SATELLITE_STATUS "+ii);
        } else if (event == GpsStatus.GPS_EVENT_STARTED) {
                Iterable<GpsSatellite> allgps = status.getSatellites();
                Iterator<GpsSatellite> items = allgps.iterator();
                int i = 0;
                int ii = 0;
                while (items.hasNext())
                {
                    GpsSatellite tmp =  items.next();
                    if (tmp.usedInFix())
                        ii++;
                    i++;
                }
                if(ii > GPS_Num_Thred) isGpsAvailable = true;
                else isGpsAvailable = false;
                CacheUtil.getInstance(mContext).put(Const.Cache_GPS_SATE_NUM,ii);
            Log.e(TAG,"GPS_EVENT_STARTED "+ii);
        }
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
    }

    /**
     * onGetLocation: continue
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

        private int nullObjectAccNum = 0;

        private int nullObjectThreshed = 10; //if there are accumulated 10 null objects, stop the thread.

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
            nullObjectAccNum = 0;
            return super.add(object);
        }

        public void addNull(){
            nullObjectAccNum++;
        }

        public boolean isFull(){
            boolean result = size() > threshed;
            if(result == true) return true;
            else if(nullObjectAccNum > nullObjectThreshed) {
                nullObjectAccNum = 0;
                return true;
            }
            return false;
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
            //Todo find a way to select the most possible location
            if(size() > 1) result = get(size()-1);
            return result;
        }
    }
}
