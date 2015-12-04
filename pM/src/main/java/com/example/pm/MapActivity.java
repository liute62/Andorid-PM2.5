package com.example.pm;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.baidu.location.LocationClient;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.CircleOptions;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.CoordinateConverter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MapActivity extends Activity {
    private MapView mMapView;

    private BaiduMap mBaiduMap = null;

    private LatLng currentPoint;
    private HashMap<LatLng,Double> monitorPoints;
    private List<LatLng> trajectoryPoints;
    private int zooms[] = new int[]{50,100,200,500,1000,2000,5000,10000,20000,50000,100000,200000,500000,1000000,2000000};
    public boolean ViewSettingDone = false;

    private int lastZoom = 15;

    //monitor locations
    private static final int Location_TIME_INTERVAL = 1*1000;//1分钟
    private Handler LocationHandler = new Handler();
    private Runnable LocationRunnable = new Runnable() {
        @Override
        public void run() {
            drawLocation();
            LocationHandler.postDelayed(LocationRunnable, Location_TIME_INTERVAL);
        }
    };

    //trajectory
    private static final int Trajectory_TIME_INTERVAL = 1*1000;//1分钟
    private Handler TrajectoryHandler = new Handler();
    private Runnable TrajectoryRunnable = new Runnable() {
        @Override
        public void run() {
            drawTrajectory();
            TrajectoryHandler.postDelayed(TrajectoryRunnable, Trajectory_TIME_INTERVAL);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_map);

        mMapView = (MapView) this.findViewById(R.id.bmapView);


        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        //mBaiduMap.setBaiduHeatMapEnabled(true);

        trajectoryPoints = new ArrayList<LatLng>();
        monitorPoints = new HashMap<LatLng,Double>();
        simulate();

        //start the thread to update the location
        HandlerThread thread = new HandlerThread("MapActivity");
        thread.start();
        TrajectoryHandler = new Handler(thread.getLooper());
        TrajectoryHandler.post(TrajectoryRunnable);

        LocationHandler = new Handler(thread.getLooper());
        LocationHandler.post(LocationRunnable);


        mBaiduMap.setOnMapStatusChangeListener(onMapStatusChangeListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        TrajectoryHandler.removeCallbacks(TrajectoryRunnable);
        LocationHandler.removeCallbacks(LocationRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    //map zoom listener

    BaiduMap.OnMapStatusChangeListener onMapStatusChangeListener = new BaiduMap.OnMapStatusChangeListener() {
        @Override
        public void onMapStatusChangeStart(MapStatus mapStatus) {

        }

        @Override
        public void onMapStatusChange(MapStatus mapStatus) {

        }

        @Override
        public void onMapStatusChangeFinish(MapStatus mapStatus) {
            if (lastZoom!=mapStatus.zoom) {
                Log.d("zoom","zoom is changed");
                drawLocation();
                lastZoom = (int) mapStatus.zoom;
            }
        }
    };

    //simulate the trajectory
    private void simulate() {
        currentPoint = new LatLng(31.249766710565,121.48789949069);
        //simulate trajectory points
        trajectoryPoints.add(new LatLng(31.241261710015,121.48789948569));
        trajectoryPoints.add(new LatLng(31.242362710125,121.48789948669));
        trajectoryPoints.add(new LatLng(31.243463710235,121.48789948769));
        trajectoryPoints.add(new LatLng(31.244564710345,121.48789948869));
        trajectoryPoints.add(new LatLng(31.245665710455,121.48789948969));
        trajectoryPoints.add(new LatLng(31.246766710565,121.48789949069));
        trajectoryPoints.add(new LatLng(31.247766710565,121.48789949069));
        trajectoryPoints.add(new LatLng(31.248766710565,121.48789949069));
        trajectoryPoints.add(new LatLng(31.249766710565,121.48789949069));
        //simulate monitor points
        LatLng p1 = new LatLng(31.249161710015,121.48789948569);
        LatLng p2 = new LatLng(31.169152089592,121.44623500473);
        LatLng p3 = new LatLng(31.263742929076,121.39844294375);
        LatLng p4 = new LatLng(31.304510479542,121.53571659963);
        LatLng p5 = new LatLng(31.304510479542,121.40833126667);
        LatLng p6 = new LatLng(31.230895349134,121.63848131409);
        LatLng p7 = new LatLng(31.282497228987,121.49191854079);
        LatLng p8 = new LatLng(31.137700846982,121.01851301174);
        LatLng p9 = new LatLng(31.235380803488,121.454755557);
        monitorPoints.put(p1,20.0);monitorPoints.put(p2,60.0);monitorPoints.put(p3,100.0);
        monitorPoints.put(p4,120.0);monitorPoints.put(p5,160.0);monitorPoints.put(p6,200.0);
        monitorPoints.put(p7,240.0);monitorPoints.put(p8,280.0);monitorPoints.put(p9,320.0);
    }

    private int chooseColor(double density) {
       if (density<50) {
           return 0x2F00FF00;
       } else if (density<100) {
           return 0x2FFFFF00;
       } else if (density<150) {
           return 0x2FFF7F00;
       } else if (density<200) {
           return 0x2FFF0000;
       } else if (density<300) {
           return 0x2FFF3030;
       } else {
           return 0x2F000000;
       }
    }

    private void drawLocation() {
        mBaiduMap.clear();
        drawTrajectory();

        int zoom = (int) mBaiduMap.getMapStatus().zoom;
        int maxZoomLevel = (int)mBaiduMap.getMaxZoomLevel();
        int index = maxZoomLevel-zoom;
        if (index<0) {
            index = 0;
        } else if (index>=zooms.length) {
            index = zooms.length-1;
        }
        int radius = zooms[index]/5;

        for (LatLng point : monitorPoints.keySet()) {
            double density = monitorPoints.get(point);
            int color = this.chooseColor(density);
            LatLng toPoint = this.convert(point);
            Log.d("radius",zoom+" "+radius+" "+mBaiduMap.getMaxZoomLevel()+" "+mBaiduMap.getMinZoomLevel());
            OverlayOptions ooPolyline = new CircleOptions().center(toPoint).radius(radius).fillColor(color);
            mBaiduMap.addOverlay(ooPolyline);
        }
    }

    public void drawTrajectory() {
        List<LatLng> points = this.convertPoints();

        if (!ViewSettingDone) {
            setViewAngle(currentPoint);
            ViewSettingDone = true;
        }
        if (points.size()>=2) {
            OverlayOptions ooPolyline = new PolylineOptions().width(5).color(0xAAFF0000).points(points);
            mBaiduMap.addOverlay(ooPolyline);
        }
    }

    private void setViewAngle(LatLng cenpt) {
        MapStatus mMapStatus = new MapStatus.Builder().target(cenpt).zoom(15).build();
        MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
        mBaiduMap.setMapStatus(mMapStatusUpdate);

    }

    private List<LatLng> convertPoints() {
        List<LatLng> toPoints = new ArrayList<LatLng>();
        for (LatLng point : trajectoryPoints) {
            toPoints.add(convert(point));
        }
        return toPoints;
    }

    private LatLng convert(LatLng from) {
        CoordinateConverter converter = new CoordinateConverter();
        LatLng to = converter.coord(from).from(CoordinateConverter.CoordType.GPS).convert();
        return to;
    }
}
