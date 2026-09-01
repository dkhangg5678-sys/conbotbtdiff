package com.conbot.client

import android.app.Application
import com.conbot.client.data.CredentialVault
import com.conbot.client.data.DeviceSocket
import com.conbot.client.data.ProvisioningApi
import com.conbot.client.data.ProvisioningRepository

class ConbotApplication : Application() {
  val repository by lazy { ProvisioningRepository(CredentialVault(this), ProvisioningApi()) }
  val socket by lazy { DeviceSocket() }
}
