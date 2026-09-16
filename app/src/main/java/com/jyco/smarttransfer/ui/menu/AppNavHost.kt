package com.jyco.smarttransfer.ui.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jyco.smarttransfer.ui.screen.ContentSelectionScreen
import com.jyco.smarttransfer.ui.screen.MainScreen
import com.jyco.smarttransfer.ui.screen.MediaSelectionScreen
import com.jyco.smarttransfer.ui.screen.PimsSelectionScreen
import com.jyco.smarttransfer.ui.screen.ReceiverScreen
import com.jyco.smarttransfer.ui.screen.ReceivingScreen
import com.jyco.smarttransfer.ui.screen.SenderScreen
import com.jyco.smarttransfer.ui.screen.SettingsScreen
import com.jyco.smarttransfer.ui.screen.TransferResultScreen
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(){
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(
        initialValue = DrawerValue.Closed
    )
    val scope = rememberCoroutineScope()
    var showMoreMenu by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Smart Transfer",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )

                HorizontalDivider()

                appMenuItem.forEach{
                    NavigationDrawerItem(
                        label = {Text(text = it.title)},
                        selected = false,
                        onClick = {
                            scope.launch {drawerState.close()}
                            navController.navigate(it.route)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    ){
        Scaffold(
            modifier = Modifier.fillMaxSize(),

            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Smart Transfer") },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch { drawerState.open() }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Menu"
                            )
                        }
                    },
                    actions = {
                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options"
                                )
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                appMenuItem.forEach {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = it.title,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            navController.navigate(it.route)
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Main.route,
                modifier = Modifier.padding(innerPadding)
            ) {
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
                composable(Screen.ContentSelection.route) {
                    ContentSelectionScreen(navController = navController)
                }
                composable(Screen.MediaSelection.route) {
                    MediaSelectionScreen(navController = navController)
                }
                composable(Screen.PimsSelection.route) {
                    PimsSelectionScreen(navController = navController)
                }
                composable(Screen.Receiving.route) {
                    ReceivingScreen(navController = navController)
                }
            }
        }

    }

}