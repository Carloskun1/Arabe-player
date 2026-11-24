package com.dcm.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcm.music.playback.QueueItem
import com.dcm.music.ui.theme.DcmBlack
import com.dcm.music.ui.theme.DcmGray
import com.dcm.music.ui.theme.DcmNeon

@Composable
fun QueueScreen(
    items: List<QueueItem>,
    currentIndex: Int,
    onClose: () -> Unit,
    onJump: (Int) -> Unit,
    onRemove: (Int) -> Unit
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentIndex.coerceAtLeast(0))
    Column(
        Modifier
            .fillMaxSize()
            .background(DcmBlack)
            .clickable(enabled = false) {}
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(Modifier.padding(top = 8.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.KeyboardArrowDown, "Cerrar", tint = Color.White)
            }
            Text(
                "Cola de reproducción (${items.size})",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (items.isEmpty()) {
            Text("La cola está vacía.", color = DcmGray, modifier = Modifier.padding(16.dp))
        }
        LazyColumn(state = listState) {
            itemsIndexed(items) { index, item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onJump(index) }
                        .padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                        Text(
                            item.title,
                            color = if (index == currentIndex) DcmNeon else Color.White,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(item.artist, color = DcmGray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { onRemove(index) }) {
                        Icon(Icons.Filled.Close, "Quitar de la cola", tint = DcmGray)
                    }
                }
            }
        }
    }
}
