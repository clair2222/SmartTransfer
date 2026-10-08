package com.jyco.smarttransfer.ui.permission


import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
            permissions.forEach { permission->
                markPermissionRequested(context, permission)
            }
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
fun getContactPermissions():Array<String>{
    return arrayOf(Manifest.permission.READ_CONTACTS)
}

fun getMessagePermissions() : Array<String>{
    return arrayOf(Manifest.permission.READ_SMS)
}
fun getCalendarPermissions() : Array<String>{
    return arrayOf(Manifest.permission.READ_CALENDAR)
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
fun Context.findActivity(): Activity?{
    var context = this

    while(context is ContextWrapper){
        if(context is Activity) return context
        else context = context.baseContext
    }
    return null
}
enum class PermissionStatus{
    GRANTED,
    CAN_POPUP,
    GO_SETTING
}
fun isPermissionPopupNeeded(context: Context, permissions : Array<String>) : PermissionStatus{
    val activity = context.findActivity()
    val status = permissions.map {permission->
        val needRationale = activity?.shouldShowRequestPermissionRationale(permission) ?: false
        val hasPermission = hasPermissions(context, arrayOf(permission))
        val requestedBefore = getPermissionRequestedBefore(context, permission)

        when{
            (hasPermission) ->{
                PermissionStatus.GRANTED
            } // granted
            (needRationale) ->{
                PermissionStatus.CAN_POPUP
            } // don't allow just only once
            (requestedBefore) ->{
                PermissionStatus.GO_SETTING
            } // don't allow repeatedly
            else -> {
                PermissionStatus.CAN_POPUP
            } // for the first time
        }
    }

    return when{
        status.contains(PermissionStatus.GO_SETTING) -> PermissionStatus.GO_SETTING
        status.contains(PermissionStatus.CAN_POPUP) -> PermissionStatus.CAN_POPUP
        else -> {PermissionStatus.GRANTED}
    }

}
fun markPermissionRequested(context: Context, permission : String){
    context.getSharedPreferences(permission, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(permission, true)
        .apply()
}
fun getPermissionRequestedBefore(context: Context, permission : String) : Boolean{
    val prefs = context.getSharedPreferences(permission, Context.MODE_PRIVATE)
    return prefs.getBoolean(permission, false)
}





