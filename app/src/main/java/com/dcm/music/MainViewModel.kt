package com.dcm.music

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dcm.music.data.MusicRepository
import com.dcm.music.data.PlaylistUi
import com.dcm.music.data.Prefs
import com.dcm.music.data.Song
import com.dcm.music.data.SortBy
import com.dcm.music.data.db.AppDatabase
import com.dcm.music.data.db.FavoriteEntity
import com.dcm.music.data.db.PlaylistEntity
import com.dcm.music.data.db.PlaylistSongEntity
import com.dcm.music.data.db.RecentEntity
import com.dcm.music.playback.PlayerConnection
import com.dcm.music.playback.QueueItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = MusicRepository(app)
    private val player = PlayerConnection(app)
    private val dao = AppDatabase.get(app).dao()
    private val prefs = Prefs(app)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    val playerState = player.state

    val favoriteIds: StateFlow<List<Long>> =
        dao.favorites().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favorites: StateFlow<List<Song>> =
        combine(_songs, favoriteIds) { all, ids ->
            val map = all.associateBy { it.id }
            ids.mapNotNull { map[it] }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recents: StateFlow<List<Song>> =
        combine(_songs, dao.recents()) { all, ids ->
            val map = all.associateBy { it.id }
            ids.mapNotNull { map[it] }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val playlists: StateFlow<List<PlaylistUi>> =
        combine(_songs, dao.playlists(), dao.allPlaylistSongs()) { all, lists, links ->
            val map = all.associateBy { it.id }
            val byList = links.groupBy { it.playlistId }
            lists.map { pl ->
                val songs = (byList[pl.id] ?: emptyList())
                    .sortedBy { it.position }
                    .mapNotNull { map[it.songId] }
                PlaylistUi(pl.id, pl.name, songs)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val sortBy: StateFlow<SortBy> =
        prefs.sort.stateIn(viewModelScope, SharingStarted.Eagerly, SortBy.NAME)

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(500)
                player.refreshPosition()
            }
        }
        // Historial: se guarda cada canción que empieza a sonar
        viewModelScope.launch {
            player.state
                .map { it.mediaId }
                .distinctUntilChanged()
                .filterNotNull()
                .collect { id ->
                    val songId = id.toLongOrNull()
                    if (songId != null) {
                        dao.addRecent(RecentEntity(songId, System.currentTimeMillis()))
                        dao.trimRecents()
                    }
                }
        }
    }

    fun refresh() {
        if (_loading.value) return
        viewModelScope.launch {
            _loading.value = true
            _songs.value = repo.loadSongs()
            _loading.value = false
        }
    }

    // Reproducción
    fun play(list: List<Song>, index: Int) { player.play(list, index) }
    fun playShuffled(list: List<Song>) { if (list.isNotEmpty()) player.play(list.shuffled(), 0) }
    fun togglePlay() { player.togglePlay() }
    fun next() { player.next() }
    fun previous() { player.previous() }
    fun seekTo(ms: Long) { player.seekTo(ms) }
    fun toggleShuffle() { player.toggleShuffle() }
    fun cycleRepeat() { player.cycleRepeat() }

    // Cola
    fun queue(): List<QueueItem> = player.queue()
    fun queueIndex(): Int = player.currentIndex()
    fun jumpTo(index: Int) { player.jumpTo(index) }
    fun removeFromQueue(index: Int) { player.removeFromQueue(index) }
    fun addToQueue(song: Song) { player.addToQueue(song) }
    fun playNext(song: Song) { player.playNext(song) }

    // Favoritos
    fun toggleFavoriteId(songId: Long) {
        viewModelScope.launch {
            if (songId in favoriteIds.value) dao.removeFavorite(songId)
            else dao.addFavorite(FavoriteEntity(songId, System.currentTimeMillis()))
        }
    }

    fun toggleFavorite(song: Song) { toggleFavoriteId(song.id) }

    // Historial
    fun clearRecents() { viewModelScope.launch { dao.clearRecents() } }

    // Playlists
    fun createPlaylist(name: String, firstSong: Song? = null) {
        viewModelScope.launch {
            val id = dao.insertPlaylist(PlaylistEntity(name = name.trim(), createdAt = System.currentTimeMillis()))
            if (firstSong != null) dao.addPlaylistSong(PlaylistSongEntity(id, firstSong.id, 0))
        }
    }

    fun renamePlaylist(id: Long, name: String) {
        viewModelScope.launch { dao.renamePlaylist(id, name.trim()) }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch { dao.deletePlaylistFully(id) }
    }

    fun addToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch {
            val position = dao.maxPosition(playlistId) + 1
            dao.addPlaylistSong(PlaylistSongEntity(playlistId, song.id, position))
        }
    }

    fun removeFromPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch { dao.removePlaylistSong(playlistId, song.id) }
    }

    fun moveInPlaylist(playlistId: Long, from: Int, to: Int) {
        val playlist = playlists.value.firstOrNull { it.id == playlistId } ?: return
        if (from !in playlist.songs.indices || to !in playlist.songs.indices) return
        val ids = playlist.songs.map { it.id }.toMutableList()
        val moved = ids.removeAt(from)
        ids.add(to, moved)
        viewModelScope.launch { dao.reorder(playlistId, ids) }
    }

    // Preferencias
    fun setSort(value: SortBy) {
        viewModelScope.launch { prefs.setSort(value) }
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}
