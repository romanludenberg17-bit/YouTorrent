package com.rommy.youtorrent

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class TorrentRepository(context: Context) {
    private val prefs = context.getSharedPreferences("torrents", Context.MODE_PRIVATE)
    private val _torrents = MutableStateFlow(load())
    val torrents: StateFlow<List<TorrentItem>> = _torrents.asStateFlow()

    fun upsert(item: TorrentItem) {
        val next = _torrents.value.toMutableList()
        val index = next.indexOfFirst { it.hash == item.hash }
        if (index >= 0) next[index] = item else next.add(item)
        save(next)
        _torrents.value = next
    }

    fun remove(hash: String) {
        val next = _torrents.value.filterNot { it.hash == hash }
        save(next)
        _torrents.value = next
    }

    private fun save(items: List<TorrentItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("hash", item.hash)
                put("magnet", item.magnet)
                put("name", item.name)
                put("total", item.totalBytes)
                put("downloaded", item.downloadedBytes)
                put("progress", item.progress.toDouble())
                put("downloadRate", item.downloadRate)
                put("uploadRate", item.uploadRate)
                put("peers", item.peers)
                put("paused", item.paused)
                put("finished", item.finished)
            })
        }
        prefs.edit().putString("items", array.toString()).apply()
    }

    private fun load(): List<TorrentItem> {
        val raw = prefs.getString("items", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList(array.length()) {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(
                        TorrentItem(
                            hash = item.getString("hash"),
                            magnet = item.getString("magnet"),
                            name = item.getString("name"),
                            totalBytes = item.optLong("total"),
                            downloadedBytes = item.optLong("downloaded"),
                            progress = item.optDouble("progress").toFloat(),
                            downloadRate = item.optInt("downloadRate"),
                            uploadRate = item.optInt("uploadRate"),
                            peers = item.optInt("peers"),
                            paused = item.optBoolean("paused"),
                            finished = item.optBoolean("finished"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}

object TorrentRepositoryProvider {
    @Volatile private var instance: TorrentRepository? = null

    fun get(context: Context): TorrentRepository = instance ?: synchronized(this) {
        instance ?: TorrentRepository(context.applicationContext).also { instance = it }
    }
}
