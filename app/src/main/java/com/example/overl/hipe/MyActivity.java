package com.example.overl.hipe;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.overl.hipe.ui.MainActivity;
import com.junkchen.blelib.BleService;
import com.junkchen.blelib.MultipleBleService;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import com.example.overl.hipe.util.PersonID;


@SuppressLint("NewApi")


public class MyActivity extends BaseBleMapbox implements View.OnClickListener{

    // 调试用
    private static final String ACTIVITY_TAG="BleMapbox";

    String name_path = "";

    private Button mBt_scan;

    //其他按钮
    private Button bt_start;
    private Button bt_end;

    private Button bt_point;
    private EditText et_point;

    private Button bt_timeclick;
    private TextView tv_timeNum;

    String time_click="time:";

    //标记打点按钮
    public static int TimeButtonPush = 0;

    // 发送数据信息
    private byte []sendmessage = null;

    protected BroadcastReceiver bleReceiver = new BroadcastReceiver() {
        //这里，会先回调ble自己的onConnectionStateChange等事件，然后才广播通知
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BleService.ACTION_BLUETOOTH_DEVICE)) {
                String tmpDevName = intent.getStringExtra("name");
                String tmpDevAddress = intent.getStringExtra("address");
                HashMap<String, Object> deviceMap = new HashMap<>();
                deviceMap.put("name", tmpDevName);
                deviceMap.put("address", tmpDevAddress);
                deviceMap.put("isConnect", DISCONNECTED);
                deviceMap.put("dhhaodeata", DEFAULTDATA);
            } else if (intent.getAction().equals(BleService.ACTION_GATT_CONNECTED)) {
                mHandler.sendEmptyMessage(CONNECT_CHANGE);
                dismissDialog();
            } else if (intent.getAction().equals(BleService.ACTION_GATT_DISCONNECTED)) {
                mHandler.sendEmptyMessage(CONNECT_CHANGE);
                dismissDialog();
            } else if (intent.getAction().equals(BleService.ACTION_SCAN_FINISHED)) {
                mBt_scan.setEnabled(true);
                dismissDialog();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        // 动态权限申请
        initPermission(this);
        initView();
        initAdapter();
        registerReceiver(bleReceiver, makeIntentFilter());
        startService(new Intent(this, MultipleBleService.class));//这个地方服务不开启
        doBindService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        doUnBindService();
        mBleService.stopSelf();
        unregisterReceiver(bleReceiver);
        finish();
        System.exit(0);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    /*
     * 动态申请权限
     * */
    /**
     * android 6.0 以上需要动态申请权限
     */
    public static void initPermission(MyActivity mainActivity) {
        String permissions[] = {
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.RECORD_AUDIO,           //need
                Manifest.permission.READ_PHONE_STATE,       //need
                Manifest.permission.WRITE_SETTINGS,         //need
                Manifest.permission.WRITE_EXTERNAL_STORAGE, //need
                Manifest.permission.WRITE_SETTINGS,         //need
                Manifest.permission.ACCESS_FINE_LOCATION,   //need
                Manifest.permission.ACCESS_COARSE_LOCATION, //need
                Manifest.permission.BLUETOOTH,              //Normal
                Manifest.permission.BLUETOOTH_ADMIN,        //Normal
                Manifest.permission.ACCESS_NETWORK_STATE,   //Normal
                Manifest.permission.INTERNET,               //Normal
                Manifest.permission.WAKE_LOCK               //Normal
        };
        ArrayList<String> toApplyList = new ArrayList<String>();
        for (String perm :permissions){
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(mainActivity, perm)) {
                toApplyList.add(perm);
                //进入到这里代表没有权限.

            }
        }
        String tmpList[] = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()){
            ActivityCompat.requestPermissions(mainActivity, toApplyList.toArray(tmpList), 123);
        }
    }


    /*初始化适配器*/
    private void initAdapter() {
        mList_device.add(DEFAULT_SPINNER_DATA);
        mAdapter_device = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, mList_device);
        mSp_device.setAdapter(mAdapter_device);
    }

    /*
    绑定服务
    * */
    private void doBindService() {
        Intent serviceIntent = new Intent(this, MultipleBleService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    /*
    * 取消绑定服务
    * */
    private void doUnBindService() {
        if (mIsBind) {
            unbindService(serviceConnection);
            //mBleService = null;
            mIsBind = false;
        }
    }

    /*
    * 初始化界面相关函数
    * */
    private void initView() {

        tv1 = findViewById(R.id.tv_res1);
        tv2 = findViewById(R.id.tv_res2);

        mBt_scan = findViewById(R.id.bt_scan);
        bt_start = findViewById(R.id.bt_start);
        bt_end = findViewById(R.id.bt_end);

        bt_timeclick= findViewById(R.id.bt_time);
        bt_point= findViewById(R.id.bt_point);
        et_point= findViewById(R.id.ed_point);

        bt_start.setOnClickListener(this);
        bt_end.setOnClickListener(this);
        mBt_scan.setOnClickListener(this);

        bt_timeclick.setOnClickListener(this);
        bt_point.setOnClickListener(this);

        mTv_deviceNum = findViewById(R.id.tv_deviceNum);
        tv_timeNum= findViewById(R.id.tv_timeNum);

        mSp_device = findViewById(R.id.spinner_device);
        findViewById(R.id.bt_my_to_map).setOnClickListener(view -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            startActivity(intent);
        });
//        bt_start.setEnabled(true);
//        bt_end.setEnabled(false);

    }


    public String getFileNamebyDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        Date date = new Date();
        String str = sdf.format(date);
        return str;
    }

    /*
    * 调用ScanBluetoothDevice进行蓝牙扫描，选中后返回mac地址和设备名
    * */
    public void onClick_scan(View view) {
        Intent ble_intent = new Intent(MyActivity.this, ScanBluetoothDevice.class);
        ble_intent.putExtra("listname", (Serializable) connectnames);
        ble_intent.putExtra("listaddress", (Serializable) connectaddresses);
        startActivityForResult(ble_intent, ConstantsBluetooth.REQUEST_CONNECT_DEVICE);
    }

    /*
    点击开始IMU
    * */
    public void onClick_start_allimu(View view){
//        bt_start.setEnabled(false);
//        bt_end.setEnabled(true);

        Sys_t0 = System.currentTimeMillis();//得到系统启动的时间
        //初始化文件路径
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File localFile1 = Environment.getExternalStorageDirectory();
            folder_path = localFile1.getPath() + "/" + "Foot-PDR" + "/";
            File localFile2 = new File(folder_path);
            if (!localFile2.exists()) {
                localFile2.mkdirs();
            }
        }
        String str = getFileNamebyDate();
        folder_path = folder_path + str + "/";
        File localFile1 = new File(folder_path);
        if (!localFile1.exists()) {
            localFile1.mkdirs();
        }

        /*测试时间格式转换*/
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");//24小时制
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));

        Sys_t0_long = System.currentTimeMillis();

        // 时间设置
        Date date = new Date();
        date.setTime(Sys_t0_long);
        String calendar=simpleDateFormat.format(date);
        long mills=Sys_t0_long%1000;

        String []times=calendar.split("-");
        //得到unsignedchar 时间
        sendmessage=new byte[9];

        int index=0;
        for (int i=0;i<times.length;i++){
            if (i==0){
                int temp= Integer.parseInt(times[i]);
                int temp_up=temp/100;
                sendmessage[index]=(byte)temp_up;index++;

                int temp_down=temp%100;
                sendmessage[index]=(byte)temp_down;index++;
            }
            else{
                int temp= Integer.parseInt(times[i]);
                sendmessage[index]=(byte)temp;index++;
            }
        }
        int temp= (int)mills;
        int temp_up=temp/100;
        sendmessage[index]=(byte)temp_up;index++;
        int temp_down=temp%100;
        sendmessage[index]=(byte)temp_down;index++;

        imuNum = connectnames.size();
        for (int i = 0; i < imuNum; i++) {
            fw_imu.add(new File(folder_path + connectnames.get(imuNum-1-i) + ".txt"));
            try {
                if (!fw_imu.get(i).exists()) {
                    fw_imu.get(i).createNewFile();
                    fos_imu.add(new FileOutputStream(fw_imu.get(i)));
                    Log.e("aaa","1");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //还要重新设置下文件开关，免得每次都要重新启动程序
        fw_imupos = new File(folder_path + "imupos.txt");
        try {
            if (!fw_imupos.exists()) {
                fw_imupos.createNewFile();
            }
            fos_imupos = new FileOutputStream(fw_imupos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String fileName_landmark = folder_path + "TimeLandmarks.txt";
        fw_landmark = new File(fileName_landmark);
        try {
            fos_landmark = new FileOutputStream(fw_landmark);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        if_start_logging = 1;

        /*向每个imu发送数据*/
        String address;
        for (int i = 0; i < connectaddresses.size(); i++) {

            address = connectaddresses.get(i);
            //String address = "DA:15:08:4F:08:90";
            BluetoothGatt gatt = mBleService.getConnectedBluetoothGatt().get(address);
            BluetoothGattCharacteristic characteristic =
                    gatt.getService(UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e"))
                            .getCharacteristic(UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"));
            writeCharacteristicBytes(gatt, characteristic, sendmessage);
        }

    }

    /*记录点号
    * */
    public void onClick_record(View view){
        if ("".equals(et_point.getText().toString())) {
            return;
        }
        landmark_point = Integer.parseInt(et_point.getText().toString());
        TimeButtonPush = 1;
        if(Sys_t0!=0.0){
            double t_click = (System.currentTimeMillis() - Sys_t0) / 1000.0;
            String str_landmark_this_one = String.format("%.1f ", t_click);
            TimeButtonPush = 1;

            String str_landmark = landmark_point+" "+str_landmark_this_one + "\n";
            byte[] bytes_landmark = str_landmark.getBytes();
            try {
                fos_landmark.write(bytes_landmark);
            } catch (IOException e) {
                e.printStackTrace();
            }

            /*输入打点号*/
            byte[] bytes=new byte[2];
            bytes[0]='s';
            bytes[1]=(byte)landmark_point;

            String address;
            for (int i = 0; i < connectaddresses.size(); i++) {
                address = connectaddresses.get(i);
                //String address = "DA:15:08:4F:08:90";
                BluetoothGatt gatt = mBleService.getConnectedBluetoothGatt().get(address);
                BluetoothGattCharacteristic characteristic =
                        gatt.getService(UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e"))
                                .getCharacteristic(UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"));
                writeCharacteristicBytes(gatt, characteristic, bytes);
            }
        }
    }

    /*点击结束调用函数*/
    public void onClick_end(View view) {
//        bt_start.setEnabled(true);
//        bt_end.setEnabled(false);
//        Sys_t0_long = 0;
        if_start_logging = 0;
        fos_imu.clear();
        fw_imu.clear();
        final String deviceName = mSp_device.getSelectedItem().toString();

        for (int i = 0; i < connectaddresses.size(); i++) {
            BluetoothGatt gatt = mBleService.getConnectedBluetoothGatt().get(connectaddresses.get(i));
            BluetoothGattCharacteristic characteristic = gatt.getService(UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e"))
                    .getCharacteristic(UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"));
            String message = "255";
            writeCharacteristic(gatt, characteristic, message);
        }


    }

    /**startActivityForResult回调
     * 点击“连接”时，回调该方法进行蓝牙连接
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ConstantsBluetooth.REQUEST_CONNECT_DEVICE:
                if (resultCode == RESULT_OK) {
                    String address = data.getExtras().getString(ConstantsBluetooth.DEVICE_ADDRESS);
                    PersonID.ID_Num=Integer.parseInt(data.getExtras().getString(ConstantsBluetooth.PERSON_ID));

                    //存在则断开连接
                    for (int i = 0; i < connectaddresses.size(); i++) {
                        if (address.equals(connectaddresses.get(i))) {
                            mBleService.disconnect(address);
                            return;
                        }
                    }
                    //不存在则连接
                    mBleService.connect(address);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_scan:
                onClick_scan(v);
                break;

            case R.id.bt_start:
                onClick_start_allimu(v);
                break;
            case R.id.bt_end:
                onClick_end(v);
                break;

            case R.id.bt_time:
                onClick_time(v);
                break;

            case R.id.bt_point:
                onClick_record(v);
                break;
        }
    }

    /*
    * 查看时间点
    * */
    private void onClick_time(View v){
        if (if_start_logging == 1) {
            long timeMill = System.currentTimeMillis();
            double t = (double) (timeMill - Sys_t0_long);
            time_click = time_click + "," + t / 1000;
            tv_timeNum.setText(time_click);
        }
    }
}
