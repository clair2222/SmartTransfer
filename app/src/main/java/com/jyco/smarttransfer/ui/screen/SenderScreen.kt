package com.jyco.smarttransfer.ui.screen


import android.Manifest
import android.annotation.SuppressLint
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import com.jyco.smarttransfer.viewmodel.ConnectionState
import com.jyco.smarttransfer.viewmodel.SenderViewModel
import org.koin.androidx.compose.koinViewModel


@SuppressLint("MissingPermission")
@Composable
fun SenderScreen(navController: NavController,
                 viewModel: SenderViewModel = koinViewModel()
){
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current

    var requestPermission by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null)}
    var requestKey by remember { mutableIntStateOf(1000) }
    var shouldCheckOnResume by remember { mutableStateOf(false) }
    val devices by viewModel.devices.collectAsState()

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver{ _, event ->
            if(Lifecycle.Event.ON_RESUME == event && shouldCheckOnResume){
                requestKey++
                requestPermission = true
            }
        }
        val TAG = "SenderScreen"
        val receiver = WifiDirectBroadcastReceiver(
            viewModel.getWifiDirectManager(),
            onPeerChanged = {viewModel.requestPeers()
                Log.d(TAG, "Sender : PEERS_CHANGED")
            },
            onConnectionChanged = {viewModel.requestConnectionInfo()
                Log.d(TAG, "Sender : CONNECTION_CHANGED")
            }
        )
        val intentFilter = IntentFilter().apply{
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        }

        lifecycle.lifecycle.addObserver(observer)
        context.registerReceiver(receiver, intentFilter )

        onDispose {
            lifecycle.lifecycle.removeObserver(observer)
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
                viewModel.resetSession{viewModel.startDiscovery()}

            },
            onDenied = {
                requestPermission = false
                errorMessage = getPermissionErrorMessage()
            })
    }

    errorMessage?.let{
        ShowResult(it, onRetry = {
            requestPermission = true
            errorMessage = null
            requestKey += 1
        },
            onOpenSettings = {
                shouldCheckOnResume = true
                openAppSettings(context)
            })
    } ?: SenderContent(devices, viewModel)
}


@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
@Composable
fun SenderContent(devices : List<WifiP2pDevice>, viewModel: SenderViewModel){
    val state by viewModel.connectionState.collectAsState()
    val enteredPin by viewModel.enteredPin.collectAsState()
    var subTitle = "This is Sender Screen"
    var inProgress = remember{ mutableStateOf(false) }

    when(state){
        ConnectionState.Idle ->{
            subTitle = "This is Sender Screen"
            inProgress.value = false
        }

        ConnectionState.Discovering,
        ConnectionState.Ready,
        ConnectionState.P2PConnecting,
        ConnectionState.P2PConnected,
        ConnectionState.SocketConnecting,
        ConnectionState.SocketConnected  -> {
            subTitle = "Connecting Devices..."
            inProgress.value = true
        }

        ConnectionState.Authenticating ->{
            subTitle = "Enter Pin what you are seeing on Receiver Devices."
            inProgress.value = false
        }

        ConnectionState.Authenticated -> {
            subTitle = "Authentication Success"
            inProgress.value = false
        }
        ConnectionState.Failed -> {
            subTitle = "Connection Failed."
        }
        else -> {
//                Transfer,
//                Disconnected,
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)
        , verticalArrangement = Arrangement.Center
        , horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1.0f))
        Text(text=subTitle, style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1.0f))
        if(inProgress.value == true){
            CircularProgressIndicator()
        }

        if(state == ConnectionState.Authenticating){
            PinEntry(Modifier.padding(10.dp),
                pin = enteredPin,
                onPinChanged = viewModel::updatePin,
                onSubmit = {viewModel.submitPin()})
        }
        Spacer(Modifier.weight(1.0f))
    }
}



