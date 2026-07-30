package com.jyco.smarttransfer.viewmodel

import android.Manifest
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyco.smarttransfer.data.socket.SocketManager
import com.jyco.smarttransfer.data.wifi.WifiDirectManager
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
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState : MutableStateFlow<ConnectionState> = _connectionState

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
    fun updatePeers(peers : Collection<WifiP2pDevice>){
        _devices.value = peers.toList()
        viewModelScope.launch {
            _msg.emit("Sender : Found ${peers.size} devices")
        }

    }
    fun getWifiDirectManager() = wifiDirectManager

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    fun connectToDevice(device:WifiP2pDevice){
        _connectionState.value = ConnectionState.P2PConnecting
        wifiDirectManager.connectToDevice(device,
            onSuccess = {
                _connectionState.value = ConnectionState.P2PConnected
                viewModelScope.launch { _msg.emit("Sender : Connection request sent to ${device.deviceName}") }
            },
            onFailure = {reason->
                _connectionState.value = ConnectionState.Failed
                viewModelScope.launch { _msg.emit("Sender : Connection failed: ${reason.toWifiP2pReasonMessage()}") }
            })
    }
    fun requestConnectionInfo( ){
        _connectionState.value = ConnectionState.SocketConnecting

        wifiDirectManager.requestConnectionInfo { info->
            _wifiP2pInfo.value = info
            handleConnectionInfo(info)
        }
    }

    fun handleConnectionInfo(info : WifiP2pInfo){
        viewModelScope.launch {
            if(info.groupFormed){
                if(info.isGroupOwner) {
                    Log.d(TAG, "Sender : handleConnectionInfo : info.isGroupOwner true")
                    _msg.emit("Sender : Group Owner")
                    var server = socketManager.startServerSocket()
                    if(server != null){
                        _connectionState.value = ConnectionState.SocketConnected
                        _msg.emit("Sender : Connected as Server")
                        Log.d(TAG, "Sender : Connected as Server")
                    }else{
                        _connectionState.value = ConnectionState.Failed
                        _msg.emit("Sender : Server socket stopped - Group Owner")
                        Log.d(TAG, "Sender : Server socket stopped - Group Owner")
                    }
                }else {
                    Log.d(TAG, "Sender : handleConnectionInfo : info.isGroupOwner false")
                    _msg.emit("Sender : Group Client")
                    val hostAddress = info.groupOwnerAddress?.hostAddress
                    Log.d(TAG, "Sender : handleConnectionInfo : ${info.groupOwnerAddress}")
                    if (hostAddress != null) {
                        _msg.emit("Sender : Client : Group Host IP is ${hostAddress}")
                        var client = socketManager.startClientSocket(hostAddress)
                        if(client != null){
                            _connectionState.value = ConnectionState.SocketConnected
                            _msg.emit("Sender : Connected as Client : Group Host IP is ${hostAddress}\"")
                            Log.d(TAG, "Sender : Connected as Client : Group Host IP is ${hostAddress}\"")
                        }else{
                            _connectionState.value = ConnectionState.Failed
                            _msg.emit("Sender : Client Socket stopped : Group Host IP is ${hostAddress}")
                            Log.d(TAG, "Sender : Client Socket stopped : Group Host IP is ${hostAddress}")
                        }
                    }else{
                        _msg.emit("Sender : Unknown IP")
                        Log.d(TAG, "Sender : Unknown IP")
                    }
                }
            }else{
                Log.d(TAG, "Sender : Connection info received, but group is not formed yet.")
                _msg.emit("Sender : Connection info received, but group is not formed yet.")
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
    }
}