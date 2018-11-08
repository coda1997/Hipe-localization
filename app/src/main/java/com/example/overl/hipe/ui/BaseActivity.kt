package com.example.overl.hipe.ui

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.example.overl.hipe.R
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapView
import org.jetbrains.anko.find

/**
 * Created by overl on 2018/11/7.
 */
open class BaseActivity: AppCompatActivity() {
    protected var mapView: MapView? = null
    //override some methods
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, "pk.eyJ1IjoiZGFkYWNoZW4iLCJhIjoiY2pvNnVuazVoMGtjajN2bXh0dDQ1YmFoZiJ9.wTJh2IPcUimFP1R7w_qWfA")
        setContentView(R.layout.activity_main_ui)
        mapView = find(R.id.mapView)

        mapView?.onCreate(savedInstanceState)
    }


    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        if (outState != null) {
            mapView?.onSaveInstanceState(outState)

        }
    }
}