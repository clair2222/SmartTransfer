package com.jyco.smarttransfer.ui.screen

import android.annotation.SuppressLint
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.jyco.smarttransfer.data.wifi.WifiDirectBroadcastReceiver
import com.jyco.smarttransfer.ui.common.ShowResult
import com.jyco.smarttransfer.ui.common.getPermissionErrorMessage
import com.jyco.smarttransfer.ui.permission.RequestPermissions
import com.jyco.smarttransfer.ui.permission.getWifiPermissions
import com.jyco.smarttransfer.ui.permission.openAppSettings
import com.jyco.smarttransfer.viewmodel.ReceiverViewModel
import org.koin.androidx.compose.koinViewModel

@SuppressLint("MissingPermission")
@Composable
fun ReceiverScreen(navController: NavController,
                   viewModel : ReceiverViewModel = koinViewModel()
){
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var requestPermission by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null)}
    var requestKey by remember{ mutableIntStateOf(0)}
    var shouldCheckOnResume by remember { mutableStateOf(false) }
    val devices by viewModel.devices.collectAsState()


    DisposableEffect(lifecycleOwner){
        val observer = LifecycleEventObserver{_, event ->
            if(event == Lifecycle.Event.ON_RESUME && shouldCheckOnResume){
                requestKey++
                requestPermission = true
            }

        }
        val receiver = WifiDirectBroadcastReceiver(
            viewModel.getWifiDirectManager(),
            onPeerChanged = {
                //Toast.makeText(context, "Peers changed!", Toast.LENGTH_SHORT).show()
                viewModel.requestPeers()},
            onConnectionChanged = {viewModel.requestConnectionInfo()}
        )
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction((WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION))
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        context.registerReceiver(receiver, intentFilter)



        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            context.unregisterReceiver(receiver)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.msg.collect{
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    if(requestPermission){
        RequestPermissions(permissions = getWifiPermissions(),
            requestKey = requestKey,
            onGranted = {
                requestPermission = false
                errorMessage = null
                //start discovery
                viewModel.startDiscovery()
            },
            onDenied = {
                requestPermission = false
                errorMessage = getPermissionErrorMessage()
            })
    }

    errorMessage?.let{
        ShowResult(it,
            onRetry = {
            errorMessage = null
            requestKey++
            requestPermission = true
        },
            onOpenSettings = {
                shouldCheckOnResume = true
                openAppSettings(context)
        })
    } ?:
    ReceiverContent(devices, viewModel)



}

@SuppressLint("MissingPermission")
@Composable
fun ReceiverContent(devices : List<WifiP2pDevice>, viewModel:ReceiverViewModel){

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center ){
        Text(text = "This is Reciever Screen")
    }
    LazyColumn {
        items(devices){device->
            ListItem(headlineContent = {Text(device.deviceName.ifBlank { "Unknown Device" })},
                supportingContent = {Text(device.deviceAddress)},
                modifier = Modifier.clickable { viewModel.connectToDevice(device) })
        }
    }
}

