package com.jyco.smarttransfer.ui.screen

import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun MainScreen(navController: NavController){
    val drawerState = rememberDrawerState(
        initialValue = androidx.compose.material3.DrawerValue.Closed
    )
}