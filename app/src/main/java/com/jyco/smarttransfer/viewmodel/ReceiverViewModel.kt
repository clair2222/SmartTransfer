package com.jyco.smarttransfer.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.net.wifi.p2p.WifiP2pDevice
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyco.smarttransfer.data.wifi.WifiDirectManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class ReceiverViewModel(private val wifiDirectManager :WifiDirectManager
) : ViewModel() {

    private val _devices = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val devices : StateFlow<List<WifiP2pDevice>> = _devices

    private val _msg = MutableSharedFlow<String>()
    val msg = _msg.asSharedFlow()

 @SuppressLint("MissingPermission")
 fun startDiscovery(){
        wifiDirectManager.discoverPeers(
            onSuccess = { } ,
            onFailure = { }
        )
    }

@SuppressLint("MissingPermission")
fun requestPeers(){
    wifiDirectManager.requestPeers { peers ->
        _devices.value = peers.toList()
         viewModelScope.launch{
             _msg.emit("Pear changed")
         }

    }
}
fun updatePeers(peers:Collection<WifiP2pDevice>){
        _devices.value = peers.toList()
    }
fun getWifiDirectManager() = wifiDirectManager
}