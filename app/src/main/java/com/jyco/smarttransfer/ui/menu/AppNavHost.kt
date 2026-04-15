package com.jyco.smarttransfer.ui.menu

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jyco.smarttransfer.ui.screen.MainScreen
import com.jyco.smarttransfer.ui.screen.ReceiverScreen
import com.jyco.smarttransfer.ui.screen.SenderScreen
import com.jyco.smarttransfer.ui.screen.SettingsScreen
import com.jyco.smarttransfer.ui.screen.TransferResultScreen


@Composable
fun AppNavHost(){
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ){
        composable(Screen.Main.route) {
            MainScreen(navController = navController)
        }
        composable(Screen.Sender.route) {
            SenderScreen(navController = navController)
        }
        composable(Screen.Receiver.route) {
            ReceiverScreen(navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
        composable(Screen.TransferResult.route) {
            TransferResultScreen(navController = navController)
        }
    }

}