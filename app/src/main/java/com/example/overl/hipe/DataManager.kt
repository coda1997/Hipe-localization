package com.example.overl.hipe

/**
 * Created by overl on 2018/11/7.
 */
interface DataManager {

    fun doProcess(fId:String,longitude :Double,latitude:Double,action:Int):String
}