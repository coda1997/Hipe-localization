package com.example.overl.hipe.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by CXG on 2017/10/23.
 *
 * 权限相关函数
 */

public class Permissions {

    private final int CODE_PERMISSIONS = 0;
    public static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;
    /*
    * 动态申请权限
    *
    * 传入activity 和context
    * */
    public void requestPermissions(Context context, Activity activity) {
        //得到一些权限
        String[] neededPermissions = {                                                              //权限获取
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CONTROL_LOCATION_UPDATES,
                Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
        };
        ActivityCompat.requestPermissions(activity, neededPermissions, CODE_PERMISSIONS);

        for (int i = 0; i < neededPermissions.length; i++) {
            if (ContextCompat.checkSelfPermission(context, neededPermissions[i])
                    != PackageManager.PERMISSION_GRANTED) {
                //申请WRITE_EXTERNAL_STORAGE权限
                ActivityCompat.requestPermissions(activity, new String[]{neededPermissions[i]},
                        CODE_PERMISSIONS);
            }
        }

    }

    public void handleVersionPermission(String TAG,Context context, Activity activity) {

        if (Build.VERSION.SDK_INT >= 23) {
            Log.i(TAG, "onCreate: checkSelfPermission");
            //检查是否具备某个权限
            int checkSelfPermission = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_COARSE_LOCATION);
            Log.i(TAG, "handleVersionPermission: checkSelfPermission = " + checkSelfPermission);
            if (checkSelfPermission != PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "onCreate: Android 6.0 动态申请权限");

                boolean showRequestPermissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity,
                        Manifest.permission.ACCESS_COARSE_LOCATION);
                Log.i(TAG, "handleVersionPermission: showRequestPermissionRationale = " + showRequestPermissionRationale);
                if (showRequestPermissionRationale) {
                    Log.i(TAG, "*********onCreate: shouldShowRequestPermissionRationale**********");
                    Toast.makeText(activity, "只有允许访问位置才能搜索到蓝牙设备", Toast.LENGTH_SHORT).show();
                }
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_CODE_ACCESS_COARSE_LOCATION);
            } else {
                Log.i(TAG, "handleVersionPermission: scanning...");
                //showDialog(getResources().getString(R.string.scanning));
                //mBleService.scanLeDevice(true);
            }
        } else {
            Log.i(TAG, "handleVersionPermission: scanning...");
            //showDialog(getResources().getString(R.string.scanning));
            //mBleService.scanLeDevice(true);
        }
    }
}
