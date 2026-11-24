package com.dcm.music.data.online

import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/** Búsqueda en YouTube y obtención del audio mediante NewPipeExtractor. */
object YouTubeClient {

    private var ready = false
    private val cache = ConcurrentHashMap<String, Pair<Long, AudioInfo>>()
    private val idRegex = Regex("[?&]v=([A-Za-z0-9_-]{11})")
    private const val CACHE_MS = 30 * 60 * 1000L

    private fun ensureInit() {
        synchronized(this) {
            if (!ready) {
                NewPipe.init(DownloaderImpl.instance, Localization("es", "ES"), ContentCountry("ES"))
                ready = true
            }
        }
    }

    fun search(query: String): List<OnlineVideo> {
        ensureInit()
        val extractor = ServiceList.YouTube.getSearchExtractor(
            query,
            listOf(YoutubeSearchQueryHandlerFactory.VIDEOS),
            ""
        )
        extractor.fetchPage()
        return extractor.initialPage.items
            .filterIsInstance<StreamInfoItem>()
            .mapNotNull { item ->
                val id = idRegex.find(item.url)?.groupValues?.get(1) ?: return@mapNotNull null
                OnlineVideo(
                    id = id,
                    title = item.name.orEmpty(),
                    uploader = item.uploaderName.orEmpty(),
                    durationSec = item.duration.coerceAtLeast(0L),
                    thumbUrl = item.thumbnails.maxByOrNull { img -> img.height }?.url.orEmpty()
                )
            }
            .distinctBy { it.id }
    }

    fun resolveAudio(videoId: String): AudioInfo {
        val cached = cache[videoId]
        if (cached != null && System.currentTimeMillis() - cached.first < CACHE_MS) {
            return cached.second
        }
        ensureInit()
        val info = StreamInfo.getInfo(ServiceList.YouTube, "https://www.youtube.com/watch?v=$videoId")
        val streams = info.audioStreams.filter {
            it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && it.isUrl
        }
        val best = streams.filter { it.format == MediaFormat.M4A }.maxByOrNull { it.averageBitrate }
            ?: streams.maxByOrNull { it.averageBitrate }
            ?: throw IOException("YouTube no devolvió audio utilizable para este video")
        val format = best.format
        val result = AudioInfo(
            url = best.content,
            mimeType = format?.mimeType ?: "audio/mp4",
            suffix = format?.suffix ?: "m4a",
            bitrate = best.averageBitrate
        )
        cache[videoId] = System.currentTimeMillis() to result
        return result
    }
}
