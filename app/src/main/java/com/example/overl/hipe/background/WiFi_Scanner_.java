package com.example.overl.hipe.background;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;

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

    static DecimalFormat df = new DecimalFormat( "0.00000000");

    private String building_name;
    private int floor;

    private int count = 0;

    private String wifi_path = "AAAAA/Wi-Fi_Data/";
    private String ibeacon_path = "AAAAA/iBeacon_Data/";

    private final double max_longitude_delta = 10;
    private final double max_latitude_delta = 10;

    private double longitude, latitude;

    private ArrayList<Point> pts = new ArrayList<>();

    private boolean isRunning = false;
    private long max_time_in_second = 0;
    private long start_time = 0;

    private ScannerListener scannerListener;


    public WifiManager wifiManager;

    private class Point{
        double longitude, latitude;
        String name;
        String device;
        public Point(double longitude, double latitude, String device){
            this.latitude = latitude;
            this.longitude = longitude;
            name = df.format(latitude) + "_" + df.format(longitude);
            this.device = device;
        }
        public Point(double longitude, double latitude, String name, String device){
            this.latitude = latitude;
            this.longitude = longitude;
            this.name = name;
            this.device = device;
        }
    }

    public interface ScannerListener {
        void onScanFinished(int count);
        void onScan(int round);
    }

    public void setScannerListener(ScannerListener scannerListener){
        this.scannerListener = scannerListener;
    }

    public WiFi_Scanner_(WifiManager wifiManager){
        try {
            if (wifiManager == null)
                throw new Exception("WiFi_Scanner Creating Failed");
            this.wifiManager = wifiManager;
        }catch (Exception e){
            Log.e("WiFi_Scanner","Create Failed");
        }
    }

    private int loadPoints(){
        pts.clear();
        File wifi_dir = new File(Environment.getExternalStorageDirectory(), wifi_path);
        File building_dir = new File(wifi_dir, building_name);
        File floor_dir = new File(building_dir, "floor_" + floor);
        if(floor_dir.exists()){
            File[] files = floor_dir.listFiles();
            int file_count = files.length;
            for(int i = 0; i < file_count; ++i){
                String fname = files[i].getName();
                String[] strs = fname.split("_");
                if(strs.length != 3)
                    continue;
                double current_longitude, current_latitude;
                current_longitude = Double.valueOf(strs[0]);
                current_latitude = Double.valueOf(strs[1]);
                Log.e(strs[0] + "_" + strs[1] + "_" +  strs[2], "???");
                String model = strs[2].substring(0, strs[2].length() - 4);
                Log.e(model, model);
                pts.add(new Point(current_longitude, current_latitude, strs[0] + "_" + strs[1], model));
            }
        }
        return pts.size();
    }

    public boolean setBuildingFloor(String building_name, int floor){
       this.building_name = building_name;
       this.floor = floor;
       return(loadPoints() > 0);
    }

    public boolean startScan(double longitude, double latitude, int timeInSecond){
        if(!isRunning){
            start_time = System.currentTimeMillis();
            isRunning = true;
            max_time_in_second = timeInSecond;
            this.longitude = longitude;
            this.latitude = latitude;
            start();
            return true;
        }
        return false;
    }

    public boolean delete(double longitude, double latitude){
        int length = pts.size();
        double min_delta2 = 10000000;
        int closest = -1;
        for(int i = 0; i < length; ++i){
            Point pt = pts.get(i);
            double d0 = 1000000 * (longitude - pt.longitude);
            double d1 = 1000000 * (latitude - pt.latitude);
            if(d0 < max_longitude_delta && d1 < max_latitude_delta){
                double d2 = d0 * d0 + d1 * d1;
                if(d2 < min_delta2){
                    min_delta2 = d2;
                    closest = i;
                }
            }
        }
        Log.e("CLO", ""+closest);
        if(closest >= 0){
            Log.e("CLOSEST", pts.get(closest).longitude + "_" + pts.get(closest).latitude);
            delete_wifi_file(pts.get(closest).longitude, pts.get(closest).latitude);
            pts.remove(closest);
            return true;
        }
        return false;
    }

    public ArrayList<double []> getLocalPoints(){
        int length = pts.size();
        ArrayList<double []> result = new ArrayList<>(length);
        for(int i = 0; i < length; ++i){
            result.add(new double[]{pts.get(i).longitude, pts.get(i).latitude});
        }
        return result;
    }

    synchronized private boolean start(){
        synchronized ("0") {
            if (wifiManager != null) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        long current_time;
                        int round = 0;
                        scannerListener.onScan(0);
                        while (isRunning && (current_time = System.currentTimeMillis() - start_time) + 3010 < max_time_in_second * 1000) {
                            ++round;
                            try {
                                Thread.sleep(3000);
                                scannerListener.onScan(round);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        while (isRunning && (current_time = System.currentTimeMillis() - start_time) < max_time_in_second * 1000) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        write_wifi_file(longitude, latitude);
                        pts.add(new Point(longitude, latitude, android.os.Build.MODEL));
                        scannerListener.onScanFinished(round);
                        isRunning = false;
                    }
                }).start();

                return true;
            } else return false;
        }
    }

    synchronized public boolean stop(){
        isRunning = false;
        return false;
    }

    private boolean write_wifi_file(double longitude, double latitude){
        File wifi_dir = new File(Environment.getExternalStorageDirectory(), wifi_path);
        File building_dir = new File(wifi_dir, building_name);
        File floor_dir = new File(building_dir, "floor_" + floor);
        if(!floor_dir.exists())
            if(!floor_dir.mkdirs())
                return false;

        if(!floor_dir.isDirectory())
            return false;

        String fname = df.format(longitude) + "_" + df.format(latitude) + "_" + android.os.Build.MODEL + ".csv";
        String fname_temp = "TEMP_" + fname;
        File file_temp = new File(floor_dir, fname_temp);
        File file = new File(floor_dir, fname);

        if(file.exists())
            file.delete();
        if(file_temp.exists())
            file_temp.delete();

        try {
            if(!file_temp.createNewFile())
                return false;
            if(!file_temp.renameTo(file))
                return false;
        }catch (IOException e){
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private boolean delete_wifi_file(double longitude, double latitude){
        File wifi_dir = new File(Environment.getExternalStorageDirectory(), wifi_path);
        File building_dir = new File(wifi_dir, building_name);
        File floor_dir = new File(building_dir, "floor_" + floor);
        if(!floor_dir.exists())
            if(!floor_dir.mkdirs())
                return false;

        if(!floor_dir.isDirectory())
            return false;

        String fname = df.format(longitude) + "_" + df.format(latitude) + "_" + android.os.Build.MODEL + ".csv";
        File file = new File(floor_dir, fname);
        return (file.exists() && file.delete());
    }


}
