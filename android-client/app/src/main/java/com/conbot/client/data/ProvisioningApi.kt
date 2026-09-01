package com.conbot.client.data

import com.conbot.client.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ProvisioningApi(private val client: OkHttpClient = OkHttpClient()) {
  private val json = "application/json".toMediaType()

  suspend fun provisionBootstrap(token: CharArray): DeviceCredential {
    val body = JSONObject().put("kind", "bootstrap").put("token", token.concatToString())
      .put("appVersion", BuildConfig.VERSION_NAME)
    return provision(body)
  }

  suspend fun resetGrant(current: DeviceCredential): String {
    val body = JSONObject().put("deviceId", current.deviceId).put("credential", current.secret)
    val response = execute(Request.Builder().url("${BuildConfig.SERVER_URL}/devices/reset-grant")
      .post(body.toString().toRequestBody(json)).build())
    response.use {
      if (!it.isSuccessful) throw IOException("Không thể xác nhận cấp lại kết nối")
      return JSONObject(it.body?.string().orEmpty()).getString("grant")
    }
  }

  suspend fun provisionReset(deviceId: String, grant: String): DeviceCredential = provision(
    JSONObject().put("kind", "reset").put("deviceId", deviceId).put("grant", grant)
      .put("appVersion", BuildConfig.VERSION_NAME),
  )

  private suspend fun provision(body: JSONObject): DeviceCredential {
    val response = execute(Request.Builder().url("${BuildConfig.SERVER_URL}/provision")
      .post(body.toString().toRequestBody(json)).build())
    response.use {
      if (it.code != 201) throw IOException("Thiết bị chưa được cấp quyền")
      val parsed = JSONObject(it.body?.string().orEmpty())
      if (parsed.getString("protocolVersion") != BuildConfig.PROTOCOL_VERSION) throw IOException("Phiên bản giao thức không tương thích")
      return DeviceCredential(parsed.getString("deviceId"), parsed.getString("credential"))
    }
  }

  private suspend fun execute(request: Request): Response = suspendCancellableCoroutine { continuation ->
    val call = client.newCall(request)
    continuation.invokeOnCancellation { call.cancel() }
    call.enqueue(object : Callback {
      override fun onFailure(call: Call, e: IOException) { if (continuation.isActive) continuation.resumeWithException(e) }
      override fun onResponse(call: Call, response: Response) { continuation.resume(response) }
    })
  }
}
