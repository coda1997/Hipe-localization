package com.example.overl.hipe.service

import android.graphics.Point
import retrofit2.http.GET
import retrofit2.http.Path


interface SyncService{

    @GET("/point/{time}")
    fun getPoints(@Path("time") time:Long):List<Point>


}