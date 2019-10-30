package com.example.overl.hipe.service

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

private fun getRetrofit() = Retrofit.Builder()
        .baseUrl("http://203.207.224.107:33001")
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .client(OkHttpClient())
        .build()

fun getSyncService():SyncService{
    return getRetrofit().create(SyncService::class.java)
}
