package com.rommy.youtorrent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialMagnet = intent?.dataString?.takeIf(String::isMagnet)
        startTorrentService(initialMagnet, savedInstanceState == null)

        setContent {
            YouTorrentTheme {
                YouTorrentApp(
                    initialMagnet = initialMagnet,
                    onRequestNotifications = ::requestNotificationPermissionIfNeeded,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent?.dataString?.takeIf(String::isMagnet)?.let(::dispatchMagnet)
    }

    private fun startTorrentService(initialMagnet: String?, dispatchMagnet: Boolean) {
        ContextCompat.startForegroundService(this, Intent(this, TorrentService::class.java))
        if (dispatchMagnet) initialMagnet?.let(::dispatchMagnet)
    }

    private fun dispatchMagnet(magnet: String) {
        startService(
            Intent(this, TorrentService::class.java)
                .setAction(TorrentService.ACTION_ADD_MAGNET)
                .putExtra(TorrentService.EXTRA_MAGNET, magnet),
        )
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

private fun String.isMagnet(): Boolean = startsWith("magnet:?", ignoreCase = true)

@Composable
private fun YouTorrentTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colors = if (isSystemInDarkTheme()) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
private fun YouTorrentApp(
    initialMagnet: String?,
    onRequestNotifications: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember { TorrentRepositoryProvider.get(context) }
    val torrents by repository.torrents.collectAsStateWithLifecycle()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var magnet by rememberSaveable { mutableStateOf(initialMagnet.orEmpty()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YouTorrent", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add torrent")
            }
        },
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.surface,
        ) {
            if (torrents.isEmpty()) {
                EmptyState(onAdd = { showAddDialog = true })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            "Downloads",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    items(torrents, key = { it.hash }) { torrent ->
                        TorrentCard(
                            torrent = torrent,
                            onToggle = {
                                context.startService(
                                    Intent(context, TorrentService::class.java)
                                        .setAction(TorrentService.ACTION_TOGGLE)
                                        .putExtra(TorrentService.EXTRA_HASH, torrent.hash),
                                )
                            },
                            onRemove = {
                                context.startService(
                                    Intent(context, TorrentService::class.java)
                                        .setAction(TorrentService.ACTION_REMOVE)
                                        .putExtra(TorrentService.EXTRA_HASH, torrent.hash),
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTorrentDialog(
            magnet = magnet,
            onMagnetChange = { magnet = it },
            onDismiss = { showAddDialog = false },
            onAdd = {
                val value = magnet.trim()
                if (value.isMagnet()) {
                    context.startService(
                        Intent(context, TorrentService::class.java)
                            .setAction(TorrentService.ACTION_ADD_MAGNET)
                            .putExtra(TorrentService.EXTRA_MAGNET, value),
                    )
                    magnet = ""
                    showAddDialog = false
                }
            },
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            onDismiss = { showSettingsDialog = false },
            onRequestNotifications = {
                showSettingsDialog = false
                onRequestNotifications()
            },
        )
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(96.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(42.dp),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "nothing downloading",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "add a magnet link to get started",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAdd) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Add torrent")
            }
        }
    }
}

@Composable
private fun TorrentCard(
    torrent: TorrentItem,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    val progress = torrent.progress.coerceIn(0f, 1f)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        torrent.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        buildString {
                            append(formatBytes(torrent.downloadedBytes))
                            if (torrent.totalBytes > 0) {
                                append(" / ").append(formatBytes(torrent.totalBytes))
                            }
                            if (torrent.downloadRate > 0) {
                                append(" · ").append(formatRate(torrent.downloadRate))
                            }
                            append(" · ").append(torrent.peers).append(" peers")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (torrent.paused) "Resume" else "Pause") },
                            leadingIcon = {
                                Icon(
                                    if (torrent.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onToggle()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Remove") },
                            onClick = {
                                menuExpanded = false
                                confirmRemove = true
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onToggle) {
                    Icon(
                        if (torrent.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = if (torrent.paused) "Resume" else "Pause",
                    )
                }
            }
        }
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remove torrent?") },
            text = { Text("The torrent will be removed from YouTorrent. Downloaded files are kept.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRemove = false
                        onRemove()
                    },
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun AddTorrentDialog(
    magnet: String,
    onMagnetChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
) {
    val valid = magnet.trim().isMagnet()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add torrent") },
        text = {
            OutlinedTextField(
                value = magnet,
                onValueChange = onMagnetChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Magnet link") },
                minLines = 3,
                isError = magnet.isNotBlank() && !valid,
            )
        },
        confirmButton = {
            TextButton(onClick = onAdd, enabled = valid) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun SettingsDialog(
    onDismiss: () -> Unit,
    onRequestNotifications: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("YouTorrent settings", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Notification permission is optional. Enable it if you want download status in the notification shade.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onRequestNotifications) {
                Icon(Icons.Rounded.Notifications, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Notifications")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

private fun formatBytes(value: Long): String {
    if (value < 1024) return "$value B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var number = value.toDouble()
    var index = -1
    do {
        number /= 1024.0
        index++
    } while (number >= 1024.0 && index < units.lastIndex)
    return "%.1f %s".format(number, units[index])
}

private fun formatRate(value: Int): String = "${formatBytes(value.toLong())}/s"
