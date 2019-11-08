package com.example.overl.hipe.ui

import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import com.example.overl.hipe.R
import com.example.overl.hipe.background.WiFi_Scanner_
import com.example.overl.hipe.service.SyncService
import com.example.overl.hipe.service.getSyncService
import com.example.overl.hipe.util.Data
import com.example.overl.hipe.util.OriginalRes
import com.example.overl.hipe.util.Point
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
import java.util.*

class MainActivity : BaseActivity(), WiFi_Scanner_.ScannerListener {
    private var round: Int = 0
    override fun onScan(list: ArrayList<OriginalRes>?) {

//        if (round!=0){
//            pointNum++
//            wifiScanner.stop()
//            recordPoint(pointNum)
//        }
    }
    private var isStoped = true
    private var pointNum = 0
    private fun recordPoint(num:Int){
        val tClick = (System.currentTimeMillis()-systime)/1000.0
        val landmark = String.format("%.1f",tClick)
        val strLandmark = "$num $landmark+ \n".toByteArray()

        val bytes = ByteArray(2).apply {
            this[0]='s'.toByte()
            this[1]=num.toByte()
        }
        for (address in MyActivity.connectaddresses){
            val gatt = MyActivity.mBleService.connectedBluetoothGatt[address]
            val characteristic =
                    gatt?.getService(UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e"))
                            ?.getCharacteristic(UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"))
            characteristic?.value = bytes
            gatt?.writeCharacteristic(characteristic)
        }

    }




    private var systime = MyActivity.Sys_t0
    override fun onScanFinished(point: Point?) {
//        if (round == 0) {
////            runOnUiThread {
////                toast("采集取消")
////            }
//            return
//        }
//        round = 0
//        if (point != null) {
//            CompositeDisposable().add(
//                    service.getPoints(timestamp + 1, currentFloor).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe { t1: Data?, t2: Throwable? ->
//                        t1?.data?.points?.forEach {
//                            mapboxMap?.addMarker(MarkerOptions().position(LatLng(it.latitude, it.longitude)).icon(IconFactory.getInstance(this).fromResource(R.mipmap.edit_maker_green_upload)))
//                            pointsUploaded.add(it)
//                        }
//                        t2?.printStackTrace()
//                    }
//            )
//            CompositeDisposable().add(service.addPoint(point)
//                    .subscribeOn(Schedulers.io())
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribe { body: Data?, ex: Throwable? ->
//                        if (body != null && body.code == 200) {
//                            toast("上传成功")
//                            getMarkerByPoint(point)?.apply {
//                                icon = IconFactory.getInstance(this@MainActivity).fromResource(R.mipmap.edit_maker_green_upload)
//                                mapboxMap?.updateMarker(this)
//                                preMarker = null
//                            }
//                            pointsUploaded.add(point)
//                        } else {
//                            toast("上传失败")
//                        }
//                        ex?.printStackTrace()
//                    })
//            if (!isStoped){
//                startScan()
//            }
//        }
    }

    private var timestamp: Long = 0L
    private val pointsUploaded = mutableListOf<Point>()
    private var currentMarker: Marker? = null
    private var currentFloor = 1
    private lateinit var wifiScanner: WiFi_Scanner_
    private lateinit var service: SyncService


    private var mapboxMap: MapboxMap? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initMenu()
        initMapView()
        service = getSyncService()
        wifiScanner = WiFi_Scanner_(applicationContext).apply {
            setScannerListener(this@MainActivity)
            setBuildingFloor("shilintong", currentFloor)
        }
    }

    private fun initMenu() {
        val toolBar = find<Toolbar>(R.id.toolbar)
        toolBar.title = "采集模式"
        toolBar.inflateMenu(R.menu.main_menu)
        toolBar.setOnMenuItemClickListener { item: MenuItem? ->
            when (item?.itemId) {
                // R.id.menu_bt_loc -> toLocActivity()
                R.id.menu_bt_1f -> changeFloorMap(1)
                R.id.menu_bt_2f -> changeFloorMap(2)
                R.id.menu_bt_3f -> changeFloorMap(3)
                R.id.menu_bt_4f -> changeFloorMap(4)
                R.id.menu_bt_5f -> changeFloorMap(5)
            }
            true
        }
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

        //init map view without load local points
        private fun initMapView() {
            mapView?.getMapAsync { mapboxMap ->
                this.mapboxMap = mapboxMap
//                mapboxMap.addOnMapLongClickListener(this)
                loadLocalMapResource()
                //init mapbox window adapter
                initWindowAdapter()
                drawPoints(currentFloor)

            }

        }

        private fun initWindowAdapter() {
            mapboxMap?.setInfoWindowAdapter { marker ->
                val v = View.inflate(this, R.layout.base_point_maker_window_info, null)
                val basePointBt = v.find<Button>(R.id.bt_decide)
                basePointBt.onClick {
                    //activate coord function
                    baseCoord = LatLng(marker.position.latitude, marker.position.longitude)
                    pointNum=0
                    offsetF[0] = MyActivity.coords[0]
                    offsetF[1] = MyActivity.coords[1]
                    toast("已选取基准点")
                    isStoped=true
                }

                v
            }

        }

        private fun loadLocalMapResource() {
            val utils = GeoJsonUtils(this@MainActivity, mapboxMap!!)
            utils.filePath = "shilintong/MapData${currentFloor}.txt"
            utils.execute()
        }


        private fun drawPoints(floor: Int) {
            val list = doAsyncResult {
                wifiScanner.setBuildingFloor("shilintong", floor)
                wifiScanner.localPoints
            }.get()
            list.forEach {
                pointsUploaded.add(it)

                mapboxMap?.addMarker(MarkerOptions().position(LatLng(it.latitude, it.longitude)).icon(IconFactory.getInstance(this).fromResource(R.mipmap.edit_maker_green_upload)))
            }
            timestamp = list.maxBy { it.id }?.id ?: timestamp
            CompositeDisposable().add(
                    service.getPoints(timestamp + 1, floor).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe { t1: Data?, t2: Throwable? ->
                        t1?.data?.points?.forEach { mapboxMap?.addMarker(MarkerOptions().position(LatLng(it.latitude, it.longitude)).icon(IconFactory.getInstance(this).fromResource(R.mipmap.edit_maker_green_upload))) }
                        t1?.data?.points?.apply { pointsUploaded.addAll(this) }?.forEach {
                            val f = wifiScanner.savePointInLocalStorage(it)
                            Log.e("save point", "$f")
                        }
                        if (t2 == null) {
                            toast("同步成功")
                        }
                        t2?.printStackTrace()
                    }
            )

        }

        /*
        * 添加一个当前位置的全局变量，可以通过校正点来校正
        *
        * */
        private lateinit var baseCoord: LatLng
        private var offsetF = FloatArray(2)
        private var currentCoord: LatLng = LatLng(0.0, 0.0)
        private var preMarker: Marker? = null
        private fun fetchCoord() {
            var lat = baseCoord.latitude
            var lng = baseCoord.longitude
            val offset = MyActivity.coords
            offset[0] -= offsetF[0]
            offset[1] -= offsetF[1]
            lat += offset[0] * 0.00000899
            lng += offset[1] * 0.00001141// 换算系数
//            val latString = "lat:$lat"
//            tv_lat.text = latString
//            val lngString = "lng:${lng}"
//            tv_lng.text = lngString

            currentCoord.latitude = lat
            currentCoord.longitude = lng
//        preMarker?.apply {
//            mapboxMap?.removeMarker(this)
//        }
            runOnUiThread {
                preMarker = mapboxMap?.addMarker(MarkerOptions().position(currentCoord).icon(IconFactory.getInstance(this).fromResource(R.drawable.ic_person_pin_circle_green_500_24dp)))
            }

        }


        private fun getMarkerByPoint(point: Point): Marker? {
            return mapboxMap?.markers?.filter { it.position.latitude == point.latitude && it.position.longitude == point.longitude }?.get(0)
        }
        private fun startScan(){
            fetchCoord()
            isStoped=false
            wifiScanner.startScan(currentCoord.longitude,currentCoord.latitude,5)
        }

    }
