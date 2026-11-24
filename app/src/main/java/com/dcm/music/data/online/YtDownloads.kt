package com.dcm.music.data.online

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.TimeUnit

/** Descarga el audio de YouTube a Música/DCM MUSIC para que aparezca en la biblioteca. */
class YtDownloads(private val context: Context) {

    private val _progress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val progress: StateFlow<Map<String, Int>> = _progress.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun download(video: OnlineVideo): String = withContext(Dispatchers.IO) {
        if (_progress.value.containsKey(video.id)) {
            throw IllegalStateException("Esta canción ya se está descargando")
        }
        setProgress(video.id, 0)
        try {
            val audio = YouTubeClient.resolveAudio(video.id)
            val fileName = safeName(video.title) + "." + audio.suffix
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveWithMediaStore(video, audio, fileName)
            } else {
                saveLegacy(video, audio, fileName)
            }
            video.title
        } finally {
            _progress.update { it - video.id }
        }
    }

    private fun setProgress(id: String, percent: Int) {
        _progress.update { it + (id to percent) }
    }

    private fun safeName(title: String): String {
        val cleaned = title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        return cleaned.take(80).ifBlank { "cancion" }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveWithMediaStore(video: OnlineVideo, audio: AudioInfo, fileName: String) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, audio.mimeType)
            put(MediaStore.Audio.Media.TITLE, video.title)
            put(MediaStore.Audio.Media.ARTIST, video.uploader)
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/DCM MUSIC")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values) ?: throw IOException("No se pudo crear el archivo")
        try {
            val out = resolver.openOutputStream(uri) ?: throw IOException("No se pudo abrir el archivo")
            out.use { copyStream(audio.url, it, video.id) }
            val done = ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(video: OnlineVideo, audio: AudioInfo, fileName: String) {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "DCM MUSIC")
        if (!dir.exists() && !dir.mkdirs()) throw IOException("No se pudo crear la carpeta de música")
        val file = File(dir, fileName)
        FileOutputStream(file).use { copyStream(audio.url, it, video.id) }
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
    }

    private fun copyStream(url: String, out: OutputStream, id: String) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("YouTube respondió HTTP ${response.code}")
            val body = response.body ?: throw IOException("Respuesta vacía")
            val total = body.contentLength()
            var done = 0L
            var last = -1
            body.byteStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    out.write(buffer, 0, n)
                    done += n
                    if (total > 0) {
                        val percent = (done * 100 / total).toInt()
                        if (percent != last) {
                            last = percent
                            setProgress(id, percent)
                        }
                    }
                }
            }
        }
    }
}
