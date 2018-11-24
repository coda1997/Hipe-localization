package com.example.overl.hipe.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import com.example.overl.hipe.*
import com.example.overl.hipe.background.WiFi_Scanner_
import com.example.overl.hipe.service.SyncService
import com.example.overl.hipe.service.getSyncService

import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers


import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.ArrayList

class MainActivity : BaseActivity(), MapboxMap.OnMapLongClickListener, WiFi_Scanner_.ScannerListener {
    var round: Int = 0
    override fun onScan(list: ArrayList<OriginalRes>?) {

        val msg = "现在已采集${++round}轮"
        runOnUiThread { currentDialog?.setMessage(msg) }
    }

    override fun onScanFinished(point: Point?) {
        round = 0

        if (point != null) {
            CompositeDisposable().add(
                    service.getPoints(timestamp+1, currentFloor).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe { t1: Data?, t2: Throwable? ->
                        t1?.data?.points?.forEach {
                            mapboxMap?.addMarker(MarkerOptions().position(LatLng(it.latitude, it.longitude)).icon(IconFactory.getInstance(this).fromResource(R.mipmap.edit_maker_green_upload)))
                            pointsUploaded.add(it)
                            wifiScanner.savePointInLocalStorage(it)
                        }
                        t2?.printStackTrace()
                    }
            )
            CompositeDisposable().add(service.addPoint(point)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { body: Data?, ex: Throwable? ->
                        if (body != null && body.code == 200) {
                            toast("上传成功")
                            getMarkerByPoint(point)?.apply {
                                icon = IconFactory.getInstance(this@MainActivity).fromResource(R.mipmap.edit_maker_green_upload)
                                mapboxMap?.updateMarker(this)

                            }
                            pointsUploaded.add(point)
                        } else {
                            toast("上传失败")
                        }
                        ex?.printStackTrace()
                    })
        }
    }

    private var timestamp: Long = 0L
    private val pointsUploaded = mutableListOf<Point>()
    private var currentMarker: Marker? = null
    private var currentFloor = 1
    private var currentDialog: AlertDialog? = null
    private lateinit var wifiScanner: WiFi_Scanner_
    private lateinit var service: SyncService
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
        service = getSyncService()
        wifiScanner = WiFi_Scanner_(applicationContext).apply {
            setScannerListener(this@MainActivity)
            setBuildingFloor("shilintong", currentFloor)
        }
//        val shareref = getSharedPreferences("point", Context.MODE_WORLD_READABLE)
//        timestamp = shareref.getLong("point", 0)
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
                wifiScanner.startScan(marker.position.longitude, marker.position.latitude, 64)
                marker.icon = IconFactory.getInstance(this@MainActivity).fromResource(R.mipmap.edit_maker_blue_collected)
                mapboxMap?.updateMarker(marker)
                mapboxMap?.deselectMarker(marker)
                currentMarker = null

            }
            deleteBt.onClick { _ ->
                //call methods to delete points
//                mapboxMap?.removeMarker(marker)

                this@MainActivity.alert("Delete this point ?", "Delete") {
                    cancelButton { }
                    okButton { _ ->
                        wifiScanner.delete(marker.position.longitude, marker.position.latitude)
                        mapboxMap?.removeMarker(marker)
                        currentMarker = null
                        val points = pointsUploaded.filter { it.latitude == marker.position.latitude && it.longitude == marker.position.longitude&&it.floor==currentFloor }
                        if (points.isNotEmpty()){
                            service.deletePoint(points[0].id).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe { res, ex ->
                                if (res != null && res.code == 200) {
                                    wifiScanner.delete(marker.position.longitude, marker.position.latitude)
                                    mapboxMap?.removeMarker(marker)
                                    currentMarker = null
                                    toast("删除成功")
                                } else {
                                    toast("删除失败${ex.localizedMessage}")
                                }
                            }
                        }else{
                            toast("目标点已删除")
                        }

                    }
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
        val intent = Intent()
        intent.setClass(this, LocActivity::class.java)
        startActivity(intent)
    }

    private fun drawPoints(floor: Int) {
        val list = doAsyncResult {
            wifiScanner.setBuildingFloor("shilintong", floor)
            wifiScanner.localPoints
        }.get()
        list.forEach { it->
            pointsUploaded.add(it)

            mapboxMap?.addMarker(MarkerOptions().position(LatLng(it.latitude, it.longitude)).icon(IconFactory.getInstance(this).fromResource(R.mipmap.edit_maker_green_upload)))
        }
        timestamp = list.maxBy { it.id }?.id?:timestamp
        CompositeDisposable().add(
                service.getPoints(timestamp+1, floor).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe { t1: Data?, t2: Throwable? ->
                    t1?.data?.points?.forEach { mapboxMap?.addMarker(MarkerOptions().position(LatLng(it.latitude, it.longitude)).icon(IconFactory.getInstance(this).fromResource(R.mipmap.edit_maker_green_upload))) }
                    t1?.data?.points?.apply { pointsUploaded.addAll(this) }?.forEach {
                        val f = wifiScanner.savePointInLocalStorage(it)
                        Log.e("save point","$f")
                    }
                    if (t2==null){
                        toast("同步成功")
                    }
                    t2?.printStackTrace()
                }
        )

    }

    private fun getMarkerByPoint(point: Point): Marker? {
        return mapboxMap?.markers?.filter { it.position.latitude == point.latitude && it.position.longitude == point.longitude }?.get(0)
    }

}
