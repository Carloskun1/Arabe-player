package com.dcm.music.data.online

data class OnlineVideo(
    val id: String,
    val title: String,
    val uploader: String,
    val durationSec: Long,
    val thumbUrl: String
)

data class AudioInfo(
    val url: String,
    val mimeType: String,
    val suffix: String,
    val bitrate: Int
)
