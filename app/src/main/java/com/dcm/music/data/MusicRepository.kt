package com.dcm.music.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {

    suspend fun loadSongs(): List<Song> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Song>()
        try {
            val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATE_ADDED
            )
            val selection =
                "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 10000"
            val sort = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
            val artBase = Uri.parse("content://media/external/audio/albumart")

            context.contentResolver.query(collection, projection, selection, null, sort)?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val albumId = c.getLong(albumIdCol)
                    val artist = c.getString(artistCol).orEmpty()
                    val album = c.getString(albumCol).orEmpty()
                    result.add(
                        Song(
                            id = id,
                            title = c.getString(titleCol).orEmpty().ifBlank { "Sin título" },
                            artist = if (artist.isBlank() || artist == "<unknown>") "Artista desconocido" else artist,
                            album = if (album.isBlank() || album == "<unknown>") "Álbum desconocido" else album,
                            albumId = albumId,
                            durationMs = c.getLong(durCol),
                            uri = ContentUris.withAppendedId(collection, id),
                            artUri = ContentUris.withAppendedId(artBase, albumId),
                            dateAdded = c.getLong(dateCol)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Sin permisos o error de lectura: se devuelve lo que se haya leído
        }
        result
    }
}
