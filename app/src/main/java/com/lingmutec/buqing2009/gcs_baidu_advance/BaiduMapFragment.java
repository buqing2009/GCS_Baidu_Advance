package com.lingmutec.buqing2009.gcs_baidu_advance;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.CoordinateConverter;

/**
 * Created by buqing2009 on 15-11-26.
 */
public class BaiduMapFragment extends Fragment implements View.OnClickListener{

    private MapView mMapView = null;
    private BaiduMap bdMap;
    private View saveView;
    //    private MapController mMapController = null;
//    private Toast mToast=null;
//    private BMapManager mBMapManager=null;
    private boolean firstLocation;
    private boolean droneFirstLocation;
    private BitmapDescriptor mCurrentMarker;
    private MyLocationConfiguration config;
    private LocationClient mLocationClient = null;

    //新建drone的marker
    BitmapDescriptor droneMarker;
    BitmapDescriptor destiMarker;//设置终点marker
    //下面获取Dronekit Fragment的GPS信息
    FragmentManager fragmentManager;
    DronekitFragment dronekitFragment;

    //定义Handler
    Handler myhandler = new Handler();

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        //注意该方法要再setContentView方法之前实现
        SDKInitializer.initialize(getActivity().getApplicationContext());
        View view = inflater.inflate(R.layout.baidumap_fragment, container, false);
        return view;
    }

    @Override

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

//        setContentView(R.layout.baidumap);
        //获取地图控件引用
        mMapView = (MapView) getActivity().findViewById(R.id.bmapView);
        bdMap = mMapView.getMap();
        MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(15f);
        bdMap.setMapStatus(msu);

        //GPS在百度地图上的显示具体变量
        droneMarker = BitmapDescriptorFactory.fromResource(R.drawable.drone_marker);
        destiMarker = BitmapDescriptorFactory.fromResource(R.drawable.destimarker);
        fragmentManager = this.getActivity().getFragmentManager();
        dronekitFragment = (DronekitFragment) fragmentManager.findFragmentByTag("Dronekit");

        // 定位初始化
        mLocationClient = new LocationClient(this.getActivity().getApplicationContext());
        firstLocation = true;
        droneFirstLocation = true;

        // 设置定位的相关配置
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setOpenGps(true);
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);
        mLocationClient.setLocOption(option);

        BitmapDescriptor myMarker = BitmapDescriptorFactory
                .fromResource(R.drawable.download);

        MyLocationConfiguration config = new MyLocationConfiguration(
                MyLocationConfiguration.LocationMode.FOLLOWING, true, myMarker);

        Button btn_sel_map_type = (Button) getActivity().findViewById(R.id.mtype_select_bottom);
        Button btn_locate_map = (Button) getActivity().findViewById(R.id.mlocate_bottom);

        btn_sel_map_type.setOnClickListener(this);
        btn_locate_map.setOnClickListener(this);

        mLocationClient.registerLocationListener(new BDLocationListener() {
            @Override
            public void onReceiveLocation(BDLocation location) {
                // map view 销毁后不在处理新接收的位置
                if (location == null || mMapView == null)
                    return;
                // 构造定位数据
                MyLocationData locData = new MyLocationData.Builder()
                        .accuracy(location.getRadius())
                                // 此处设置开发者获取到的方向信息，顺时针0-360
                        .direction(100).latitude(location.getLatitude())
                        .longitude(location.getLongitude()).build();
                // 设置定位数据
                bdMap.setMyLocationData(locData);

                // 第一次定位时，将地图位置移动到当前位置
                if (firstLocation) {
                    firstLocation = false;
                    LatLng xy = new LatLng(location.getLatitude(),
                            location.getLongitude());
                    MapStatusUpdate status = MapStatusUpdateFactory.newLatLng(xy);
                    bdMap.animateMapStatus(status);
                }
            }
        });

        //调用百度地图循环获取GPS坐标的任务
        myhandler.post(getGPSTask);//立即调用

        //添加长按地图获得标记终点
       bdMap.setOnMapLongClickListener(new BaiduMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(final LatLng select_point) {
                Log.e("bluking_point",select_point.toString());
                //创建InfoWindow展示的view
                Button setDesPoint = new Button(getActivity().getApplicationContext());
                setDesPoint.setText("选择为终点");
//                button.setBackgroundResource(R.drawable.popup);
                //创建InfoWindow , 传入 view， 地理坐标， y 轴偏移量
                InfoWindow mInfoWindow = new InfoWindow(setDesPoint, select_point, -47);
                //显示InfoWindow
                bdMap.showInfoWindow(mInfoWindow);

                setDesPoint.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        OverlayOptions options = new MarkerOptions().position(select_point).icon(destiMarker);
                        bdMap.addOverlay(options);

                        // 将GPS设备采集的原始GPS坐标转换成百度坐标
                        CoordinateConverter converter  = new CoordinateConverter();
                        converter.from(CoordinateConverter.CoordType.GPS);
                        // sourceLatLng待转换坐标
                        converter.coord(select_point);
                        LatLng destiConverted = converter.convert();
                        //@bluking通过投机取巧方法把百度坐标转化为GPS坐标
                        LatLng destiGPSPoint = new LatLng(2*select_point.latitude - destiConverted.latitude,2*select_point.longitude-destiConverted.longitude);

                        dronekitFragment.setGPSPos(destiGPSPoint);
                    }
                });
            }
        });


    }


    @Override
    public void onStart() {
        // 如果要显示位置图标,必须先开启图层定位
        bdMap.setMyLocationEnabled(true);
        super.onStart();
    }

    @Override
    public void onStop() {
        // 关闭图层定位
        bdMap.setMyLocationEnabled(false);
        mLocationClient.stop();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }



    protected void selectMap() {
        if (bdMap.getMapType() == BaiduMap.MAP_TYPE_NORMAL) {
            bdMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
        } else {
            bdMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        }
    }

    protected void blu_locate() {
        if (!mLocationClient.isStarted()) {
            mLocationClient.start();
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.mtype_select_bottom:
                selectMap();
                break;
            case R.id.mlocate_bottom:
                blu_locate();
                break;
        }
    }

    //新建一个循环获取GPS的任务
    protected Runnable getGPSTask = new Runnable() {
        @Override
        public void run() {
            myhandler.postDelayed(this,500);
            updateGPSOnBaiduMap();
        }
    };

    //@bluking 另开线程实时刷新GPS位置并在显示在地图上
    public void updateGPSOnBaiduMap(){
            if (dronekitFragment.isGPSReturn()) {
                if(droneFirstLocation){
                    droneFirstLocation = false;
                    Double[] gpsPos = dronekitFragment.getGPSPos();
                    if (gpsPos[0] != null && gpsPos[1] != null) {
                        LatLng dronePos = new LatLng(gpsPos[1], gpsPos[0]);
                        // 将GPS设备采集的原始GPS坐标转换成百度坐标
                        CoordinateConverter converter  = new CoordinateConverter();
                        converter.from(CoordinateConverter.CoordType.GPS);
                        // sourceLatLng待转换坐标
                        converter.coord(dronePos);
                        LatLng dronePos_bd = converter.convert();
                        OverlayOptions options = new MarkerOptions().position(dronePos_bd).icon(droneMarker);
                        MapStatusUpdate status = MapStatusUpdateFactory.newLatLng(dronePos_bd);
                        bdMap.animateMapStatus(status);
                        bdMap.addOverlay(options);
                    }
                }else{
                    Double[] gpsPos = dronekitFragment.getGPSPos();
                    Log.e("GPS_JIN", gpsPos[0].toString());
                    Log.e("GPS_WEI", gpsPos[1].toString());
                    if (gpsPos[0] != null && gpsPos[1] != null) {
                        LatLng dronePos = new LatLng(gpsPos[1], gpsPos[0]);
                        // 将GPS设备采集的原始GPS坐标转换成百度坐标
                        CoordinateConverter converter  = new CoordinateConverter();
                        converter.from(CoordinateConverter.CoordType.GPS);
                        // sourceLatLng待转换坐标
                        converter.coord(dronePos);
                        LatLng dronePos_bd = converter.convert();
                        OverlayOptions options = new MarkerOptions().position(dronePos_bd).icon(droneMarker);
                        bdMap.addOverlay(options);

                    }
                }
            }
    }





}