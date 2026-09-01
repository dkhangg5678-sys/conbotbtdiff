package com.conbot.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val app = application as ConbotApplication
    val viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(app.repository, app.socket) as T
    })[MainViewModel::class.java]
    setContent { ConbotTheme { MainScreen(viewModel) } }
  }
}
