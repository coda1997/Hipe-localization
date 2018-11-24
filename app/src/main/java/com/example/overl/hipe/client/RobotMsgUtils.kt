package com.example.overl.hipe.client

import java.io.PrintWriter
import java.net.Socket

/**
 * Created by overl on 2018/11/22.
 */

var socket:Socket?=null

fun sendMsg(message:String,ip:String,prot:Int){
    if (socket!=null){
            val output = socket!!.getOutputStream()
            val writer = PrintWriter(output)
            writer.write(message)
            writer.flush()
    }else{
        socket = Socket(ip,prot)
    }

}


