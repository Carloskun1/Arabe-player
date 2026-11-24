package com.dcm.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcm.music.data.Group
import com.dcm.music.data.PlaylistUi
import com.dcm.music.data.Song
import com.dcm.music.data.SortBy
import com.dcm.music.data.folderName
import com.dcm.music.data.sortSongs
import com.dcm.music.ui.components.ConfirmDialog
import com.dcm.music.ui.components.Cover
import com.dcm.music.ui.components.DcmChip
import com.dcm.music.ui.components.NameDialog
import com.dcm.music.ui.components.SongRow
import com.dcm.music.ui.theme.DcmGray
import com.dcm.music.ui.theme.DcmNeon

private val tabNames = listOf(
    "Canciones", "Artistas", "Álbumes", "Carpetas", "Favoritos", "Recientes", "Playlists", "Descargas"
)

@Composable
fun LibraryScreen(
    songs: List<Song>,
    favorites: List<Song>,
    recents: List<Song>,
    playlists: List<PlaylistUi>,
    sort: SortBy,
    onSortChange: (SortBy) -> Unit,
    currentId: String?,
    onPlay: (List<Song>, Int) -> Unit,
    onShuffle: (List<Song>) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onRemoveFromPlaylist: (Long, Song) -> Unit,
    onMoveInPlaylist: (Long, Int, Int) -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    var openPlaylistId by remember { mutableStateOf<Long?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<PlaylistUi?>(null) }
    var deleteTarget by remember { mutableStateOf<PlaylistUi?>(null) }

    val openPlaylist = playlists.firstOrNull { it.id == openPlaylistId }
    BackHandler(enabled = openPlaylist != null) { openPlaylistId = null }

    if (showCreate) {
        NameDialog("Nueva playlist", "", "Crear", { onCreatePlaylist(it); showCreate = false }, { showCreate = false })
    }
    renameTarget?.let { pl ->
        NameDialog("Renombrar playlist", pl.name, "Guardar", { onRenamePlaylist(pl.id, it); renameTarget = null }, { renameTarget = null })
    }
    deleteTarget?.let { pl ->
        ConfirmDialog(
            "Eliminar playlist",
            "Se eliminará «${pl.name}». Tus canciones no se borran del teléfono.",
            "Eliminar",
            { onDeletePlaylist(pl.id); deleteTarget = null; openPlaylistId = null },
            { deleteTarget = null }
        )
    }

    if (openPlaylist != null) {
        PlaylistDetail(
            pl = openPlaylist,
            currentId = currentId,
            onBack = { openPlaylistId = null },
            onPlay = onPlay,
            onShuffle = onShuffle,
            onRename = { renameTarget = openPlaylist },
            onDelete = { deleteTarget = openPlaylist },
            onRemove = onRemoveFromPlaylist,
            onMove = onMoveInPlaylist
        )
        return
    }

    Column {
        Text(
            "Biblioteca",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabNames.forEachIndexed { index, name ->
                DcmChip(name, tab == index) { tab = index }
            }
        }

        when (tab) {
            0 -> {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    DcmChip("Ordenar: ${sort.label}", false) { menuOpen = true }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        SortBy.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = { onSortChange(option); menuOpen = false }
                            )
                        }
                    }
                }
                val sorted = remember(songs, sort) { sortSongs(songs, sort) }
                SongList(sorted, currentId, "No hay canciones.", onPlay, onShuffle)
            }
            1, 2, 3 -> {
                val groups = remember(songs, tab) {
                    val grouped = when (tab) {
                        1 -> songs.groupBy { it.artist }
                        2 -> songs.groupBy { it.album }
                        else -> songs.groupBy { it.folder }
                    }
                    grouped.map { Group(it.key, it.value) }.sortedBy { it.name.lowercase() }
                }
                LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)) {
                    items(groups, key = { it.name }) { g ->
                        GroupRow(
                            group = g,
                            round = tab == 1,
                            title = if (tab == 3) folderName(g.name) else g.name,
                            subtitle = if (tab == 3) g.name else null
                        ) { onPlay(g.songs, 0) }
                    }
                }
            }
            4 -> SongList(favorites, currentId, "Aún no tienes favoritos. Usa el menú ⋮ de una canción o el corazón del reproductor.", onPlay, onShuffle)
            5 -> SongList(recents, currentId, "Todavía no has reproducido nada.", onPlay, onShuffle)
            6 -> {
                LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)) {
                    item {
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            DcmChip("+ Nueva playlist", true) { showCreate = true }
                        }
                    }
                    if (playlists.isEmpty()) {
                        item {
                            Text("Aún no tienes playlists.", color = DcmGray, modifier = Modifier.padding(16.dp))
                        }
                    }
                    items(playlists, key = { it.id }) { pl ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { openPlaylistId = pl.id }
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Cover(pl.songs.firstOrNull()?.artUri, Modifier.size(54.dp), 10.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(pl.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${pl.songs.size} canciones", color = DcmGray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            else -> {
                val downloads = remember(songs) { songs.filter { it.folder.contains("/Download", ignoreCase = true) } }
                SongList(downloads, currentId, "No hay música en tu carpeta de Descargas.", onPlay, onShuffle)
            }
        }
    }
}

@Composable
private fun SongList(
    list: List<Song>,
    currentId: String?,
    emptyText: String,
    onPlay: (List<Song>, Int) -> Unit,
    onShuffle: (List<Song>) -> Unit
) {
    if (list.isEmpty()) {
        Text(emptyText, color = DcmGray, modifier = Modifier.padding(16.dp))
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DcmChip("▶ Reproducir", true) { onPlay(list, 0) }
                DcmChip("Aleatorio", false) { onShuffle(list) }
            }
        }
        itemsIndexed(list, key = { _, s -> s.id }) { index, song ->
            SongRow(song, song.id.toString() == currentId) { onPlay(list, index) }
        }
    }
}

@Composable
private fun PlaylistDetail(
    pl: PlaylistUi,
    currentId: String?,
    onBack: () -> Unit,
    onPlay: (List<Song>, Int) -> Unit,
    onShuffle: (List<Song>) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onRemove: (Long, Song) -> Unit,
    onMove: (Long, Int, Int) -> Unit
) {
    Column {
        Row(Modifier.padding(top = 8.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Volver", tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(pl.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${pl.songs.size} canciones", color = DcmGray, fontSize = 12.sp)
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DcmChip("▶ Reproducir", true) { if (pl.songs.isNotEmpty()) onPlay(pl.songs, 0) }
            DcmChip("Aleatorio", false) { if (pl.songs.isNotEmpty()) onShuffle(pl.songs) }
            DcmChip("Renombrar", false, onRename)
            DcmChip("Eliminar", false, onDelete)
        }
        if (pl.songs.isEmpty()) {
            Text(
                "Esta playlist está vacía. Añade canciones desde el menú ⋮ de cualquier canción.",
                color = DcmGray,
                modifier = Modifier.padding(16.dp)
            )
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            itemsIndexed(pl.songs, key = { _, s -> s.id }) { index, song ->
                SongRow(
                    song = song,
                    isCurrent = song.id.toString() == currentId,
                    trailing = {
                        IconButton(onClick = { onMove(pl.id, index, index - 1) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.KeyboardArrowUp, "Subir", tint = DcmGray)
                        }
                        IconButton(onClick = { onMove(pl.id, index, index + 1) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.KeyboardArrowDown, "Bajar", tint = DcmGray)
                        }
                        IconButton(onClick = { onRemove(pl.id, song) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Close, "Quitar de la playlist", tint = DcmNeon)
                        }
                    }
                ) { onPlay(pl.songs, index) }
            }
        }
    }
}

@Composable
fun GroupRow(
    group: Group,
    round: Boolean,
    title: String = group.name,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(group.songs.first().artUri, Modifier.size(54.dp), if (round) 27.dp else 10.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle ?: "${group.songs.size} canciones",
                color = DcmGray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text("▶", color = DcmNeon)
    }
}
