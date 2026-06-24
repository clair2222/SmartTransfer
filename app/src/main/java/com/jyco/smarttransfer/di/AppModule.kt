package com.jyco.smarttransfer.di

import com.jyco.smarttransfer.data.socket.SocketManager
import com.jyco.smarttransfer.data.wifi.WifiDirectManager
import com.jyco.smarttransfer.viewmodel.ReceiverViewModel
import com.jyco.smarttransfer.viewmodel.SenderViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single{
        WifiDirectManager(context = get())
    }
    factory{
        SocketManager()
    }
    viewModel{
        SenderViewModel(wifiDirectManager = get(), socketManager = get())
    }
    viewModel{
        ReceiverViewModel(wifiDirectManager = get(), socketManager = get())
    }
}