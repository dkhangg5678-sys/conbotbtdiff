package com.conbot.client

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF15253D)
private val Blue = Color(0xFF155EEF)
private val Mist = Color(0xFFF7F9FC)
private val Slate = Color(0xFF5E6C84)
private val Green = Color(0xFF14866D)

@Composable
fun ConbotTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = Blue, background = Mist, surface = Color.White, onSurface = Ink), content = content)
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
  val state by viewModel.state.collectAsState()
  Surface(color = Mist, modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(38.dp).background(Blue, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
          Icon(Icons.Outlined.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
        }
        Column { Text("CONBOT", color = Ink, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp); Text("Kết nối thiết bị", color = Slate, fontSize = 14.sp) }
      }
      Spacer(Modifier.weight(1f))
      Box(Modifier.size(112.dp).background(if (state.connected) Green.copy(alpha = .12f) else Blue.copy(alpha = .10f), CircleShape), contentAlignment = Alignment.Center) {
        if (state.busy) CircularProgressIndicator(color = Blue)
        else Box(Modifier.size(22.dp).background(if (state.connected) Green else Blue, CircleShape))
      }
      Spacer(Modifier.height(28.dp))
      Text(if (state.connected) "Đã kết nối an toàn" else if (state.provisioned) "Đang kết nối lại" else "Đang kích hoạt thiết bị", color = Ink, fontWeight = FontWeight.Bold, fontSize = 26.sp, textAlign = TextAlign.Center)
      Spacer(Modifier.height(10.dp))
      Text(if (state.provisioned) "Credential riêng của thiết bị được bảo vệ bởi Android Keystore." else "Ứng dụng đang xác minh quyền kích hoạt với máy chủ.", color = Slate, fontSize = 16.sp, lineHeight = 24.sp, textAlign = TextAlign.Center)
      state.error?.let {
        Spacer(Modifier.height(20.dp)); Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        if (!state.provisioned) { Spacer(Modifier.height(12.dp)); Button(onClick = viewModel::retry) { Text("Thử lại") } }
      }
      Spacer(Modifier.weight(1f))
      OutlinedButton(onClick = viewModel::requestReset, enabled = state.provisioned && !state.busy, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp)) {
        Icon(Icons.Outlined.Refresh, contentDescription = null); Text("Cấp lại kết nối", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.SemiBold)
      }
      Spacer(Modifier.height(12.dp))
      Text("Chỉ xóa credential cũ sau khi máy chủ đã chấp thuận.", color = Slate, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
  }
  if (state.confirmReset) AlertDialog(
    onDismissRequest = viewModel::cancelReset,
    title = { Text("Cấp lại kết nối?") },
    text = { Text("Máy chủ sẽ xác minh credential hiện tại và cấp credential mới. Nếu đang ngoại tuyến, credential hiện tại được giữ nguyên.") },
    confirmButton = { Button(onClick = viewModel::reset, colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text("Tiếp tục") } },
    dismissButton = { TextButton(onClick = viewModel::cancelReset) { Text("Hủy") } },
  )
}
