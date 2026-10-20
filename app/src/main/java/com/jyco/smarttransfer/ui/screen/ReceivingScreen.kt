package com.jyco.smarttransfer.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jyco.smarttransfer.viewmodel.ReceivingViewModel
import org.koin.androidx.compose.koinViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun ReceivingScreen(navController: NavController, viewModel: ReceivingViewModel = koinViewModel()) {
    val progress by viewModel.progress.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = progress.error ?: "${progress.message} (${progress.completed}/${progress.total})")
    }

}