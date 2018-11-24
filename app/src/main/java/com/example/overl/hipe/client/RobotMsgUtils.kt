package com.example.overl.hipe.client

import java.io.PrintWriter
import java.net.Socket

/**
 * Created by overl on 2018/11/22.
 */

fun sendMsg(message:String,ip:String,prot:Int){
    val socket = Socket(ip,prot)
    val output = socket.getOutputStream()
    val writer = PrintWriter(output)
    writer.write(message)
    writer.flush()
    socket.shutdownOutput()
    socket.close()
}


