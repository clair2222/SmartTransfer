package com.jyco.smarttransfer.ui.menu

sealed class Screen(val route: String, val title: String) {
    data object Main : Screen("main", "Smart Transfer")
    data object Sender : Screen("sender", "Sender")
    data object Receiver : Screen("receiver","Receiver")
    data object Settings : Screen("settings", "Settings")
    data object TransferResult : Screen("transfer_result", "Transfer Result")

}

data class AppMenuItem(val route: String, val title: String)

val appMenuItem = listOf(
    AppMenuItem(Screen.Settings.route, Screen.Settings.title),
    AppMenuItem(Screen.TransferResult.route, Screen.TransferResult.title)
)
