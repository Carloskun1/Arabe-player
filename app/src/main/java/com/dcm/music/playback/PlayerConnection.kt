package com.dcm.music.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.dcm.music.data.Song
import com.dcm.music.data.yt.YtVideo
import com.dcm.music.data.yt.toMediaItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerUiState(
    val hasMedia: Boolean = false,
    val mediaId: String? = null,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val artUri: Uri? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF
)

fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(uri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(artUri)
            .build()
    )
    .build()

/** Conecta la interfaz con el servicio de reproducción mediante MediaController. */
class PlayerConnection(context: Context) {

    private val appContext = context.applicationContext
    private var controller: MediaController? = null

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private val listener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            _errors.tryEmit("No se pudo reproducir esta canción")
            val c = controller ?: return
            if (c.hasNextMediaItem()) {
                c.seekToNextMediaItem()
                c.prepare()
                c.play()
            }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            sync()
        }
    }

    init {
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        future.addListener({
            try {
                controller = future.get()
                controller?.addListener(listener)
                sync()
            } catch (e: Exception) {
                // No se pudo conectar con el servicio
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    private fun sync() {
        val c = controller ?: return
        val md = c.mediaMetadata
        _state.value = PlayerUiState(
            hasMedia = c.currentMediaItem != null,
            mediaId = c.currentMediaItem?.mediaId,
            title = md.title?.toString().orEmpty(),
            artist = md.artist?.toString().orEmpty(),
            album = md.albumTitle?.toString().orEmpty(),
            artUri = md.artworkUri,
            isPlaying = c.isPlaying,
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = if (c.duration == C.TIME_UNSET) 0L else c.duration,
            shuffle = c.shuffleModeEnabled,
            repeatMode = c.repeatMode
        )
    }

    fun refreshPosition() {
        val c = controller ?: return
        if (c.isPlaying) {
            _state.value = _state.value.copy(positionMs = c.currentPosition.coerceAtLeast(0L))
        }
    }

    fun play(songs: List<Song>, index: Int) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        c.setMediaItems(songs.map { it.toMediaItem() }, index.coerceIn(0, songs.lastIndex), 0L)
        c.prepare()
        c.play()
    }

    fun playYoutube(videos: List<YtVideo>, index: Int) {
        val c = controller ?: return
        if (videos.isEmpty()) return
        c.setMediaItems(videos.map { it.toMediaItem() }, index.coerceIn(0, videos.lastIndex), 0L)
        c.prepare()
        c.play()
    }

    fun addYoutubeToQueue(video: YtVideo) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) playYoutube(listOf(video), 0) else c.addMediaItem(video.toMediaItem())
    }

    fun playYoutubeNext(video: YtVideo) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) playYoutube(listOf(video), 0)
        else c.addMediaItem(c.currentMediaItemIndex + 1, video.toMediaItem())
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() { controller?.seekToNext() }

    fun previous() { controller?.seekToPrevious() }

    fun seekTo(ms: Long) { controller?.seekTo(ms) }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun queue(): List<QueueItem> {
        val c = controller ?: return emptyList()
        return (0 until c.mediaItemCount).map { i ->
            val item = c.getMediaItemAt(i)
            QueueItem(
                index = i,
                mediaId = item.mediaId,
                title = item.mediaMetadata.title?.toString().orEmpty(),
                artist = item.mediaMetadata.artist?.toString().orEmpty()
            )
        }
    }

    fun currentIndex(): Int = controller?.currentMediaItemIndex ?: 0

    fun jumpTo(index: Int) {
        val c = controller ?: return
        if (index in 0 until c.mediaItemCount) {
            c.seekToDefaultPosition(index)
            c.play()
        }
    }

    fun removeFromQueue(index: Int) {
        val c = controller ?: return
        if (index in 0 until c.mediaItemCount) c.removeMediaItem(index)
    }

    fun addToQueue(song: Song) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) play(listOf(song), 0) else c.addMediaItem(song.toMediaItem())
    }

    fun playNext(song: Song) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) play(listOf(song), 0)
        else c.addMediaItem(c.currentMediaItemIndex + 1, song.toMediaItem())
    }

    fun release() {
        controller?.removeListener(listener)
        controller?.release()
        controller = null
    }
}

data class QueueItem(val index: Int, val mediaId: String, val title: String, val artist: String)
