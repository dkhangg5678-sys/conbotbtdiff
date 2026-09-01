package com.conbot.client.data

class ProvisioningRepository(private val vault: CredentialVault, private val api: ProvisioningApi) {
  fun current(): DeviceCredential? = vault.read()

  suspend fun initialProvision(rawToken: String): DeviceCredential {
    require(rawToken.length >= 32) { "Bootstrap token unavailable" }
    val token = rawToken.toCharArray()
    return try {
      val issued = api.provisionBootstrap(token)
      check(vault.replaceAtomically(issued)) { "Không thể lưu credential an toàn" }
      issued
    } finally { token.fill('\u0000') }
  }

  suspend fun resetProvisioning(): DeviceCredential {
    val current = requireNotNull(vault.read()) { "Không có credential hiện tại" }
    val grant = api.resetGrant(current)
    val replacement = api.provisionReset(current.deviceId, grant)
    check(vault.replaceAtomically(replacement)) { "Không thể lưu credential mới" }
    return replacement
  }
}
