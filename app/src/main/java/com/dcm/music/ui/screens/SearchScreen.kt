package com.dcm.music.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcm.music.data.Group
import com.dcm.music.data.PlaylistUi
import com.dcm.music.data.Song
import com.dcm.music.data.folderName
import com.dcm.music.data.yt.YtPlaylist
import com.dcm.music.data.yt.YtSearchState
import com.dcm.music.data.yt.YtVideo
import com.dcm.music.ui.components.DcmChip
import com.dcm.music.ui.components.SectionTitle
import com.dcm.music.ui.components.SongRow
import com.dcm.music.ui.theme.DcmGray
import com.dcm.music.ui.theme.DcmNeon

private val filters = listOf("Todo", "Canciones", "Artistas", "Álbumes", "Playlists", "Carpetas")

@Composable
fun LocalSearchContent(
    songs: List<Song>,
    playlists: List<PlaylistUi>,
    currentId: String?,
    onPlay: (List<Song>, Int) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(0) }
    val q = query.trim()

    val songResults = remember(songs, q) {
        if (q.isEmpty()) emptyList()
        else songs.filter {
            it.title.contains(q, true) || it.artist.contains(q, true) || it.album.contains(q, true)
        }
    }
    val artistResults = remember(songs, q) {
        if (q.isEmpty()) emptyList()
        else songs.groupBy { it.artist }.filter { it.key.contains(q, true) }.map { Group(it.key, it.value) }
    }
    val albumResults = remember(songs, q) {
        if (q.isEmpty()) emptyList()
        else songs.groupBy { it.album }.filter { it.key.contains(q, true) }.map { Group(it.key, it.value) }
    }
    val folderResults = remember(songs, q) {
        if (q.isEmpty()) emptyList()
        else songs.groupBy { it.folder }.filter { folderName(it.key).contains(q, true) }.map { Group(it.key, it.value) }
    }
    val playlistResults = remember(playlists, q) {
        if (q.isEmpty()) emptyList()
        else playlists.filter { it.name.contains(q, true) && it.songs.isNotEmpty() }
    }

    Column {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Canciones, artistas, álbumes, playlists…", color = DcmGray) },
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
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEachIndexed { index, name ->
                DcmChip(name, filter == index) { filter = index }
            }
        }

        val nothing = songResults.isEmpty() && artistResults.isEmpty() && albumResults.isEmpty() &&
            folderResults.isEmpty() && playlistResults.isEmpty()

        if (q.isEmpty()) {
            Text("Escribe para buscar en tu música.", color = DcmGray, modifier = Modifier.padding(16.dp))
        } else if (nothing) {
            Text("Sin resultados para «$q».", color = DcmGray, modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                if ((filter == 0 || filter == 4) && playlistResults.isNotEmpty()) {
                    item { SectionTitle("Playlists") }
                    items(playlistResults, key = { "p_" + it.id }) { pl ->
                        GroupRow(Group(pl.name, pl.songs), round = false) { onPlay(pl.songs, 0) }
                    }
                }
                if ((filter == 0 || filter == 2) && artistResults.isNotEmpty()) {
                    item { SectionTitle("Artistas") }
                    items(artistResults, key = { "a_" + it.name }) { g ->
                        GroupRow(g, round = true) { onPlay(g.songs, 0) }
                    }
                }
                if ((filter == 0 || filter == 3) && albumResults.isNotEmpty()) {
                    item { SectionTitle("Álbumes") }
                    items(albumResults, key = { "b_" + it.name }) { g ->
                        GroupRow(g, round = false) { onPlay(g.songs, 0) }
                    }
                }
                if ((filter == 0 || filter == 5) && folderResults.isNotEmpty()) {
                    item { SectionTitle("Carpetas") }
                    items(folderResults, key = { "f_" + it.name }) { g ->
                        GroupRow(g, round = false, title = folderName(g.name), subtitle = g.name) { onPlay(g.songs, 0) }
                    }
                }
                if ((filter == 0 || filter == 1) && songResults.isNotEmpty()) {
                    item { SectionTitle("Canciones") }
                    items(songResults, key = { "s_" + it.id }) { s ->
                        SongRow(s, s.id.toString() == currentId) { onPlay(songResults, songResults.indexOf(s)) }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchScreen(
    songs: List<Song>,
    playlists: List<PlaylistUi>,
    currentId: String?,
    onPlay: (List<Song>, Int) -> Unit,
    yt: YtSearchState,
    onYtSearch: (String) -> Unit,
    onYtSearchPlaylists: (String) -> Unit,
    onYtPlay: (List<YtVideo>, Int) -> Unit,
    onYtDownload: (YtVideo) -> Unit,
    onYtQueue: (YtVideo) -> Unit,
    onYtNext: (YtVideo) -> Unit,
    onYtSave: (YtVideo) -> Unit,
    onYtImportPlaylist: (YtPlaylist) -> Unit
) {
    var mode by remember { mutableStateOf(0) }
    Column {
        Text(
            "Buscar",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
        Row(
            Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DcmChip("Mi música", mode == 0) { mode = 0 }
            DcmChip("YouTube", mode == 1) { mode = 1 }
        }
        if (mode == 0) {
            LocalSearchContent(songs, playlists, currentId, onPlay)
        } else {
            YoutubeSearchContent(
                state = yt,
                currentId = currentId,
                onSearchSongs = onYtSearch,
                onSearchPlaylists = onYtSearchPlaylists,
                onPlay = onYtPlay,
                onDownload = onYtDownload,
                onQueue = onYtQueue,
                onNext = onYtNext,
                onSave = onYtSave,
                onImportPlaylist = onYtImportPlaylist
            )
        }
    }
}
