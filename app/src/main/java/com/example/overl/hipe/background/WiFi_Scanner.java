package com.example.overl.hipe.background;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;
//import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by zhang on 2018/3/27.
 */

public class WiFi_Scanner {
    private String building_name;
    private int floor;


    public WifiManager wifiManager;

    final java.text.SimpleDateFormat sDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    private boolean isRunning = false;
    private boolean collecting_w = false;

    List<List<MyRSS>> wifi_list_list;
    private final int initial_list_num = 200;

    private LinkedList<Long> time_list_w = new LinkedList<>();

    public interface ScannerListener {
        void onScanFinished();
    }

    class MyRSS implements Comparable{
        public int rss;
        String mac;
        String name;
        public long timestamp;
        public MyRSS(String mac, int rss, long timestamp){
            this.rss = rss;
            this.mac = mac;
            this.timestamp = timestamp;
        }
        public MyRSS(String name, String mac, int rss, long timestamp){
            this.name = name;
            this.rss = rss;
            this.mac = mac;
            this.timestamp = timestamp;
        }
        @Override
        public int compareTo(Object object){
            MyRSS myRSS = (MyRSS)object;
            if (this.rss > myRSS.rss)
                return -1;
            else if(this.rss < myRSS.rss)
                return 1;
            else return 0;
        }
    }

    public WiFi_Scanner(WifiManager wifiManager){
        try {
            if (wifiManager == null)
                throw new Exception("WiFi_Scanner Creating Failed");
            this.wifiManager = wifiManager;
        }catch (Exception e){
            Log.e("WiFi_Scanner","Create Failed");
        }
    }

    public boolean setBuilding(String building_name){

        return false;
    }

    public boolean setFloor(int floor){

        return false;
    }

    public boolean sendCommand(double longtitude, double latitude, int cmd){
        switch (cmd){
            case Command.SCAN:{

            }break;
            case Command.RESCAN:{

            }
            case Command.DELETE:{

            }break;
        }
        return false;
    }

    public ArrayList<double []> getLocalPoints(){
        ArrayList<double []> pts = new ArrayList<>();

        return pts;
    }

    synchronized private boolean start(){
        synchronized ("0") {
            if (wifiManager != null) {
                isRunning = true;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            wifi_list_list = new ArrayList<>(initial_list_num);
                            time_list_w = new LinkedList<>();
                            collecting_w = true;
                            boolean first_scan = true;
                            while (collecting_w) {
                                Thread.sleep(3000);
                                wifiManager.startScan();
                                if (!first_scan){
                                    ArrayList<ScanResult> scanResults = (ArrayList<ScanResult>) wifiManager.getScanResults();
                                    if (scanResults != null) {
                                        time_list_w.add(System.currentTimeMillis());
                                        int AP_n = scanResults.size();
                                        List<MyRSS> wifi_list = new ArrayList<>(AP_n);
                                        for (int i = 0; i < AP_n; i++) {
                                            ScanResult scanResult = scanResults.get(i);
                                            wifi_list.add(new MyRSS(scanResult.SSID, scanResult.BSSID, scanResult.level, scanResult.timestamp));
                                        }
                                        Collections.sort(wifi_list);
                                        wifi_list_list.add(wifi_list);
                                    }
                                }
                                first_scan = false;
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

                return true;
            } else return false;
        }
    }

    synchronized public boolean stop(boolean toWrite){
        collecting_w = false;
        boolean writted = false;
        if (isRunning && toWrite && wifi_list_list != null) {
            String fname = "";
            wirte_file(fname, "Wi-Fi_Data/", building_name, wifi_list_list, time_list_w);
            writted = true;
        }
        isRunning = false;
        if (wifi_list_list != null)
            wifi_list_list.clear();
        if (time_list_w != null)
            time_list_w.clear();
        if (writted) {
            return true;
        }
        else
            return false;
    }

    private void wirte_file(String fname, String dname0, String dname1, List<List<MyRSS>> list_list, List<Long> time_list){
        try {
            List<String> macs = new LinkedList<>();
            List<String> names = new LinkedList<>();
            HashMap<String, Integer> macs_detected = new HashMap<>(1024);
            int macs_detected_num = 0;
            int list_num = list_list.size();
            int current_size;
            String current_mac;
            List<MyRSS> current_list;
            MyRSS current_data;
            for (int i = 0; i < list_num; i++) {
                current_list = list_list.get(i);
                current_size = current_list.size();
                for (int j = 0; j < current_size; j++) {
                    current_mac = current_list.get(j).mac;
                    if (!macs_detected.containsKey(current_mac)) {
                        macs.add(current_mac);
                        names.add(current_list.get(j).name);
                        macs_detected_num++;
                        macs_detected.put(current_mac, macs_detected_num - 1);
                        //Log.e("add_key", current_mac + " " + (macs_detected_num - 1));
                    }

                }
            }
            //Log.e("add_key", "complete");

            int[][] level_data = new int[list_num][macs_detected_num];
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

            File dir = new File(Environment.getExternalStorageDirectory(), "DATA_"+android.os.Build.MODEL);
            if (!dir.exists())
                dir.mkdir();
            dir = new File(dir, dname0);
            if (!dir.exists())
                dir.mkdir();
            dir = new File(dir, dname1);
            if (!dir.exists())
                dir.mkdir();
            File file = new File(dir, fname);
            if(file.exists())
                file.delete();
            DataWriter fw = new DataWriter(file, false);

            Iterator iterator0 = names.iterator();
            while (iterator0.hasNext()) {
                fw.write("," + iterator0.next());
            }
            fw.write("\r\n");

            Iterator iterator = macs.iterator();
            // write mac list
            while (iterator.hasNext()) {
                current_mac = (String) iterator.next();
                fw.write("," + current_mac);
            }
            fw.write("\r\n");

            //write time and rss
            for (int i = 0; i < list_num; i++) {
                fw.write(String.valueOf(time_list.get(i)));
                for (int j = 0; j < macs_detected_num; j++)
                    fw.write("," + level_data[i][j]);
                fw.write("\r\n");
            }
            fw.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }


}
