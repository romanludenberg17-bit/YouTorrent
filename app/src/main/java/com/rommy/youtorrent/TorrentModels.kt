package com.rommy.youtorrent

data class TorrentItem(
    val hash: String,
    val magnet: String,
    val name: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val progress: Float,
    val downloadRate: Int,
    val uploadRate: Int,
    val peers: Int,
    val paused: Boolean,
    val finished: Boolean,
)
