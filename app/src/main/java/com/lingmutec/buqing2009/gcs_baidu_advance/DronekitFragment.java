package com.lingmutec.buqing2009.gcs_baidu_advance;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import com.baidu.mapapi.model.LatLng;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
//曾经的dronestate包更新为vehicle api
import com.o3dr.android.client.apis.VehicleApi;
//曾经的guided api变为control api包
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.connection.ConnectionResult;

import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.Home;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.android.client.apis.*;
import com.o3dr.services.android.lib.model.AbstractCommandListener;

import java.util.List;


/**
 * Created by buqing2009 on 15-11-26.
 */
public class DronekitFragment extends Fragment implements View.OnClickListener, DroneListener, TowerListener {

    private Drone drone;
    private int droneType = Type.TYPE_UNKNOWN;
    private ControlTower controlTower;
    private VehicleApi dronestate;
    private ControlApi guide;
    private final Handler handler = new Handler();
    private final int DEFAULT_UDP_PORT = 14550;
    private final int DEFAULT_USB_BAUD_RATE = 57600;
    Spinner modeSelector;
    private LatLng destiGPSPos;//获取百度地图终点的GPS坐标
    FragmentManager fragmentManager;
    ControllerFragment controllerFragment;
    //定义Handler
    Handler myhandler = new Handler();
    //是否允许控制
    boolean allow_control;
    //存放控制量
    Double[] controller_val;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.dronekit_fragment, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        allow_control = false;
        controller_val = new Double[4];


        final Context context = getActivity().getApplicationContext();
        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);

        fragmentManager = this.getActivity().getFragmentManager();
        controllerFragment = (ControllerFragment) fragmentManager.findFragmentByTag("Controller");
        //新式获取API的方式，比以往更为安全
        this.dronestate = VehicleApi.getApi(this.drone);
        this.guide = ControlApi.getApi(this.drone);
        this.modeSelector = (Spinner) getActivity().findViewById(R.id.modeSelect);
        this.modeSelector.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onFlightModeSelected(view);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        Button btnConnect = (Button) getActivity().findViewById(R.id.btnConnect);
        Button btnArmTakeOff = (Button) getActivity().findViewById(R.id.btnArmTakeOff);
        Button button_up = (Button) getActivity().findViewById(R.id.button_up);
        Button button_down = (Button) getActivity().findViewById(R.id.button_down);
        Button button_control = (Button) getActivity().findViewById(R.id.RC_Control);

        btnConnect.setOnClickListener(this);
        btnArmTakeOff.setOnClickListener(this);
        button_up.setOnClickListener(this);
        button_down.setOnClickListener(this);
        button_control.setOnClickListener(this);

        myhandler.post(getController);//立即调用


    }

//    public static DronekitFragment getInstance(Bundle bundle) {
//        DronekitFragment dronekitFragment = new DronekitFragment();
//        dronekitFragment.setArguments(bundle);
//        return dronekitFragment;
//    }


    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);
        updateVehicleModesForType(this.droneType);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
            updateConnectedButton(false);
        }
        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }

    // 3DR Services Listener
    // ==========================================================

    @Override
    public void onTowerConnected() {
        alertUser("3DR Services Connected");
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);
    }

    @Override
    public void onTowerDisconnected() {
        alertUser("3DR Service Interrupted");
    }

    // Drone Listener
    // ==========================================================

    @Override
    public void onDroneEvent(String event, Bundle extras) {

        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                alertUser("Drone Connected");
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();

                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
                break;

            case AttributeEvent.STATE_UPDATED:
            case AttributeEvent.STATE_ARMING:
                updateArmButton();
                break;

            case AttributeEvent.TYPE_UPDATED:
                Type newDroneType = this.drone.getAttribute(AttributeType.TYPE);
                if (newDroneType.getDroneType() != this.droneType) {
                    this.droneType = newDroneType.getDroneType();
                    updateVehicleModesForType(this.droneType);
                }
                break;

            case AttributeEvent.STATE_VEHICLE_MODE:
                updateVehicleMode();
                break;


            case AttributeEvent.SPEED_UPDATED:
                updateSpeed();
                break;

            case AttributeEvent.ALTITUDE_UPDATED:
                updateAltitude();
                break;

            case AttributeEvent.HOME_UPDATED:
                updateDistanceFromHome();
                break;


            default:
//                Log.i("DRONE_EVENT", event); //Uncomment to see events from the drone
                break;
        }

    }

    @Override
    public void onDroneConnectionFailed(ConnectionResult result) {
        alertUser("Connection Failed:" + result.getErrorMessage());
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

    // UI Events
    // ==========================================================

    public void onBtnConnectTap(View view) {
        if (this.drone.isConnected()) {
            this.drone.disconnect();
        } else {
            Spinner connectionSelector = (Spinner) getActivity().findViewById(R.id.selectConnectionType);
            int selectedConnectionType = connectionSelector.getSelectedItemPosition();

            Bundle extraParams = new Bundle();
            if (selectedConnectionType == ConnectionType.TYPE_USB) {
                extraParams.putInt(ConnectionType.EXTRA_USB_BAUD_RATE, DEFAULT_USB_BAUD_RATE); // Set default baud rate to 57600
            } else {
                extraParams.putInt(ConnectionType.EXTRA_UDP_SERVER_PORT, DEFAULT_UDP_PORT); // Set default baud rate to 14550
            }
            ConnectionParameter connectionParams = new ConnectionParameter(selectedConnectionType, extraParams, null);
            this.drone.connect(connectionParams);
        }

    }

    public void onFlightModeSelected(View view) {
        VehicleMode vehicleMode = (VehicleMode) this.modeSelector.getSelectedItem();
//        this.drone.changeVehicleMode(vehicleMode);
        this.dronestate.setVehicleMode(vehicleMode);
    }

    public void onArmButtonTap(View view) {
        Button thisButton = (Button) view;
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        if (vehicleState.isFlying()) {
            // Land
//            this.drone.changeVehicleMode(VehicleMode.COPTER_LAND);
            this.dronestate.setVehicleMode(VehicleMode.COPTER_LAND);
        } else if (vehicleState.isArmed()) {
            // Take off
//            this.drone.doGuidedTakeoff(10);
            this.guide.takeoff(10, new AbstractCommandListener() {
                @Override
                public void onSuccess() {
                    Log.d("bluking", "Takeoff Successfully");
                }

                @Override
                public void onError(int executionError) {
                    Log.d("bluking", "Takeoff Failure");
                }

                @Override
                public void onTimeout() {
                    Log.d("bluking", "Takeoff Timeout");
                }
            });
        } else if (!vehicleState.isConnected()) {
            // Connect
            alertUser("Connect to a drone first");
        } else {
            // Connected but not Armed
//            this.drone.arm(true);
            this.dronestate.arm(true);
        }
    }

    // UI updating
    // ==========================================================

    protected void updateConnectedButton(Boolean isConnected) {
        Button connectButton = (Button) getActivity().findViewById(R.id.btnConnect);
        if (isConnected) {
            connectButton.setText("Disconnect");
        } else {
            connectButton.setText("Connect");
        }
    }

    protected void updateArmButton() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Button armButton = (Button) getActivity().findViewById(R.id.btnArmTakeOff);

        if (!this.drone.isConnected()) {
            armButton.setVisibility(View.INVISIBLE);
        } else {
            armButton.setVisibility(View.VISIBLE);
        }

        if (vehicleState.isFlying()) {
            // LandGuidedApi
            armButton.setText("LAND");
        } else if (vehicleState.isArmed()) {
            // Take off
            armButton.setText("TAKE OFF");
        } else if (vehicleState.isConnected()) {
            // Connected but not Armed
            armButton.setText("ARM");
        }
    }

    protected void updateAltitude() {
        TextView altitudeTextView = (TextView) getActivity().findViewById(R.id.altitudeValueTextView);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        altitudeTextView.setText(String.format("%3.1f", droneAltitude.getAltitude()) + "m");
    }

    protected void updateSpeed() {
        TextView speedTextView = (TextView) getActivity().findViewById(R.id.speedValueTextView);
        Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
        speedTextView.setText(String.format("%3.1f", droneSpeed.getGroundSpeed()) + "m/s");

    }

    protected void updateDistanceFromHome() {
        TextView distanceTextView = (TextView) getActivity().findViewById(R.id.distanceValueTextView);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        double vehicleAltitude = droneAltitude.getAltitude();
        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        LatLong vehiclePosition = droneGps.getPosition();

        double distanceFromHome = 0;

        if (droneGps.isValid()) {
            LatLongAlt vehicle3DPosition = new LatLongAlt(vehiclePosition.getLatitude(), vehiclePosition.getLongitude(), vehicleAltitude);
            Home droneHome = this.drone.getAttribute(AttributeType.HOME);
            distanceFromHome = distanceBetweenPoints(droneHome.getCoordinate(), vehicle3DPosition);
        } else {
            distanceFromHome = 0;
        }

        distanceTextView.setText(String.format("%3.1f", distanceFromHome) + "m");
    }

    protected void updateVehicleModesForType(int droneType) {

        List<VehicleMode> vehicleModes = VehicleMode.getVehicleModePerDroneType(droneType);
        // ArrayAdapter<VehicleMode> vehicleModeArrayAdapter = new ArrayAdapter<VehicleMode>(this.getActivity().getApplicationContext(), android.R.layout.simple_list_item_1, vehicleModes);
        //@bluking 动态增加mode菜单选项，并自定义样式列表
        ArrayAdapter<VehicleMode> vehicleModeArrayAdapter = new ArrayAdapter<VehicleMode>(this.getActivity().getApplicationContext(), R.layout.myspinner, vehicleModes);
        //@bluking 动态应用样式列表到下拉菜单所有item类目
        vehicleModeArrayAdapter.setDropDownViewResource(R.layout.checkedtest_blu);
        this.modeSelector.setAdapter(vehicleModeArrayAdapter);
    }

    protected void updateVehicleMode() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        ArrayAdapter arrayAdapter = (ArrayAdapter) this.modeSelector.getAdapter();
        this.modeSelector.setSelection(arrayAdapter.getPosition(vehicleMode));
    }

    // Helper methods
    // ==========================================================

    protected void alertUser(String message) {
        Toast.makeText(getActivity().getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    protected double distanceBetweenPoints(LatLongAlt pointA, LatLongAlt pointB) {
        if (pointA == null || pointB == null) {
            return 0;
        }
        double dx = pointA.getLatitude() - pointB.getLatitude();
        double dy = pointA.getLongitude() - pointB.getLongitude();
        double dz = pointA.getAltitude() - pointB.getAltitude();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public void onUpBottonTap(View view) {

        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        if (vehicleState.isFlying()) {
//            Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
//            LatLong vehiclePosition = droneGps.getPosition();
            Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);//获取当前的海拔高度
            double altitude_set = droneAltitude.getAltitude();
            this.guide.climbTo((altitude_set + 2.0));
//            this.guide.sendGuidedPoint(this.drone,vehiclePosition,true);
//            this.guide.pauseAtCurrentLocation(this.drone);

        }
    }

    public void onDownBottonTap(View view) {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        if (vehicleState.isFlying()) {
//            Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
//            LatLong vehiclePosition = droneGps.getPosition();
            Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);//获取当前的海拔高度
            double altitude_set = droneAltitude.getAltitude();
            this.guide.climbTo((altitude_set - 2.0));
//            this.guide.sendGuidedPoint(this.drone,vehiclePosition,true);
//            this.guide.pauseAtCurrentLocation(this.drone);
        }
    }

    public boolean isGPSReturn() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        if (vehicleState.isFlying()) {
            return true;
        } else {
            return false;
        }
    }

    public Double[] getGPSPos() {
        Double[] gpsPos = new Double[2];
        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        LatLong vehiclePosition = droneGps.getPosition();
        gpsPos[0] = vehiclePosition.getLongitude();//获取经度
        gpsPos[1] = vehiclePosition.getLatitude();//获取纬度
        return gpsPos;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnConnect:
                onBtnConnectTap(v);
                break;
            case R.id.btnArmTakeOff:
                onArmButtonTap(v);
                break;
            case R.id.button_up:
                onUpBottonTap(v);
                break;
            case R.id.button_down:
                onDownBottonTap(v);
                break;
            case R.id.RC_Control:
                controllerAllow(v);
                break;
            default:
                break;
        }

    }

    public void setGPSPos(LatLng destipos) {
        destiGPSPos = destipos;
        runSimpleMission();

    }

    //从起点到终点的简单飞行任务
    public void runSimpleMission() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        if (vehicleState.isFlying()) {
            LatLong destiGPSPos_fordk = new LatLong(destiGPSPos.latitude, destiGPSPos.longitude);
            this.guide.goTo(destiGPSPos_fordk, true, new AbstractCommandListener() {
                @Override
                public void onSuccess() {

                    Log.d("bluking", "GO To target successfully");
                }

                @Override
                public void onError(int executionError) {
                    Log.d("bluking", "GO To target failure");
                }

                @Override
                public void onTimeout() {

                    Log.d("bluking", "GO To target timeout");
                }
            });
        }
    }

    protected Runnable getController = new Runnable() {
        @Override
        public void run() {
//            if (allow_control) {
            myhandler.postDelayed(this, 500);

            RC_Controller();
//                Log.e("bluking","Runnable!");
//            }
        }
    };

    protected void controllerAllow(View v) {
        allow_control = true;
        alertUser("Control Passed!");
//        Log.e("bluking","Allow Button Pressed!");
    }

    public void RC_Controller() {
        final ControlApi controller;
        controller = this.guide;
        if (allow_control) {

            controller_val = controllerFragment.getControlValues();
//            Log.e("bluking", controller_val.toString());
            final double throttle_val, pitch_val, yaw_val, roll_val;
            throttle_val = controller_val[0];
            yaw_val = controller_val[1];
            pitch_val = controller_val[2];
            roll_val = controller_val[3];
//            Log.e("bluking",Double.toString(throttle_val));
            State vehicleState = this.drone.getAttribute(AttributeType.STATE);
            controller.enableManualControl(true, new ControlApi.ManualControlStateListener() {
                @Override
                public void onManualControlToggled(boolean isEnabled) {

                    if(isEnabled) {
                        //发送舵量,前后和左右值
                        controller.manualControl((float) pitch_val, (float) roll_val, (float) -throttle_val, new AbstractCommandListener() {
                            @Override
                            public void onSuccess() {
                                Log.e("bluking", "Controller successfully");
                            }

                            @Override
                            public void onError(int executionError) {

                            }

                            @Override
                            public void onTimeout() {

                            }
                        });
                        //发送yaw转向角
                        controller.turnTo((float)(5*yaw_val), (float)yaw_val, true, new AbstractCommandListener() {
                            @Override
                            public void onSuccess() {

                            }

                            @Override
                            public void onError(int executionError) {

                            }

                            @Override
                            public void onTimeout() {

                            }
                        });
                    }
                }
            });




//            this.guide.turnTo((float)yaw_val,);


        }
    }
}