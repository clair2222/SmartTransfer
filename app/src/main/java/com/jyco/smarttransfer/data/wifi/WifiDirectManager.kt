package com.jyco.smarttransfer.data.wifi

import android.Manifest
import android.content.Context
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import androidx.annotation.RequiresPermission
import com.jyco.smarttransfer.ui.permission.getWifiPermissions
import com.jyco.smarttransfer.ui.permission.hasPermissions

class WifiDirectManager(private val context : Context) {
    private val TAG = WifiDirectManager::class.java.simpleName
    private val manager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val channel = manager.initialize(context,
        context.mainLooper,
        null)
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    fun discoverPeers(onSuccess:()->Unit, onFailure:(Int)->Unit){
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener{
            override fun onFailure(reason: Int) {
                onFailure(reason)
            }

            override fun onSuccess() {
                onSuccess()
            }

        })
    }
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    fun requestPeers(onPeerReceived:(Collection<WifiP2pDevice>)->Unit){
        manager.requestPeers(channel) {peers ->
            onPeerReceived(peers.deviceList)
         }
    }

    fun getManager() = manager
    fun getChannel() = channel

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    fun connectToDevice(device: WifiP2pDevice, onSuccess: () -> Unit, onFailure: (Int) -> Unit){
        if(!hasPermissions(context, getWifiPermissions())){
            onFailure(WifiP2pManager.ERROR)
            return
        }

        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
        }

        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onFailure(reason: Int) {
                onFailure(reason)
            }

            override fun onSuccess() {
                onSuccess()
            }

        })
    }

    fun requestConnectionInfo(onConnectionInfoAvailable: (WifiP2pInfo)->Unit){
        manager.requestConnectionInfo(channel){info ->
            onConnectionInfoAvailable(info)
        }

    }
}