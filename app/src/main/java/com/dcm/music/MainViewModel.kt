package com.dcm.music

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
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
import com.dcm.music.data.db.YtPlaylistEntity
import com.dcm.music.data.db.YtPlaylistSongEntity
import com.dcm.music.data.db.YtSongEntity
import com.dcm.music.data.yt.YouTubeRepository
import com.dcm.music.data.yt.YtPlaylist
import com.dcm.music.data.yt.YtSearchState
import com.dcm.music.data.yt.YtVideo
import com.dcm.music.playback.PlayerConnection
import com.dcm.music.playback.QueueItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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

/** Playlist de YouTube guardada en la biblioteca, lista para la UI. */
data class YtPlaylistUi(val id: Long, val name: String, val songs: List<YtSongEntity>)

/** Convierte un video en entidad para guardar en la biblioteca. */
fun YtVideo.toEntity() = YtSongEntity(
    ytId = id,
    title = title,
    uploader = uploader,
    durationSec = durationSec,
    thumbnail = thumbnail,
    addedAt = System.currentTimeMillis()
)

/** Convierte una canción guardada en video para reproducirla de nuevo. */
fun YtSongEntity.toVideo() = YtVideo(id = ytId, title = title, uploader = uploader, durationSec = durationSec, thumbnail = thumbnail)

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

    val premium: StateFlow<Boolean> =
        prefs.premium.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setPremium(value: Boolean) {
        viewModelScope.launch { prefs.setPremium(value) }
    }

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _yt = MutableStateFlow(YtSearchState())
    val yt: StateFlow<YtSearchState> = _yt.asStateFlow()

    init {
        viewModelScope.launch { player.errors.collect { _messages.emit(it) } }
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

    // YouTube (solo audio)
    fun searchYoutube(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _yt.value = YtSearchState(query = query, loading = true)
            try {
                val results = withContext(Dispatchers.IO) { YouTubeRepository.search(query) }
                _yt.value = YtSearchState(query = query, results = results, searched = true)
            } catch (e: Exception) {
                _yt.value = YtSearchState(
                    query = query,
                    error = "No se pudo buscar en YouTube. Revisa tu conexión a internet.",
                    searched = true
                )
            }
        }
    }

    fun playYoutube(list: List<YtVideo>, index: Int) { player.playYoutube(list, index) }

    /** Reproduce canciones guardadas de la biblioteca de YouTube. */
    fun playYoutubeSaved(songs: List<YtSongEntity>) {
        if (songs.isEmpty()) return
        player.playYoutube(songs.map { it.toVideo() }, 0)
    }

    fun addYoutubeToQueue(video: YtVideo) { player.addYoutubeToQueue(video) }
    fun playYoutubeNext(video: YtVideo) { player.playYoutubeNext(video) }

    fun searchYoutubePlaylists(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _yt.value = YtSearchState(query = query, loading = true)
            try {
                val results = withContext(Dispatchers.IO) { YouTubeRepository.searchPlaylists(query) }
                _yt.value = YtSearchState(query = query, playlistResults = results, searched = true)
            } catch (e: Exception) {
                _yt.value = YtSearchState(
                    query = query,
                    error = "No se pudo buscar en YouTube. Revisa tu conexión a internet.",
                    searched = true
                )
            }
        }
    }

    /** Importa una playlist de YouTube: guarda sus canciones y crea la playlist en la biblioteca. */
    fun importYoutubePlaylist(playlist: YtPlaylist) {
        viewModelScope.launch {
            _messages.emit("Importando «${playlist.title}»…")
            try {
                val videos = withContext(Dispatchers.IO) { YouTubeRepository.playlistVideos(playlist.id) }
                if (videos.isEmpty()) {
                    _messages.emit("La playlist está vacía o no se pudo leer.")
                    return@launch
                }
                val plId = dao.insertYtPlaylist(YtPlaylistEntity(name = playlist.title, createdAt = System.currentTimeMillis()))
                videos.forEachIndexed { index, video ->
                    val rowId = dao.upsertYtSong(video.toEntity())
                    dao.addYtPlaylistSong(YtPlaylistSongEntity(plId, rowId, index))
                }
                _messages.emit("Playlist «${playlist.title}» importada (${videos.size} canciones)")
            } catch (e: Exception) {
                _messages.emit("No se pudo importar la playlist: ${e.message ?: "error desconocido"}")
            }
        }
    }

    val ytSongs: StateFlow<List<YtSongEntity>> =
        dao.ytSongs().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val ytPlaylists: StateFlow<List<YtPlaylistUi>> =
        combine(dao.ytPlaylists(), dao.allYtPlaylistSongs(), ytSongs) { lists, links, songs ->
            val map = songs.associateBy { it.id }
            val byList = links.groupBy { it.playlistId }
            lists.map { pl ->
                val songsIn = (byList[pl.id] ?: emptyList())
                    .sortedBy { it.position }
                    .mapNotNull { map[it.ytSongId] }
                YtPlaylistUi(pl.id, pl.name, songsIn)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun saveYtSong(video: YtVideo) {
        viewModelScope.launch { dao.upsertYtSong(video.toEntity()) }
    }

    fun removeYtSong(id: Long) {
        viewModelScope.launch { dao.deleteYtSongFully(id) }
    }

    fun deleteYtPlaylist(id: Long) {
        viewModelScope.launch { dao.deleteYtPlaylistFully(id) }
    }

    fun addYtSongToPlaylist(playlistId: Long, song: YtSongEntity) {
        viewModelScope.launch {
            val position = dao.maxYtPosition(playlistId) + 1
            dao.addYtPlaylistSong(YtPlaylistSongEntity(playlistId, song.id, position))
        }
    }

    fun createYtPlaylist(name: String, videos: List<YtVideo> = emptyList()) {
        viewModelScope.launch {
            val plId = dao.insertYtPlaylist(YtPlaylistEntity(name = name.trim(), createdAt = System.currentTimeMillis()))
            videos.forEachIndexed { index, video ->
                val rowId = dao.upsertYtSong(video.toEntity())
                dao.addYtPlaylistSong(YtPlaylistSongEntity(plId, rowId, index))
            }
        }
    }

    fun downloadYoutube(video: YtVideo) {
        viewModelScope.launch {
            _messages.emit("Preparando descarga…")
            try {
                val audio = withContext(Dispatchers.IO) { YouTubeRepository.resolveAudio(video.id) }
                val ext = audio.format?.suffix ?: "m4a"
                val safeName = video.title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().take(80)
                val request = DownloadManager.Request(Uri.parse(audio.content))
                    .setTitle(video.title)
                    .setDescription("DCM MUSIC")
                    .addRequestHeader("User-Agent", YouTubeRepository.USER_AGENT)
                    .setMimeType(audio.format?.mimeType ?: "audio/mp4")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, "DCM MUSIC/$safeName.$ext")
                val manager = getApplication<Application>().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                manager.enqueue(request)
                _messages.emit("Descargando «${video.title}». Se guardará en Música/DCM MUSIC")
            } catch (e: Exception) {
                _messages.emit("No se pudo descargar: ${e.message ?: "error desconocido"}")
            }
        }
    }

    /** Se llama cuando termina una descarga: espera a que Android indexe el archivo y reescanea. */
    fun refreshDelayed() {
        viewModelScope.launch {
            delay(2500)
            refresh()
        }
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
