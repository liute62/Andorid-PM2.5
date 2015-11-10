package com.example.pm;

import android.app.Activity;
import android.location.LocationManager;
import android.os.Bundle;

import com.amap.api.maps.AMap;
import com.amap.api.maps.MapView;

public class HotMapActivity extends Activity {

    MapView mMapView;
    AMap aMap;
    LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hot_map);
        mMapView = (MapView) findViewById(R.id.hot_map_mapview);
        mMapView.onCreate(savedInstanceState);
        aMap = mMapView.getMap();
        setLocation();
    }

    private void setLocation() {

    }
}
