package com.jyco.smarttransfer.data.transfer

import android.net.Uri
import com.jyco.smarttransfer.data.contact.ContactData
import com.jyco.smarttransfer.data.message.MessageData

enum class ContentType(val type :String){
    PHOTOS("Photos"),
    VIDEOS("Videos"),
    MESSAGES("Messages"),
    CONTACTS("Contacts"),
    CALENDAR("Calender")
}
enum class MessagePeriod(val type : String){
    THREE_MONTHS("3 months"),
    SIX_MONTHS("6 months"),
    ONE_YEAR("1 year"),
    CUSTOM("Custom")
}

data class ContentSelectionItem(val type : ContentType, val selected : Boolean = false,
                               val detail:String? = null)


data class PhotoUris(val uris : List<Uri>)
data class VideoUris(val uris : List<Uri>)

enum class TransferStatusType {
    idle,
    transferring,
    transferred
}
sealed class TransferResult(val message : String, val time : Long?, val reason : String?){
    data object Success: TransferResult(message = "Success", time = null, reason = null)
    data object Fail: TransferResult(message = "Failed", time = null, reason = null)
}

data class TransferSelectionContent(
    val photos: List<Uri> = emptyList(),
    val videos: List<Uri> = emptyList(),
    val contacts: List<ContactData> = emptyList(),
    val messages: List<MessageData> = emptyList()
)

sealed interface TransferItem{
    data class Photo(val uri : Uri) : TransferItem
    data class Video(val uri : Uri) : TransferItem
    data class Contact(val contact : ContactData) : TransferItem
    data class Message(val message : MessageData) : TransferItem
}

