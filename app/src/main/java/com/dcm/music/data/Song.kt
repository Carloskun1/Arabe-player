package com.dcm.music.data

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: Uri,
    val artUri: Uri,
    val dateAdded: Long,
    val folder: String
)

data class Group(val name: String, val songs: List<Song>)

data class PlaylistUi(val id: Long, val name: String, val songs: List<Song>)

enum class SortBy(val label: String) {
    NAME("Nombre"),
    ARTIST("Artista"),
    ALBUM("Álbum"),
    DURATION("Duración"),
    DATE("Fecha")
}

fun sortSongs(list: List<Song>, by: SortBy): List<Song> = when (by) {
    SortBy.NAME -> list.sortedBy { it.title.lowercase() }
    SortBy.ARTIST -> list.sortedBy { it.artist.lowercase() }
    SortBy.ALBUM -> list.sortedBy { it.album.lowercase() }
    SortBy.DURATION -> list.sortedBy { it.durationMs }
    SortBy.DATE -> list.sortedByDescending { it.dateAdded }
}

fun folderName(path: String): String = path.substringAfterLast('/').ifBlank { "Raíz" }
