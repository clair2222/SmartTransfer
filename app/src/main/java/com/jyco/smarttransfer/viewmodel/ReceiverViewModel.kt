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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jyco.smarttransfer.data.socket.AuthProtocal
import com.jyco.smarttransfer.data.socket.AuthProtocal.AUTH_FAIL
import com.jyco.smarttransfer.data.socket.AuthProtocal.AUTH_OK
import com.jyco.smarttransfer.data.socket.AuthProtocal.generatePin
import com.jyco.smarttransfer.data.socket.AuthProtocal.parsePin
import com.jyco.smarttransfer.data.socket.SocketManager
import com.jyco.smarttransfer.data.wifi.WifiDirectManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.Socket

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

    private var isStartSocket = false

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if(_connectionState.value != ConnectionState.Idle) {
            Log.d(TAG, "Receiver : startDiscovery : failed - not idle")
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
                _connectionState.value = ConnectionState.P2PConnected
                viewModelScope.launch {
                    _msg.emit("Receiver : Connection request sent to ${device.deviceName}")
                }
            },
            onFailure = { reason ->
                _connectionState.value = ConnectionState.Failed
                viewModelScope.launch {
                    _msg.emit("Receiver : Connection failed: ${reason.toWifiP2pReasonMessage()}")
                }
            }
        )
    }

    fun requestConnectionInfo(){

        _connectionState.value = ConnectionState.SocketConnecting

        wifiDirectManager.requestConnectionInfo {info->
            _wifiP2pInfo.value = info
            handleConnectionInfo(info)
        }
    }
    fun handleConnectionInfo(info : WifiP2pInfo){
        viewModelScope.launch {
            if(info.groupFormed){
                if(info.isGroupOwner) {
                    Log.d(TAG, "Receiver : handleConnectionInfo : info.isGroupOwner true")
                    val socket = socketManager.startServerSocket()
                    if(socket != null){
                        _connectionState.value = ConnectionState.SocketConnected
                        _msg.emit("Receiver : Connected as Server - Group Owner")
                        Log.d(TAG, "Receiver : Connected as Server - Group Owner")
                        startReceivierAuthentication()
                    }else{
                        //_connectionState.value = ConnectionState.Failed
                        _msg.emit("Receiver : Server socket stopped - Group Owner")
                        Log.d(TAG, "Receiver : Server socket stopped - Group Owner")
                    }
                }else {
                    Log.d(TAG, "Receiver : handleConnectionInfo : info.isGroupOwner false")
                    val hostAddress = info.groupOwnerAddress?.hostAddress
                    Log.d(TAG, "Receiver : handleConnectionInfo : ${info.groupOwnerAddress}")
                    //_msg.emit("Receiver : Group Client")
                    if (hostAddress != null) {
                        //_msg.emit("Receiver : Client : Group Host IP is ${hostAddress}")
                        val socket = socketManager.startClientSocket(hostAddress)
                        if(socket != null){
                            _connectionState.value = ConnectionState.SocketConnected
                            _msg.emit("Receiver : Connected as Client : Group Host IP is ${hostAddress}")
                            startReceivierAuthentication()
                        }else{
                            _connectionState.value = ConnectionState.Failed
                            _msg.emit("Receiver : Client Socket stopped : Group Host IP is ${hostAddress}")
                        }
                    }else{
                        _msg.emit("Receiver : Client : Unknown IP")
                    }
                }
            }else{
                Log.d(TAG, "Receiver : Connection info received, but group is not formed yet")
                _msg.emit("Receiver : Connection info received, but group is not formed yet.")
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

        if(_connectionState.value == ConnectionState.Authenticating ||
            _connectionState.value == ConnectionState.Authenticated){
            return
        }
        val pin = generatePin()
        _authPin.value = pin
        _connectionState.value = ConnectionState.Authenticating

        Log.d(TAG, "Receiver : authentication started")
        viewModelScope.launch {
            val message = socketManager.readLines().getOrElse { exception: Throwable ->
                _connectionState.value = ConnectionState.Failed
                Log.e(TAG, exception.toString())
                _authError.value = "Failed to receive authentication pin"
                return@launch
            }
            if(message == pin){
                handleAuthSuccess(message)
            }else{
                handleAuthFailue(message)
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
        val errorMessage = "Received pin is {$message}, It should be {$_authPin}"
        _authError.value = errorMessage
        _connectionState.value = ConnectionState.Failed
        Log.e(TAG, "Receiver : pin authentication Failed - $errorMessage")


    }
}