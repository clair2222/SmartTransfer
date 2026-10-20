package com.jyco.smarttransfer.data.contact

import android.content.Context
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactRepository(private val context : Context) {


    suspend fun getContacts() = withContext(Dispatchers.IO){

        val contacts = mutableListOf<ContactData>()

        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
        )

        context.contentResolver.query(ContactsContract.Contacts.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC"
            )?.use{ cursor->

            val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            while(cursor.moveToNext()){
                val contactId = cursor.getLong(idIndex)
                val name = cursor.getString(nameIndex)
                val phoneNumbers = getPhoneNumbers(contactId)
                val emails = getEmails(contactId)
                contacts.add(ContactData(id = contactId, name = name, phones = phoneNumbers, emails = emails))
            }
        }
        contacts
    }

    private fun getPhoneNumbers(contactId : Long) : List<String>{
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val selection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?"
        val selectionArg = arrayOf(contactId.toString())
        val numbers = mutableListOf<String>()

        context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            selectionArg,
            null
            )?.use{ cursor->
                val numberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while(cursor.moveToNext()){
                    numbers += cursor.getString(numberIndex)
                }

        }
        return numbers
    }
    private fun getEmails(contactId : Long) : List<String>{
        val emails = mutableListOf<String>()

        val projection = arrayOf(ContactsContract.CommonDataKinds.Email.CONTACT_ID,
            ContactsContract.CommonDataKinds.Email.ADDRESS)
        val selection = "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?"
        val selectionArgs = arrayOf(contactId.toString())

        context.contentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null)?.use{ cursor->
                val idAddress = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
                while(cursor.moveToNext()){
                    emails += cursor.getString(idAddress)
                }
        }
        return emails
    }
}