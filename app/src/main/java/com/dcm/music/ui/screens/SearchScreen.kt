package com.dcm.music.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.dcm.music.data.Song
import com.dcm.music.ui.components.DcmChip
import com.dcm.music.ui.components.SectionTitle
import com.dcm.music.ui.components.SongRow
import com.dcm.music.ui.theme.DcmGray
import com.dcm.music.ui.theme.DcmNeon

@Composable
fun SearchScreen(songs: List<Song>, currentId: String?, onPlay: (List<Song>, Int) -> Unit) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(0) } // 0 todo, 1 canciones, 2 artistas, 3 álbumes
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

    Column {
        Text(
            "Buscar",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Canciones, artistas, álbumes", color = DcmGray) },
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
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DcmChip("Todo", filter == 0) { filter = 0 }
            DcmChip("Canciones", filter == 1) { filter = 1 }
            DcmChip("Artistas", filter == 2) { filter = 2 }
            DcmChip("Álbumes", filter == 3) { filter = 3 }
        }

        if (q.isEmpty()) {
            Text("Escribe para buscar en tu música.", color = DcmGray, modifier = Modifier.padding(16.dp))
        } else if (songResults.isEmpty() && artistResults.isEmpty() && albumResults.isEmpty()) {
            Text("Sin resultados para «$q».", color = DcmGray, modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
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
