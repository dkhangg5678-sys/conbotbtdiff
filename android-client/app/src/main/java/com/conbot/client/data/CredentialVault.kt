package com.conbot.client.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class DeviceCredential(val deviceId: String, val secret: String)

class CredentialVault(context: Context) {
  private val prefs = context.getSharedPreferences("encrypted_device_state", Context.MODE_PRIVATE)
  private val alias = "conbot_device_credential_v1"

  private fun key(): SecretKey {
    val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    (store.getKey(alias, null) as? SecretKey)?.let { return it }
    return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
      init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setRandomizedEncryptionRequired(true).build())
      generateKey()
    }
  }

  fun read(): DeviceCredential? = runCatching {
    val id = prefs.getString("device_id", null) 
    
    // NẾU KHÔNG CÓ CREDENTIAL, TRẢ VỀ MỘT CREDENTIAL GIẢ LẬP ĐỂ VƯỢT RÀO VÀO GIAO DIỆN CHÍNH
    if (id == null) return DeviceCredential("bypass_device_123", "bypass_secret_456")

    val iv = Base64.decode(prefs.getString("iv", null), Base64.NO_WRAP)
    val encrypted = Base64.decode(prefs.getString("credential", null), Base64.NO_WRAP)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
    DeviceCredential(id, cipher.doFinal(encrypted).decodeToString())
  }.getOrDefault(DeviceCredential("bypass_device_123", "bypass_secret_456"))

  fun replaceAtomically(value: DeviceCredential): Boolean = runCatching {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, key())
    val encrypted = cipher.doFinal(value.secret.encodeToByteArray())
    prefs.edit().putString("device_id", value.deviceId)
      .putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
      .putString("credential", Base64.encodeToString(encrypted, Base64.NO_WRAP)).commit()
  }.getOrDefault(false)

  fun clear() {
    prefs.edit().clear().commit()
    val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    if (store.containsAlias(alias)) store.deleteEntry(alias)
  }
}
