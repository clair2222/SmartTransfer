package com.jyco.smarttransfer.ui.screen

import android.widget.ImageButton
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jyco.smarttransfer.MainActivity
import com.jyco.smarttransfer.ui.menu.AppMenuItem
import com.jyco.smarttransfer.ui.menu.Screen
import com.jyco.smarttransfer.ui.menu.appMenuItem
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.JdkConstants.HorizontalAlignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController){
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
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {Text("Smart Transfer")},
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {drawerState.open()}
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Menu"
                            )
                        }
                    },
                    actions = {
                        Box{
                            IconButton(
                                onClick = {showMoreMenu = true}
                            ){
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options"
                                )
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = {showMoreMenu = false}
                            ) {
                                appMenuItem.forEach{
                                    DropdownMenuItem(
                                        text = { Text(text = it.title, color = MaterialTheme.colorScheme.onSurface) },
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

            Column(modifier = Modifier.fillMaxSize()
                .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Column(modifier = Modifier.weight(0.1f)
                    // .background(MaterialTheme.colorScheme.error)
                    .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                )
                {
                    Text(
                        text = "Choose Transfer Mode",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(modifier = Modifier.weight(0.9f)
                    .fillMaxWidth()
                    //.background(MaterialTheme.colorScheme.onErrorContainer)
                    //.padding(top = 100.dp, bottom = 100.dp)
                    ,
                    verticalArrangement = Arrangement.spacedBy(24.dp, alignment = Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MainActionCard(
                        modifier = Modifier.fillMaxWidth(0.8f)
                            .aspectRatio(1.8f)
                        ,
                        title = Screen.Sender.title,
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        onClick = {
                            navController.navigate(Screen.Sender.route)
                        }
                    )
                    MainActionCard(
                        modifier = Modifier.fillMaxWidth(0.8f)
                            .aspectRatio(1.8f)
                        ,
                        title = Screen.Receiver.title,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = {
                            navController.navigate(Screen.Receiver.route)
                        })
                }
            }
        }
    }
}
@Composable
fun MainActionCard(modifier: Modifier = Modifier, title : String, icon: ImageVector, onClick: () -> Unit){
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Icon(imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(76.dp)
        )
        Text(text = title,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
    }
}



@Preview
@Composable
fun MainScreenPreview()
    {
        val innerPadding = 10.dp
        Column(modifier = Modifier.fillMaxSize()
            .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,

        ) {
            Column(modifier = Modifier.weight(0.1f)
               // .background(MaterialTheme.colorScheme.error)
                .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            )
            {
                Text(
                    text = "Choose Transfer Mode",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(modifier = Modifier.weight(0.9f)
                .fillMaxWidth()
                //.background(MaterialTheme.colorScheme.onErrorContainer)
                //.padding(top = 100.dp, bottom = 100.dp)
                ,
                verticalArrangement = Arrangement.spacedBy(24.dp, alignment = Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
                ) {
                MainActionCard(modifier = Modifier.fillMaxWidth(0.8f)
                    .aspectRatio(1.8f)
                    ,
                    title = Screen.Sender.title,
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = {
                        //navController.navigate(Screen.Sender.route)
                    }
                )
                MainActionCard(modifier = Modifier.fillMaxWidth(0.8f)
                    .aspectRatio(1.8f)
                    ,
                    title = Screen.Receiver.title,
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = {
                        //navController.navigate(Screen.Receiver.route)
                    })
            }
        }

    }

