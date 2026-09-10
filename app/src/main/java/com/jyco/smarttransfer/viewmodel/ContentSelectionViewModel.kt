package com.jyco.smarttransfer.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.jyco.smarttransfer.data.MessagePeriod
import com.jyco.smarttransfer.data.TransferContentItem
import com.jyco.smarttransfer.data.TransferContentType
import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ContentSelectionViewModel : ViewModel() {

    private val _contents  = MutableStateFlow(
        TransferContentType.entries.map{
            TransferContentItem(type = it)
        }
    )
    val contents = _contents.asStateFlow()

    private val _messagePeriod = MutableStateFlow(MessagePeriod.THREE_MONTHS)
    val messagePeriod = _messagePeriod.asStateFlow()

    private val _messageExpended = MutableStateFlow(false)
    val messageExpended = _messageExpended.asStateFlow()

    private val _customMessageDate = MutableStateFlow(Pair<Long?, Long?>(null, null))
    val customMessageStartDate = _customMessageDate.asStateFlow()

    fun toggleContent(type : TransferContentType){
        _contents.update { items->
            items.map{item ->
                if(item.type == type) {
                    item.copy(selected = !item.selected)
                }
                else{
                    item
                }
            }

        }
    }
    fun updateMessagePeriod(period : MessagePeriod){
        _messagePeriod.value = period
    }
    fun updateMessageExpended(extended : Boolean){
        _messageExpended.value = extended
    }
    fun updateCustomMessageDate(date : Pair<Long?, Long?>){
        _customMessageDate.value = date
    }

}