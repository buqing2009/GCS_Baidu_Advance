package com.lingmutec.buqing2009.gcs_baidu_advance;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jmedeisis.bugstick.Joystick;
import com.jmedeisis.bugstick.JoystickListener;


/**
 * Created by buqing2009 on 12/4/15.
 */
public class ControllerFragment extends Fragment {

//    private static final float MAX_BUG_SPEED_DP_PER_S = 300f;
    private static final double PI = 3.1415926;
    private double throttle_val, pitch_val, yaw_val, roll_val;
    private Double[] control_array;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.controller_fragment, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

//        final TextView angleView = (TextView) getActivity().findViewById(R.id.tv_angle);
//        final TextView offsetView = (TextView) getActivity().(R.id.tv_offset);
//
//        final BugView bugView = (BugView) findViewById(R.id.bugview);
        control_array = new Double[4];//定义存储4个控制量的数组
        throttle_val = pitch_val = yaw_val = roll_val = 0.0;

        final String angleNoneString = getString(R.string.angle_value_none);
        final String angleValueString = getString(R.string.angle_value);
        final String offsetNoneString = getString(R.string.offset_value_none);
        final String offsetValueString = getString(R.string.offset_value);

        final TextView throttle = (TextView) getActivity().findViewById(R.id.TextViewX1);//油门
        final TextView pitch = (TextView) getActivity().findViewById(R.id.TextViewX2);//前后
        final TextView yaw = (TextView) getActivity().findViewById(R.id.TextViewY1);//转向
        final TextView roll = (TextView) getActivity().findViewById(R.id.TextViewY2);//左右



        Joystick joystick1 = (Joystick) getActivity().findViewById(R.id.joystick1);
        Joystick joystick2 = (Joystick) getActivity().findViewById(R.id.joystick2);

        joystick1.setJoystickListener(new JoystickListener() {
            @Override
            public void onDown() {

            }

            @Override
            public void onDrag(float degrees, float offset) {
                throttle_val =  offset * Math.sin(degrees*PI/180.0);
                yaw_val = offset * Math.cos(degrees*PI/180.0);
                if(degrees >=0){
                    //油门向上
                    //计算油门大小

                    throttle.setText(String.format(angleValueString, throttle_val));

                }else{
                    //油门向下
                    throttle.setText(String.format(angleValueString, throttle_val));
                }

                if(degrees > -90.0 && degrees <= 90.0){

                    //顺时针旋转
                    yaw.setText(String.format(angleValueString, yaw_val));
                }else{
                    //逆时针旋转
                    yaw.setText(String.format(angleValueString, yaw_val));
                }
            }

            @Override
            public void onUp() {
                throttle_val = yaw_val = 0.0;
                throttle.setText("0.0");
                yaw.setText("0.0");
            }
        });

        joystick2.setJoystickListener(new JoystickListener() {
            @Override
            public void onDown() {

            }

            @Override
            public void onDrag(float degrees, float offset) {
                pitch_val = offset * Math.sin(degrees*PI/180.0);
                roll_val = offset * Math.cos(degrees*PI/180.0);
                if(degrees >=0){
                    //前
                    pitch.setText(String.format(angleValueString, pitch_val));

                }else{
                    //后
                    pitch.setText(String.format(angleValueString, pitch_val));
                }

                if(degrees > -90.0 && degrees <= 90.0){
                    //左
                    roll.setText(String.format(angleValueString, roll_val));
                }else{
                    //右
                    roll.setText(String.format(angleValueString, roll_val));
                }
            }

            @Override
            public void onUp() {
                pitch_val = roll_val = 0.0;
                pitch.setText("0.0");
                roll.setText("0.0");
            }
        });
    }

    public Double[] getControlValues(){
        control_array[0] = throttle_val;
        control_array[1] = yaw_val;
        control_array[2] = pitch_val;
        control_array[3] = roll_val;
        return control_array;

    }
}

