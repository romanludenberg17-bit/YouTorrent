package com.rommy.youtorrent

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.libtorrent4j.SessionManager
import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.swig.torrent_flags_t
import java.io.File
import java.util.Collections

class TorrentEngine private constructor(private val context: Context) {
    private val repository = TorrentRepositoryProvider.get(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val session = SessionManager()
    private val pendingMagnets = Collections.synchronizedList(mutableListOf<String>())
    private var pollJob: Job? = null
    private var started = false
    private val saveDir: File by lazy {
        File(context.getExternalFilesDir(null) ?: context.filesDir, "torrents").also { it.mkdirs() }
    }

    @Synchronized
    fun start() {
        if (started) return
        session.start()
        started = true
        restore()
        pollJob = scope.launch { poll() }
    }

    fun addMagnet(magnet: String) {
        val normalized = magnet.trim()
        if (!normalized.startsWith("magnet:?")) return
        start()
        pendingMagnets.add(normalized)
        runCatching { session.download(normalized, saveDir, torrent_flags_t()) }
    }

    fun toggle(hash: String) {
        runCatching {
            val handle = find(hash) ?: return
            val current = repository.torrents.value.firstOrNull { it.hash == hash } ?: return
            if (current.paused) handle.resume() else handle.pause()
        }
    }

    fun remove(hash: String) {
        runCatching { find(hash)?.let(session::remove) }
        repository.remove(hash)
    }

    private suspend fun poll() {
        while (isActive) {
            runCatching {
                session.getTorrentHandles().forEach { handle ->
                    if (!handle.isValid()) return@forEach
                    val status = handle.status()
                    val hash = handle.infoHash().toString()
                    val previous = repository.torrents.value.firstOrNull { it.hash == hash }
                    val magnet = previous?.magnet ?: pendingMagnets.removeFirstOrNull().orEmpty()
                    val item = TorrentItem(
                        hash = hash,
                        magnet = magnet,
                        name = status.name().ifBlank { previous?.name ?: "Torrent" },
                        totalBytes = status.total(),
                        downloadedBytes = status.totalDone(),
                        progress = status.progress().coerceIn(0f, 1f),
                        downloadRate = status.downloadRate(),
                        uploadRate = status.uploadRate(),
                        peers = status.numPeers(),
                        paused = status.isPaused(),
                        finished = status.progress() >= 0.99999f,
                    )
                    repository.upsert(item)
                }
            }
            delay(1000)
        }
    }

    private fun restore() {
        repository.torrents.value
            .filter { it.magnet.isNotBlank() }
            .forEach { item ->
                runCatching { session.download(item.magnet, saveDir, torrent_flags_t()) }
            }
    }

    private fun find(hash: String): TorrentHandle? =
        session.getTorrentHandles().firstOrNull { it.isValid() && it.infoHash().toString() == hash }

    companion object {
        @Volatile private var instance: TorrentEngine? = null

        fun get(context: Context): TorrentEngine = instance ?: synchronized(this) {
            instance ?: TorrentEngine(context.applicationContext).also { instance = it }
        }
    }
}
