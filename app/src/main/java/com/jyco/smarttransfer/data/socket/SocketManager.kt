package com.jyco.smarttransfer.data.socket

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

class SocketManager() {

    companion object{
        private val TAG = SocketManager::class.java.simpleName
        const val PORT = 8988
        const val TIME_OUT = 10_000
    }
    private var socket:Socket? = null
    private var serverSocket:ServerSocket? = null

    fun setSocket(socket: Socket) {
        this.socket = socket

    }

    fun setServerSocket(serverSocket: ServerSocket){
        this.serverSocket = serverSocket
    }

    fun getSocket() : Socket? = socket

    suspend fun startServerSocket() = withContext(Dispatchers.IO){
        close()
        try{
            serverSocket = ServerSocket(PORT)
            socket = serverSocket?.accept()
            socket
        }catch (e: SocketException){
            Log.d(TAG, "Server socket closed: ${e.message}")
            null
        }catch(e: IOException){
            Log.e(TAG, "Server socket failed ", e)
            null
        }

    }

    suspend fun startClientSocket(addr : String) = withContext(Dispatchers.IO){
        close()
        try {
            socket = Socket().apply {
                connect(InetSocketAddress(addr, PORT), TIME_OUT)
            }
            socket
        }catch (e:SocketException){
            Log.d(TAG, "Client socket closed ${e.message}")
            null
        }catch (e:IOException){
            Log.e(TAG, "Socket connect failed", e)
            null
        }

    }

    suspend fun sendLines(message : String)= withContext(Dispatchers.IO){
        socket?.getOutputStream()
            ?.bufferedWriter()
            ?.use {writer ->
                writer.write(message)
                writer.newLine()
                writer.flush()
            }
    }
    suspend fun readLines():String?=withContext(Dispatchers.IO){
        socket?.getInputStream()
            ?.bufferedReader()
            ?.readLine()
    }

    fun close(){
        try {
            socket?.close()
        }catch (_:Exception){
        }

        try {
            serverSocket?.close()
        }catch (_:Exception){}

        socket = null
        serverSocket = null
    }
}