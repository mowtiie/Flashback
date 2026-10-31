package com.mowtiie.flashback

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mowtiie.flashback.theme.FlashbackTheme
import com.mowtiie.flashback.ui.navigation.FlashbackNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as FlashbackApp).container
        setContent {
            FlashbackTheme {
                FlashbackNavHost(container)
            }
        }
    }
}
