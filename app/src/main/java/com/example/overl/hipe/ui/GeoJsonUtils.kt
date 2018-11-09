package com.example.overl.hipe.ui

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Color
import android.os.AsyncTask
import android.provider.CalendarContract
import android.text.TextUtils
import android.util.Log
import com.mapbox.geojson.gson.GeoJsonAdapterFactory
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.annotations.PolygonOptions
import com.mapbox.mapboxsdk.annotations.PolylineOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import org.jetbrains.anko.runOnUiThread
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Created by overl on 2018/11/7.
 */
class GeoJsonUtils(val context: Context, val mapboxMap: MapboxMap) : AsyncTask<Void, Void, List<LatLng>>() {


    var filePath: String = ""

    override fun doInBackground(vararg params: Void?): List<LatLng> {
        val points = arrayListOf<LatLng>()

        val inputStream = context.assets.open(filePath)
        val bufferedReader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val stringBuffer = StringBuffer()
        var cp: Int
        do {
            cp = bufferedReader.read()
            if (cp == -1) {
                break
            } else {
                stringBuffer.append(cp.toChar())
            }
        } while (true)

        val json = JSONObject(stringBuffer.toString())
        val info = json.getJSONArray("info")
        Log.i("geojson type","total num is ${info.length()}")
        for (index in 0 until info.length()){
            val type = info.getJSONObject(index)
            val typeName = type.getString("type")
            Log.i("geojson type","type name is $typeName")

        }
        //draw single points
//        val unitPoint = info.getJSONObject(0)
//        val featureCollection = unitPoint.getJSONObject("features")
//        val features = featureCollection.getJSONArray("features")
//        for(i in 0 until  features.length()){
//            val feature = features.getJSONObject(i)
//            val geometry = feature.getJSONObject("geometry")
//            if (geometry != null) {
//                val type = geometry.getString("type")
//                if (!TextUtils.isEmpty(type) && type.equals("Point", ignoreCase = true)) {
//                    val coords = geometry.getJSONArray("coordinates")
//                    val latLng = LatLng(coords.getDouble(1), coords.getDouble(0))
//                    points.add(latLng)
//
//                }
//            }
//        }

        //draw multi polygons
        val unitPoly = info.getJSONObject(1)
        val featureCollection = unitPoly.getJSONObject("features")
        val features = featureCollection.getJSONArray("features")
        for(i in 0 until features.length()){
            val polygon = features.getJSONObject(i)
            val geometry = polygon.getJSONObject("geometry")
            val ress = arrayListOf<LatLng>()
            geometry?.let {
                val type = geometry.getString("type")
                if (type.isNotEmpty()&&type.equals("MultiPolygon",ignoreCase = true)){
                    val coords = geometry.getJSONArray("coordinates")
                    val coordds = coords.getJSONArray(0).getJSONArray(0)
                    (0 until coordds.length())
                            .map { coordds.getJSONArray(it) }
                            .mapTo(ress) { LatLng(it.getDouble(1), it.getDouble(0)) }
                }
            }
            context.runOnUiThread {
                mapboxMap.addPolygon(PolygonOptions().addAll(ress.toList()).fillColor(Color.LTGRAY))
            }
        }


        return points

    }
    private fun drawPolygon(root:JSONObject){

    }




}