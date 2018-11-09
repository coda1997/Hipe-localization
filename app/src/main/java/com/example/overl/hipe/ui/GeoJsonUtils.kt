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
class GeoJsonUtils(private val context: Context, val mapboxMap: MapboxMap) : AsyncTask<Void, Void, List<LatLng>>() {


    var filePath: String = ""

    override fun doInBackground(vararg params: Void?): List<LatLng> {

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
        Log.i("geojson type", "total num is ${info.length()}")
        (0 until info.length())
                .map { info.getJSONObject(it) }
                .map { it.getString("type") }
                .forEach { Log.i("geojson type", "type name is $it") }


        //draw multi polygons
        drawPolygon(info.getJSONObject(1))

        //draw boundary
        drawBoundary(info.getJSONObject(3))

//          drawDoor(info.getJSONObject(2))
        return listOf()

    }


    private fun drawPolygon(root: JSONObject) {
        val featureCollection = root.getJSONObject("features")
        val features = featureCollection.getJSONArray("features")
        for (i in 0 until features.length()) {
            val polygon = features.getJSONObject(i)
            val geometry = polygon.getJSONObject("geometry")
            val ress = arrayListOf<LatLng>()
            geometry?.let {
                val type = geometry.getString("type")
                if (type.isNotEmpty() && type.equals("MultiPolygon", ignoreCase = true)) {
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
    }

    private fun drawDoor(root: JSONObject) {
        val featrueCollection = root.getJSONObject("features")
        val features = featrueCollection.getJSONArray("features")
        for (i in 0 until features.length()) {
            val door = features.getJSONObject(i)
            val doorGeomerty = door.getJSONObject("geometry")
            val points = arrayListOf<LatLng>()
            doorGeomerty?.let {
                val type = doorGeomerty.getString("type")
                if (type == "Polygon") {
                    val coords = doorGeomerty.getJSONArray("coordinates").getJSONArray(0)
                    for (j in 0 until coords.length()) {
                        val coord = coords.getJSONArray(j)
                        val latLog = LatLng(coord.getDouble(1), coord.getDouble(0))
                        if (points.find { it.isSamePoint(latLog) } == null) {
                            points.add(latLog)
                        }
                    }

                }
            }
            context.runOnUiThread { mapboxMap.addPolyline(PolylineOptions().addAll(points.toList()).color(Color.RED)) }
        }
    }

    private fun drawBoundary(root: JSONObject) {
        val featrueCollection = root.getJSONObject("features")
        val features = featrueCollection.getJSONArray("features")
        for (i in 0 until features.length()) {
            val door = features.getJSONObject(i)
            val doorGeomerty = door.getJSONObject("geometry")
            val points = arrayListOf<LatLng>()

            doorGeomerty?.let {
                val type = doorGeomerty.getString("type")
                if (type == "Polygon") {
                    val coords = doorGeomerty.getJSONArray("coordinates").getJSONArray(0)
                    for (j in 0 until coords.length()) {
                        val coord = coords.getJSONArray(j)
                        val latLog = LatLng(coord.getDouble(1), coord.getDouble(0))
                            points.add(latLog)

                    }
                }
            }

            context.runOnUiThread { mapboxMap.addPolygon(PolygonOptions().addAll(points.toList()).fillColor(Color.DKGRAY)) }
        }

    }

    private fun LatLng.isSamePoint(other: LatLng) = latitude == other.latitude && longitude == other.longitude


}