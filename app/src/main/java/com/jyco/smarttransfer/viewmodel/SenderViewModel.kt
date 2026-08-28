package com.jyco.smarttransfer.viewmodel

import android.Manifest
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyco.smarttransfer.data.socket.AuthProtocal
import com.jyco.smarttransfer.data.socket.SocketManager
import com.jyco.smarttransfer.data.wifi.WifiDirectManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SenderViewModel(
    private val wifiDirectManager : WifiDirectManager,
    private val socketManager: SocketManager
) : ViewModel() {
    private val TAG = SenderViewModel::class.java.simpleName

    private val _devices = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val devices : StateFlow<List<WifiP2pDevice>> = _devices

    private val _msg = MutableSharedFlow<String>()
    val msg = _msg.asSharedFlow()

    private val _wifiP2pInfo = MutableStateFlow<WifiP2pInfo?>(null)
    val wifiP2pInfo : StateFlow<WifiP2pInfo?> = _wifiP2pInfo
    
    private val _connectionState = MutableStateFlow(ConnectionState.Idle)
    val connectionState : StateFlow<ConnectionState> = _connectionState

    private var isSocketStarted = false

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    fun startDiscovery(){
        _connectionState.value = ConnectionState.Discovering
        wifiDirectManager.discoverPeers(
            onSuccess = {
                viewModelScope.launch {
                    _msg.emit("Sender : Searching for nearby devices...")
                }
            } ,
            onFailure = { reason ->
                _connectionState.value = ConnectionState.Failed
                viewModelScope.launch {
                    _msg.emit(reason.toWifiP2pReasonMessage())
                }
            }
        )
    }
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    fun requestPeers(){
        wifiDirectManager.requestPeers {
            updatePeers(it)
            viewModelScope.launch {
                _msg.emit("Sender : Peer changed")
            }
        }
    }
    private fun updatePeers(peers : Collection<WifiP2pDevice>){
        _devices.value = peers.toList()
        viewModelScope.launch {
            _msg.emit("Sender : Found ${peers.size} devices")
        }

    }
    fun getWifiDirectManager() = wifiDirectManager

    @RequiresApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    fun requestConnectionInfo( ){

        wifiDirectManager.requestConnectionInfo { info->
            _wifiP2pInfo.value = info
            handleConnectionInfo(info)
        }
    }
    private val _enteredPin = MutableStateFlow<String>("")
    val enteredPin : StateFlow<String> = _enteredPin
    private val _authError = MutableStateFlow<String?>(null)
    val authError : StateFlow<String?> = _authError


    fun updatePin(enteredPin : String){
        _enteredPin.value = enteredPin
    }
    fun submitPin(){
        if(_connectionState.value != ConnectionState.Authenticating){
            Log.d(TAG, "Sender : submitPin : not in Authentication - $_connectionState.value")
            return
        }

        viewModelScope.launch {
            val request = AuthProtocal.createAuthRequest(_enteredPin.value)
            val result = socketManager.sendLines(request)

            if(result.isFailure){
                Log.d(TAG, "Sender : submitPin : Failed to send Pin")
                _authError.value = "Sender : failed to send Pin"
                _connectionState.value = ConnectionState.Failed
                return@launch
            }

            waitForAuthenticationResult()

        }


    }
    private suspend fun waitForAuthenticationResult(){
        val response = socketManager.readLines().getOrElse { throwable ->
            Log.e(TAG, "Failed to receive auth response", throwable)
            _authError.value = "Failed to receive auth response"
            _connectionState.value = ConnectionState.Failed
            return
        }
        when(response){
            AuthProtocal.AUTH_OK -> {
                _authError.value = null
                _connectionState.value = ConnectionState.Authenticated
                Log.d(TAG, "Sender authentication successful ")
            }
            AuthProtocal.AUTH_FAIL -> {
                _authError.value = "Incorrect Pin"
                _connectionState.value = ConnectionState.Failed
                Log.e(TAG, "Incorrect Pin: ")
            }
            else ->{
                _authError.value = "Invalid authentication response"
                _connectionState.value = ConnectionState.Failed
                Log.e(TAG, "Unknown auth response: $response")
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private fun handleConnectionInfo(info : WifiP2pInfo){
        viewModelScope.launch {

            if (!info.groupFormed) {
                Log.d(TAG, "Sender : Connection info received, but group is not formed yet.")
                _msg.emit("Sender : Connection info received, but group is not formed yet.")
                return@launch
            }
            if(isSocketStarted){
                Log.d(TAG, "Sender : Socket Already started - ignore")
                return@launch
            }

            isSocketStarted = true

            onP2PConnected()

            if (info.isGroupOwner) {
                val server = socketManager.startServerSocket()
                var message = "Sender : GroupOwner"
                if (server != null) {
                    message += " - Server"
                    onSocketConnected(message)
                } else {
                    message += " - Server socket stopped"
                    onSocketConnectionFailed(message)
                }
            } else {
                val hostAddress = info.groupOwnerAddress?.hostAddress
                var message = "Sender : Client"
                if (hostAddress != null) {
                    val client = socketManager.startClientSocket(hostAddress)
                    message += " - $hostAddress"
                    if (client != null) {
                        onSocketConnected(message)
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
            WifiP2pManager.P2P_UNSUPPORTED ->"Sender : Wi-Fi Direct not supported"
            WifiP2pManager.BUSY ->"Sender : Wi-Fi Direct is busy"
            WifiP2pManager.ERROR -> "Sender : Internal error"
            else ->  "Sender : Unknown error: $this"
        }
    }

    override fun onCleared() {
        super.onCleared()
        socketManager.close()
        _connectionState.value = ConnectionState.Idle
    }

    fun resetSession(onComplete  : ()->Unit){
        Log.d(TAG, "Sender : reset Session")
        socketManager.close()
        wifiDirectManager.resetConnection {
            Log.d(TAG, "Sender : Wi-Fi Direct session reset")
            onComplete()
        }
        isSocketStarted = false
        _enteredPin.value = ""
        _authError.value = null
        _devices.value = emptyList()
        _wifiP2pInfo.value = null
        _connectionState.value = ConnectionState.Idle
    }

    private fun onP2PConnected(){
        _connectionState.value = ConnectionState.P2PConnected
        Log.d(TAG, "Sender : onP2PConnected, starting socket")

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
        _enteredPin.value = ""
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