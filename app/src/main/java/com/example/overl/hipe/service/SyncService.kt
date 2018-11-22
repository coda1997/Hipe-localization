package com.example.overl.hipe.service

import com.example.overl.hipe.Point
import com.example.overl.hipe.RespondBody
import retrofit2.http.*


interface SyncService{

    @GET("/point/{time}")
    fun getPoints(@Path("time") time:Long):RespondBody

    @PATCH("/point/{time}")
    fun updatePoint(@Path("time") time:Long, point: Point):RespondBody

    @DELETE("/point/{time}")
    fun deletePoint(@Path("time") time:Long):RespondBody

    @GET("/point")
    fun getAllPoints():RespondBody

    @POST("/point")
    fun addPoint(point: Point):RespondBody

}