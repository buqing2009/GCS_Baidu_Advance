package com.lingmutec.buqing2009.gcs_baidu_advance;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Color;

/**
 * 项目的主Activity，所有的Fragment都嵌入在这里。
 *
 * @author guolin
 */
public class MainActivity extends Activity implements OnClickListener {

    /**
     * 用于展示消息的Fragment
     */
    private BaiduMapFragment baiduMapFragment;

    /**
     * 用于展示联系人的Fragment
     */
    private DronekitFragment dronekitFragment;


    /**
     * 消息界面布局
     */
    private View dronekitLayout;

    /**
     * 联系人界面布局
     */
    private View baiduMapLayout;


    /**
     * 在Tab布局上显示消息图标的控件
     */
    private ImageView dronekitImage;

    /**
     * 在Tab布局上显示联系人图标的控件
     */
    private ImageView baiduMapImage;


    /**
     * 在Tab布局上显示消息标题的控件
     */
    private TextView dronekitText;

    /**
     * 在Tab布局上显示联系人标题的控件
     */
    private TextView baiduMapText;



    /**
     * 用于对Fragment进行管理
     */
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        // 初始化布局元素
        initViews();
        fragmentManager = getFragmentManager();
        // 第一次启动时选中第0个tab
        setTabSelection(0);
    }

    /**
     * 在这里获取到每个需要用到的控件的实例，并给它们设置好必要的点击事件。
     */
    private void initViews() {
        dronekitLayout = findViewById(R.id.dronekit_layout);
        baiduMapLayout = findViewById(R.id.baidumap_layout);

        dronekitImage = (ImageView) findViewById(R.id.dronekit_image);
        baiduMapImage = (ImageView) findViewById(R.id.baidumap_image);

        dronekitText = (TextView) findViewById(R.id.dronekit_text);
        baiduMapText = (TextView) findViewById(R.id.baidumap_text);

        dronekitLayout.setOnClickListener(this);
        baiduMapLayout.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.dronekit_layout:
                // 当点击了消息tab时，选中第1个tab
                setTabSelection(0);
                break;
            case R.id.baidumap_layout:
                // 当点击了联系人tab时，选中第2个tab
                setTabSelection(1);
                break;
            default:
                break;
        }
    }

    /**
     * 根据传入的index参数来设置选中的tab页。
     *
     * @param index
     *            每个tab页对应的下标。0表示消息，1表示联系人，2表示动态，3表示设置。
     */
    private void setTabSelection(int index) {
        // 每次选中之前先清楚掉上次的选中状态
        clearSelection();
        // 开启一个Fragment事务
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        // 先隐藏掉所有的Fragment，以防止有多个Fragment显示在界面上的情况
        hideFragments(transaction);


        switch (index) {
            case 0:
                // 当点击了消息tab时，改变控件的图片和文字颜色
                dronekitImage.setImageResource(R.drawable.message_selected);
                dronekitText.setTextColor(Color.WHITE);

                if (dronekitFragment == null) {
                    // 如果MessageFragment为空，则创建一个并添加到界面上
                    dronekitFragment = new DronekitFragment();
                    transaction.add(R.id.content, dronekitFragment,"Dronekit");
                } else {
                    // 如果MessageFragment不为空，则直接将它显示出来
                    transaction.show(dronekitFragment);
                }
                break;

            case 1:
            default:
                // 当点击了联系人tab时，改变控件的图片和文字颜色
                baiduMapImage.setImageResource(R.drawable.contacts_selected);
                baiduMapText.setTextColor(Color.WHITE);
                if (baiduMapFragment == null) {
                    // 如果ContactsFragment为空，则创建一个并添加到界面上
                    baiduMapFragment = new BaiduMapFragment();
                    transaction.add(R.id.content, baiduMapFragment,"BaiduMap");
                } else {
                    // 如果ContactsFragment不为空，则直接将它显示出来
                    transaction.show(baiduMapFragment);
                }
                break;
        }
        transaction.commit();
    }

    /**
     * 清除掉所有的选中状态。
     */
    private void clearSelection() {
        dronekitImage.setImageResource(R.drawable.message_unselected);
        dronekitText.setTextColor(Color.parseColor("#82858b"));
        baiduMapImage.setImageResource(R.drawable.contacts_unselected);
        baiduMapText.setTextColor(Color.parseColor("#82858b"));
    }

    /**
     * 将所有的Fragment都置为隐藏状态。
     *
     * @param transaction
     *            用于对Fragment执行操作的事务
     */
    private void hideFragments(FragmentTransaction transaction) {
        if (dronekitFragment != null) {
            transaction.hide(dronekitFragment);
        }
        if (baiduMapFragment != null) {
            transaction.hide(baiduMapFragment);
        }

    }
}