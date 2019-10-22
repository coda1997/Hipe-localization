package com.example.overl.hipe;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.junkchen.blelib.BleService;
import com.junkchen.blelib.MultipleBleService;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import com.example.overl.hipe.tools.BleData;
import com.example.overl.hipe.tools.BleXYZF;
import com.example.overl.hipe.tools.Permissions;

import static com.example.overl.hipe.tools.MathUtil.isEqualFloat;


public class BaseBleMapbox extends AppCompatActivity {

    //相关设置参数
    int M_Epochs = 10; //当前缓存的实时imu信息

    public static final int REPAINT = 10;//更新位置标志
    DecimalFormat df = new DecimalFormat("######0.00");
    DecimalFormat df_file = new DecimalFormat("######0.000");
    DecimalFormat dfblh = new DecimalFormat("######0.000000");
    //相关设置参数

    //Debugging
    protected static final String TAG = BaseBleMapbox.class.getSimpleName();

    //Constant
    public static final int SERVICE_BIND = 1;
    public static final int CONNECT_CHANGE = 2;
    public static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;
    public static final String DISCONNECTED = "未连接";
    public static final String CONNECTED = "已连接";
    public static final String DEFAULTDATA = "No Data";
    public static final String DEFAULT_SPINNER_DATA = "请选择";

    //服务是否绑定
    protected boolean mIsBind;

    //多连接相关
    protected List<BluetoothGattService> gattServiceList;
    protected List<String> serviceList = new ArrayList<>();
    protected List<String[]> characteristicList = new ArrayList<>();

    protected List<String> connectnames = new ArrayList<>();//连接设备的名字
    protected List<String> connectaddresses = new ArrayList<>();//连接设备的名字

    protected MultipleBleService mBleService;

    //控件
    protected Spinner mSp_device;
    protected ArrayAdapter mAdapter_device;//发送选择列表的下拉框
    protected TextView mTv_deviceNum;
    protected List<String> mList_device = new ArrayList<>();//选择发送的下拉框
    //结果显示tv
    TextView tv1 = null;
    TextView tv2 = null;
    // 结果记录
    static int if_start_logging = 0;
    static String folder_path = "";
    static long Sys_t0_long = 0;
    static int landmark_point = 0;
    protected List<FileOutputStream> fos_imu = new ArrayList<>();
    protected List<File> fw_imu = new ArrayList<>();
    int imuNum = 0;

    public FileOutputStream fos_imupos = null;
    public File fw_imupos = null;

    public FileOutputStream fos_landmark;
    public File fw_landmark;

    //数据缓冲
    Vector<Vector<BleData>> vDevices = new Vector<>();//用于存放不同的ble的接收的数据，目前只存在1.0位置信息
    double Sys_t0;

    //日期格式
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /*
    * 连接相关服务
    * */
    protected ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBleService = ((MultipleBleService.LocalBinder) service).getService();
            mIsBind = true;
            if (mBleService != null) mHandler.sendEmptyMessage(SERVICE_BIND);
            if (mBleService.initialize()) {
                if (mBleService.enableBluetooth(true)) {
                    Permissions permissions=new Permissions();
                    permissions.handleVersionPermission(TAG,BaseBleMapbox.this,BaseBleMapbox.this);
                    Toast.makeText(BaseBleMapbox.this, "Bluetooth was opened", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(BaseBleMapbox.this, "not support Bluetooth", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            //mBleService = null;
            mIsBind = false;
        }
    };

    protected Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SERVICE_BIND:
                    setBleServiceListener();
                    break;
                case CONNECT_CHANGE:
                    //deviceAdapter.notifyDataSetChanged();
                    mAdapter_device.notifyDataSetChanged();
                    mTv_deviceNum.setText(getString(R.string.dev_conn_number) +
                            mBleService.getConnectedBluetoothGatt().size());
                    Log.i(TAG, "handleMessage: " + mBleService.getConnectDevices().toString());
                    break;
                case REPAINT:

                    repaint();

                    break;

                default:
                    break;
            }
        }
    };


    /*
    * 设置ble服务监听
    * */
    private void setBleServiceListener() {
        mBleService.setOnServicesDiscoveredListener((gatt, status) -> {
            BluetoothGattCharacteristic UartCharacteristicsReceive = null;

            if (status == BluetoothGatt.GATT_SUCCESS) {
                gattServiceList = gatt.getServices();
                serviceList.clear();
                for (BluetoothGattService service :
                        gattServiceList) {
                    String serviceUuid = service.getUuid().toString();
                    serviceList.add(MyGattAttributes.lookup(serviceUuid, "Unknown") + "\n" + serviceUuid);
                    Log.i(TAG, "service=" + MyGattAttributes.lookup(serviceUuid, "Unknown") + serviceUuid);

                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                    String[] charArra = new String[characteristics.size()];
                    for (int i = 0; i < characteristics.size(); i++) {
                        String charUuid = characteristics.get(i).getUuid().toString();
                        charArra[i] = MyGattAttributes.lookup(charUuid, "Unknown") + "\n" + charUuid;
                        Log.i(TAG, "characteristics=" + MyGattAttributes.lookup(charUuid, "Unknown") + charUuid);

                        ///////////////////////////////////////////////////获取串口接收 characteristics
                        if (charUuid.toUpperCase().equals(MyGattAttributes.CHARACTERISTIC_UART_RECEIVE)) {
                            if (UartCharacteristicsReceive == null || UartCharacteristicsReceive.getDescriptors().size() == 0) {
                                UartCharacteristicsReceive = characteristics.get(i);
                            }
                        }
                        ///////////////////////////////////////////

                        BluetoothGattCharacteristic characteristic = characteristics.get(i);
                        List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
                        for (BluetoothGattDescriptor descriptor : descriptors) {
                            String descriptorUuid = descriptor.getUuid().toString();
                            Log.i(TAG, "descriptor=" + MyGattAttributes.lookup(descriptorUuid, "Unknown") + descriptorUuid);
                        }
                    }
                    characteristicList.add(charArra);
                }
                if (UartCharacteristicsReceive != null) {
                    setCharacteristicNotification(gatt, UartCharacteristicsReceive, true);
                }
            }
        });

        //Callback indicating when GATT client has connected/disconnected to/from a remoteGATT server.
        mBleService.setOnConnectListener((gatt, status, newState) -> {
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                for (int i = 0; i < connectaddresses.size(); i++) {
                    if (connectaddresses.get(i).equals(gatt.getDevice().getAddress())) {
                        //移除记录的数据
                        connectaddresses.remove(i);
                        connectnames.remove(i);
                    }
                }
                String deviceName = gatt.getDevice().getName();
                mList_device.remove(deviceName);

            } else if (newState == BluetoothProfile.STATE_CONNECTING) {

            } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();

                //不存在则加入
                if (!(connectaddresses.contains(gatt.getDevice().getAddress()))) {
                    connectaddresses.add(gatt.getDevice().getAddress());
                    connectnames.add(gatt.getDevice().getName());
                }
                if (!mList_device.contains(gatt.getDevice().getName())) {
                    mList_device.add(gatt.getDevice().getName());
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {

            }
        });

        mBleService.setOnDataAvailableListener(new MultipleBleService.OnDataAvailableListener() {

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            }
            //读数据函数，估计是另一端通知他
            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
//                Log.e("数据传入手机","111");
                String strblenaem = gatt.getDevice().toString();//最好用地址表达
                characteristic.getFloatValue(50, 0);
                characteristic.getFloatValue(50, 1);
                characteristic.getFloatValue(50, 2);
                byte[] arrayBytes = characteristic.getValue();
                ByteArrayInputStream localByteArrayInputStream = new ByteArrayInputStream(arrayBytes);
                DataInputStream localDataInputStream = new DataInputStream(localByteArrayInputStream);

                BleData localbledata = new BleData();
                double c_t = (System.currentTimeMillis() - Sys_t0) / 1000.0;

                float x, y, z, t;
                int i;
                try {
                    float f = localDataInputStream.readFloat();
                    if ((isEqualFloat(f, 1.0f)) || (isEqualFloat(f, 2.0f)) || (isEqualFloat(f, 3.0f)) || (isEqualFloat(f, 4.0f)) || (isEqualFloat(f, 5.0f))) {
                        x = localDataInputStream.readFloat();
                        y = localDataInputStream.readFloat();
                        z = localDataInputStream.readFloat();
                        t=  localDataInputStream.readFloat();

                        localbledata.blename = strblenaem;
                        localbledata.x = x;
                        localbledata.y = y;
                        localbledata.z = z;
                        localbledata.t = t;

                        //结果写入文件
                        Object[] arrayOfObject = new Object[6];
                        arrayOfObject[0] = c_t;
                        arrayOfObject[1] = localbledata.t;
                        arrayOfObject[2] = localbledata.x;
                        arrayOfObject[3] = localbledata.y;
                        arrayOfObject[4] = localbledata.z;
                        arrayOfObject[5] = Float.valueOf("0.0");
                        if(MyActivity.TimeButtonPush == 1) // 用于标记何时按下了按键
                        {
                            MyActivity.TimeButtonPush = 0;
                            arrayOfObject[5] = Float.valueOf("1.0");
                        }
                        byte[] arrayOfByte2 = String.format("%.3f,%.3f,%.3f,%.3f,%.3f,%.3f\n", arrayOfObject).getBytes();

                        if (isEqualFloat(f, 1.0f)){
                            try {
                                fos_imupos.write(arrayOfByte2);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        //存储位置信息
                        if (isEqualFloat(f, 1.0f)) {

                            //看是否存在
                            boolean ifHava = false;
                            for (int kk = 0; kk < vDevices.size(); kk++) {
                                if (vDevices.get(kk).get(0).blename.equals(strblenaem)) {
                                    ifHava = true;
                                    break;
                                }
                            }

                            if (ifHava) {//存在在相应的ble后面添加
                                for (int kk = 0; kk < vDevices.size(); kk++) {
                                    if (vDevices.get(kk).get(0).blename.equals(strblenaem)) {
                                        vDevices.get(kk).add(localbledata);

                                    }
                                }
                            } else {//不存在则新添加一个ble

                                Vector vBle = new Vector();
                                vBle.add(localbledata);
                                vDevices.add(vBle);
                            }

                            //当数组的维度大于10的时候，清楚最后一个元素
                            for (int kk = 0; kk < vDevices.size(); kk++) {
                                if (vDevices.get(kk).size() > M_Epochs) {
                                    vDevices.get(kk).remove(0);
                                }
                            }
                            //重绘轨迹
                            mHandler.sendEmptyMessage(REPAINT);

                        }

                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

                mHandler.post(() -> {
                    //deviceAdapter.notifyDataSetChanged();
                    //Toast.makeText(MyActivity.this,b, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

            }
        });

    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public boolean setCharacteristicNotification(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic characteristic,
                                                 boolean enabled) {
        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        if (enabled && MyGattAttributes.CHARACTERISTIC_UART_RECEIVE.equals(characteristic.getUuid().toString().toUpperCase())) {
            Log.i(TAG, "setCharacteristicNotification");
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));

            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            boolean result = bluetoothGatt.writeDescriptor(descriptor);
        }
        return true;
    }

    /*
 * 在textview上显示加画图，同时重绘轨迹
 * */
    public void repaint() {
        if (if_start_logging==1) {
            for (int kk = 0; kk < vDevices.size(); kk++) {
                if (vDevices.get(kk).size() >= 1) {
                    BleData ble1 = vDevices.get(kk).get(vDevices.get(kk).size() - 1);
                    BleXYZF ble2 = new BleXYZF();
                    ble2.floor = 0; // 楼层
                    ble2.x = ble1.x;
                    ble2.y = ble1.y;
                    ble2.z = ble1.z;

                    coords[0] = ble1.x;
                    coords[1] = ble1.y;
                    coords[2] = ble1.z;


                    // 结果写入文件
                    long timeMill = System.currentTimeMillis();
                    double timeMill_double = System.currentTimeMillis();
                    double c_t = (double) (timeMill - Sys_t0_long) / 1000;
                    String deviceName = null;
                    for (int j = 0; j < connectaddresses.size(); j++) {
                        if (connectaddresses.get(j).equals(ble1.blename)) {
                            deviceName = connectnames.get(j);
                            break;
                        }
                    }

                    String arrayString =  this.df_file.format(timeMill_double/1000) + "," + c_t + "," + this.df_file.format(ble1.x)
                            + "," + this.df_file.format(ble1.y) + "," + this.df_file.format(ble1.z) + "," + landmark_point + "\n";
                    byte[] arrayOfByte2 = arrayString.getBytes();
                    try {
                        fos_imu.get(kk).write(arrayOfByte2);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // 由于只设置了两个位置显示，也一般只有两个
                    if (kk == 0) {
                        tv1.setText("x=" + this.df.format(ble1.x) + "m y=" + this.df.format(ble1.y) + "m z=" + this.df.format(ble1.z) + "m");
                    }
                    if (kk == 1) {
                        tv2.setText("x=" + this.df.format(ble1.x) + "m y=" + this.df.format(ble1.y) + "m z=" + this.df.format(ble1.z) + "m");
                    }
                    if (kk == vDevices.size()-1){
                        landmark_point = 0;
                    }
                }
            }
        }
    }


    /**
     * Show dialog
     */
    protected ProgressDialog progressDialog;

    protected void showDialog(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    protected void dismissDialog() {
        if (progressDialog == null) return;
        progressDialog.dismiss();
        progressDialog = null;
    }

    /*
    * 事件过滤
    * */
    protected static IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_BLUETOOTH_DEVICE);
        intentFilter.addAction(BleService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleService.ACTION_SCAN_FINISHED);
        return intentFilter;
    }

    /*
    * 向ble外设写
    * */
    public void writeCharacteristic(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic characteristic,
                                    String data) {
        if (data.equals("230")) {
            byte[] Writebytes = {2, 3, 0};
            characteristic.setValue(Writebytes);
            bluetoothGatt.writeCharacteristic(characteristic);
        } else if (data.equals("255")) {
            byte[] Writebytes = {127};
            characteristic.setValue(Writebytes);
            bluetoothGatt.writeCharacteristic(characteristic);
        }
    }

    /*
   * 向ble外设写,参数是bytes
   * */
    public void writeCharacteristicBytes(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic characteristic,
                                    byte[] data) {
        characteristic.setValue(data);
        bluetoothGatt.writeCharacteristic(characteristic);


    }
    public static float[] coords = new float[3];

    private int keyBackCount = 0;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode==KeyEvent.KEYCODE_BACK&&event.getRepeatCount()==0){
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
}
