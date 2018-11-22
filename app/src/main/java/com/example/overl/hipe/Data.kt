package com.example.overl.hipe

data class Data(val code: Int, val msg: String, val data: RespondBody? = null)

data class RespondBody(val points: List<Point>?=null, val building: Building? = null,val buildings:List<Building>?=null)

data class Building(val name: String, val floors: List<Floor>? = null)

data class Floor(val name: String, val content: String)

data class Point(val id: Long, val latitude: Double, val longitude: Double, val floor: Int, val buildingName: String="shilintong", val wifiScanRes: List<WifiScanRes> = emptyList(), val blueToothScanRes: List<BlueToothScanRes> = emptyList())

class BlueToothScanRes // not used ye   t

    data class WifiScanRes(val id:Int=0,val ctime:String, val ress:List<OriginalRes>,val pid:Int)

data class OriginalRes(val id:Int=0,val ssid:String, val level:Int,val sid:Int)