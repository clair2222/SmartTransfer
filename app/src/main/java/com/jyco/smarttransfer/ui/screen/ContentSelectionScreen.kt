package com.jyco.smarttransfer.ui.screen


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jyco.smarttransfer.data.TransferContentItem
import com.jyco.smarttransfer.data.TransferContentType
import com.jyco.smarttransfer.viewmodel.ContentSelectionViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ContentSelectionScreen(navController: NavController,
                           viewModel: ContentSelectionViewModel = koinViewModel()){

    val contents by viewModel.contents.collectAsState()
    val messagePeriod by viewModel.messagePeriod.collectAsState()
    val messageExtended by viewModel.messageExtended.collectAsState()

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(text = "Select content to transfer", style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        LazyColumn {
            items(contents, key = {it.type}){ item->
                ContentSelectionCard(item,
                    {viewModel.toggleContent(item.type)},
                    {})
            }
        }
    }
}
@Composable
fun ContentSelectionCard(item : TransferContentItem,
                         onCheckboxChanged: ()->Unit,
                         onDetailsChanged: ()->Unit
                         ){
    Card(onClick = onCheckboxChanged, modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        )
    {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFF6D365), Color(0xFFFDA085))
                )
            ),
            contentAlignment = Alignment.Center
        )
        {
            Column(verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically){
                    Text(text = item.type.toString(), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    Checkbox(
                        checked = item.selected,
                        onCheckedChange = {onCheckboxChanged()})
                }
                TextButton(onClick = {}, contentPadding = PaddingValues(10.dp)) {
                    Text(textAlign = TextAlign.Center,
                        text = when(item.type){
                        TransferContentType.MESSAGES ->
                            "Messages from $(messagePeriod.title}"
                        else ->
                            "Select detail items"
                    }, style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun ContentSelectionPreview(){
    val contents = makeDummyTransferItems()

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
        ) {
        Text(text = "Select content to transfer", style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        LazyColumn {
            items(contents, key = {it.type}){ item->
                ContentSelectionCard(item, {}, {})
            }
        }
    }

}



fun makeDummyTransferItems() = TransferContentType.entries.map{ item->
    TransferContentItem(item, false, "Test detail set...")
}
fun makeDummyTransferContentItem() = TransferContentItem(type = TransferContentType.MESSAGES, detail = "Messages from 6 months ago")