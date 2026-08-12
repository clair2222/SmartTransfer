package com.jyco.smarttransfer.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Shapes
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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

@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
@Composable
fun ReceiverContent(devices : List<WifiP2pDevice>, viewModel:ReceiverViewModel){
    val pin by viewModel.authPin.collectAsState()
    val connectionState by viewModel.conncetionState.collectAsState()
    var subTitle by remember { mutableStateOf("") }

    when(connectionState){
        ConnectionState.Idle -> IdleContent()

        ConnectionState.Discovering,
        ConnectionState.Ready,
        ConnectionState.P2PConnecting,
        ConnectionState.P2PConnected
                //-> SearchingContent()
            -> SearchingContent(subTitle = when(connectionState) {
            ConnectionState.Ready -> "Select a Device to connect as a Sender"
            ConnectionState.P2PConnected -> "Connected"
            else -> "Searching..."
        }, devices, viewModel)

        ConnectionState.SocketConnecting,
        ConnectionState.SocketConnected,
        ConnectionState.Authenticating,
        ConnectionState.Authenticated
             -> PinContent(subTitle = when(connectionState){
                    ConnectionState.Authenticating -> "Enter below pin number on the Sender Device"
                    ConnectionState.Authenticated -> "Authenticated"
                else -> "Connecting..."
             },
            pin.toString())
        //ConnectionState.Transfer -> TODO()
        //ConnectionState.Disconnected -> TODO()
        //ConnectionState.Failed -> TODO()
        else -> {}
    }

}
@SuppressLint("MissingPermission")
//@Preview
@Composable
fun SearchingContent(subTitle : String, devices : List<WifiP2pDevice>, viewModel:ReceiverViewModel){
//fun SearchingContent(){
    //only Receiver can select a device to connect.
    val subTitle = "Select a Device to connect as a Sender"
//    val devices =
//        List(10){ index ->
//        WifiP2pDevice().apply {
//            this.deviceName = "Test Device $index"
//            this.deviceAddress = "111.222.333.$index"
//        }
//    }
    Column(modifier = Modifier
        .background(MaterialTheme.colorScheme.surface)
        .border(
            border = BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.surfaceContainer
            )
        )
        .fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        Spacer(Modifier.weight(1.0f))
        Text(text=subTitle,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1.0f))
        LazyColumn {
            items(devices) { device ->
                ListItem(
                    headlineContent = { Text(device.deviceName.ifBlank { "Unknown Device" }) },
                    supportingContent = { Text(device.deviceAddress) },
                    modifier = Modifier.clickable {  }
                    //modifier = Modifier.clickable { viewModel.connectToDevice(device) }
                )
            }
        }
        Spacer(Modifier.weight(1.0f))
    }
}

@Composable
fun IdleContent(){
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center ){
        Text(text = "This is Receiver Screen", style = MaterialTheme.typography.displayLarge)
    }
}

//@Preview
@Composable
//fun PinContent(){
fun PinContent(subTitle : String, pin : String){
    //val subTitle = "Enter below pin number on the Sender Device"
    Column(modifier = Modifier
        .background(MaterialTheme.colorScheme.surface)
        .border(
            border = BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.surfaceContainer
            )
        )
        .fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        Spacer(Modifier.weight(1.0f))
        Text(text=subTitle,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1.0f))
        LazyRow(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)

        ) {
            //val list = "123456".toList()
            val list = pin.toList()
            items(list){ number->
                Text(text = number.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                /*
                OutlinedButton(onClick = {},
                    colors = ButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        disabledContentColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                    //shape = ButtonDefaults.outlinedShape,
                    modifier = Modifier.padding(2.dp)) {
                    Text(text = number.toString(), style = MaterialTheme.typography.headlineLarge/*, color = MaterialTheme.colorScheme.onPrimary*/)
                }

                 */
            }

        }
        Spacer(Modifier.weight(1.0f))
        CircularProgressIndicator()
        Text(text = "Wating for authentication",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.weight(1.0f))
    }
}
