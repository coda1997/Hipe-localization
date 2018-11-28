package com.example.overl.hipe.background;

import android.content.Context;
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
import java.util.Map;

/**
 * Created by zhang on 2018/11/16.
 */

public class WiFi_Localizer_WKNN {

    private class Result implements Comparable{
        public Point point;
        public double distance;
        Result(Point point, double distance){
            this.point = point;
            this.distance = distance;
        }

        @Override
        public int compareTo(Object ob){
            Result r1 = (Result) ob;
            if(distance > r1.distance)
                return 1;
            else if(distance < r1.distance)
                return -1;
            else
                return 0;
        }
    }

    private class Point {
        double longitude, latitude;
        int floor;
        String name;
        String device;
        public double[] avg_rss;
        public int[] count;
        public double[] weight;


        public Point(double longitude, double latitude, int floor, String device) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.floor = floor;
            this.name = df.format(longitude) + "_" + df.format(latitude) + "_" + floor;
            this.device = device;
        }

        public void set_feature(ArrayList<double []> rssis){
            if(rssis == null || rssis.size() == 0)
                return;
            int round_count = rssis.size();
            avg_rss = new double[building_mac_count];
            count = new int[building_mac_count];
            weight = new double[building_mac_count];
            for(int i = 0; i < building_mac_count; ++i){
                avg_rss[i] = 0;
                count[i] = 0;
                weight[i] = 0;
            }
            for(int i = 0; i < round_count; ++i){
                for(int j = 0; j < building_mac_count; ++j){
                    if(rssis.get(i)[j] >= -100) {
                        avg_rss[j] += rssis.get(i)[j];
                        ++count[j];
                    }
                    else{
                        avg_rss[j] += -100;
                        ++count[j];
                    }
                }
            }
            for(int i = 0; i < building_mac_count; ++i){
                if(count[i] > 0)
                    avg_rss[i] /= (double)count[i];
                else
                    avg_rss[i] = -100;
            }
            double [] std = new double[building_mac_count];
            for(int i = 0; i < building_mac_count; ++i){
                std[i] = 0;
            }
            for(int i = 0; i < round_count; ++i){
                for(int j = 0; j < building_mac_count; ++j){
                    double current_rss;
                    if((current_rss = rssis.get(i)[j]) >= -99.9) {
                        current_rss -= avg_rss[j];
                        std[j] += current_rss * current_rss;
                    }
                }
            }

            double sum = 0;
            for(int i = 0; i < building_mac_count; ++i){
                if(count[i] > 3){
                    std[i] = Math.sqrt(std[i]/(count[i] - 1));
                    weight[i] = 1 / (std[i] + 1);
                }
                else
                    weight[i] = 0.1;
                std[i] = Math.sqrt(std[i]/(count[i] - 1));
                weight[i] = 1 / (std[i] + 1);

                sum += weight[i];
            }
            for(int i = 0; i < building_mac_count; ++i) {
                weight[i] /= sum;
            }
        }
    }
    static DecimalFormat df = new DecimalFormat("0.00000000");
    private String wifi_path = "AAAAA/Wi-Fi_Data/";

    WifiManager wifiManager;

    private String building_name;

    private ArrayList<ArrayList<Point>> points_building_floors = new ArrayList<>();
    private ArrayList<Integer> floor_list = new ArrayList<>();
    private ArrayList<Point> points_building = new ArrayList<>();
    private HashMap<String, Integer> building_mac_index = new HashMap<>();
    private int building_mac_count;

    private boolean isRunning = false;

    PosListenner posListenner;

    ArrayList<double []> recent_scan = new ArrayList<>();
    int max_recent_count = 1;
    int max_K = 1;

    public WiFi_Localizer_WKNN(Context context){
        try {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                if (!wifiManager.isWifiEnabled()) {
                    wifiManager.setWifiEnabled(true);
                }
            } else throw new Exception("Wifi Exception");
            while (!wifiManager.isWifiEnabled());
            this.wifiManager = wifiManager;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean setBuilding(String building_name) {
        this.building_name = building_name;
        return (loadLocalData() > 0);
    }

    private int loadLocalData() {
        points_building.clear();
        points_building_floors.clear();
        floor_list.clear();
        building_mac_index.clear();
        building_mac_count = 0;

        load_mac();
        load_points();

        return points_building.size();
    }

    private void load_mac(){
        File wifi_dir = new File(Environment.getExternalStorageDirectory(), wifi_path);
        File building_dir = new File(wifi_dir, building_name);
        if(building_dir.exists()) {
            File[] files_floor = building_dir.listFiles();
            for (int i = 0; i < files_floor.length; ++i) {
                String file_name = files_floor[i].getName();
                String[] parts = file_name.split("_");
                if (parts.length != 2 || !parts[0].equals("floor"))
                    continue;
                int current_floor = Integer.valueOf(parts[1]);
                File floor_dir = files_floor[i];
                if (floor_dir.exists()) {
                    File[] files = floor_dir.listFiles();
                    int file_count = files.length;
                    for (int j = 0; j < file_count; ++j) {
                        String fname = files[j].getName();
                        String[] strs = fname.split("_");
                        if (strs.length != 3)
                            continue;
                        try {
                            DataReader dataReader = new DataReader(files[j]);
                            String current_line;
                            if(dataReader.readLine() != null && (current_line = dataReader.readLine()) != null){
                                String[] macs = current_line.split(",");
                                for(int k = 0; k < macs.length; ++k){
                                    if(!macs[k].equals("") && !building_mac_index.containsKey(macs[k])){
                                        building_mac_index.put(macs[k], building_mac_count++);
                                    }
                                }
                            }
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        Iterator iterator = building_mac_index.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry entry = (Map.Entry) iterator.next();
            String key = (String) entry.getKey();
            Integer val = (Integer) entry.getValue();
            Log.e("map key", key +"  "+val);

        }
    }

    private void load_points(){
        File wifi_dir = new File(Environment.getExternalStorageDirectory(), wifi_path);
        File building_dir = new File(wifi_dir, building_name);
        if(building_dir.exists()) {
            File[] files_floor = building_dir.listFiles();
            for(int i = 0; i < files_floor.length; ++i){
                String file_name = files_floor[i].getName();
                String[] parts = file_name.split("_");
                if(parts.length != 2 || !parts[0].equals("floor"))
                    continue;
                int current_floor = Integer.valueOf(parts[1]);
                floor_list.add(current_floor);
                ArrayList<Point> points_current_floor = new ArrayList<Point>(100);
                points_building_floors.add(points_current_floor);

                File floor_dir = files_floor[i];
                if (floor_dir.exists()) {
                    File[] files = floor_dir.listFiles();
                    int file_count = files.length;
                    for (int j = 0; j < file_count; ++j) {
                        String fname = files[j].getName();
                        String[] strs = fname.split("_");
                        if (strs.length != 3)
                            continue;
                        double current_longitude, current_latitude;
                        current_longitude = Double.valueOf(strs[0]);
                        current_latitude = Double.valueOf(strs[1]);
                        Point current_point = new Point(current_longitude, current_latitude, current_floor, "");
                        try{
                            DataReader dataReader = new DataReader(files[j]);
                            String ssids = dataReader.readLine();  //第一个为空
                            String[] macs = dataReader.readLine().split(",");   //第一个为空
                            int current_macs_len = macs.length - 1;
                            int [] current_macs_index = new int[current_macs_len];
                            ArrayList<double []> rssis = new ArrayList<>(30);
                            for(int k = 1; k < macs.length; ++k){
                                String current_mac = macs[k];
                                Integer index;
                                if((index = building_mac_index.get(current_mac)) != null){
                                    current_macs_index[k - 1] = index;
                                }
                                else{
                                    current_macs_index[k - 1] = -1;
                                }
                            }
                            String current_line;
                            while((current_line = dataReader.readLine()) != null){
                                String [] current_round_str = current_line.split(",");
                                double [] current_round = new double[building_mac_count];
                                for(int k = 0; k < building_mac_count; ++k)
                                    current_round[k] = -100;
                                for(int k = 0; k < current_macs_len; ++k){
                                    current_round[current_macs_index[k]] = Double.valueOf(current_round_str[k + 1]);
                                }
                                rssis.add(current_round);
                            }
                            current_point.set_feature(rssis);
                        }catch (Exception e){
                            e.printStackTrace();
                            continue;
                        }
                        points_current_floor.add(current_point);
                        points_building.add(current_point);
                    }
                }
            }

        }
    }

    private int judge_floor(){
        return -1;
    }

    private double distance(ArrayList<double []> rss, Point referrence_point){
        if(rss == null || referrence_point == null)
            return -1;
        if(rss.size() == 0 || rss.get(0).length != referrence_point.avg_rss.length)
            return -1;
        if(referrence_point.avg_rss.length != building_mac_count)
            return -1;
        double distance = 0;
        int round = rss.size();
        //Log.e("current_round_count", ""+round);
        double[] current_avg_rss = new double[building_mac_count];
        int[] current_count = new int[building_mac_count];
        for(int i = 0; i < building_mac_count; ++i){
            current_avg_rss[i] = 0;
            current_count[i] = 0;
        }
        for(int i = 0; i < round; ++i){
            for(int j = 0; j < building_mac_count; ++j){
                if(rss.get(i)[j] > -99.9) {
                    current_avg_rss[j] += rss.get(i)[j];
                    ++current_count[j];
                }
            }
        }
        for(int i = 0; i < building_mac_count; ++i){
            if(current_count[i] > 0)
                current_avg_rss[i] /= (double)current_count[i];
            else
                current_avg_rss[i] = -100;
        }
        for(int i = 0; i < building_mac_count; ++i) {

        }

        for(int i = 0; i < building_mac_count; ++i){
            double delta = current_avg_rss[i] - referrence_point.avg_rss[i];
            //Log.e("mac"+i+":", current_avg_rss[i]+"  "+delta + "  " + referrence_point.weight[i]);
            distance += delta * delta * referrence_point.weight[i];
        }
        distance = Math.sqrt(distance);
        return distance;
    }

    public void start(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                isRunning = true;
                while(isRunning) {
                    try {
                        wifiManager.startScan();
                        Thread.sleep(3000);
                        if(!isRunning)
                            break;
                        ArrayList<ScanResult> scanResults = (ArrayList<ScanResult>) wifiManager.getScanResults();
                        for(int i = 0; i < scanResults.size(); ++i){
                            //("scan", scanResults.get(i).BSSID + "::" + scanResults.get(i).level);
                        }
                        double[] current_rssis = new double[building_mac_count];
                        for(int i = 0; i < building_mac_count; ++i)
                            current_rssis[i] = -100;
                        int result_len = scanResults.size();
                        if(recent_scan.size() >= max_recent_count)
                            recent_scan.remove(0);
                        for(int i = 0; i < result_len; ++i){
                            Integer index = building_mac_index.get(scanResults.get(i).BSSID);
                            if(index != null)
                                current_rssis[index] = scanResults.get(i).level;
                        }
                        recent_scan.add(current_rssis);
                        int point_count = points_building.size();
                        ArrayList<Result> calculate_results = new ArrayList<>();
                        for(int i = 0; i < point_count; ++i){
                            Point current_RP = points_building.get(i);
                            double distance = distance(recent_scan, current_RP);
                            calculate_results.add(new Result(current_RP, distance));
                        }
                        ArrayList<Result> closest_results = new ArrayList<>();
                        Collections.sort(calculate_results);
                        for(int i = 0; i < point_count; ++i) {
                            Log.e("distance" + i, "" + calculate_results.get(i).distance);
                        }
                        for(int i = 0; i < max_K && i < calculate_results.size(); ++i)
                            closest_results.add(calculate_results.get(i));

                        if(posListenner != null) {
                            //Log.e("ALERT", "listener");
                            double current_longitude = 0, current_latitude = 0;
                            int current_floor = closest_results.get(0).point.floor;
                            Double[] weight = new Double[max_K];

                            Double sum = 0.;
                            for(int i = 0; i < max_K && i<closest_results.size(); ++i){
                                weight[i] = 1/(closest_results.get(i).distance * closest_results.get(i).distance);
                                sum += weight[i];
                            }
                            for(int i = 0; i < max_K && i<closest_results.size(); ++i){
                                weight[i] /= sum;
                                //Log.e("weight"+i, ""+weight[i]);
                            }
                            if (weight[0].equals(Double.POSITIVE_INFINITY) || sum.equals(Double.POSITIVE_INFINITY)) {
                                Point point = closest_results.get(0).point;
                                current_longitude = point.longitude;
                                current_latitude = point.latitude;
                            }
                            for(int i = 0; i < max_K && i<closest_results.size(); ++i){
                                Point point = closest_results.get(i).point;
                                current_longitude += weight[i] * point.longitude;
                                current_latitude += weight[i] * point.latitude;
                            }
                            current_floor = closest_results.get(0).point.floor;
                            posListenner.onNewPosition(current_longitude, current_latitude, current_floor);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        Log.e("localizier", "");
    }

    public void stop(){
        isRunning = false;
    }

    public boolean isRunning(){
        return isRunning;
    }

    public void setPosListener(PosListenner posListener){
        this.posListenner = posListener;
    }

    public ArrayList<double []> getPointsFloor(int floor){
        int index = -1000000;
        ArrayList<double[]> points_floor = new ArrayList<>();
        for(index = 0; index < floor_list.size(); ++index){
            if(floor_list.get(index) == floor){
                ArrayList<Point> pts = points_building_floors.get(index);
                for(int i = 0; i < pts.size(); ++i){
                    points_floor.add(new double[]{pts.get(i).longitude, pts.get(i).latitude});
                }
                break;
            }
        }
        return points_floor;
    }

    public interface PosListenner {
        void onNewPosition(double longitude, double latitude, int floor);
    }
}
