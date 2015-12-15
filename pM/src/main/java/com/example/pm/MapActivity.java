package com.example.pm;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.CircleOptions;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolygonOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.map.TextOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.mapapi.utils.CoordinateConverter;
import com.baidu.mapapi.utils.DistanceUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import app.model.Polution;
import app.model.Position;
import app.utils.Const;


public class MapActivity extends Activity {
    private MapView mMapView;

    private BaiduMap mBaiduMap = null;

    private LatLng currentPoint;
    private HashMap<LatLng, Double> monitorPoints;
    private HashMap<LatLng, Double> radiusPoints;
    private List<LatLng> trajectoryPoints;
    private int zooms[] = new int[]{50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000, 100000, 200000, 500000, 1000000, 2000000};
    public boolean ViewSettingDone = false;

    private int lastZoom = 15;

    //monitor locations
    private static final int Location_TIME_INTERVAL = 100 * 1000;//1分钟
    private Handler LocationHandler = new Handler();
    private Runnable LocationRunnable = new Runnable() {
        @Override
        public void run() {
            drawLocation();
            LocationHandler.postDelayed(LocationRunnable, Location_TIME_INTERVAL);
        }
    };

    //trajectory
    private static final int Trajectory_TIME_INTERVAL = 100 * 1000;//1分钟
    private Handler TrajectoryHandler = new Handler();
    private Runnable TrajectoryRunnable = new Runnable() {
        @Override
        public void run() {
            drawTrajectory();
            TrajectoryHandler.postDelayed(TrajectoryRunnable, Trajectory_TIME_INTERVAL);
        }
    };

    private String position_result = "";
    private String polution_result = "";
    private String city = "上海";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_map);

        mMapView = (MapView) this.findViewById(R.id.bmapView);
        mMapView.showZoomControls(false);
        mMapView.showScaleControl(false);

        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        //mBaiduMap.setBaiduHeatMapEnabled(true);

        trajectoryPoints = new ArrayList<LatLng>();
        monitorPoints = new HashMap<LatLng, Double>();
        radiusPoints = new HashMap<>();
        simulate();
        //start the thread to update the location
        HandlerThread thread = new HandlerThread("MapActivity");
        thread.start();
        TrajectoryHandler = new Handler(thread.getLooper());
        TrajectoryHandler.post(TrajectoryRunnable);

        LocationHandler = new Handler(thread.getLooper());
        LocationHandler.post(LocationRunnable);

        //drawLegend();

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
                Log.d("zoom", "zoom is changed");
                drawLocation();
                lastZoom = (int) mapStatus.zoom;
        }
    };

    private void sendAllpositionsRequest() {
        RequestQueue mQueue = Volley.newRequestQueue(this);
        String url = "http://ilab.tongji.edu.cn/pm25/web/restful/area-positions";
        JsonObjectRequest position = new JsonObjectRequest(url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("position", response.toString());
                        position_result = response.toString();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("TAG", error.getMessage(), error);
            }
        });
        mQueue.add(position);
        mQueue.start();
    }

    private void sendPolutionRequest() {
        RequestQueue mQueue = Volley.newRequestQueue(this);
        String city = "上海";
        String url = "http://ilab.tongji.edu.cn/pm25/web/restful/air-qualities" + "?area=" + city;
        Log.e("url", url);
        JsonArrayRequest polution = new JsonArrayRequest(url,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.d("polution", response.toString());
                        polution_result = response.toString();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("TAG", "Error: " + error.getMessage());
            }
        });
        mQueue.add(polution);
    }

    private void getLocationDensity() {
        Vector<Polution> polutions = getPolution(city, polution_result);
        HashMap<String,Vector<Position>> areas = getAllPositions(position_result);
        Vector<Position> positions = areas.get(city);
        for (Polution polution:polutions) {
            for (Position position:positions) {
                if (polution.getPosition_name().equals(position.getName())) {
                    LatLng point = new LatLng(position.getLatitude(),position.getLongtitude());
                    monitorPoints.put(point,new Double(polution.getPM2_5()));
                }
            }
        }
        for (LatLng point : monitorPoints.keySet()) {
            double distance = getMinimumDistance(point,monitorPoints.keySet());
            Log.d("","distance"+distance);
            radiusPoints.put(point, distance / 2);
        }
    }

    private HashMap<String,Vector<Position>> getAllPositions(String result) {
        HashMap<String,Vector<Position>> areas = new HashMap<String,Vector<Position>>();

        JSONObject object;
        try {
            object = new JSONObject(result);
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONArray array = object.getJSONArray(key);
                Vector<Position> positions = new Vector<Position>();
                for(int i=0 ; i < array.length();i++)
                {
                    JSONObject myjObject = array.getJSONObject(i);
                    String position_name = myjObject.getString("position_name");
                    double latitude = myjObject.getDouble("latitude");
                    double longtitude = myjObject.getDouble("longtitude");
                    String alias = myjObject.getString("alias");
                    positions.add(new Position(position_name,latitude,longtitude,alias));
                }
                areas.put(key, positions);
            }
            return areas;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Vector<Polution> getPolution(String location,String result) {
        if (location==null) {
            return null;
        }
        Vector<Polution> polutions = new Vector<Polution>();
        try {
            System.out.println(result);
            JSONArray array;
            array = new JSONArray(result);
            for(int i=0 ; i < array.length();i++)
            {
                JSONObject myjObject = array.getJSONObject(i);
                int id = myjObject.getInt("id");
                int aqi = myjObject.getInt("aqi");
                String area = myjObject.getString("area");
                String position_name = myjObject.getString("position_name");
                String station_code = myjObject.getString("station_code");
                int pm2_5 = myjObject.getInt("pm2_5");
                int pm2_5_24h = myjObject.getInt("pm2_5_24h");
                String primary_pollutant = myjObject.getString("primary_pollutant");
                String quality = myjObject.getString("quality");
                String time_point = myjObject.getString("time_point");

                polutions.add(new Polution(id, aqi, area,position_name,station_code, pm2_5,
                        pm2_5_24h,primary_pollutant, quality, time_point));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return polutions;
    }

    //simulate the trajectory
    private void simulate() {
        currentPoint = new LatLng(31.249766710565, 121.48789949069);
        //simulate trajectory points
        trajectoryPoints.add(new LatLng(31.241261710015, 121.48789948569));
        trajectoryPoints.add(new LatLng(31.242362710125, 121.48789948669));
        trajectoryPoints.add(new LatLng(31.243463710235, 121.48789948769));
        trajectoryPoints.add(new LatLng(31.244564710345, 121.48789948869));
        trajectoryPoints.add(new LatLng(31.245665710455, 121.48789948969));
        trajectoryPoints.add(new LatLng(31.246766710565, 121.48789949069));
        trajectoryPoints.add(new LatLng(31.247766710565, 121.48789949069));
        trajectoryPoints.add(new LatLng(31.248766710565, 121.48789949069));
        trajectoryPoints.add(new LatLng(31.249766710565, 121.48789949069));
        //simulate monitor points
        LatLng p1 = new LatLng(31.249161710015, 121.48789948569);
        LatLng p2 = new LatLng(31.169152089592, 121.44623500473);
        LatLng p3 = new LatLng(31.263742929076, 121.39844294375);
        LatLng p4 = new LatLng(31.304510479542, 121.53571659963);
        LatLng p5 = new LatLng(31.304510479542, 121.40833126667);
        LatLng p6 = new LatLng(31.230895349134, 121.63848131409);
        LatLng p7 = new LatLng(31.282497228987, 121.49191854079);
        LatLng p8 = new LatLng(31.137700846982, 121.01851301174);
        LatLng p9 = new LatLng(31.235380803488, 121.454755567);
        monitorPoints.put(p1, 20.0);
        monitorPoints.put(p2, 60.0);
        monitorPoints.put(p3, 100.0);
        monitorPoints.put(p4, 120.0);
        monitorPoints.put(p5, 160.0);
        monitorPoints.put(p6, 200.0);
        monitorPoints.put(p7, 240.0);
        monitorPoints.put(p8, 280.0);
        monitorPoints.put(p9, 320.0);

        for (LatLng point : monitorPoints.keySet()) {
            double distance = getMinimumDistance(point,monitorPoints.keySet());
            Log.d("","distance"+distance);
            radiusPoints.put(point, distance / 2);
        }
    }

    private double getMinimumDistance(LatLng point, Set<LatLng> latLngs) {
        double minDistance = 1000000;
        for (LatLng latLng : latLngs) {
            double distance = this.getDistance(point,latLng);
            if (distance!=0&&distance<minDistance) {
                minDistance = distance;
            }
        }
        return minDistance;
    }

    private double getDistance(LatLng point, LatLng latLng) {
        return DistanceUtil.getDistance(point,latLng);
    }

    private int chooseColor(double density) {
        if (density < 50) {
            return 0x2F00FF00;
        } else if (density < 100) {
            return 0x2FFFFF00;
        } else if (density < 150) {
            return 0x2FFF7F00;
        } else if (density < 200) {
            return 0x2FFF0000;
        } else if (density < 300) {
            return 0x2FFF3030;
        } else {
            return 0x2F000000;
        }
    }

    private void drawLegend() {
        LatLngBounds bounds = mMapView.getMap().getMapStatus().bound;
        LatLng southwest = bounds.southwest;
        LatLng northeast = bounds.northeast;
        double widthGPS = northeast.longitude - southwest.longitude;
        double heightGPS = northeast.latitude - southwest.latitude;
        String densities[] = Const.airDensity;
        int size = densities.length;
        double averageWidth = widthGPS/size;
        LatLng center = mMapView.getMap().getMapStatus().target;
        Log.d("","latlng"+southwest.latitude+" "+southwest.longitude+ " "+center.latitude);
        LatLng startPoint = new LatLng(southwest.latitude+heightGPS/20,southwest.longitude+widthGPS/50);
        averageWidth = averageWidth;
        int i = 0;
        for (i=0;i<densities.length-1;i++) {
            LatLng endPoint = new LatLng(startPoint.latitude,startPoint.longitude+averageWidth);
            int color = this.chooseColor(Double.valueOf(densities[i])) + 0xD0000000;
            this.plotLine(startPoint,endPoint,color);
            this.plotText(new LatLng(startPoint.latitude-heightGPS/80,startPoint.longitude),densities[i],0xFF000000);
            startPoint = endPoint;
        }

        this.plotText(new LatLng(startPoint.latitude - heightGPS / 80, startPoint.longitude), densities[i], 0xFF000000);

    }

    private void plotLine(LatLng startPoint,LatLng endPoint,int color) {
        //add a line
        List<LatLng> points = new ArrayList<>();
        points.add(startPoint);points.add(endPoint);
        OverlayOptions ooPolyline = new PolylineOptions().width(5).color(color).points(points);
        mBaiduMap.addOverlay(ooPolyline);
    }

    private void plotText(LatLng startPoint,String text,int color) {
        //构建文字Option对象，用于在地图上添加文字
        OverlayOptions textOption = new TextOptions()
                .fontSize(40)
                .fontColor(color)
                .text(text)
                .position(startPoint);
        //在地图上添加该文字对象并显示
        mBaiduMap.addOverlay(textOption);
    }

    private void drawLocation() {
        mBaiduMap.clear();
        drawTrajectory();
        drawLegend();
        int zoom = (int) mBaiduMap.getMapStatus().zoom;
        int maxZoomLevel = (int) mBaiduMap.getMaxZoomLevel();
        int index = maxZoomLevel - zoom;
        if (index < 0) {
            index = 0;
        } else if (index >= zooms.length) {
            index = zooms.length - 1;
        }
        initMonitorPoints();
        for (LatLng point : monitorPoints.keySet()) {
            int radius = zooms[index]/5;
            double density = monitorPoints.get(point);
            int color = this.chooseColor(density);
            LatLng toPoint = this.convert(point);
            int maxRadius = (int) radiusPoints.get(point).doubleValue();
            Log.d("radius", zoom + " " + maxRadius + " " + radius + " " + mBaiduMap.getMaxZoomLevel() + " " + mBaiduMap.getMinZoomLevel());

            if (radius>maxRadius) {
                radius = maxRadius;
            }

            OverlayOptions ooPolyline = new CircleOptions().center(toPoint).radius(radius).fillColor(color);
            mBaiduMap.addOverlay(ooPolyline);
        }
    }

    private void initMonitorPoints() {
        position_result = "";
        polution_result = "";
        long start = System.currentTimeMillis()/1000/60;
        this.sendAllpositionsRequest();
        this.sendPolutionRequest();
        long end = System.currentTimeMillis()/1000/60;
        while (position_result==""||polution_result=="") {
            if (end-start>2) {
                break;
            }
            end = System.currentTimeMillis()/1000/60;
        }
        if (position_result!=""&& polution_result!="") {
            monitorPoints.clear();
            getLocationDensity();
        }
    }

    public void drawTrajectory() {
        List<LatLng> points = this.convertPoints();

        if (!ViewSettingDone) {
            setViewAngle(currentPoint);
            ViewSettingDone = true;
        }
        if (points.size() >= 2) {
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
