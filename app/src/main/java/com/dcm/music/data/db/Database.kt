package com.dcm.music.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "favorites")
data class FavoriteEntity(@PrimaryKey val songId: Long, val addedAt: Long)

@Entity(tableName = "recents")
data class RecentEntity(@PrimaryKey val songId: Long, val playedAt: Long)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    indices = [Index("playlistId")]
)
data class PlaylistSongEntity(val playlistId: Long, val songId: Long, val position: Int)

/** Canción de YouTube guardada en la biblioteca. */
@Entity(tableName = "yt_songs", indices = [Index(value = ["ytId"], unique = true)])
data class YtSongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ytId: String,
    val title: String,
    val uploader: String,
    val durationSec: Long,
    val thumbnail: String?,
    val addedAt: Long
)

/** Playlist de YouTube guardada (nombre local; los videos se guardan como yt_songs). */
@Entity(tableName = "yt_playlists")
data class YtPlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long
)

@Entity(
    tableName = "yt_playlist_songs",
    primaryKeys = ["playlistId", "ytSongId"],
    indices = [Index("playlistId")]
)
data class YtPlaylistSongEntity(val playlistId: Long, val ytSongId: Long, val position: Int)

@Dao
abstract class LibraryDao {

    @Query("SELECT songId FROM favorites ORDER BY addedAt DESC")
    abstract fun favorites(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun addFavorite(item: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    abstract suspend fun removeFavorite(songId: Long)

    @Query("SELECT songId FROM recents ORDER BY playedAt DESC LIMIT 50")
    abstract fun recents(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun addRecent(item: RecentEntity)

    @Query("DELETE FROM recents WHERE songId NOT IN (SELECT songId FROM recents ORDER BY playedAt DESC LIMIT 50)")
    abstract suspend fun trimRecents()

    @Query("DELETE FROM recents")
    abstract suspend fun clearRecents()

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    abstract fun playlists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist_songs ORDER BY playlistId, position")
    abstract fun allPlaylistSongs(): Flow<List<PlaylistSongEntity>>

    @Insert
    abstract suspend fun insertPlaylist(item: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    abstract suspend fun renamePlaylist(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    abstract suspend fun deletePlaylist(id: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :id")
    abstract suspend fun clearPlaylistSongs(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addPlaylistSong(item: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    abstract suspend fun removePlaylistSong(playlistId: Long, songId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_songs WHERE playlistId = :playlistId")
    abstract suspend fun maxPosition(playlistId: Long): Int

    @Query("UPDATE playlist_songs SET position = :position WHERE playlistId = :playlistId AND songId = :songId")
    abstract suspend fun setPosition(playlistId: Long, songId: Long, position: Int)

    @Transaction
    open suspend fun deletePlaylistFully(id: Long) {
        clearPlaylistSongs(id)
        deletePlaylist(id)
    }

    @Transaction
    open suspend fun reorder(playlistId: Long, orderedSongIds: List<Long>) {
        orderedSongIds.forEachIndexed { index, songId -> setPosition(playlistId, songId, index) }
    }

    // ----- Biblioteca de YouTube -----

    @Query("SELECT * FROM yt_songs ORDER BY addedAt DESC")
    abstract fun ytSongs(): Flow<List<YtSongEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertYtSong(item: YtSongEntity): Long

    @Query("SELECT * FROM yt_songs WHERE ytId = :ytId")
    abstract suspend fun ytSongByYtId(ytId: String): YtSongEntity?

    @Query("DELETE FROM yt_songs WHERE id = :id")
    abstract suspend fun deleteYtSong(id: Long)

    @Query("DELETE FROM yt_playlist_songs WHERE ytSongId = :id")
    abstract suspend fun deleteYtPlaylistSongsFor(id: Long)

    @Transaction
    open suspend fun deleteYtSongFully(id: Long) {
        deleteYtPlaylistSongsFor(id)
        deleteYtSong(id)
    }

    @Query("SELECT * FROM yt_playlists ORDER BY createdAt DESC")
    abstract fun ytPlaylists(): Flow<List<YtPlaylistEntity>>

    @Query("SELECT * FROM yt_playlist_songs ORDER BY playlistId, position")
    abstract fun allYtPlaylistSongs(): Flow<List<YtPlaylistSongEntity>>

    @Insert
    abstract suspend fun insertYtPlaylist(item: YtPlaylistEntity): Long

    @Query("UPDATE yt_playlists SET name = :name WHERE id = :id")
    abstract suspend fun renameYtPlaylist(id: Long, name: String)

    @Query("DELETE FROM yt_playlists WHERE id = :id")
    abstract suspend fun deleteYtPlaylist(id: Long)

    @Query("DELETE FROM yt_playlist_songs WHERE playlistId = :id")
    abstract suspend fun clearYtPlaylistSongs(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addYtPlaylistSong(item: YtPlaylistSongEntity)

    @Query("DELETE FROM yt_playlist_songs WHERE playlistId = :playlistId AND ytSongId = :ytSongId")
    abstract suspend fun removeYtPlaylistSong(playlistId: Long, ytSongId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM yt_playlist_songs WHERE playlistId = :playlistId")
    abstract suspend fun maxYtPosition(playlistId: Long): Int

    @Transaction
    open suspend fun deleteYtPlaylistFully(id: Long) {
        clearYtPlaylistSongs(id)
        deleteYtPlaylist(id)
    }

    /** Guarda una canción de YouTube si no existe y devuelve su id de fila. */
    @Transaction
    open suspend fun upsertYtSong(song: YtSongEntity): Long {
        ytSongByYtId(song.ytId)?.let { return it.id }
        return insertYtSong(song)
    }
}

@Database(
    entities = [
        FavoriteEntity::class, RecentEntity::class, PlaylistEntity::class, PlaylistSongEntity::class,
        YtSongEntity::class, YtPlaylistEntity::class, YtPlaylistSongEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): LibraryDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dcm_music.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}