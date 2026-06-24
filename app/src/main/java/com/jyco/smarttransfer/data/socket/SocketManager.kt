package com.jyco.smarttransfer.data.socket

import java.net.ServerSocket
import java.net.Socket

class SocketManager() {
    private var socket:Socket? = null
    private var serverSocket:ServerSocket? = null

    fun setSocket(socket: Socket) {
        this.socket = socket

    }

    fun setServerSocket(serverSocket: ServerSocket){
        this.serverSocket = serverSocket
    }

    fun getSocket() : Socket? = socket

    fun close(){
        socket?.close()
        serverSocket?.close()

        socket = null
        serverSocket = null
    }
}