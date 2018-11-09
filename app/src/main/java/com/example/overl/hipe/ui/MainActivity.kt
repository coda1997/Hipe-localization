package com.example.overl.hipe.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import com.example.overl.hipe.R
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.GeoJson
import com.mapbox.geojson.gson.GeoJsonAdapterFactory
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.plugins.annotation.AnnotationManager
import com.mapbox.mapboxsdk.plugins.annotation.LineManager
import com.mapbox.mapboxsdk.style.layers.Layer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import java.nio.charset.Charset

class MainActivity : BaseActivity(), MapboxMap.OnMapLongClickListener {

    var currentMarker: Marker? = null


    override fun onMapLongClick(point: LatLng) {
        if (currentMarker != null) {
            currentMarker?.apply {
                this.position=point
                mapboxMap?.updateMarker(this)
            }
        }else{
            mapboxMap?.addMarker(MarkerOptions()
                    .position(point)
                    .icon(IconFactory.getInstance(this).fromResource(R.mipmap.edit_maker_red_uncollected))
            )
            currentMarker=mapboxMap?.markers?.last()

        }
    }


    private var mapboxMap: MapboxMap? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("mapView info:", "is null?" + (mapView == null))
        initMapView()
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

                marker.icon=IconFactory.getInstance(this@MainActivity).fromResource(R.mipmap.edit_maker_blue_collected)
                mapboxMap?.updateMarker(marker)
                mapboxMap?.deselectMarker(marker)
                currentMarker=null

            }
            deleteBt.onClick {
                //call methods to delete points
//                mapboxMap?.removeMarker(marker)
                this@MainActivity.alert("Delete this point ?", "Delete") {
                    okButton {
                        mapboxMap?.removeMarker(marker)
                        currentMarker=null
                    }
                    cancelButton { }
                }.show()
            }
            v
        }



        val utils = GeoJsonUtils(this@MainActivity, mapboxMap!!)
        utils.filePath="shilintong/MapData1.txt"
        utils.execute()
    }


}
