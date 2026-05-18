package com.jyco.smarttransfer.data.wifi

import android.Manifest
import android.content.Context
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import androidx.annotation.RequiresPermission

class WifiDirectManager(private val context : Context) {
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
}