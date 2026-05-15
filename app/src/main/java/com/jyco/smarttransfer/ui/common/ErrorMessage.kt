package com.jyco.smarttransfer.ui.common

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jyco.smarttransfer.ui.permission.getWifiPermissions

fun getPermissionErrorMessage()=
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        "NEARBY_WIFI_DEVICES"
    else "ACCESS_FINE_LOCATION" + "\n" +
        "ACCESS_COARSE_LOCATION"

@Composable
//@Preview
//fun ShowResult(){
fun ShowResult(message : String,
               onRetry: ()-> Unit,
               onOpenSettings: () -> Unit
){
    val title = "Please grant below Permissions to use this WiFi direct"
    val testResultMessage = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        "NEARBY_WIFI_DEVICES"

    else "ACCESS_FINE_LOCATION" + "\n" +
            "ACCESS_COARSE_LOCATION"

    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ){
        Column (modifier = Modifier.fillMaxSize(0.8f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)

        ){
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Text(
                text = testResultMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Button(onClick = {
                onRetry()
            }){Text("Retry")}

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(onClick = onOpenSettings) {
                Text("Open Settings")
            }
        }

    }
}