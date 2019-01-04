package com.example.overl.hipe.background;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;

import com.example.overl.hipe.BlueToothScanRes;
import com.example.overl.hipe.OriginalRes;
import com.example.overl.hipe.Point;
import com.example.overl.hipe.WifiScanRes;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

//import android.util.Log;

/**
 * Created by zhang on 2018/3/27.
 */

public class WiFi_Scanner_ {

    class MyRSS implements Comparable {
        public int rss;
        String mac;
        String name;
        public long timestamp;

        public MyRSS(String mac, int rss, long timestamp) {
            this.rss = rss;
            this.mac = mac;
            this.timestamp = timestamp;
            this.name = "";
        }

        public MyRSS(String name, String mac, int rss, long timestamp) {
            this.name = name;
            this.rss = rss;
            this.mac = mac;
            this.timestamp = timestamp;
        }

        @Override
        public int compareTo(Object object) {
            MyRSS myRSS = (MyRSS) object;
            if (this.rss > myRSS.rss)
                return -1;
            else if (this.rss < myRSS.rss)
                return 1;
            else return 0;
        }

        @Override
        public boolean equals(Object ob){
            if(ob instanceof MyRSS){
                if(ob == null)
                    return false;
                return ((MyRSS) ob).mac.equals(this.mac);
            }
            return false;
        }
    }
    private final int SIGNAL_TYPE_WIFI = 1;
    private final int SIGNAL_TYPE_IBEACON = 2;

    static DecimalFormat df = new DecimalFormat("0.000000");
    final java.text.SimpleDateFormat sDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    private String building_name;
    private int floor;

    private int count = 0;

    private String wifi_path = "AAAAA/Wi-Fi_Data/";
    private String ibeacon_path = "AAAAA/iBeacon_Data/";

    private final double max_longitude_delta = 10;
    private final double max_latitude_delta = 10;

    private double longitude, latitude;

    private ArrayList<PointA> pts = new ArrayList<>();
    private ArrayList<Long> pts_timestamp = new ArrayList<>();

    private boolean wifi_scanning = false, bt_scanning = false;
    private long max_time_in_second = 0;
    private long start_time = 0;

    private long time_bt_threshold = 500000000;
    private long last_bt_timestamp = -1;
    private long last_bt_timestamp_nano = -1;

    private Point point_output;

    private ScannerListener scannerListener;


    public WifiManager wifiManager;
    BluetoothLeScanner bTScanner;

    List<List<MyRSS>> wifi_list_list, bt_list_list;
    List<MyRSS> bt_list;
    private final int initial_list_num = 200;
    private LinkedList<Long> time_list_w = new LinkedList<>();
    private LinkedList<Long> time_list_b = new LinkedList<>();



    private class PointA {
        double longitude, latitude;
        String name;
        String device;

        public PointA(double longitude, double latitude, String device) {
            this.latitude = latitude;
            this.longitude = longitude;
            name = df.format(latitude) + "_" + df.format(longitude);
            this.device = device;
        }

        public PointA(double longitude, double latitude, String name, String device) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.name = name;
            this.device = device;
        }
    }

    public interface ScannerListener {
        void onScanFinished(Point point);

        void onScan(ArrayList<OriginalRes> list);
    }

    public void setScannerListener(ScannerListener scannerListener) {
        this.scannerListener = scannerListener;
    }

    public WiFi_Scanner_(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            BluetoothManager bm = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter bTAdatper;
            if (wifiManager != null) {
                if (!wifiManager.isWifiEnabled()) {
                    wifiManager.setWifiEnabled(true);
                }
            } else throw new Exception("Wifi Exception");

            if(bm != null) {
                if (!(bTAdatper = bm.getAdapter()).isEnabled()) {
                    bTAdatper.enable();
                }
            }else{
                throw new Exception("Bluetooth Exception");
            }

            while (!wifiManager.isWifiEnabled() || !bTAdatper.isEnabled());

            this.wifiManager = wifiManager;
            this.bTScanner = bTAdatper.getBluetoothLeScanner();

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("WiFi_Scanner", "Create Failed");
        }
    }

    private int loadPoints() {
        pts.clear();
        pts_timestamp.clear();
        File wifi_dir = new File(Environment.getExternalStorageDirectory(), wifi_path);
        File building_dir = new File(wifi_dir, building_name);
        File floor_dir = new File(building_dir, "floor_" + floor);
        if (floor_dir.exists()) {
            File[] files = floor_dir.listFiles();
            int file_count = files.length;
            for (int i = 0; i < file_count; ++i) {
                String fname = files[i].getName();
                String[] strs = fname.split("_");
                if (strs.length != 4)
                    continue;
                double current_longitude, current_latitude;
                current_longitude = Double.valueOf(strs[0]);
                current_latitude = Double.valueOf(strs[1]);
                long point_timestamp = Long.valueOf(strs[2]);
                String model = strs[3].substring(0, strs[3].length() - 4);
                pts_timestamp.add(point_timestamp);
                pts.add(new PointA(current_longitude, current_latitude, strs[0] + "_" + strs[1], model));
                Log.e("1"+pts.size(), ""+pts_timestamp.size());
            }
        }
        return pts.size();
    }

    public boolean setBuildingFloor(String building_name, int floor) {
        this.building_name = building_name;
        this.floor = floor;
        return (loadPoints() > 0);
    }

    public boolean startScan(double longitude, double latitude, int timeInSecond) {
        if (!wifi_scanning && !bt_scanning) {
            start_time = System.currentTimeMillis();
            wifi_scanning = true;
            bt_scanning = true;
            max_time_in_second = timeInSecond;
            this.longitude = longitude;
            this.latitude = latitude;
            point_output = new Point(System.currentTimeMillis(), latitude, longitude, floor, building_name, new ArrayList<>(), new ArrayList<>(), android.os.Build.MODEL);
            start();
            return true;
        }
        return false;
    }

    public boolean savePointInLocalStorage(Point point){
        double longitude = point.getLongitude();
        double latitude = point.getLatitude();
        String device_model = point.getDeviceModel();
        Log.e("down point", df.format(longitude) + " ::: " + df.format(latitude) + "_" + point.getId());
        int floor = point.getFloor();
        String path = wifi_path;
        File wifi_dir = new File(Environment.getExternalStorageDirectory(), path);
        File building_dir = new File(wifi_dir, building_name);
        File floor_dir = new File(building_dir, "floor_" + floor);
        if (!floor_dir.exists())
            if (!floor_dir.mkdirs())
                return false;
        if (!floor_dir.isDirectory())
            return false;
        pts.add(new PointA(longitude, latitude, device_model));
        pts_timestamp.add(point.getId());
        Log.e("2"+pts.size(), ""+pts_timestamp.size());
        String fname = df.format(longitude) + "_" + df.format(latitude) + "_" + point.getId() + "_" + android.os.Build.MODEL + ".csv";
        String fname_temp = "TEMP_" + fname;
        File file_temp = new File(floor_dir, fname_temp);
        File file = new File(floor_dir, fname);

        if (file.exists())
            file.delete();
        if (file_temp.exists())
            file_temp.delete();

        try {
            if (!file_temp.createNewFile())
                return false;
            List<List<MyRSS>> list_list = new ArrayList<>();
            ArrayList<WifiScanRes> scan_list_list = (ArrayList<WifiScanRes>) point.getWifiScanRes();
            ArrayList<Long> time_list = new ArrayList<>();
            long current_ttt = point.getId();
            for(int i = 0; i < scan_list_list.size(); ++i){
                ArrayList<OriginalRes> ori_list = (ArrayList<OriginalRes>) scan_list_list.get(i).getRess();
                ArrayList<MyRSS> list = new ArrayList<>();
                for(int j = 0; j < ori_list.size(); ++j)
                    list.add(new MyRSS("", ori_list.get(j).getSsid(), ori_list.get(j).getLevel(), 0));
                list_list.add(list);
                time_list.add(new Long(current_ttt));
                current_ttt += 3000;
            }
            write_file(file_temp, list_list, time_list);
            if (!file_temp.renameTo(file))
                return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    public boolean delete(double longitude, double latitude) {
        int length = pts.size();
        double min_delta2 = 1000000;
        int closest = -1;
        for (int i = 0; i < length; ++i) {
            PointA pt = pts.get(i);

            double d0 = 1000000 * Math.abs(longitude - pt.longitude);
            double d1 = 1000000 * Math.abs(latitude - pt.latitude);
            if (d0 < max_longitude_delta && d1 < max_latitude_delta) {
                double d2 = d0 * d0 + d1 * d1;
                if (d2 < min_delta2) {
                    min_delta2 = d2;
                    closest = i;
                }
            }
        }
        Log.e("CLO", "" + closest);
        if (closest >= 0) {
            Log.e("CLOSEST", df.format(pts.get(closest).longitude) + "_" + df.format(pts.get(closest).latitude) + "_" + pts_timestamp.get(closest));
            delete_file(pts.get(closest).longitude, pts.get(closest).latitude, pts_timestamp.get(closest), SIGNAL_TYPE_WIFI);
            delete_file(pts.get(closest).longitude, pts.get(closest).latitude, pts_timestamp.get(closest), SIGNAL_TYPE_IBEACON);
            pts.remove(closest);
            pts_timestamp.remove(closest);
            return true;
        }
        return false;
    }

    public ArrayList<Point> getLocalPoints() {
        int length = pts.size();
        /*
        ArrayList<double[]> result = new ArrayList<>(length);
        for (int i = 0; i < length; ++i) {
            result.add(new double[]{pts.get(i).longitude, pts.get(i).latitude});
        }
        */
        ArrayList<Point> result = new ArrayList<>(length);
        for(int i = 0; i < length; ++i){
            PointA pta = pts.get(i);
            result.add(new Point(pts_timestamp.get(i), pta.latitude, pta.longitude, floor, building_name, new ArrayList<>(), new ArrayList<>(), pta.device));
        }
        return result;
    }

    synchronized private boolean start() {
        synchronized ("0") {
            if (wifiManager != null) {

                Thread wifi_thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        long current_time;
                        int round = 0;
                        boolean fisrt_scan = true;
                        wifi_list_list = new ArrayList<>(initial_list_num);
                        time_list_w = new LinkedList<>();
                        //scannerListener.onScan(0);
                        ArrayList<WifiScanRes> wifiScans = (ArrayList<WifiScanRes>) point_output.getWifiScanRes();
                        while (wifi_scanning && ((current_time = System.currentTimeMillis()) - start_time) + 3010 < max_time_in_second * 1000) {
                            try {
                                wifiManager.startScan();
                                Thread.sleep(3000);
                                if(fisrt_scan){
                                    fisrt_scan = false;
                                    continue;
                                }
                                ++round;
                                ArrayList<ScanResult> scanResults = (ArrayList<ScanResult>) wifiManager.getScanResults();
                                Log.e("Wi-Fi List", "" + scanResults.size());

                                time_list_w.add(System.currentTimeMillis());
                                int AP_n = scanResults.size();
                                List<MyRSS> wifi_list = new ArrayList<>(AP_n);
                                ArrayList<OriginalRes> ori_list = new ArrayList<>();
                                Log.e("scanResult Size", ""+AP_n);
                                for (int i = 0; i < AP_n; i++) {
                                    ScanResult scanResult = scanResults.get(i);
                                    wifi_list.add(new MyRSS(scanResult.SSID, scanResult.BSSID, scanResult.level, scanResult.timestamp));
                                    //Log.e("scanResult", scanResult.BSSID + ": " + scanResult.level);
                                    ori_list.add(new OriginalRes(scanResult.BSSID, scanResult.level));
                                }
                                Collections.sort(wifi_list);
                                wifi_list_list.add(wifi_list);

                                WifiScanRes currentWifiScanRes = new WifiScanRes(sDateFormat.format(current_time), ori_list);
                                wifiScans.add(currentWifiScanRes);

                                scannerListener.onScan(ori_list);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        while (wifi_scanning && ((current_time = System.currentTimeMillis()) - start_time) < max_time_in_second * 1000) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        wifi_scanning = false;
                    }
                });

                Thread ibeacon_thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        long current_time;
                        bt_list_list = new ArrayList<>(initial_list_num);
                        time_list_b = new LinkedList<>();
                        bTScanner.startScan(mLeScanCallback);
                        try {
                            while (bt_scanning  && ((current_time = System.currentTimeMillis()) - start_time) < max_time_in_second * 1000) {
                                Thread.sleep(500);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        bTScanner.stopScan(mLeScanCallback);
                        bt_scanning = false;
                    }
                });

                Thread writer_thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            while (bt_scanning || wifi_scanning || wifi_thread.isAlive() || ibeacon_thread.isAlive()) {
                                Thread.sleep(500);
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        boolean r0 = record(longitude, latitude, point_output.getId(), SIGNAL_TYPE_WIFI);
                        //boolean r1 = record(longitude, latitude, point_output.getId(), SIGNAL_TYPE_IBEACON);
                        if(r0) {
                            pts.add(new PointA(longitude, latitude, android.os.Build.MODEL));
                            pts_timestamp.add(point_output.getId());
                            //Log.e("record"+pts.size(), ""+pts_timestamp.size());
                            Log.e("point_out", df.format(point_output.getLongitude()) + "_" + df.format(point_output.getLatitude()) + "_" + point_output.getId());
                            scannerListener.onScanFinished(point_output);
                            for(int i = 0; i < pts_timestamp.size(); i++){
                                //Log.e(""+pts.size(), ""+pts_timestamp.size());
                                //Log.e("point "+i, df.format(pts.get(i).longitude) + "_" + df.format(pts.get(i).latitude) +"_" + pts_timestamp.get(i));
                            }
                        }
                        else
                            scannerListener.onScanFinished(null);
                        clear_temp_data();
                    }
                });

                wifi_thread.start();
                ibeacon_thread.start();
                writer_thread.start();


                return true;
            } else return false;
        }
    }

    synchronized public void stop() {
        wifi_scanning = false;
        bt_scanning = false;
    }

    private boolean record(double longitude, double latitude, long timestamp, int signal_type) {
        List<List<MyRSS>> list_list;
        LinkedList<Long> time_list;
        String path;

        if(signal_type == SIGNAL_TYPE_WIFI){
            list_list = wifi_list_list;
            time_list = time_list_w;
            path = wifi_path;
        }
        else if(signal_type == SIGNAL_TYPE_IBEACON){
            list_list = bt_list_list;
            time_list = time_list_b;
            path = ibeacon_path;
        }
        else
            return false;

        File wifi_dir = new File(Environment.getExternalStorageDirectory(), path);
        File building_dir = new File(wifi_dir, building_name);
        File floor_dir = new File(building_dir, "floor_" + floor);
        if (!floor_dir.exists())
            if (!floor_dir.mkdirs())
                return false;
        if (!floor_dir.isDirectory())
            return false;
        String fname = df.format(longitude) + "_" + df.format(latitude) + "_" + timestamp + "_" + android.os.Build.MODEL + ".csv";
        String fname_temp = "TEMP_" + fname;
        File file_temp = new File(floor_dir, fname_temp);
        File file = new File(floor_dir, fname);

        if (file.exists())
            file.delete();
        if (file_temp.exists())
            file_temp.delete();

        try {
            if (!file_temp.createNewFile())
                return false;
            write_file(file_temp, list_list, time_list);
            if (!file_temp.renameTo(file))
                return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean delete_file(double longitude, double latitude, long timestamp, int signal_type) {
        String path;
        if(signal_type == SIGNAL_TYPE_WIFI){
            path = wifi_path;
        }
        else if (signal_type == SIGNAL_TYPE_IBEACON){
            path = ibeacon_path;
        }
        else
            return false;
        File wifi_dir = new File(Environment.getExternalStorageDirectory(), path);
        File building_dir = new File(wifi_dir, building_name);
        File floor_dir = new File(building_dir, "floor_" + floor);
        if (!floor_dir.exists())
            if (!floor_dir.mkdirs())
                return false;

        if (!floor_dir.isDirectory())
            return false;

        String fname = df.format(longitude) + "_" + df.format(latitude) + "_" + timestamp +"_" + android.os.Build.MODEL + ".csv";

        File file = new File(floor_dir, fname);
        Log.e("delete file", file.getAbsolutePath());
        return (file.exists() && file.delete());
    }

    private ScanCallback mLeScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {
                    if(!bt_scanning)
                        return;
                    long current_timestamp = System.currentTimeMillis();
                    long current_timestamp_nano = result.getTimestampNanos();
                    final iBeaconClass.iBeacon ibeacon = iBeaconClass.fromScanData(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
                    if(ibeacon == null)
                        return;
                    MyRSS item = new MyRSS(ibeacon.name, ibeacon.bluetoothAddress, ibeacon.rssi, last_bt_timestamp);
                    if (bt_list != null && !bt_list.contains(item) && current_timestamp_nano - last_bt_timestamp_nano < time_bt_threshold) {          //上一批数据
                        bt_list.add(item);
                    }else {                                                                                               //新一批数据
                        bt_list = new ArrayList<>(100);
                        bt_list_list.add(bt_list);
                        time_list_b.add(current_timestamp);
                        last_bt_timestamp = current_timestamp;
                        bt_list.add(item);

                    }
                    last_bt_timestamp_nano = result.getTimestampNanos();
                }
            };

    private void write_file(File file, List<List<MyRSS>> list_list, List<Long> time_list) throws IOException{
        List<String> macs = new LinkedList<>();
        List<String> ssids = new LinkedList<>();
        HashMap<String, Integer> macs_detected = new HashMap<>(1024);
        int macs_detected_num = 0;
        int list_num = list_list.size();
        int current_size;
        String current_mac, current_ssid;
        List<MyRSS> current_list;
        MyRSS current_data;
        for (int i = 0; i < list_num; i++) {
            current_list = list_list.get(i);
            current_size = current_list.size();
            for (int j = 0; j < current_size; j++) {
                current_mac = current_list.get(j).mac;
                current_ssid = current_list.get(j).name;
                if (!macs_detected.containsKey(current_mac)) {
                    macs.add(current_mac);
                    ssids.add(current_ssid);
                    macs_detected_num++;
                    macs_detected.put(current_mac, macs_detected_num - 1);
                }

            }
        }

        int[][] level_data = new int[list_num][macs_detected_num];
        int current_rssi = -200;
        for (int i = 0; i < list_num; i++)
            for (int j = 0; j < macs_detected_num; j++)
                level_data[i][j] = -200;
        for (int i = 0; i < list_num; i++) {
            current_list = list_list.get(i);
            current_size = current_list.size();
            for (int j = 0; j < current_size; j++) {
                current_data = current_list.get(j);
                current_mac = current_data.mac;
                //Log.e("key",current_mac);
                int index = macs_detected.get(current_mac);
                //Log.e("value",""+index);
                level_data[i][index] = current_data.rss;
            }
        }

        DataWriter dw = new DataWriter(file, false);

        Iterator iterator = ssids.iterator();

        // write ssid list
        while (iterator.hasNext()) {
            current_ssid = (String) iterator.next();
            dw.write("," + current_ssid);
        }
        dw.write("\r\n");

        iterator = macs.iterator();

        // write mac list
        while (iterator.hasNext()) {
            current_mac = (String) iterator.next();
            dw.write("," + current_mac);
        }
        dw.write("\r\n");

        //write time and rss
        for (int i = 0; i < list_num; i++) {
            dw.write(String.valueOf(time_list.get(i)));
            for (int j = 0; j < macs_detected_num; j++)
                dw.write("," + level_data[i][j]);
            dw.write("\r\n");
        }
        dw.close();
    }
    private void clear_temp_data(){
        wifi_list_list = null;
        bt_list_list = null;
        bt_list = null;
        time_list_b = null;
        time_list_w = null;
    }
}
