package com.example.overl.hipe

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapView
import org.jetbrains.anko.find

class MainActivity : AppCompatActivity() {

    private var mapView: MapView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, "pk.eyJ1IjoiZGFkYWNoZW4iLCJhIjoiY2puem1xZXl4MDNtZDNybzlhdGs1OHpxbCJ9.niu0Pf8Q7WOBMiwaxr4yNQ");
        setContentView(R.layout.activity_main)
        mapView = find(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
    }

    //override some methods
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
