package com.example.overl.hipe

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.annotation.UiThread
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.mapbox.geojson.gson.GeoJsonAdapterFactory
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdate
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.style.layers.Layer
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick

class MainActivity : BaseActivity(), MapboxMap.OnMapLongClickListener {
    override fun onMapLongClick(point: LatLng) {
        mapboxMap?.addMarker(MarkerOptions()
                .position(point)
                .icon(IconFactory.getInstance(this).fromResource(R.mipmap.edit_maker_red_uncollected))
        )
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
            }
            deleteBt.onClick {
                //call methods to delete points
                mapboxMap?.removeMarker(marker)

            }
            v
        }


    }


}
