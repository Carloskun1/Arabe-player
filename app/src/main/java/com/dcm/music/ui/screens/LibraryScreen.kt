package com.dcm.music.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.dcm.music.data.Song
import com.dcm.music.data.SortBy
import com.dcm.music.data.sortSongs
import com.dcm.music.ui.components.Cover
import com.dcm.music.ui.components.DcmChip
import com.dcm.music.ui.theme.DcmGray
import com.dcm.music.ui.theme.DcmNeon

@Composable
fun LibraryScreen(songs: List<Song>, currentId: String?, onPlay: (List<Song>, Int) -> Unit) {
    var tab by remember { mutableStateOf(0) }
    var sort by remember { mutableStateOf(SortBy.NAME) }
    var menuOpen by remember { mutableStateOf(false) }

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
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DcmChip("Canciones", tab == 0) { tab = 0 }
            DcmChip("Artistas", tab == 1) { tab = 1 }
            DcmChip("Álbumes", tab == 2) { tab = 2 }
        }

        if (tab == 0) {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                DcmChip("Ordenar: ${sort.label}", false) { menuOpen = true }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    SortBy.values().forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = { sort = option; menuOpen = false }
                        )
                    }
                }
            }
            val sorted = remember(songs, sort) { sortSongs(songs, sort) }
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                itemsIndexed(sorted, key = { _, s -> s.id }) { index, song ->
                    com.dcm.music.ui.components.SongRow(song, song.id.toString() == currentId) {
                        onPlay(sorted, index)
                    }
                }
            }
        } else {
            val groups = remember(songs, tab) {
                val grouped = if (tab == 1) songs.groupBy { it.artist } else songs.groupBy { it.album }
                grouped.map { Group(it.key, it.value) }.sortedBy { it.name.lowercase() }
            }
            LazyColumn(
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
            ) {
                items(groups, key = { it.name }) { g ->
                    GroupRow(g, round = tab == 1) { onPlay(g.songs, 0) }
                }
            }
        }
    }
}

@Composable
fun GroupRow(group: Group, round: Boolean, onClick: () -> Unit) {
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
            Text(group.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${group.songs.size} canciones", color = DcmGray, fontSize = 12.sp)
        }
        Text("▶", color = DcmNeon)
    }
}
