package com.example.overl.hipe.ui

import android.app.AlertDialog
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import com.example.overl.hipe.OriginalRes
import com.example.overl.hipe.Point
import com.example.overl.hipe.R
import com.example.overl.hipe.background.WiFi_Scanner_
import com.example.overl.hipe.client.LocationTransUtils
import com.example.overl.hipe.client.MacAddressUtils
import com.example.overl.hipe.client.sendMsg
import com.google.gson.Gson
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap

import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import java.util.ArrayList

class MainActivity : BaseActivity(), MapboxMap.OnMapLongClickListener, WiFi_Scanner_.ScannerListener {
    private var round: Int = 0
    private lateinit var macAddress:String
    override fun onScan(list: ArrayList<OriginalRes>?) {
        val msg = "现在已采集${++round}轮"
        runOnUiThread { currentDialog?.setMessage(msg) }
        if (list != null) {
            doAsync {
                val gson2 = "{\"Number\":${list.size},\"IPAddress\":\"$macAddress\",\"Type\":1,\"Address\":${list.map { "\"${it.ssid}\"" }},\"Signals\":${list.map { it.level }}}"
                val gson = String.format("{\"Protocal\":%4d,\"Number\":%8d,\"Length\":%8d}",21,2,gson2.length)
                val msg = gson + gson2
                Log.d("data info: ", msg)
                sendMsg(msg, ip, port.toInt())
            }
        }

    }

    override fun onScanFinished(point: Point?) {
        round = 0
        if (point != null) {
            runOnUiThread {
                val resultDialog: AlertDialog = this@MainActivity.alert(title = "", message = "采集成功") {
                    isCancelable = false
                    okButton {
                    }
                }.show()
            }
            doAsync {
                val xy = LocationTransUtils.bigTransfer(doubleArrayOf(point.latitude,point.longitude))
                val gson2 = "{\"x\":${xy[0]},\"y\":${xy[1]}}"
                val gson = String.format("{\"Protocal\":%4d,\"Number\":%8d,\"Length\":%8d}",21,2,gson2.length)
                val msg = gson + gson2
                Log.d("data info: ", msg)
                sendMsg(msg, ip, port.toInt())
            }
        } else {
            runOnUiThread {
                this@MainActivity.alert(message = "采集失败") {
                    okButton {
                    }
                }
            }
        }
    }


    private var currentMarker: Marker? = null
    private var currentFloor = 1
    private var currentDialog: AlertDialog? = null
    private lateinit var wifiScanner: WiFi_Scanner_

    override fun onMapLongClick(point: LatLng) {
        if (currentMarker != null) {
            currentMarker?.apply {
                this.position = point
                mapboxMap?.updateMarker(this)
            }
        } else {
            mapboxMap?.addMarker(MarkerOptions()
                    .position(point)
                    .icon(IconFactory.getInstance(this).fromResource(R.mipmap.edit_maker_red_uncollected))
            )
            currentMarker = mapboxMap?.markers?.last()

        }
    }

    lateinit var ip: String
    lateinit var port: String

    private var mapboxMap: MapboxMap? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("mapView info:", "is null?" + (mapView == null))
        initMenu()
        initMapView()
        macAddress= MacAddressUtils.getMac(this)
        wifiScanner = WiFi_Scanner_(applicationContext).apply {
            setScannerListener(this@MainActivity)
            setBuildingFloor("shilintong", currentFloor)
        }
        alert {
            isCancelable=false
            customView {
                verticalLayout {
                    val ipTx = editText(text = "127.0.0.1") {
                        hint = "IP address"
                    }
                    val portTx = editText(text = "8080") {
                        hint = "port"
                    }
                    positiveButton("OK") {
                        ip = ipTx.text.toString()
                        port = portTx.text.toString()
                        Log.d("ip and port", "$ip and $port")
                    }
                }

            }
        }.show()
    }

    private fun initMenu() {
        val toolBar = find<Toolbar>(R.id.toolbar)
        toolBar.title = "采集模式"
        toolBar.inflateMenu(R.menu.main_menu)
        toolBar.setOnMenuItemClickListener { item: MenuItem? ->
            when (item?.itemId) {
                R.id.menu_bt_loc -> toLocActivity()
                R.id.menu_bt_1f -> changeFloorMap(1)
                R.id.menu_bt_2f -> changeFloorMap(2)
                R.id.menu_bt_3f -> changeFloorMap(3)
                R.id.menu_bt_4f -> changeFloorMap(4)
                R.id.menu_bt_5f -> changeFloorMap(5)
            }
            true
        }
    }

    private fun initMapView() {
        mapView?.getMapAsync { mapboxMap ->
            this.mapboxMap = mapboxMap
            mapboxMap.addOnMapLongClickListener(this)
            loadLocalMapResource()
            drawPoints(currentFloor)
        }
    }

    private fun loadLocalMapResource() {
        mapboxMap?.setInfoWindowAdapter { marker: Marker ->
            val v = View.inflate(this, R.layout.maker_window_info, null)
            val collectBt = v.find<Button>(R.id.bt_collect)
            val deleteBt = v.find<Button>(R.id.bt_delete)
            collectBt.onClick {
                //call methods to collect information
                toast("collecting data now !")
                currentDialog = this@MainActivity.alert(title = "开始采集", message = "现在已采集0轮") {
                    isCancelable = false
                    okButton {
                        title = "停止采集并保存"
                        currentDialog = null
                        wifiScanner.stop()

                    }
                }.show()
                wifiScanner.startScan(marker.position.longitude, marker.position.latitude, 60)
                marker.icon = IconFactory.getInstance(this@MainActivity).fromResource(R.mipmap.edit_maker_blue_collected)
                mapboxMap?.updateMarker(marker)
                mapboxMap?.deselectMarker(marker)
                currentMarker = null

            }
            deleteBt.onClick {
                //call methods to delete points
//                mapboxMap?.removeMarker(marker)
                this@MainActivity.alert("Delete this point ?", "Delete") {
                    okButton {
                        wifiScanner.delete(marker.position.longitude, marker.position.latitude)
                        mapboxMap?.removeMarker(marker)
                        currentMarker = null
                    }
                    cancelButton { }
                }.show()
            }
            v
        }

        val utils = GeoJsonUtils(this@MainActivity, mapboxMap!!)
        utils.filePath = "shilintong/MapData1.txt"
        utils.execute()
    }

    private fun changeFloorMap(floor: Int) {
        if (floor != currentFloor) {
            mapboxMap?.clear()
            currentMarker = null
            val utils = GeoJsonUtils(this@MainActivity, mapboxMap!!)
            utils.filePath = "shilintong/MapData$floor.txt"
            utils.execute()
            drawPoints(floor)
            currentFloor = floor
        }
    }

    private fun toLocActivity() {
        startActivity<LocActivity>()
    }

    private fun drawPoints(floor: Int) {
        doAsyncResult {
            wifiScanner.setBuildingFloor("shilintong", floor)
            wifiScanner.localPoints
        }.get().forEach {
            mapboxMap?.addMarker(MarkerOptions().position(LatLng(it[1], it[0])).icon(IconFactory.getInstance(this).fromResource(R.mipmap.edit_maker_blue_collected)))
        }
    }

}
