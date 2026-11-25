package com.dcm.music.data.yt

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.concurrent.ConcurrentHashMap

data class YtVideo(
    val id: String,
    val title: String,
    val uploader: String,
    val durationSec: Long,
    val thumbnail: String?
)

data class YtPlaylist(
    val id: String,
    val title: String,
    val uploader: String,
    val thumbnail: String?,
    val videoCount: Long
)

data class YtSearchState(
    val query: String = "",
    val loading: Boolean = false,
    val results: List<YtVideo> = emptyList(),
    val playlistResults: List<YtPlaylist> = emptyList(),
    val error: String? = null,
    val searched: Boolean = false
)

/** Convierte un resultado de YouTube en un elemento de la cola (la URL real se resuelve al reproducir). */
fun YtVideo.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId("yt:$id")
    .setUri(Uri.parse("dcmyt:$id"))
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(uploader)
            .setAlbumTitle("YouTube")
            .setArtworkUri(thumbnail?.let { Uri.parse(it) })
            .build()
    )
    .build()

object YouTubeRepository {

    const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; rv:140.0) Gecko/20100101 Firefox/140.0"

    private var ready = false
    private val urlCache = ConcurrentHashMap<String, Pair<String, Long>>()
    private val idRegex = Regex("(?:v=|/shorts/|youtu\\.be/)([A-Za-z0-9_-]{11})")

    @Synchronized
    fun ensureInit() {
        if (!ready) {
            NewPipe.init(NewPipeDownloader(), Localization("es", "ES"))
            ready = true
        }
    }

    private fun extractId(url: String): String? = idRegex.find(url)?.groupValues?.get(1)

    private fun playlistId(url: String): String? =
        Regex("[?&]list=([A-Za-z0-9_-]+)").find(url)?.groupValues?.get(1)

    fun search(query: String): List<YtVideo> {
        ensureInit()
        val yt = ServiceList.YouTube
        val handler = yt.searchQHFactory.fromQuery(query, listOf("videos"), "")
        val info = SearchInfo.getInfo(yt, handler)
        return info.relatedItems
            .filterIsInstance<StreamInfoItem>()
            .mapNotNull { item ->
                val id = extractId(item.url) ?: return@mapNotNull null
                if (item.duration <= 0) return@mapNotNull null // se omiten transmisiones en vivo
                YtVideo(
                    id = id,
                    title = item.name,
                    uploader = item.uploaderName ?: "",
                    durationSec = item.duration,
                    thumbnail = item.thumbnails.firstOrNull()?.url
                )
            }
    }

    /** Busca playlists de YouTube (se abre en «Buscar» pestaña YouTube, modo playlists). */
    fun searchPlaylists(query: String): List<YtPlaylist> {
        ensureInit()
        val yt = ServiceList.YouTube
        val handler = yt.searchQHFactory.fromQuery(query, listOf("playlists"), "")
        val info = SearchInfo.getInfo(yt, handler)
        return info.relatedItems.mapNotNull { item ->
            val pl = item as? PlaylistInfoItem ?: return@mapNotNull null
            val id = playlistId(pl.url) ?: return@mapNotNull null
            YtPlaylist(
                id = id,
                title = pl.name ?: "",
                uploader = pl.uploaderName ?: "",
                thumbnail = pl.thumbnails.firstOrNull()?.url,
                videoCount = pl.streamCount
            )
        }
    }

    /** Devuelve los videos de una playlist de YouTube, en orden. */
    fun playlistVideos(playlistId: String): List<YtVideo> {
        ensureInit()
        val yt = ServiceList.YouTube
        val info = PlaylistInfo.getInfo(yt, "https://www.youtube.com/playlist?list=$playlistId")
        return info.relatedItems
            .filterIsInstance<StreamInfoItem>()
            .mapNotNull { item ->
                val id = extractId(item.url) ?: return@mapNotNull null
                if (item.duration <= 0) return@mapNotNull null
                YtVideo(
                    id = id,
                    title = item.name ?: "",
                    uploader = item.uploaderName ?: "",
                    durationSec = item.duration,
                    thumbnail = item.thumbnails.firstOrNull()?.url
                )
            }
    }

    /** Elige el mejor audio directo (prefiere M4A por compatibilidad). */
    fun resolveAudio(videoId: String): AudioStream {
        ensureInit()
        val info = StreamInfo.getInfo(ServiceList.YouTube, "https://www.youtube.com/watch?v=$videoId")
        val candidates = info.audioStreams.filter {
            it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && it.isUrl
        }
        return candidates.filter { it.format == MediaFormat.M4A }.maxByOrNull { it.averageBitrate }
            ?: candidates.maxByOrNull { it.averageBitrate }
            ?: throw IllegalStateException("Este video no tiene audio disponible")
    }

    /** URL de audio con caché de 4 horas (las URL de YouTube caducan). */
    fun resolveAudioUrl(videoId: String): String {
        val now = System.currentTimeMillis()
        urlCache[videoId]?.let { (url, expires) -> if (expires > now) return url }
        val url = resolveAudio(videoId).content
        urlCache[videoId] = url to (now + 4 * 60 * 60 * 1000L)
        return url
    }
}
