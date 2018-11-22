package com.example.overl.hipe

data class Data(val code: Int = 200, val msg: String = "OK", val data: RespondBody? = null)

data class RespondBody(val points: List<Point>?=null, val building: Building? = null,val buildings:List<Building>?=null)

data class Building(val name: String, val floors: List<Floor>? = null)

data class Floor(val name: String, val content: String)

data class Point(val id: Long, val latitude: Double, val longitude: Double, val floor: Int, val buildingName: String="shilintong", val wifiScanRes: List<WifiScanRes> = emptyList(), val blueToothScanRes: List<BlueToothScanRes> = emptyList())

class BlueToothScanRes // not used ye   t

    data class WifiScanRes(val ctime:String, val ress:List<OriginalRes>)

data class OriginalRes(val ssid:String, val level:Int)