package com.example.overl.hipe.background;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.overl.hipe.R;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, WiFi_Scanner_.ScannerListener{

    private TextView tv0,tv1, tv2;
    private EditText et0, et1, et2;
    private Button bt0, bt1, bt2;

    private WiFi_Scanner_ wiFi_scanner_;

    private long key_back_down = -1;

    private int current_round = 0;

    private long start_time;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:{
                    tv1.setText("结束用时_总轮次: " + (System.currentTimeMillis() - start_time) + "_" +current_round);
                }break;
                case 1: {
                    tv0.setText("时间(ms)_次数: " + (System.currentTimeMillis() - start_time) + "_" + current_round);
                    tv1.setText("");
                }break;
                case 2:{
                    ArrayList<double []> pts = wiFi_scanner_.getLocalPoints();
                    StringBuffer stringBuffer = new StringBuffer("本地点:\r\n");
                    for(int i = 0; i < pts.size(); ++i){
                        stringBuffer = stringBuffer.append(pts.get(i)[0] + "_" + pts.get(i)[1] + "\r\n");
                    }
                    tv2.setText(stringBuffer.toString());
                }break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Context context = getApplicationContext();
        tv0 = findViewById(R.id.tv0);
        tv0.setText(" ");
        tv1 = findViewById(R.id.tv1);
        tv1.setText(" ");
        tv2 = findViewById(R.id.tv2);
        tv2.setText(" ");
        et0 = findViewById(R.id.et0);
        et1 = findViewById(R.id.et1);
        et2 = findViewById(R.id.et2);
        bt0 = findViewById(R.id.bt0);
        bt0.setText("采集");
        bt0.setOnClickListener(this);

        bt1 = findViewById(R.id.bt1);
        bt1.setText("删除");
        bt1.setOnClickListener(this);

        bt2 = findViewById(R.id.bt2);
        bt2.setText("停止");
        bt2.setOnClickListener(this);

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }
        }
        wiFi_scanner_ = new WiFi_Scanner_((WifiManager)context.getSystemService(Context.WIFI_SERVICE));
        wiFi_scanner_.setScannerListener(this);
        wiFi_scanner_.setBuildingFloor("shilintong", 1);

        Message msg = handler.obtainMessage();
        msg.what = 2;
        handler.sendMessage(msg);
    }

    @Override
    public void onScanFinished(int count){
        current_round = count;
        Message msg = handler.obtainMessage();
        msg.what = 0;
        handler.sendMessage(msg);
        msg = handler.obtainMessage();
        msg.what = 2;
        handler.sendMessage(msg);
    }

    @Override
    public void onScan(int round){
        current_round = round;
        Message msg = handler.obtainMessage();
        msg.what = 1;
        handler.sendMessage(msg);
    }

    @Override
    synchronized public void onClick(View v){
        switch (v.getId()) {
            case R.id.bt0:{
                double longitude = Double.valueOf(et0.getText().toString());
                double latitude = Double.valueOf(et1.getText().toString());
                int time_in_second = Integer.valueOf(et2.getText().toString());
                start_time = System.currentTimeMillis();
                wiFi_scanner_.startScan(longitude, latitude, time_in_second);
            }break;
            case R.id.bt1:{
                double longitude = Double.valueOf(et0.getText().toString());
                double latitude = Double.valueOf(et1.getText().toString());
                wiFi_scanner_.delete(longitude, latitude);
                Message msg = handler.obtainMessage();
                msg.what = 2;
                handler.sendMessage(msg);
            }break;
            case R.id.bt2:{
                wiFi_scanner_.stop();
            }break;
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

    }
    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:{
                long current_time = System.currentTimeMillis();
                if (current_time - key_back_down > 0 && current_time - key_back_down < 1000) {
                    Intent home = new Intent(Intent.ACTION_MAIN);
                    home.addCategory(Intent.CATEGORY_HOME);
                    startActivity(home);
                } else {
                    //Toast.makeText(this, "再次点击回到桌面", Toast.LENGTH_SHORT).show();
                    //T.show(getApplicationContext(), "再次点击回到桌面", Toast.LENGTH_SHORT, R.color.grey);
                }
                key_back_down = current_time;
            }break;
        }
        return true;
    }

}
