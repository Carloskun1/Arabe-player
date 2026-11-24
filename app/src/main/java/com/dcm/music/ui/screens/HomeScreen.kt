package com.dcm.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcm.music.data.Group
import com.dcm.music.data.Song
import com.dcm.music.ui.components.CoverCard
import com.dcm.music.ui.components.SectionTitle
import com.dcm.music.ui.theme.DcmElectric
import com.dcm.music.ui.theme.DcmGray
import com.dcm.music.ui.theme.DcmNeon
import java.util.Calendar

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Buenos días"
        hour < 20 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}

@Composable
fun HomeScreen(
    songs: List<Song>,
    onPlay: (List<Song>, Int) -> Unit,
    onShuffleAll: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenLibrary: () -> Unit
) {
    val greeting = remember { greeting() }
    val recent = remember(songs) { songs.sortedByDescending { it.dateAdded }.take(12) }
    val selection = remember(songs) {
        val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR).toLong()
        songs.shuffled(java.util.Random(day)).take(12)
    }
    val artists = remember(songs) { songs.groupBy { it.artist }.map { Group(it.key, it.value) }.sortedBy { it.name.lowercase() }.take(20) }
    val albums = remember(songs) { songs.groupBy { it.album }.map { Group(it.key, it.value) }.sortedBy { it.name.lowercase() }.take(20) }

    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(DcmElectric.copy(alpha = 0.30f), Color.Transparent)))
                    .padding(16.dp)
            ) {
                Text("DCM MUSIC", color = DcmNeon, fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 3.sp)
                Spacer(Modifier.height(6.dp))
                Text(greeting, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 30.sp)
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onOpenSearch)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Search, null, tint = DcmGray)
                    Spacer(Modifier.width(10.dp))
                    Text("Buscar canciones, artistas, álbumes", color = DcmGray)
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeButton("Reproducir todo", Modifier.weight(1f)) { if (songs.isNotEmpty()) onPlay(songs, 0) }
                    HomeButton("Aleatorio", Modifier.weight(1f)) { if (songs.isNotEmpty()) onShuffleAll() }
                    HomeButton("Biblioteca", Modifier.weight(1f), onOpenLibrary)
                }
            }
        }

        if (songs.isEmpty()) {
            item {
                Text(
                    "No se encontró música en tu dispositivo.\nCopia canciones al teléfono y pulsa «Volver a escanear» en Ajustes.",
                    color = DcmGray,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            item { SectionTitle("Añadidas recientemente") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp)) {
                    items(recent, key = { it.id }) { s ->
                        CoverCard(s.artUri, s.title, s.artist) { onPlay(recent, recent.indexOf(s)) }
                    }
                }
            }
            item { SectionTitle("DCM Selection") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp)) {
                    items(selection, key = { it.id }) { s ->
                        CoverCard(s.artUri, s.title, s.artist) { onPlay(selection, selection.indexOf(s)) }
                    }
                }
            }
            item { SectionTitle("Artistas") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp)) {
                    items(artists, key = { it.name }) { g ->
                        CoverCard(g.songs.first().artUri, g.name, "${g.songs.size} canciones", round = true) { onPlay(g.songs, 0) }
                    }
                }
            }
            item { SectionTitle("Álbumes") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp)) {
                    items(albums, key = { it.name }) { g ->
                        CoverCard(g.songs.first().artUri, g.name, g.songs.first().artist) { onPlay(g.songs, 0) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
