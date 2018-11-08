package com.example.overl.hipe.ui

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Color
import android.os.AsyncTask
import android.text.TextUtils
import com.mapbox.geojson.gson.GeoJsonAdapterFactory
import com.mapbox.mapboxsdk.annotations.PolylineOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Created by overl on 2018/11/7.
 */
class GeoJsonUtils(val context: Context,val mapboxMap: MapboxMap) : AsyncTask<Void, Void, List<LatLng>>() {



    var filePath :String = ""

    override fun doInBackground(vararg params: Void?): List<LatLng> {
        val points = arrayListOf<LatLng>()

        val inputStream = context.assets.open(filePath)
        val bufferedReader = BufferedReader(InputStreamReader(inputStream,Charsets.UTF_8))
        val stringBuffer = StringBuffer()
        var cp :Int
        do {
            cp=bufferedReader.read()
            if (cp==-1){
                break
            }else{
                stringBuffer.append(cp.toChar())
            }
        }while (true)

        val json = JSONObject(stringBuffer.toString())
        val features = json.getJSONArray("features")
        val feature = features.getJSONObject(0)
        val geometry = feature.getJSONObject("geometry")
        if (geometry!=null){
            val type = geometry.getString("type")
            if (!TextUtils.isEmpty(type)&&type.equals("LineString",ignoreCase = true)){
                val coords = geometry.getJSONArray("coordinates")
                for (i in 0 .. coords.length()){
                    val coord = coords.getJSONArray(i)
                    val latLng = LatLng(coord.getDouble(1),coord.getDouble(0))
                    points.add(latLng)
                }
            }
        }


        return points

    }

    override fun onPostExecute(result: List<LatLng>?) {
        super.onPostExecute(result)
        if (result!=null&& result.isNotEmpty()){
            mapboxMap.addPolyline(PolylineOptions()
                    .addAll(result.toList())
                    .color(Color.parseColor("#3bb2d0"))
                    .width(2f))
        }

    }

    public fun constructMapInBackgound():List<LatLng>{
        TODO()
        return listOf()
    }
}