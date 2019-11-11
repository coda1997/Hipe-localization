package com.example.overl.hipe.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager

class MyWiFiScanner(val context:Context, wifiScanResultSolver: WifiScanResultSolver) {
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val wifiScanReceiver = object :BroadcastReceiver(){
        override fun onReceive(c: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED,false)
            if (success){
                wifiScanResultSolver.scanSuccess(wifiManager)
                if (!endScan){
                    wifiManager.startScan()
                }
            }else{
                wifiScanResultSolver.scanFailure(wifiManager)
            }
        }
    }
    private var endScan = false
    private val intentFilter = IntentFilter()
    private var isBind = false
    fun bindService(){
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(wifiScanReceiver, intentFilter)
        isBind=true
    }

    fun startScan():Boolean{
        return if (isBind){
            wifiManager.startScan()
        }else{
            false
        }
    }
    fun stopScan(){
        endScan=true
    }
}

interface WifiScanResultSolver{
    fun scanSuccess(wifiManager: WifiManager)
    fun scanFailure(wifiManager: WifiManager)
}