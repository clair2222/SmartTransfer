package com.jyco.smarttransfer.ui.permission


import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun RequestPermissions(permissions: Array<String>, onGranted:() -> Unit, onDenied:()->Unit = {}) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val didAllGranted = permissions.all{ permission ->
            result[permission] == true
        }
        if(didAllGranted) onGranted()
        else onDenied()
    }

    LaunchedEffect(permissions) {
        val hasAlreadyGranted = permissions.all{ permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        if(hasAlreadyGranted) onGranted()
        else {
            launcher.launch(permissions)
        }

    }
}

fun getWifiPermissions():Array<String>{
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
        else
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

}

