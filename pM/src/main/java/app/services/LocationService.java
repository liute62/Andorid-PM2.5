package app.services;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;

/**
 * @author haodong
 */
public class LocationService {

    //// TODO: 1/18/2016 A Module for get the Location by network and GPS providers.
    Location mLastLocation = null;
    Context mContext;
    String provider = null;
    String[] providers = {LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.NETWORK_PROVIDER};
    LocationManager mLocationManager;

    public LocationService(Context context) {
        mContext = context;
    }

    public Location getLastKnownLocation() {
        mLastLocation = mLocationManager.getLastKnownLocation(provider);
        return mLastLocation;
    }

    public void run(){

    }
}
