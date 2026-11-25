package com.dcm.music.ui.screens

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcm.music.data.yt.YtPlaylist
import com.dcm.music.data.yt.YtSearchState
import com.dcm.music.data.yt.YtVideo
import com.dcm.music.ui.components.Cover
import com.dcm.music.ui.components.DcmChip
import com.dcm.music.ui.components.formatTime
import com.dcm.music.ui.theme.DcmGray
import com.dcm.music.ui.theme.DcmNeon

@Composable
fun YoutubeSearchContent(
    state: YtSearchState,
    currentId: String?,
    onSearchSongs: (String) -> Unit,
    onSearchPlaylists: (String) -> Unit,
    onPlay: (List<YtVideo>, Int) -> Unit,
    onDownload: (YtVideo) -> Unit,
    onQueue: (YtVideo) -> Unit,
    onNext: (YtVideo) -> Unit,
    onSave: (YtVideo) -> Unit,
    onImportPlaylist: (YtPlaylist) -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    var query by remember(state.query) { mutableStateOf(state.query) }
    val focus = LocalFocusManager.current
    val doSearch = {
        if (query.isNotBlank()) {
            focus.clearFocus()
            if (tab == 0) onSearchSongs(query.trim()) else onSearchPlaylists(query.trim())
        }
    }

    Column {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = {
                Text(
                    if (tab == 0) "Buscar canciones o videos en YouTube" else "Buscar playlists de YouTube",
                    color = DcmGray
                )
            },
            trailingIcon = {
                IconButton(onClick = { doSearch() }) {
                    Icon(Icons.Filled.Search, "Buscar", tint = DcmNeon)
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { doSearch() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DcmNeon,
                unfocusedBorderColor = DcmGray.copy(alpha = 0.4f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = DcmNeon
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DcmChip("Canciones", tab == 0) { tab = 0 }
            DcmChip("Playlists", tab == 1) { tab = 1 }
        }
        Text(
            "Solo audio. Úsalo con contenido que tengas derecho a escuchar o descargar.",
            color = DcmGray,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        when {
            state.loading -> {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DcmNeon)
                }
            }
            state.error != null -> {
                Text(state.error, color = DcmGray, modifier = Modifier.padding(16.dp))
            }
            !state.searched -> {
                Text("Escribe qué quieres escuchar y pulsa buscar.", color = DcmGray, modifier = Modifier.padding(16.dp))
            }
            (if (tab == 0) state.results else state.playlistResults).isEmpty() -> {
                Text("Sin resultados.", color = DcmGray, modifier = Modifier.padding(16.dp))
            }
            tab == 0 -> {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    itemsIndexed(state.results, key = { _, v -> v.id }) { index, video ->
                        YtRow(
                            video = video,
                            isCurrent = currentId == "yt:${video.id}",
                            onClick = { onPlay(state.results, index) },
                            onDownload = { onDownload(video) },
                            onQueue = { onQueue(video) },
                            onNext = { onNext(video) },
                            onSave = { onSave(video) }
                        )
                    }
                }
            }
            else -> {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    itemsIndexed(state.playlistResults, key = { _, p -> p.id }) { _, pl ->
                        YtPlaylistRow(playlist = pl, onImport = { onImportPlaylist(pl) })
                    }
                }
            }
        }
    }
}

@Composable
private fun YtPlaylistRow(playlist: YtPlaylist, onImport: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onImport)
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(playlist.thumbnail?.let { Uri.parse(it) }, Modifier.width(88.dp).height(50.dp), 8.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                playlist.title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                buildString {
                    append(playlist.uploader)
                    if (playlist.videoCount > 0) append(" • ${playlist.videoCount} videos")
                },
                color = DcmGray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Filled.MoreVert, "Más opciones", tint = DcmGray)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Importar a mi biblioteca") }, onClick = { menuOpen = false; onImport() })
            }
        }
    }
}

@Composable
private fun YtRow(
    video: YtVideo,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onQueue: () -> Unit,
    onNext: () -> Unit,
    onSave: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(video.thumbnail?.let { Uri.parse(it) }, Modifier.width(88.dp).height(50.dp), 8.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                video.title,
                color = if (isCurrent) DcmNeon else Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${video.uploader} • ${formatTime(video.durationSec * 1000)}",
                color = DcmGray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Filled.MoreVert, "Más opciones", tint = DcmGray)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Guardar en mi biblioteca") }, onClick = { menuOpen = false; onSave() })
                DropdownMenuItem(text = { Text("Descargar audio") }, onClick = { menuOpen = false; onDownload() })
                DropdownMenuItem(text = { Text("Reproducir a continuación") }, onClick = { menuOpen = false; onNext() })
                DropdownMenuItem(text = { Text("Añadir a la cola") }, onClick = { menuOpen = false; onQueue() })
            }
        }
    }
}
