package com.jyco.smarttransfer.viewmodel

import androidx.lifecycle.ViewModel
import com.jyco.smarttransfer.data.wifi.WifiDirectManager

class SenderViewModel(private val wifiDirectManager : WifiDirectManager
) : ViewModel() {

    fun startDiscovery(){
        wifiDirectManager.startDiscovery()
    }
}