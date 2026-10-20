package com.jyco.smarttransfer.data.message

import android.content.Context
import android.os.Build
import android.provider.Telephony
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MessageRepository(private val context : Context) {
    companion object{
        val TAG = MessageRepository::class.java.simpleName
    }

    suspend fun getMessages(startMillis : Long?, endMillis : Long?) : List<MessageData> = withContext(Dispatchers.IO){

        val projection = arrayOf(Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.SUBJECT,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )
        val selection = "${Telephony.Sms.DATE} >= ? AND " +
                "${Telephony.Sms.DATE} <= ?"
        val selectionArg = arrayOf(startMillis.toString(), endMillis.toString())
        val sortOrder = "${Telephony.Sms.DATE} ASC"

        buildList {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                context.contentResolver.query(Telephony.Sms.CONTENT_URI,
                    projection,
                    selection,
                    selectionArg,
                    sortOrder
                )?.use{ cursor->
                    val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                    val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                    val subjectIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.SUBJECT)
                    val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                    val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                    val typeIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)

                    while(cursor.moveToNext()){
                        add(MessageData(id = cursor.getLong(idIndex), address = cursor.getString(addressIndex),
                            subject = cursor.getString(subjectIndex), body = cursor.getString(bodyIndex),
                            date = cursor.getLong(dateIndex), type = cursor.getInt(typeIndex)))
                    }
                }
            }

        }


    }
}