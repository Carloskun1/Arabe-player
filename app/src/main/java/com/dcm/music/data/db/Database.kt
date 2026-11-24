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
}

@Database(
    entities = [FavoriteEntity::class, RecentEntity::class, PlaylistEntity::class, PlaylistSongEntity::class],
    version = 1,
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
                ).build().also { instance = it }
            }
    }
}
