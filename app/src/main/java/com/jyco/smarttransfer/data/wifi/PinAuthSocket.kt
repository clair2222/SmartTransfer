package com.jyco.smarttransfer.data.wifi

class PinAuthSocket {
    object PinAuthProtocol{
        const val PORT = "8988"
        const val AUTH_OK = "AUTH_OK"
        const val AUTH_FAIL = "AUTH_FAIL"
    }
    fun generatePin():String{
        return (100000..999999).random().toString()
    }
}