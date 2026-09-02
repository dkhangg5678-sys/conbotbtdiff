package com.conbot.client

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conbot.client.data.DeviceSocket
import com.conbot.client.data.ProvisioningRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
  val provisioned: Boolean = true,
  val connected: Boolean = false,
  val busy: Boolean = false,
  val error: String? = null,
  val confirmReset: Boolean = false,
)

class MainViewModel(
  private val repository: ProvisioningRepository,
  private val socket: DeviceSocket,
) : ViewModel() {

  private val mutableState = MutableStateFlow(MainUiState())
  val state: StateFlow<MainUiState> = mutableState.asStateFlow()

  init {
    repository.current()?.let { credential ->
      socket.connect(viewModelScope, credential) { connected ->
        mutableState.update {
          it.copy(connected = connected)
        }
      }
    }
  }

  fun requestReset() {
    mutableState.update {
      it.copy(confirmReset = true, error = null)
    }
  }

  fun cancelReset() {
    mutableState.update {
      it.copy(confirmReset = false)
    }
  }

  fun reset() = viewModelScope.launch {
    mutableState.update {
      it.copy(
        confirmReset = false,
        busy = true,
        error = null,
      )
    }

    runCatching {
      repository.resetProvisioning()
    }.onSuccess { replacement ->
      socket.disconnect()

      mutableState.update {
        it.copy(
          busy = false,
          provisioned = true,
          connected = false,
        )
      }

      socket.connect(viewModelScope, replacement) { connected ->
        mutableState.update {
          it.copy(connected = connected)
        }
      }
    }.onFailure { throwable ->
      mutableState.update {
        it.copy(
          busy = false,
          error = throwable.message ?: "Không thể cấp lại kết nối",
        )
      }
    }
  }

  fun retry() {
    mutableState.update {
      it.copy(error = null)
    }

    repository.current()?.let { credential ->
      socket.connect(viewModelScope, credential) { connected ->
        mutableState.update {
          it.copy(connected = connected)
        }
      }
    }
  }

  override fun onCleared() {
    socket.disconnect()
    super.onCleared()
  }
}
