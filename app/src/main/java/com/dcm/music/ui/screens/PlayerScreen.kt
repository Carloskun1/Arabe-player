package com.dcm.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.dcm.music.R
import com.dcm.music.playback.PlayerUiState
import com.dcm.music.ui.components.Cover
import com.dcm.music.ui.components.formatTime
import com.dcm.music.ui.theme.DcmBlack
import com.dcm.music.ui.theme.DcmElectric
import com.dcm.music.ui.theme.DcmGray
import com.dcm.music.ui.theme.DcmNeon

@Composable
fun PlayerScreen(
    state: PlayerUiState,
    onClose: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onQueue: () -> Unit
) {
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val duration = state.durationMs.coerceAtLeast(1L)
    val shown = if (dragging) dragValue else state.positionMs.toFloat() / duration

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(DcmElectric.copy(alpha = 0.45f), Color(0xFF07102A), DcmBlack)
                )
            )
            .clickable(enabled = false) {}
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.KeyboardArrowDown, "Cerrar", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Text(
                    "REPRODUCIENDO",
                    color = DcmNeon,
                    fontSize = 12.sp,
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onQueue) {
                    Icon(painterResource(R.drawable.ic_queue), "Cola de reproducción", tint = Color.White)
                }
            }

            Spacer(Modifier.height(16.dp))
            Cover(
                state.artUri,
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                24.dp
            )
            Spacer(Modifier.height(28.dp))

            Text(
                state.title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                state.artist,
                color = DcmGray,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    state.album,
                    color = DcmGray.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onFavorite) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        "Favorito",
                        tint = if (isFavorite) DcmNeon else Color.White
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Slider(
                value = shown.coerceIn(0f, 1f),
                onValueChange = { dragging = true; dragValue = it },
                onValueChangeFinished = {
                    onSeek((dragValue * duration).toLong())
                    dragging = false
                },
                colors = SliderDefaults.colors(
                    thumbColor = DcmNeon,
                    activeTrackColor = DcmNeon,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val current = if (dragging) (dragValue * duration).toLong() else state.positionMs
                Text(formatTime(current), color = DcmGray, fontSize = 12.sp)
                Text(formatTime(state.durationMs), color = DcmGray, fontSize = 12.sp)
            }

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onShuffle) {
                    Icon(
                        painterResource(R.drawable.ic_shuffle), "Aleatorio",
                        tint = if (state.shuffle) DcmNeon else Color.White
                    )
                }
                IconButton(onClick = onPrevious) {
                    Icon(painterResource(R.drawable.ic_skip_previous), "Anterior", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Box(
                    Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(DcmNeon)
                        .clickable(onClick = onToggle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                        "Reproducir o pausar",
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(painterResource(R.drawable.ic_skip_next), "Siguiente", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = onRepeat) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painterResource(R.drawable.ic_repeat), "Repetir",
                            tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) DcmNeon else Color.White
                        )
                        if (state.repeatMode == Player.REPEAT_MODE_ONE) {
                            Text("1", color = DcmNeon, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}
