package com.jyco.smarttransfer.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import com.jyco.smarttransfer.ui.permission.RequestPermissions
import com.jyco.smarttransfer.ui.permission.getWifiPermissions

@Composable
fun ReceiverScreen(navController: NavController){
    var start by remember { mutableStateOf(false) }
    if(start){
        RequestPermissions(permissions = getWifiPermissions(),
            onGranted = {},
            onDenied = {})
    }
}