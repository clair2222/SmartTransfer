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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonColors
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
import com.jyco.smarttransfer.data.MessagePeriod
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
                    messageExpended = messageExtended,
                    onCheckboxChanged = {viewModel.toggleContent(item.type)},
                    onDetailsClick = {
                        when(item.type){
                            TransferContentType.PHOTOS -> {navController.navigate("photo_selection")}
                            TransferContentType.VIDEOS -> {navController.navigate("video_selection")}
                            TransferContentType.CALENDAR -> {navController.navigate("calendar_selection")}
                            TransferContentType.CONTACTS -> {navController.navigate("contact_selection")}
                            else -> {}
                        }
                    },
                    onMessagePeriodSelected = {viewModel.updateMessagePeriod(it)}
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = {}, Modifier.fillMaxWidth(0.8f),
            shape = RoundedCornerShape(2.dp)
        ) {
            Text(text = "Transfer", style = MaterialTheme.typography.titleLarge)
        }

    }
}
@Composable
fun ContentSelectionCard(item : TransferContentItem,
                         messageExpended : Boolean,
                         onCheckboxChanged: ()->Unit,
                         onDetailsClick: ()->Unit,
                         onMessagePeriodSelected : (MessagePeriod)->Unit
                         ){
    Card(onClick = onCheckboxChanged, modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
            //pressedElevation = 20.dp
            //focusedElevation = 20.dp
            //hoveredElevation = 20.dp
            //draggedElevation = 20.dp
        ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        )
    {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFF9F9), Color(0xFFFFFAF4))
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
                val isEnabledTextButton = !((item.type == TransferContentType.MESSAGES) && messageExpended)

                TextButton(enabled = isEnabledTextButton, onClick = {}, contentPadding = PaddingValues(10.dp)) {
                    Text(textAlign = TextAlign.Center,
                        text = when(item.type){
                        TransferContentType.MESSAGES ->
                            "Messages from $(messagePeriod.title}"
                        else ->
                            "Select detail items"
                    }, style = MaterialTheme.typography.bodyMedium
                    )
                }

                if(!isEnabledTextButton){
                    messageSelector(selectedPeriod = MessagePeriod.THREE_MONTHS, onPeriodSelected = onMessagePeriodSelected)
                }
            }
        }
    }
}

@Composable
//@Preview
//fun messageSelectorPreview(){
fun messageSelector(selectedPeriod : MessagePeriod, onPeriodSelected : (period : MessagePeriod)->Unit){
    Column(horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top) {
        MessagePeriod.entries.forEach { period->
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()) {
                RadioButton(onClick = {onPeriodSelected(period)},
                    selected = period == selectedPeriod,
                    colors = RadioButtonColors(
                        selectedColor = MaterialTheme.colorScheme.secondary,
                        unselectedColor = MaterialTheme.colorScheme.secondaryContainer,
                        disabledSelectedColor = MaterialTheme.colorScheme.primary,
                        disabledUnselectedColor = MaterialTheme.colorScheme.primary
                    )
                )
                Text(text = period.type, color = MaterialTheme.colorScheme.secondary)
            }
        }

    }

}

@Preview
@Composable
fun ContentSelectionPreview(){
    val contents = makeDummyTransferItems()
    val messageExpended = true

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
                ContentSelectionCard(item, messageExpended, {}, {}, {})

            }
        }
    }

}



fun makeDummyTransferItems() = TransferContentType.entries.map{ item->
    TransferContentItem(item, false, "Test detail set...")
}
fun makeDummyTransferContentItem() = TransferContentItem(type = TransferContentType.MESSAGES, detail = "Messages from 6 months ago")