package com.haohao;

import android.app.Service;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.BDNotifyListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;

import java.util.Date;

/**
 * 百度地图
 * @author haohao
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public MapView mapView = null;
    public BaiduMap baiduMap = null;
    private Button btnLocation;// 点击定位按钮
    private Button btnMapView;// 地图视图切换
    private TextView tvPlace;// 地点
    private EditText etSearch;

    // 定位相关声明
    public LocationClient locationClient = null;
    // 自定义图标
    boolean isFirstLoc = true;// 是否首次定位
    // 定位模式 （普通-跟随-罗盘）
    private MyLocationConfiguration.LocationMode currentMode;
    // 振动器设备
    private Vibrator mVibrator;
    private BDLocationListener locationListener;
    private BDNotifyListener notifyListener;
    private int locType;
    private Date time;
    private double longitude;// 精度
    private double latitude;// 维度
    private float radius;// 定位精度半径，单位是米
    private String addrStr;// 反地理编码
    private String province;// 省份信息
    private String city;// 城市信息
    private String district;// 区县信息
    private float direction;// 手机方向信息
    private String street;// 街道
    private String addr;// 街道号
    private String describe;// 位置描述
    private int flag = 1;
    // 定位图标描述
    private BitmapDescriptor currentMarker = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        // 在使用SDK各组件之前初始化context信息，传入ApplicationContext
        // 注意该方法要再setContentView方法之前实现
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        initView();
        addListener();
        initMap();
    }

    /**
     * 初始化控件
     */
    private void initView() {
        mapView = (MapView) this.findViewById(R.id.bmapView); // 获取地图控件引用
        btnLocation = (Button) this.findViewById(R.id.btn_location);
        btnMapView = (Button) this.findViewById(R.id.btn_mapview);
        tvPlace = (TextView) this.findViewById(R.id.tv_place);
        etSearch = (EditText) this.findViewById(R.id.et_search);
        baiduMap = mapView.getMap();
        // 普通地图
        baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        // 卫星地图
//        baiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
        // 开启交通地图
//        baiduMap.setTrafficEnabled(true);
        currentMode = MyLocationConfiguration.LocationMode.NORMAL;
        mVibrator = (Vibrator) getApplicationContext().getSystemService(
                Service.VIBRATOR_SERVICE);
        btnLocation.setText("普通");
    }

    /**
     * 监听事件
     */
    private void addListener() {
        btnLocation.setOnClickListener(this);
        btnMapView.setOnClickListener(this);
    }

    /**
     * 初始化地图信息
     */
    private void initMap() {
        baiduMap.setMyLocationEnabled(true);// 开启定位图层
        locationClient = new LocationClient(getApplicationContext()); // 初始化LocationClient类
        locationListener = new MyLocationListener();// 声明LocationListener类
        locationClient.registerLocationListener(locationListener); // 注册监听函数
        this.setLocationOption();// 设置定位参数
        // 5. 注册位置提醒监听事件
        notifyListener = new MyNotifyListener();
        notifyListener.SetNotifyLocation(longitude, latitude, 3000, "bd09ll");// 精度，维度，范围，坐标类型
        locationClient.registerNotify(notifyListener);
        // 6. 开启/关闭 定位SDK
        locationClient.start();
    }

    class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view 销毁后不在处理新接收的位置
            if (location == null || mapView == null)
                return;
            if (isFirstLoc) {
                isFirstLoc = false;
                LatLng ll = new LatLng(location.getLatitude(),
                        location.getLongitude());
                MapStatusUpdate u = MapStatusUpdateFactory
                        .newLatLngZoom(ll, 16);// 设置地图中心点以及缩放级别
                // MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
                baiduMap.animateMapStatus(u);
            }
            if (null != location
                    && location.getLocType() != BDLocation.TypeServerError) {
                StringBuffer sb = new StringBuffer(256);

                /**
                 * 时间也可以使用systemClock.elapsedRealtime()方法 获取的是自从开机以来，每次回调的时间；
                 * location.getTime() 是指服务端出本次结果的时间，如果位置不发生变化，则时间不变
                 */
                radius = location.getRadius();
                direction = location.getDirection();
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                sb.append(location.getAddrStr());
                logMsg(sb.toString());
            }
            // 构造定位数据
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(radius)//
                    .direction(direction)// 方向
                    .latitude(latitude)//
                    .longitude(longitude)//
                    .build();
            // 设置定位数据
            baiduMap.setMyLocationData(locData);
            LatLng ll = new LatLng(latitude, longitude);
            MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(ll);
            baiduMap.animateMapStatus(msu);
        }
    }

    ;

    /**
     * 位置提醒监听器
     */
    class MyNotifyListener extends BDNotifyListener {
        @Override
        public void onNotify(BDLocation bdLocation, float distance) {
            super.onNotify(bdLocation, distance);
            mVibrator.vibrate(1000);// 振动提醒已到设定位置附近
            Toast.makeText(MainActivity.this, "震动提醒", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    // 三个状态实现地图生命周期管理
    @Override
    protected void onDestroy() {
        // 退出时销毁定位
        locationClient.stop();
        baiduMap.setMyLocationEnabled(false);
        super.onDestroy();
        mapView.onDestroy();
        mapView = null;
    }

    public void logMsg(String str) {
        try {
            if (tvPlace != null)
                tvPlace.setText(str);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置定位参数
     */
    private void setLocationOption() {
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开GPS
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);// 设置定位模式
        option.setCoorType("bd09ll"); // 返回的定位结果是百度经纬度,默认值gcj02
        option.setScanSpan(60000); // 设置发起定位请求的间隔时间为6000ms
        option.setIsNeedAddress(true); // 返回的定位结果包含地址信息
        option.setNeedDeviceDirect(true); // 返回的定位结果包含手机机头的方向
        locationClient.setLocOption(option);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_location:
                switch (currentMode) {
                    case NORMAL:
                        btnLocation.setText("跟随");
                        currentMode = MyLocationConfiguration.LocationMode.FOLLOWING;
                        break;
                    case FOLLOWING:
                        btnLocation.setText("罗盘");
                        currentMode = MyLocationConfiguration.LocationMode.COMPASS;
                        break;
                    case COMPASS:
                        btnLocation.setText("普通");
                        currentMode = MyLocationConfiguration.LocationMode.NORMAL;
                        break;
                }
                baiduMap.setMyLocationConfigeration(new MyLocationConfiguration(
                        currentMode, true, currentMarker));
                break;
            case R.id.btn_mapview:
                switch (flag){
                    case 1:
                        baiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                        btnMapView.setText("卫星地图");
                        flag = 2;
                        break;
                    case 2:

                        break;
                }
                break;
        }
    }
}
