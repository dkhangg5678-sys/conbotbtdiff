package com.conbot.client.data

import com.conbot.client.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min
import kotlin.random.Random

class DeviceSocket(private val client: OkHttpClient = OkHttpClient()) {
  private var socket: WebSocket? = null
  private var reconnectJob: Job? = null

  fun connect(scope: CoroutineScope, credential: DeviceCredential, onState: (Boolean) -> Unit) {
    reconnectJob?.cancel()
    reconnectJob = scope.launch {
      var attempt = 0
      while (isActive) {
        val active = AtomicBoolean(false)
        val request = Request.Builder().url(BuildConfig.SERVER_URL.replace("https://", "wss://") + "/ws")
          .header("Authorization", "Device ${credential.deviceId}.${credential.secret}").build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
          override fun onOpen(webSocket: WebSocket, response: Response) { active.set(true); attempt = 0; onState(true) }
          override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { active.set(false); onState(false) }
          override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { active.set(false); onState(false) }
        })
        delay(1_000)
        while (isActive && active.get()) {
          delay(30_000)
          if (active.get()) socket?.send(JSONObject().put("type", "heartbeat").put("appVersion", BuildConfig.VERSION_NAME).toString())
        }
        attempt++
        delay(min(60_000L, 1_000L shl min(attempt, 6)) + Random.nextLong(500))
      }
    }
  }

  fun disconnect() { reconnectJob?.cancel(); reconnectJob = null; socket?.close(1000, "App stopped"); socket = null }
}
