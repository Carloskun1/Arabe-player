package com.dcm.music.data.yt

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.util.concurrent.TimeUnit

/** Cliente HTTP que usa el extractor de NewPipe para hablar con YouTube. */
class NewPipeDownloader : Downloader() {

    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(20, TimeUnit.SECONDS)
        .build()

    override fun execute(request: Request): Response {
        val method = request.httpMethod()
        val url = request.url()
        val data = request.dataToSend()

        var body = data?.toRequestBody(null)
        if (body == null && (method == "POST" || method == "PUT")) {
            body = ByteArray(0).toRequestBody(null)
        }

        val builder = okhttp3.Request.Builder()
            .method(method, body)
            .url(url)
            .addHeader("User-Agent", YouTubeRepository.USER_AGENT)

        for ((name, values) in request.headers()) {
            if (values.size > 1) {
                builder.removeHeader(name)
                values.forEach { builder.addHeader(name, it) }
            } else if (values.size == 1) {
                builder.header(name, values[0])
            }
        }

        val response = client.newCall(builder.build()).execute()
        if (response.code == 429) {
            response.close()
            throw ReCaptchaException("reCaptcha Challenge requested", url)
        }

        val text = response.body?.string()
        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            text,
            response.request.url.toString()
        )
    }
}
