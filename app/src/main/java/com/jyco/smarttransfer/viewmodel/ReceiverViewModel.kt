package com.jyco.smarttransfer.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyco.smarttransfer.data.socket.AuthProtocal.AUTH_FAIL
import com.jyco.smarttransfer.data.socket.AuthProtocal.AUTH_OK
import com.jyco.smarttransfer.data.socket.AuthProtocal.generatePin
import com.jyco.smarttransfer.data.socket.AuthProtocal.parsePin
import com.jyco.smarttransfer.data.socket.SocketManager
import com.jyco.smarttransfer.data.wifi.WifiDirectManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReceiverViewModel(
    private val wifiDirectManager :WifiDirectManager,
    private val socketManager: SocketManager
) : ViewModel() {

    private val TAG = ReceiverViewModel::class.java.simpleName

    private val _devices = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val devices : StateFlow<List<WifiP2pDevice>> = _devices

    private val _msg = MutableSharedFlow<String>()
    val msg = _msg.asSharedFlow()

    private val _wifiP2pInfo = MutableStateFlow<WifiP2pInfo?>(null)
    val wifiP2pInfo : StateFlow<WifiP2pInfo?> = _wifiP2pInfo

    private val _connectionState = MutableStateFlow(ConnectionState.Idle)
    val conncetionState = _connectionState.asStateFlow()

    private val _authPin = MutableStateFlow<String?>(null)
    val authPin : StateFlow<String?> = _authPin

    private val _authError = MutableStateFlow<String?>(null)
    val authError : StateFlow<String?> = _authError

    private var isSocketStarted = false

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if(_connectionState.value != ConnectionState.Idle) {
            Log.e(TAG, "Receiver : startDiscovery : failed - $_connectionState.value")
            return
        }

        _connectionState.value = ConnectionState.Discovering

        wifiDirectManager.discoverPeers(
            onSuccess = {
                viewModelScope.launch {
                    _msg.emit("Receiver : Searching for nearby devices...")
                }

            },
            onFailure = { reason ->
                _connectionState.value = ConnectionState.Failed
                val errorMessage = reason.toWifiP2pReasonMessage()
                viewModelScope.launch { _msg.emit(errorMessage) }
            }
        )
    }

    @SuppressLint("MissingPermission")
    fun requestPeers() {
        wifiDirectManager.requestPeers { peers ->
            updatePeers(peers)
            viewModelScope.launch {
                _msg.emit("Receiver : Pear changed")
            }

        }
    }

    fun updatePeers(peers: Collection<WifiP2pDevice>) {
        _devices.value = peers.toList()
        viewModelScope.launch {
            _msg.emit("Receiver : Found ${peers.size} devices")
        }
    }

    fun getWifiDirectManager() = wifiDirectManager

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    fun connectToDevice(device:WifiP2pDevice){

        _connectionState.value = ConnectionState.P2PConnecting

        wifiDirectManager.connectToDevice(
            device = device,
            onSuccess = {
                viewModelScope.launch {
                    _msg.emit("Receiver : Connection request sent to ${device.deviceName}")
                }
            },
            onFailure = { reason ->
                _connectionState.value = ConnectionState.Failed
                viewModelScope.launch {
                    _msg.emit("Receiver : Connection failed: ${reason.toWifiP2pReasonMessage()}")
                    delay(1000)

                    requestConnectionInfo()
                }
            }
        )
    }

    fun requestConnectionInfo(){
        wifiDirectManager.requestConnectionInfo {info->
            _wifiP2pInfo.value = info
            handleConnectionInfo(info)
        }
    }
    fun handleConnectionInfo(info : WifiP2pInfo){
        viewModelScope.launch {

            if (!info.groupFormed) {
                Log.d(TAG, "Receiver : handleConnectionInfo - Connection info received, but group is not formed yet")
                _msg.emit("Receiver : Connection info received, but group is not formed yet.")
                return@launch
            }

            if(isSocketStarted) {
                Log.d(TAG, "Reciever : handleConnectionInfo - Socket already started - ignore")
                return@launch
            }

            isSocketStarted = true

            onP2PConnected()

            if (info.isGroupOwner) {
                var message = "Receiver : GroupOwner"
                val socket = socketManager.startServerSocket()
                if (socket != null) {
                    message += " - Server"
                    onSocketConnected(message)
                    startReceivierAuthentication()
                } else {
                    message += " - Server socket stopped"
                    onSocketConnectionFailed(message)
                }
            } else {
                var message = "Receiver : Client"
                val hostAddress = info.groupOwnerAddress?.hostAddress
                if (hostAddress != null) {
                    message += " - $hostAddress"
                    onSocketConnected(message)
                    val socket = socketManager.startClientSocket(hostAddress)
                    if (socket != null) {
                        onSocketConnected(message)
                        startReceivierAuthentication()
                    } else {
                        message += " - Socket stopped"
                        onSocketConnectionFailed(message)
                    }
                } else {
                    message += " - Unknown IP"
                    onSocketConnectionFailed(message)
                }
            }
        }
    }

    private fun Int.toWifiP2pReasonMessage():String{
        return when(this){
            WifiP2pManager.P2P_UNSUPPORTED->"Receiver : Wi-Fi Direct not supported"
            WifiP2pManager.BUSY ->"Receiver : Wi-Fi Direct is busy"
            WifiP2pManager.ERROR -> "Receiver : Internal error"
            else -> "Receiver : Unknown error: $this"
        }
    }

    override fun onCleared() {
        super.onCleared()
        socketManager.close()
        _connectionState.value = ConnectionState.Idle
    }

    private fun startReceivierAuthentication(){

        if(_connectionState.value != ConnectionState.Authenticating ){
            Log.e(TAG, "Receiver - startReceivierAuthentication - not in Auth - ${_connectionState.value.name}")
            return
        }
        val pin = generatePin()
        _authPin.value = pin

        Log.d(TAG, "Receiver : authentication started")
        viewModelScope.launch {
            val message = socketManager.readLines().getOrElse { exception: Throwable ->
                _connectionState.value = ConnectionState.Failed
                Log.e(TAG, exception.toString())
                _authError.value = "Failed to receive authentication pin"
                return@launch
            }
            val parsedPin = parsePin(message)
            if(parsedPin.equals(pin)){
                handleAuthSuccess(message)
            }else{
                handleAuthFailue(parsedPin)
            }

 /*           val message = socketManager.readLines().getOrNull()
            parsePin(message)?.let{
                if(it == authPin.value) {
                    handleAuthSuccess(it)
                }else{
                    handleAuthFailue(it)
                }
            } ?: run {
                _authError.value = "Failed to receive authentication request"
                //_connectionState.value = ConnectionState.Failed
            }

  */
        }

    }
    private suspend fun handleAuthSuccess(message : String){
        val result = socketManager.sendLines(AUTH_OK)
        if(result.isSuccess){
            _authPin.value = null
            _authError.value = null
            _connectionState.value = ConnectionState.Authenticated
            Log.d(TAG, "Receiver : handleAuthSuccess : authentication Success")
        }else{
            val message = "fail to send result of auth_ok"
            _authError.value = message
            _connectionState.value = ConnectionState.Failed
            Log.e(TAG, "Receiver : handleAuthSuccess : $message")
        }
    }
    private suspend fun handleAuthFailue(message : String){
        socketManager.sendLines(AUTH_FAIL)
        val errorMessage = "Received pin is {$message}, It should be {${_authPin.value}}"
        _authError.value = errorMessage
        _connectionState.value = ConnectionState.Failed
        Log.e(TAG, "Receiver : pin authentication Failed - $errorMessage")


    }
    fun resetSession(onComplete: () -> Unit){
        Log.d(TAG, "Receiver : reset Session")
        socketManager.close()
        wifiDirectManager.resetConnection {
            Log.d(TAG, "Receiver : Wi-Fi Direct session reset")
            onComplete()
        }
        isSocketStarted = false
        _authPin.value = null
        _authError.value = null
        _devices.value = emptyList()
        _wifiP2pInfo.value = null
        _connectionState.value = ConnectionState.Idle
    }
    private fun onP2PConnected(){
        _connectionState.value = ConnectionState.P2PConnected
        Log.d(TAG, "Receiver : onP2PConnected, starting socket")

        startSocketConnection()
    }
    private fun startSocketConnection(){
        _connectionState.value = ConnectionState.SocketConnecting
    }
    private suspend fun onSocketConnected(message : String){
        _connectionState.value = ConnectionState.SocketConnected
        val logMessage = message+" - onSocketConnected"
        _msg.emit( "$logMessage")
        Log.d(TAG, "$logMessage")
        _authPin.value = ""
        _authError.value = null
        _connectionState.value = ConnectionState.Authenticating
    }
    private suspend fun onSocketConnectionFailed(message : String){
        _connectionState.value = ConnectionState.Failed
        val logMessage = message+" - onSocketConnectionFailed"
        _msg.emit("$logMessage")
        Log.d(TAG, "$logMessage")
    }
}