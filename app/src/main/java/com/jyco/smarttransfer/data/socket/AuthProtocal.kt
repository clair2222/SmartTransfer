package com.jyco.smarttransfer.data.socket

object AuthProtocal {
    private const val AUTH_PREFIX = "AUTH|"
    const val AUTH_OK = "AUTH_OK"
    const val AUTH_FAIL = "AUTH_FAIL"

    fun createAuthRequest(pin:String): String {
        return AUTH_PREFIX+pin

    }
    fun parsePin(message:String): String? {
        if(!message.startsWith(AUTH_PREFIX)){
            return null
        }
        return message
            .removePrefix((AUTH_PREFIX))
            .takeIf{it.matches(Regex("\\d{6}"))}
    }

}