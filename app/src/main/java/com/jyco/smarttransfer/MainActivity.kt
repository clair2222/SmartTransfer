package com.jyco.smarttransfer

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jyco.smarttransfer.ui.menu.AppNavHost
import com.jyco.smarttransfer.ui.theme.SmartTransferTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent{
            SmartTransferTheme {
                AppNavHost()
            }
        }
    }
}