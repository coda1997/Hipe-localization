package com.example.overl.hipe;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ScanBluetoothDevice extends Activity {

	public static final String DEFAULT_SPINNER_DATA = "请选择编号";
	protected List<String> mList_device = new ArrayList<>();//选择编号的下拉框
	protected ArrayAdapter mAdapter_device;//发送选择列表的下拉框
	protected Spinner mSp_device;//编号选择

	private ListView lvNewDevices, lvPairedDevices;
	private ArrayAdapter<String> adapterNewDevices, adapterPairedDevice;
	ArrayList<String> alldevices;
	private BluetoothAdapter btAdapter;
	private Button btnScan;
	private Button btnConect;
	private String address;

	View lastView = null;

	List<String> connectnames=new ArrayList<>();//连接设备的名字
	List<String> connectaddresses=new ArrayList<>();//连接设备的名字

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bluetooth_list);

		//得到传递进来的已经连接的蓝牙设备名字和地址
		Intent intent=getIntent();
		connectnames=(List<String>) intent.getSerializableExtra("listname");
		connectaddresses=(List<String>) intent.getSerializableExtra("listaddress");

		findAllViews();

		initAdapter();//初始化适配器

		btAdapter = BluetoothAdapter.getDefaultAdapter();
		if (btAdapter == null) {
			finish(); return;
		}
		alldevices = new ArrayList<String>();

		//已经配对的传递进来
		for(int i=0;i<connectaddresses.size();i++){
			adapterPairedDevice.add(connectnames.get(i) + "\n" + connectaddresses.get(i));
			alldevices.add(connectnames.get(i) + "\n" + connectaddresses.get(i));
		}

		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mReceiver, filter);
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(mReceiver, filter);
	}

	/*初始化适配器*/
	private void initAdapter() {
		mList_device.add(DEFAULT_SPINNER_DATA);
		mList_device.add("1");
		mList_device.add("2");
		mAdapter_device = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, mList_device);
		mSp_device.setAdapter(mAdapter_device);
	}


	private void findAllViews() {
		btnScan = (Button) findViewById(R.id.btnScan);
		btnConect = (Button) findViewById(R.id.btnConect);

		mSp_device = (Spinner) findViewById(R.id.ble_spinner_device);
//		mSp_para=(Spinner)findViewById(R.id.para_type) ;

		lvNewDevices = (ListView) findViewById(R.id.lvNewDevices);
		adapterNewDevices = new ArrayAdapter<String>(ScanBluetoothDevice.this, android.R.layout.simple_list_item_1);
		lvNewDevices.setAdapter(adapterNewDevices);
		lvNewDevices.setOnItemClickListener(DeviceListClickListener);

		lvPairedDevices = (ListView) findViewById(R.id.lvPairedDevices);
		adapterPairedDevice = new ArrayAdapter<String>(ScanBluetoothDevice.this, android.R.layout.simple_list_item_1);
		lvPairedDevices.setAdapter(adapterPairedDevice);
		lvPairedDevices.setOnItemClickListener(DeviceListClickListener);

		btnScan.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				discoverDevices();
			}
		});

		btnConect.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				String deviceName = mSp_device.getSelectedItem().toString();

//				String para_type=mSp_para.getSelectedItem().toString();

				if (deviceName.equals("1")||deviceName.equals("2")){

					Intent connect_intent = new Intent();
					connect_intent.putExtra(ConstantsBluetooth.DEVICE_ADDRESS, address);
					//传出标号
					connect_intent.putExtra(ConstantsBluetooth.PERSON_ID,deviceName);
					setResult(RESULT_OK, connect_intent);
					finish(); // 关闭页面，跳转到定位模式选择页面

				}else{
					Toast.makeText(getApplicationContext(), "number error!", Toast.LENGTH_SHORT).show();
				}

			}
		});
	}

	/**
	 * 扫描蓝牙设备
	 */
	private void discoverDevices() {
		btnScan.setEnabled(false);
		setTitle("扫描中...");
		if (btAdapter.isDiscovering()) {
			btAdapter.cancelDiscovery();
		}
		btAdapter.startDiscovery();
	}


	private OnItemClickListener DeviceListClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> arg0, View v, int arg2, long arg3) {
			// 取消扫描
			btAdapter.cancelDiscovery();

			// 获取蓝牙地址
			String vString = ((TextView) v).getText().toString();
			address = vString.substring(vString.length()-17);

			// 更改背景颜色
			v.setBackgroundColor(Color.parseColor("#6188CA"));
			if(lastView!=null){
				lastView.setBackgroundColor(Color.TRANSPARENT);
			}
			lastView = v;
		}
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (btAdapter != null) {
			btAdapter.cancelDiscovery();
		}
		unregisterReceiver(mReceiver);
	};

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				for (String alldevice : alldevices) {
					if (alldevice.equals(device.getName() + "\n" + device.getAddress())) {
						return;
					}
				}
				alldevices.add(device.getName() + "\n" + device.getAddress());
				adapterNewDevices.add(device.getName() + "\n" + device.getAddress());
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				btnScan.setEnabled(true);
				setTitle("选择设备");
			}
		}
	};
}
