package com.jyco.smarttransfer.di

import com.jyco.smarttransfer.data.wifi.WifiDirectManager
import com.jyco.smarttransfer.viewmodel.ReceiverViewModel
import com.jyco.smarttransfer.viewmodel.SenderViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single{
        WifiDirectManager(context = get())
    }
    viewModel{
        SenderViewModel(wifiDirectManager = get())
    }
    viewModel{
        ReceiverViewModel(wifiDirectManager = get())
    }
}