package com.jyco.smarttransfer.data.contact

data class ContactData(
    val id: Long,
    val name: String? = null,
    val phones: List<String?> = emptyList(),
    val emails: List<String?> = emptyList(),
    val address: String? = null,
    val nickName: String? = null,
    val relationships: String? = null
)
