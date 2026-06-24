package com.jyco.smarttransfer.viewmodel

import android.Manifest
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import androidx.annotation.RequiresPermission
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

    private val _devices = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val devices : StateFlow<List<WifiP2pDevice>> = _devices

    private val _msg = MutableSharedFlow<String>()
    val msg = _msg.asSharedFlow()

    private val _wifiP2pInfo = MutableStateFlow<WifiP2pInfo?>(null)
    val wifiP2pInfo : StateFlow<WifiP2pInfo?> = _wifiP2pInfo

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    fun startDiscovery(){
        wifiDirectManager.discoverPeers(
            onSuccess = {
                viewModelScope.launch {
                    _msg.emit("Searching for nearby devices...")
                }
            } ,
            onFailure = { reason ->
                val errormsg = when(reason){
                    WifiP2pManager.P2P_UNSUPPORTED -> {
                        "Wi-Fi Direct not supported"
                    }
                    WifiP2pManager.BUSY ->{
                        "Wi-Fi Direct is busy"
                    }
                    WifiP2pManager.ERROR ->{
                        "Unknown Wi-Fi Direct error"
                    }
                    else -> {
                        "Discovery Failed"
                    }
                }
                viewModelScope.launch {
                    _msg.emit(errormsg)
                }
            }
        )
    }
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    fun requestPeers(){
        wifiDirectManager.requestPeers {
            updatePeers(it)
            viewModelScope.launch {
                _msg.emit("Peer changed")
            }
        }
    }
    fun updatePeers(peers : Collection<WifiP2pDevice>){
        _devices.value = peers.toList()
        viewModelScope.launch {
            _msg.emit("Found ${peers.size} devices")
        }

    }
    fun getWifiDirectManager() = wifiDirectManager

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    fun connectToDevice(device:WifiP2pDevice){
        wifiDirectManager.connectToDevice(device,
            onSuccess = {
                viewModelScope.launch { _msg.emit("Connection request sent to ${device.deviceName}") }
            },
            onFailure = {reason->
                viewModelScope.launch { _msg.emit("Connection failed: ${reason.toWifiP2pReasonMessage()}") }
            })
    }
    fun requestConnectionInfo( ){
        wifiDirectManager.requestConnectionInfo { info->
            _wifiP2pInfo.value = info
            viewModelScope.launch {

                if(info.groupFormed){
                    val role = if(info.isGroupOwner) "Group Owner" else "Client"
                    val ip = info.groupOwnerAddress?.hostAddress ?: "Unknown IP"
                    _msg.emit("Connected as $role, Group Owner IP is $ip")
                }else{
                    _msg.emit("Connection info received, but group is not formed yet.")
                }
            }
        }
    }

    private fun Int.toWifiP2pReasonMessage():String{
        return when(this){
            WifiP2pManager.P2P_UNSUPPORTED ->"Wi-Fi Direct not supported"
            WifiP2pManager.BUSY ->"Wi-Fi Direct is busy"
            WifiP2pManager.ERROR -> "Internal error"
            else ->  "Unknown error: $this"
        }
    }

    override fun onCleared() {
        super.onCleared()
        socketManager.close()
    }
}