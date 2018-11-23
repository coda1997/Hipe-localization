package com.example.overl.hipe.service

import com.example.overl.hipe.Data
import com.example.overl.hipe.Point
import com.example.overl.hipe.RespondBody
import io.reactivex.Single
import retrofit2.http.*


interface SyncService{

    @GET("/point/{time}")
    fun getPoints(@Path("time") time:Long,@Query("floor") floor: Int):Single<Data>

    @PATCH("/point/{time}")
    fun updatePoint(@Path("time") time:Long,@Body point: Point):Single<Data>

    @DELETE("/point/{time}")
    fun deletePoint(@Path("time") time:Long):Single<Data>

    @GET("/point")
    fun getAllPoints(@Query("floor") floor:Int):Single<Data>

    @POST("/point")
    fun addPoint(@Body point: Point):Single<Data>

}