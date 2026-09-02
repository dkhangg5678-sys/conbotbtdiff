package com.conbot.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // ÉP CHẠY THẲNG VÀO MÀN HÌNH CHÍNH, BỎ QUA KIỂM TRA TOKEN
                    MainScreen(
                        serverUrl = "https://conbotbtdi.onrender.com/",
                        onUrlChange = {},
                        onTestAlert = {
                            val speaker = AlertSpeaker(this@MainActivity)
                            speaker.shoutAlert("Mình không làm được, giúp mình với!")
                        },
                        onOpenOverlay = {}
                    )
                }
            }
        }
    }
}
