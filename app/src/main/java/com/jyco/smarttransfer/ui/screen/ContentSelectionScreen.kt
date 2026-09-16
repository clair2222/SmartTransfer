package com.jyco.smarttransfer.ui.screen

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jyco.smarttransfer.data.transfer.MessagePeriod
import com.jyco.smarttransfer.data.transfer.TransferContentItem
import com.jyco.smarttransfer.data.transfer.TransferContentType
import com.jyco.smarttransfer.ui.menu.Screen
import com.jyco.smarttransfer.viewmodel.ContentSelectionViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun ContentSelectionScreen(navController: NavController,
                           viewModel: ContentSelectionViewModel = koinViewModel()){

    val contents by viewModel.contents.collectAsState()
    val messagePeriod by viewModel.messagePeriod.collectAsState()
    val messageExpended by viewModel.messageExpended.collectAsState()
    val customMessageDate by viewModel.customMessageDate.collectAsState()

    var showDatePicker = remember { mutableStateOf(false) }

    if(showDatePicker.value){
        CustomDatePickerDialog(onDismiss = {showDatePicker.value = false},
            onConfirm = {start, end ->
                viewModel.updateCustomMessageDate(Pair(start, end))
                showDatePicker.value = false
            },
            customMessageDate = customMessageDate)
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(text = "Select content to transfer",
            style = MaterialTheme.typography.headlineSmall,
            //color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        LazyColumn(modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
            userScrollEnabled = true
        ) {
            items(contents, key = {it.type}){ item->
                ContentSelectionCard(item,
                    messagePeriod = messagePeriod,
                    messageExpended = messageExpended,
                    onCheckboxChanged = {viewModel.toggleContent(item.type)
                    },
                    onDetailsClick = {
                        when(item.type){
                            TransferContentType.PHOTOS,
                            TransferContentType.VIDEOS
                                -> {
                                navController.navigate(Screen.MediaSelection.route)
                            }

                            TransferContentType.CONTACTS,
                            TransferContentType.CALENDAR -> {
                                navController.navigate(Screen.PimsSelection.route)
                            }
                            TransferContentType.MESSAGES -> {
                                viewModel.updateMessageExpended(!messageExpended)
                            }
                            else -> {}
                        }
                    },
                    onMessagePeriodSelected = {
                        viewModel.updateMessagePeriod(it)
                        if(it == MessagePeriod.CUSTOM && !showDatePicker.value){
                            showDatePicker.value = true
                        }
                    },
                    customMessageDate = customMessageDate
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = {}, Modifier.fillMaxWidth(0.8f),
            shape = RoundedCornerShape(12.dp),
            enabled = contents.any { it.selected }
        ) {
            Text(text = "Transfer", style = MaterialTheme.typography.titleLarge)
        }

    }
}


@Composable
fun ContentSelectionCard(item : TransferContentItem,
                         messageExpended : Boolean,
                         messagePeriod: MessagePeriod,
                         onCheckboxChanged: ()->Unit,
                         onDetailsClick: ()->Unit,
                         onMessagePeriodSelected : (MessagePeriod)->Unit,
                         customMessageDate : Pair<Long?, Long?>
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
                val isMessageAndExpended = (item.type == TransferContentType.MESSAGES) && messageExpended

                TextButton(enabled = true, onClick = onDetailsClick, contentPadding = PaddingValues(10.dp)) {
                    Text(textAlign = TextAlign.Center,
                        text = when(item.type){
                        TransferContentType.MESSAGES -> {
                            if(messagePeriod.type == MessagePeriod.CUSTOM.type){
                                val formatter = SimpleDateFormat(("MMM d, yyyy"), Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
                                val start = customMessageDate.first?.let{
                                    formatter.format(Date(customMessageDate.first!!))
                                } ?: {"Start"}
                                val end = customMessageDate.second?.let{
                                    formatter.format(Date(customMessageDate.second!!))
                                } ?: {"End"}

                                "Messages from $start to $end"
                            }
                            else{
                                "Messages from ${messagePeriod.type}"
                            }
                        }
                        else ->
                            "Select detail items"
                    }, style = MaterialTheme.typography.bodyMedium
                    )
                }

                if(isMessageAndExpended){
                    messageSelector(selectedPeriod = messagePeriod, onPeriodSelected = onMessagePeriodSelected)
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
    val customMessageDate = Pair(null, null)

    var showDatePicker = remember { mutableStateOf(false) }

    if(showDatePicker.value){
        CustomDatePickerDialog(onDismiss = {showDatePicker.value = false},
            onConfirm = {start, end ->
                //viewModel.updateCustomMessageDate(Pair(start, end))
                        },
            customMessageDate
            )
    }
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(text = "Select content to transfer",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            //color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        LazyColumn(modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
            userScrollEnabled = true
        ) {
            items(contents, key = {it.type}){ item->
                ContentSelectionCard(item,
                    messagePeriod = MessagePeriod.THREE_MONTHS,
                    messageExpended = messageExpended,
                    onCheckboxChanged = {//viewModel.toggleContent(item.type)
                         },
                    onDetailsClick = {
                        when(item.type){
                            TransferContentType.PHOTOS,
                            TransferContentType.VIDEOS
                                -> {
                                //navController.navigate(Screen.MediaSelection.route)
                                    }

                            TransferContentType.CONTACTS,
                            TransferContentType.CALENDAR -> {
                                //navController.navigate(Screen.PimsSelection.route)
                                }
                            else -> {}
                        }
                    },
                    onMessagePeriodSelected = {//viewModel.updateMessagePeriod(it)
                         },
                    customMessageDate = customMessageDate
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = {}, Modifier.fillMaxWidth(0.8f),
            shape = RoundedCornerShape(12.dp),
            enabled = contents.any { it.selected }
        ) {
            Text(text = "Transfer", style = MaterialTheme.typography.titleLarge)
        }

    }

}



fun makeDummyTransferItems() = TransferContentType.entries.map{ item->
    TransferContentItem(item, false, "Test detail set...")
}

@OptIn(ExperimentalMaterial3Api::class)
//@Preview
@Composable
fun CustomDatePickerDialog(onDismiss : ()-> Unit,
                           onConfirm : (Long?, Long?)-> Unit,
                           customMessageDate : Pair<Long?, Long?>,
                           ){
    val formatter = remember { SimpleDateFormat(("MMM d, yyyy"), Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") } }
    val customStartDate = customMessageDate.first
    val customEndDate = customMessageDate.second
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = customStartDate,
        initialSelectedEndDateMillis = customEndDate,
    )
    val headlineText = {
        val start = dateRangePickerState.selectedStartDateMillis
        val end = dateRangePickerState.selectedEndDateMillis
        when {
            (start != null) && (end != null) -> {
                "${formatter.format(start)} - ${formatter.format(end)}"
            }
            (start != null) -> {
                "${formatter.format(start)} - End"
            }
            (end != null) -> {
                "Start - ${formatter.format(end)}}"
            }
            else -> {
                "Start - End"
            }
        }
    }

    DatePickerDialog(
        onDismissRequest = {
            dateRangePickerState.displayMode = DisplayMode.Picker
            onDismiss},
        confirmButton = {
            TextButton(onClick =
                {onConfirm(dateRangePickerState.selectedStartDateMillis,
                    dateRangePickerState.selectedEndDateMillis)}) {
                Text(text = "Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        },
        shape = DatePickerDefaults.shape,
        tonalElevation = DatePickerDefaults.TonalElevation,
        colors = DatePickerDefaults.colors(),
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            modifier = Modifier.fillMaxSize(),
            title = {
//                Text(text = "Select dates for retrieving messages",
//                    textAlign = TextAlign.Center,
//                    style = MaterialTheme.typography.titleMedium,
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(6.dp)
//                )
//
            },
            headline = {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ){
                    Text(text = headlineText(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(10.dp))
                    Spacer(Modifier.weight(1f))

                    IconButton(onClick = {
                        if(dateRangePickerState.displayMode == DisplayMode.Picker)
                            dateRangePickerState.displayMode = DisplayMode.Input
//                        else
//                            dateRangePickerState.displayMode = DisplayMode.Picker
                    },
                        ) {
                        Icon(imageVector = Icons.Default.Edit,
                            contentDescription = "Edit date")
                    }
                }


            },
            showModeToggle = false,
            colors = DatePickerDefaults.colors()
        )
    }
}