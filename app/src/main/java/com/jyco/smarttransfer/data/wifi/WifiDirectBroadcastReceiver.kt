package com.jyco.smarttransfer.data.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager

class WifiDirectBroadcastReceiver(
    private val wifiDirectManager: WifiDirectManager,
    private val onPeerChanged: ()->Unit
): BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when(intent?.action){
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                onPeerChanged()
            }
        }
    }

}