package com.jyco.smarttransfer.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.jyco.smarttransfer.ui.common.ShowResult
import com.jyco.smarttransfer.ui.common.getPermissionErrorMessage
import com.jyco.smarttransfer.ui.permission.RequestPermissions
import com.jyco.smarttransfer.ui.permission.getWifiPermissions
import com.jyco.smarttransfer.ui.permission.openAppSettings
import com.jyco.smarttransfer.viewmodel.ReceiverViewModel
import org.koin.androidx.compose.koinViewModel

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


    DisposableEffect(lifecycleOwner){
        val observer = LifecycleEventObserver{_, event ->
            if(event == Lifecycle.Event.ON_RESUME && shouldCheckOnResume){
                requestKey++
                requestPermission = true
            }

        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if(requestPermission){
        RequestPermissions(permissions = getWifiPermissions(),
            requestKey = requestKey,
            onGranted = {
                requestPermission = false
                errorMessage = null
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
    }

    if(errorMessage == null){
        testScreen()
    }
}

@Preview
@Composable
fun testScreen(){
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center ){
        Text(text = "This is Reciever Screen")
    }
}