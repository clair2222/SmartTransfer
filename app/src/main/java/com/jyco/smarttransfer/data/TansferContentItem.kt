package com.jyco.smarttransfer.data

enum class TransferContentType(val type :String){
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
data class TransferContentItem(val type : TransferContentType, val selected : Boolean = false,
                              val detail:String? = null)

