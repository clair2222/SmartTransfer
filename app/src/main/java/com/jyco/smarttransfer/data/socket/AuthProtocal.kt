package com.jyco.smarttransfer.data.socket

import kotlin.random.Random

object AuthProtocal {
    private const val AUTH_PREFIX = "AUTH|"
    const val AUTH_OK = "AUTH_OK"
    const val AUTH_FAIL = "AUTH_FAIL"

    fun createAuthRequest(pin:String): String {
        return AUTH_PREFIX+pin

    }
    fun parsePin(message:String?): String {
        if (message == null) return ""

        if(!message.startsWith(AUTH_PREFIX)){
            return ""
        }
        return message.removePrefix((AUTH_PREFIX))
            .takeIf{it.matches(Regex("\\d{6}"))}.toString()
    }
    fun generatePin():String{
        return Random.nextInt(100_000, 1_000_000).toString()
    }

}