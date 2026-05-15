package com.jyco.smarttransfer.ui.screen


import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.jyco.smarttransfer.ui.common.ShowResult
import com.jyco.smarttransfer.ui.common.getPermissionErrorMessage
import com.jyco.smarttransfer.ui.permission.RequestPermissions
import com.jyco.smarttransfer.ui.permission.getWifiPermissions
import com.jyco.smarttransfer.ui.permission.openAppSettings
import com.jyco.smarttransfer.viewmodel.SenderViewModel
import org.koin.androidx.compose.koinViewModel


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

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver{ _, event ->
            if(Lifecycle.Event.ON_RESUME == event && shouldCheckOnResume){
                requestKey++
                requestPermission = true
            }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose {lifecycle.lifecycle.removeObserver(observer)  }
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
        ShowResult(it, onRetry = {
            requestPermission = true
            errorMessage = null
            requestKey += 1
        },
            onOpenSettings = {
                shouldCheckOnResume = true
                openAppSettings(context)
            })
    }
}


