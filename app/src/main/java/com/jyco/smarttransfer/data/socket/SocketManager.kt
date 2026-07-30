package com.jyco.smarttransfer.data.socket

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
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

    private var writer : BufferedWriter? = null
    private var reader : BufferedReader? = null

    fun setSocket(socket: Socket) {
        this.socket = socket

    }

    fun setServerSocket(serverSocket: ServerSocket){
        this.serverSocket = serverSocket
    }

    fun getSocket() : Socket? = socket
    fun initializedSocket(connectedSocket: Socket){
        socket = connectedSocket
        reader = BufferedReader(InputStreamReader(connectedSocket.getInputStream()))
        writer = BufferedWriter(OutputStreamWriter(connectedSocket.getOutputStream()))
    }

    suspend fun startServerSocket() : Boolean = withContext(Dispatchers.IO){
        close()
        try{
            serverSocket = ServerSocket(PORT)
            var acceptedSocket = serverSocket?.accept() ?: return@withContext false
            initializedSocket(acceptedSocket)
            Log.d(TAG, "Client socket connected")
            true
        }catch (e: SocketException){
            Log.d(TAG, "Server socket closed: ${e.message}")
            false
        }catch(e: IOException){
            Log.e(TAG, "Server socket failed ", e)
            false
        }

    }

    suspend fun startClientSocket(addr : String) : Boolean = withContext(Dispatchers.IO){
        close()
        try {
            var connectedSocket = Socket().apply {
                connect(InetSocketAddress(addr, PORT), TIME_OUT)
            }
            initializedSocket(connectedSocket)
            Log.d(TAG, "Connected to server: $addr")
            true
        }catch (e:SocketException){
            Log.d(TAG, "Client socket closed ${e.message}")
            false
        }catch (e:IOException){
            Log.e(TAG, "Socket connect failed", e)
            false
        }

    }

    suspend fun sendLines(message : String)= withContext(Dispatchers.IO) {
        runCatching {
            val currentWriter = writer ?: throw IllegalStateException("Socket writer is not initialized")
            currentWriter.write(message)
            currentWriter.newLine()
            currentWriter.flush()
        }.onFailure { throwable->
            Log.e(TAG, "sendLine failed", throwable)
        }.onSuccess {
            Log.d(TAG, "Sent: $message")
        }
    }
    suspend fun readLines()=withContext(Dispatchers.IO){
        runCatching {
            val currentReader = reader ?: throw IllegalStateException("Socket reader is not initialized")
            currentReader.readLine()?: throw IOException("Remote device closed the socket")

        }.onFailure { throwable ->
            Log.d(TAG, "read line failed : ", throwable)
        }.onSuccess { message ->
            Log.d(TAG, "received : $message")
        }

    }
    fun isConncted():Boolean{
       return socket?.let{
           it.isConnected && !it.isClosed
       } == true
    }

    fun close(){
        runCatching { socket?.close() }
        runCatching { serverSocket?.close()  }
        runCatching { reader?.close() }
        runCatching { writer?.close()}

        reader = null
        writer = null
        socket = null
        serverSocket = null
        Log.d(TAG, "Sockets closed")
    }
}