package com.example.overl.hipe.ui

import android.app.AlertDialog
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import com.example.overl.hipe.R
import com.example.overl.hipe.background.WiFi_Scanner_
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.GeoJson
import com.mapbox.geojson.gson.GeoJsonAdapterFactory
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap

import com.mapbox.mapboxsdk.style.layers.Layer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import java.nio.charset.Charset

class MainActivity : BaseActivity(), MapboxMap.OnMapLongClickListener, WiFi_Scanner_.ScannerListener {
    override fun onScanFinished(count: Int) {

    }

    override fun onScan(round: Int) {
        val msg = "现在已采集${round}轮"
        runOnUiThread { currentDialog?.setMessage(msg) }
    }

    private var currentMarker: Marker? = null
    private var currentFloor = 1
    private var currentDialog: AlertDialog? = null
    lateinit var wifiScanner: WiFi_Scanner_

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


    private var mapboxMap: MapboxMap? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("mapView info:", "is null?" + (mapView == null))
        initMenu()

        initMapView()
        wifiScanner = WiFi_Scanner_(getSystemService(Context.WIFI_SERVICE) as WifiManager).apply {
            setScannerListener(this@MainActivity)
            setBuildingFloor("shilintong", currentFloor)
        }
    }

    private fun initMenu() {
        val toolBar = find<Toolbar>(R.id.toolbar)
        toolBar.title = "test 1"
        toolBar.inflateMenu(R.menu.main_menu)
        toolBar.setOnMenuItemClickListener { item: MenuItem? ->
            when (item?.itemId) {
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
                        if (wifiScanner.stop()) {
                            this@MainActivity.toast("采集成功")
                        } else {
                            this@MainActivity.toast("采集失败")
                        }
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
            val utils = GeoJsonUtils(this@MainActivity, mapboxMap!!)
            utils.filePath = "shilintong/MapData$floor.txt"
            utils.execute()

            //???? the method would override fileName if caller calls this with the same building name?
            doAsyncResult {
                wifiScanner.setBuildingFloor("shilintong", floor)
                wifiScanner.localPoints

            }.get().forEach {
                mapboxMap?.addMarker(MarkerOptions().position(LatLng(it[1], it[0])).icon(IconFactory.getInstance(this).fromResource(R.mipmap.edit_maker_blue_collected))) }
            currentFloor = floor
        }
    }

}
