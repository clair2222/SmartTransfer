package com.jyco.smarttransfer.viewmodel

import android.net.Uri
import android.provider.CalendarContract.Calendars
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jyco.smarttransfer.data.contact.ContactData
import com.jyco.smarttransfer.data.contact.ContactRepository
import com.jyco.smarttransfer.data.message.MessageData
import com.jyco.smarttransfer.data.message.MessageRepository
import com.jyco.smarttransfer.data.transfer.MessagePeriod
import com.jyco.smarttransfer.data.transfer.ContentSelectionItem
import com.jyco.smarttransfer.data.transfer.ContentType
import com.jyco.smarttransfer.data.transfer.TransferItem
import com.jyco.smarttransfer.data.transfer.TransferSelectionContent
import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

class ContentSelectionViewModel(
    private val contactRepository: ContactRepository,
    private val messageRepository: MessageRepository
) : ViewModel() {

    companion object{
        val TAG = ContentSelectionViewModel::class.simpleName
    }

    private val _contents  = MutableStateFlow(
        ContentType.entries.map{
            ContentSelectionItem(type = it)
        }
    )
    val contents = _contents.asStateFlow()

    private val _messagePeriod = MutableStateFlow(MessagePeriod.THREE_MONTHS)
    val messagePeriod = _messagePeriod.asStateFlow()

    private val _messageExpended = MutableStateFlow(false)
    val messageExpended = _messageExpended.asStateFlow()

    private val _customMessageDate = MutableStateFlow(Pair<Long?, Long?>(null, null))
    val customMessageDate = _customMessageDate.asStateFlow()

    private val _transferSelections = MutableStateFlow(TransferSelectionContent())
    val transferSelections = _transferSelections.asStateFlow()

    fun buildTransferItems(): List<TransferItem>{
        return buildList{
            transferSelections.value.photos.map{ item->
                add(TransferItem.Photo(item))
            }
            transferSelections.value.videos.map{item->
                add(TransferItem.Video(item))
            }
            transferSelections.value.contacts.map { item->
                add(TransferItem.Contact(item))
            }
            transferSelections.value.messages.map{ item->
                add(TransferItem.Message(item))
            }
        }
    }
    fun toggleContent(type : ContentType){
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
    fun updatePhotoUris(photoUris : List<Uri>){
        _transferSelections.update { item->
            item.copy(photos = photoUris)
        }
        Log.d(TAG, " updatePhotoUris : $photoUris.toString()" )
    }
    fun updateVideoUris(videoUris : List<Uri>){
        _transferSelections.update { item->
            item.copy(videos = videoUris)
        }
        Log.d(TAG, " updateVideoUris : $videoUris.toString()" )
    }

    fun loadContacts(){
        viewModelScope.launch {
            runCatching {
                contactRepository.getContacts()
            }.onSuccess { contacts->
                _transferSelections.update {
                    it.copy(contacts = contacts)
                }
            }.onFailure { e->
                Log.e(TAG, "loadContacts failed - ${e.message}")
            }
        }



    }
    private fun getMessageRange() : Pair<Long?, Long?> {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        return when(_messagePeriod.value){
            MessagePeriod.CUSTOM ->{
                _customMessageDate.value.first to _customMessageDate.value.second
            }
            MessagePeriod.THREE_MONTHS ->{
                calendar.add(Calendar.MONTH, -3)
                calendar.timeInMillis to now
            }
            MessagePeriod.SIX_MONTHS ->{
                calendar.add(Calendar.MONTH, -6)
                calendar.timeInMillis to now
            }
            MessagePeriod.ONE_YEAR ->{
                calendar.add(Calendar.YEAR, -1)
                calendar.timeInMillis to now
            }
        }
    }
    fun loadMessages(){
        val range = getMessageRange()
        viewModelScope.launch {
            runCatching {
                messageRepository.getMessages(range.first, range.second)
            }.onSuccess { messages->
                _transferSelections.update {
                    it.copy(messages = messages)
                }
            }.onFailure { e->
                Log.e(TAG, "${e.message}")
            }

        }
    }
}