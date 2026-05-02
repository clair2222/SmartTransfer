package com.jyco.smarttransfer

import android.app.Application
import com.jyco.smarttransfer.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SmartTransferApp : Application(){
    override fun onCreate() {
        super.onCreate()

        startKoin{
            androidContext(this@SmartTransferApp)
            modules(appModule)
        }
    }
}