package com.jyco.smarttransfer.data.message

data class MessageData(
    val id: Long,
    val address: String? = null,
    val subject: String? = null,
    val body: String? = null,
    val date: Long? = null,
    val type: Int,
    val attachment: Byte? = null
)