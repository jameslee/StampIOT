package com.example.stampiot.ui.main

import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

class PlcSocket {
    suspend fun sendComm(plcUrl: String, plcPort: Int, command: String) {
        Socket(plcUrl, plcPort).use { socket ->
            socket.soTimeout = 500
            socket.keepAlive = true
            socket.getOutputStream().use { it ->
                it.write(command.toByteArray())
                HexUtils.hexStringToBytes(command)?.forEach {
                    println(it)
                }

                socket.getInputStream().use { inputStream ->
                    var buf = ByteArray(32)
                    inputStream.read(buf)
                    buf.forEach {b-> println(b) }
                }

            }
        }
   }
}


