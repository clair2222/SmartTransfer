package com.jyco.smarttransfer.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyco.smarttransfer.data.contact.ContactRepository
import com.jyco.smarttransfer.data.message.MessageRepository
import com.jyco.smarttransfer.data.transfer.ContentSelectionItem
import com.jyco.smarttransfer.data.transfer.ContentType
import com.jyco.smarttransfer.data.transfer.MessagePeriod
import com.jyco.smarttransfer.data.transfer.TransferItem
import com.jyco.smarttransfer.data.transfer.TransferSelectionContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

class ContentSelectionViewModel(
    private val contactRepository: ContactRepository,
    private val messageRepository: MessageRepository,
    private val transferRepository: com.jyco.smarttransfer.data.transfer.TransferRepository
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

    val transferProgress = transferRepository.progress
    private val _transferError = MutableStateFlow<String?>(null)
    val transferError = _transferError.asStateFlow()
    private val _preparing = MutableStateFlow(false)
    val preparing = _preparing.asStateFlow()

    fun startTransfer() {
        if (_preparing.value || transferProgress.value.busy) return
        val selected = _contents.value.filter { it.selected }.map { it.type }.toSet()
        val selection = _transferSelections.value
        val range = getMessageRange()
        _preparing.value = true
        _transferError.value = null
        viewModelScope.launch {
            try {
                require(ContentType.CALENDAR !in selected) { "Calendar transfer is not implemented yet" }
                if (ContentType.MESSAGES in selected && _messagePeriod.value == MessagePeriod.CUSTOM) {
                    require(range.first != null && range.second != null && range.first!! <= range.second!!) {
                        "Choose a valid start and end date"
                    }
                }
                require(ContentType.PHOTOS !in selected || selection.photos.isNotEmpty()) { "Select photos first" }
                require(ContentType.VIDEOS !in selected || selection.videos.isNotEmpty()) { "Select videos first" }
                // Await provider queries before creating a single, consistent transfer snapshot.
                val gathered = TransferSelectionContent(
                    photos = if (ContentType.PHOTOS in selected) selection.photos else emptyList(),
                    videos = if (ContentType.VIDEOS in selected) selection.videos else emptyList(),
                    contacts = if (ContentType.CONTACTS in selected) contactRepository.getContacts() else emptyList(),
                    messages = if (ContentType.MESSAGES in selected) messageRepository.getMessages(range.first, range.second) else emptyList()
                )
                _transferSelections.value = gathered
                transferRepository.send(buildTransferItems(gathered))
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                _transferError.value = error.message ?: "Transfer failed"
            } finally {
                _preparing.value = false
            }
        }
    }

    fun buildTransferItems(selection: TransferSelectionContent = _transferSelections.value): List<TransferItem> = buildList {
        selection.photos.forEach { add(TransferItem.Photo(it)) }
        selection.videos.forEach { add(TransferItem.Video(it)) }
        selection.contacts.forEach { add(TransferItem.Contact(it)) }
        selection.messages.forEach { add(TransferItem.Message(it)) }
    }

    override fun onCleared() {
        if (_preparing.value || transferProgress.value.busy) transferRepository.cancel()
        super.onCleared()
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
                val (start, end) = _customMessageDate.value
                fun localDay(value: Long): java.time.LocalDate = java.time.Instant.ofEpochMilli(value)
                    .atZone(java.time.ZoneOffset.UTC).toLocalDate()
                val zone = java.time.ZoneId.systemDefault()
                start?.let { localDay(it).atStartOfDay(zone).toInstant().toEpochMilli() } to
                    end?.let { localDay(it).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1 }
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