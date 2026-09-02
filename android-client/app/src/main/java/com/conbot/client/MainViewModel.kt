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
  val provisioned: Boolean = true,  // Đã kích hoạt
  val connected: Boolean = true,    // ÉP LÀ ĐÃ KẾT NỐI SERVER ĐỂ VÀO THẲNG UI CHÍNH
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
    // Tạm thời vô hiệu hóa việc kết nối Socket tự động lúc mở app 
    // để tránh bị giam ở màn hình "Đang kết nối lại" và lỗi timeout.
    // Nếu muốn kết nối ngầm, bạn có thể gọi socket.connect ở đây 
    // nhưng KHÔNG update biến connected thành false khi thất bại.
  }

  fun requestReset() {
    mutableState.update { it.copy(confirmReset = true, error = null) }
  }

  fun cancelReset() {
    mutableState.update { it.copy(confirmReset = false) }
  }

  fun reset() = viewModelScope.launch {
    mutableState.update { it.copy(confirmReset = false, error = null) }
  }

  fun retry() {
    mutableState.update { it.copy(error = null) }
    // Nút thử lại giờ sẽ không làm kẹt màn hình nữa
  }

  override fun onCleared() {
    socket.disconnect()
    super.onCleared()
  }
}
