package com.dcm.music.ui.screens

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
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
import com.dcm.music.YtPlaylistUi
import com.dcm.music.data.db.YtSongEntity
import com.dcm.music.data.yt.YtVideo
import com.dcm.music.ui.components.Cover
import com.dcm.music.ui.components.formatTime
import com.dcm.music.ui.theme.DcmGray
import com.dcm.music.ui.theme.DcmNeon

/**
 * Biblioteca de YouTube: canciones guardadas desde la búsqueda y playlists importadas.
 */
@Composable
fun YtLibraryScreen(
    songs: List<YtSongEntity>,
    playlists: List<YtPlaylistUi>,
    currentId: String?,
    onPlaySongs: () -> Unit,
    onPlaySong: (YtSongEntity) -> Unit,
    onDeleteSong: (YtSongEntity) -> Unit,
    onPlayPlaylist: (YtPlaylistUi) -> Unit,
    onDeletePlaylist: (YtPlaylistUi) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        if (playlists.isNotEmpty()) {
            item {
                Text(
                    "Playlists importadas",
                    color = DcmNeon,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
            items(playlists, key = { "ypl_" + it.id }) { pl ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPlayPlaylist(pl) }
                        .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.PlayArrow, null, tint = DcmNeon)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(pl.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${pl.songs.size} canciones", color = DcmGray, fontSize = 12.sp)
                    }
                    IconButton(onClick = { onDeletePlaylist(pl) }) {
                        Icon(Icons.Filled.Delete, "Eliminar", tint = DcmGray)
                    }
                }
            }
        }
        item {
            Text(
                "Canciones guardadas",
                color = DcmNeon,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
        }
        if (songs.isEmpty()) {
            item {
                Text(
                    "Guarda canciones desde la búsqueda de YouTube (menú ⋮ → Guardar) y aparecerán aquí.",
                    color = DcmGray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            item {
                Text(
                    "Reproducir todas (${songs.size})",
                    color = DcmNeon,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable(onClick = onPlaySongs)
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            items(songs, key = { "yt_" + it.id }) { song ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPlaySong(song) }
                        .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Cover(song.thumbnail?.let { Uri.parse(it) }, Modifier.width(56.dp).height(56.dp), 8.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            song.title,
                            color = if (currentId == "yt:${song.ytId}") DcmNeon else Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${song.uploader} • ${formatTime(song.durationSec * 1000)}",
                            color = DcmGray,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { onDeleteSong(song) }) {
                        Icon(Icons.Filled.Delete, "Eliminar", tint = DcmGray)
                    }
                }
            }
        }
    }
}
