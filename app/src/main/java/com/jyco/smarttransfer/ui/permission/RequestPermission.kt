package com.jyco.smarttransfer.ui.permission


import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

val TAG = "PermissionTest"

@Composable
fun RequestPermissions(permissions: Array<String>, requestKey : Int,
                       onGranted:() -> Unit, onDenied: ()->Unit = {}) {
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

    LaunchedEffect(requestKey) {

        Log.d(TAG, "LaunchedEffect requestKey=$requestKey")
        Log.d(TAG, "permissions=${permissions.joinToString()}")

        val hasAlreadyGranted = hasPermissions(context = context, permissions = permissions)

        if(hasAlreadyGranted) onGranted()
        else {
            Log.d(TAG, "launch permission request")
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

fun openAppSettings(context: Context){
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
        )

    context.startActivity(intent)
}

fun hasPermissions(context: Context, permissions: Array<String>): Boolean{
    return permissions.all {permission ->
        val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "$permission granted =$granted")
        granted
    }
}


