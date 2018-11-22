package com.example.overl.hipe.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.example.overl.hipe.R
import com.example.overl.hipe.background.WiFi_Localizer_WKNN
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap

import org.jetbrains.anko.*

class LocActivity : BaseActivity(), MapboxMap.OnMapLongClickListener, WiFi_Localizer_WKNN.PosListenner {
    override fun onNewPosition(longitude: Double, latitude: Double, floor: Int) {
        runOnUiThread {
            Log.e("LocResult", "" + longitude + "__" + latitude + "__" + floor)
            changeFloorMap(floor)
            mapboxMap?.removeMarker(mapboxMap?.markers!!.last())
            drawPoints(floor)
            mapboxMap?.addMarker(MarkerOptions()
                    .position(LatLng(latitude, longitude))
                    .icon(IconFactory.getInstance(this).fromResource(R.mipmap.edit_maker_red_uncollected))
            )
            currentMarker = mapboxMap?.markers?.last()
        }
    }

    private var currentMarker: Marker? = null
    private var currentFloor = 1
    private var currentDialog: AlertDialog? = null
    lateinit var localizer: WiFi_Localizer_WKNN

    override fun onMapLongClick(point: LatLng) {

    }


    private var mapboxMap: MapboxMap? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("mapView info:", "is null?" + (mapView == null))
        initMenu()

        initMapView()
        localizer = WiFi_Localizer_WKNN(applicationContext).apply {
            setPosListener(this@LocActivity)
            setBuilding("shilintong")
            start()
        }
    }

    private fun initMenu() {
        val toolBar = find<Toolbar>(R.id.toolbar)
        toolBar.title = "定位模式"
        toolBar.inflateMenu(R.menu.loc_menu)
        toolBar.setOnMenuItemClickListener { item: MenuItem? ->
            when (item?.itemId) {
                R.id.menu_bt_collect -> toMainActivity()
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
            val v = View.inflate(this, R.layout.marker_loc_info, null)
            val tv = v.find<TextView>(R.id.tv_location)
            v
        }

        val utils = GeoJsonUtils(this@LocActivity, mapboxMap!!)
        utils.filePath = "shilintong/MapData1.txt"
        utils.execute()
    }

    private fun changeFloorMap(floor: Int) {
        if (floor != currentFloor) {
            mapboxMap?.clear()
            currentMarker=null
            val utils = GeoJsonUtils(this@LocActivity, mapboxMap!!)
            utils.filePath = "shilintong/MapData$floor.txt"
            utils.execute()

            //???? the method would override fileName if caller calls this with the same building name?
            drawPoints(floor)
            currentFloor = floor
        }
    }

    private fun toMainActivity(){
        if(localizer.isRunning)
            localizer.stop()
        val intent = Intent()
        intent.setClass(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun drawPoints(floor: Int){
        doAsyncResult {
            localizer.getPointsFloor(floor)
        }.get().forEach {
            mapboxMap?.addMarker(MarkerOptions().position(LatLng(it[1], it[0])).icon(IconFactory.getInstance(this).fromResource(R.mipmap.edit_maker_blue_rp))) }
    }

    override fun onResume() {
        super.onResume()
        if(!localizer.isRunning)
            localizer.start()
    }

}
