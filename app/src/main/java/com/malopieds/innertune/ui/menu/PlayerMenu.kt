package com.malopieds.innertune.ui.menu

import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.malopieds.innertube.models.WatchEndpoint
import com.malopieds.innertune.LocalDatabase
import com.malopieds.innertune.LocalDownloadUtil
import com.malopieds.innertune.LocalPlayerConnection
import com.malopieds.innertune.R
import com.malopieds.innertune.constants.ListItemHeight
import com.malopieds.innertune.constants.ListThumbnailSize
import com.malopieds.innertune.constants.ThumbnailCornerRadius
import com.malopieds.innertune.constants.ShowLyricsKey
import com.malopieds.innertune.db.entities.PlaylistSongMap
import com.malopieds.innertune.models.MediaMetadata
import com.malopieds.innertune.playback.ExoDownloadService
import com.malopieds.innertune.playback.queues.YouTubeQueue
import com.malopieds.innertune.ui.component.BigSeekBar
import com.malopieds.innertune.ui.component.BottomSheetState
import com.malopieds.innertune.ui.component.DownloadGridMenu
import com.malopieds.innertune.ui.component.GridMenu
import com.malopieds.innertune.ui.component.GridMenuItem
import com.malopieds.innertune.ui.component.ListDialog
import com.malopieds.innertune.ui.component.ListItem
import com.malopieds.innertune.utils.joinByBullet
import com.malopieds.innertune.utils.makeTimeString
import java.time.LocalDateTime
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round
import com.malopieds.innertune.utils.BluetoothHeadsetManager
import com.malopieds.innertune.utils.BluetoothPermissionHandler
import com.malopieds.innertune.utils.rememberPreference
import androidx.compose.ui.draw.alpha
import com.malopieds.innertune.ui.player.ShareSongDialog
import androidx.compose.runtime.LaunchedEffect
import coil.ImageLoader
import coil.request.ImageRequest
import androidx.compose.ui.graphics.toArgb
import com.malopieds.innertune.ui.theme.extractGradientColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.Color
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun PlayerMenu(
    mediaMetadata: MediaMetadata?,
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
    isQueueTrigger: Boolean? = false,
    onShowDetailsDialog: () -> Unit,
    onDismiss: () -> Unit,
) {
    mediaMetadata ?: return
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val playerVolume = playerConnection.service.playerVolume.collectAsState()
    val activityResultLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    val librarySong by database.song(mediaMetadata.id).collectAsState(initial = null)
    val download by LocalDownloadUtil.current.getDownload(mediaMetadata.id).collectAsState(initial = null)
    val artists = remember(mediaMetadata.artists) { mediaMetadata.artists.filter { it.id != null } }
    var showChoosePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showErrorPlaylistAddDialog by rememberSaveable { mutableStateOf(false) }
    var showSelectArtistDialog by rememberSaveable { mutableStateOf(false) }
    var showPitchTempoDialog by rememberSaveable { mutableStateOf(false) }
    var showLyrics by rememberPreference(ShowLyricsKey, defaultValue = false)

    val bluetoothHeadsetManager = remember { BluetoothHeadsetManager(context) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            bluetoothHeadsetManager.openBluetoothSettings()
        }
    }

    // --- Drag Handle and Card-like UI ---
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
    ) {
        // Drag handle (clickable to close)
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 12.dp)
                .clickable { onDismiss() } // tap to close for accessibility
        ) {
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
            )
        }
        // --- Custom actions row: Like, Headphone, Share ---
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            // Like (Favorite/Star) button
            IconButton(onClick = playerConnection::toggleLike) {
                Icon(
                    painter = painterResource(
                        if (librarySong?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border
                    ),
                    contentDescription = stringResource(
                        if (librarySong?.song?.liked == true) R.string.remove_from_library else R.string.add_to_library
                    ),
                    tint = if (librarySong?.song?.liked == true) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
            // Headphone button
            IconButton(onClick = {
                if (BluetoothPermissionHandler.hasRequiredPermissions(context)) {
                    bluetoothHeadsetManager.openBluetoothSettings()
                } else {
                    permissionLauncher.launch(BluetoothPermissionHandler.getRequiredPermissions())
                }
            }) {
                Icon(
                    painter = painterResource(R.drawable.earphone),
                    contentDescription = "Headphones", // Use a hardcoded string instead of R.string.bluetooth_headset
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            // Share as image button
            var showShareDialog by remember { mutableStateOf(false) }
            IconButton(onClick = { showShareDialog = true }) {
                Icon(
                    painter = painterResource(R.drawable.share),
                    contentDescription = "Share as image",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
            val darkTheme = isSystemInDarkTheme()
            LaunchedEffect(mediaMetadata.thumbnailUrl, darkTheme) {
                withContext(Dispatchers.IO) {
                    val result = (
                        ImageLoader(context)
                            .execute(
                                ImageRequest
                                    .Builder(context)
                                    .data(mediaMetadata.thumbnailUrl)
                                    .allowHardware(false)
                                    .build(),
                            ).drawable as? BitmapDrawable
                    )?.bitmap?.extractGradientColors(darkTheme = darkTheme)
                    result?.let {
                        gradientColors = it
                    }
                }
            }
            if (showShareDialog) {
                ShareSongDialog(
                    mediaMetadata = mediaMetadata,
                    albumArt = mediaMetadata.thumbnailUrl,
                    onDismiss = { showShareDialog = false },
                    shareLink = "https://music.youtube.com/watch?v=${mediaMetadata.id}",
                    gradientColors = gradientColors
                )
            }
            // Lyrics button
            IconButton(onClick = { showLyrics = !showLyrics }) {
                Icon(
                    painter = painterResource(R.drawable.lyrics),
                    contentDescription = "Lyrics",
                    tint = if (showLyrics) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.alpha(if (showLyrics) 1f else 0.5f)
                )
            }
        }
        // --- Menu content below (no share, no start radio) ---
        if (isQueueTrigger != true) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.volume_up),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
                BigSeekBar(
                    progressProvider = playerVolume::value,
                    onProgressChange = { playerConnection.service.playerVolume.value = it },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        GridMenu(
            contentPadding = PaddingValues(8.dp),
        ) {
            GridMenuItem(
                icon = R.drawable.playlist_add,
                title = R.string.add_to_playlist,
            ) { showChoosePlaylistDialog = true }
            DownloadGridMenu(
                state = download?.state,
                onDownload = {
                    database.transaction { insert(mediaMetadata) }
                    val downloadRequest = DownloadRequest.Builder(mediaMetadata.id, mediaMetadata.id.toUri())
                        .setCustomCacheKey(mediaMetadata.id)
                        .setData(mediaMetadata.title.toByteArray())
                        .build()
                    DownloadService.sendAddDownload(
                        context,
                        ExoDownloadService::class.java,
                        downloadRequest,
                        false,
                    )
                },
                onRemoveDownload = {
                    DownloadService.sendRemoveDownload(
                        context,
                        ExoDownloadService::class.java,
                        mediaMetadata.id,
                        false,
                    )
                },
            )
            if (librarySong?.song?.inLibrary != null) {
                GridMenuItem(
                    icon = R.drawable.library_add_check,
                    title = R.string.remove_from_library,
                ) { database.query { inLibrary(mediaMetadata.id, null) } }
            } else {
                GridMenuItem(
                    icon = R.drawable.library_add,
                    title = R.string.add_to_library,
                ) {
                    database.transaction {
                        insert(mediaMetadata)
                        inLibrary(mediaMetadata.id, LocalDateTime.now())
                    }
                }
            }
            if (artists.isNotEmpty()) {
                GridMenuItem(
                    icon = R.drawable.artist,
                    title = R.string.view_artist,
                ) {
                    if (mediaMetadata.artists.size == 1) {
                        navController.navigate("artist/${mediaMetadata.artists[0].id}")
                        playerBottomSheetState.collapseSoft()
                        onDismiss()
                    } else {
                        showSelectArtistDialog = true
                    }
                }
            }
            if (mediaMetadata.album != null) {
                GridMenuItem(
                    icon = R.drawable.album,
                    title = R.string.view_album,
                ) {
                    navController.navigate("album/${mediaMetadata.album.id}")
                    playerBottomSheetState.collapseSoft()
                    onDismiss()
                }
            }
            if (isQueueTrigger != true) {
                GridMenuItem(
                    icon = R.drawable.info,
                    title = R.string.details,
                ) {
                    onShowDetailsDialog()
                    onDismiss()
                }
                GridMenuItem(
                    icon = R.drawable.equalizer,
                    title = R.string.equalizer,
                ) {
                    val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                        putExtra(AudioEffect.EXTRA_AUDIO_SESSION, playerConnection.player.audioSessionId)
                        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        activityResultLauncher.launch(intent)
                    }
                    onDismiss()
                }
                GridMenuItem(
                    icon = R.drawable.tune,
                    title = R.string.advanced,
                ) { showPitchTempoDialog = true }
            }
        }
    }
    // --- Dialogs remain outside the card ---
    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onAdd = { playlist ->
            database.transaction {
                insert(mediaMetadata)
                if (checkInPlaylist(playlist.id, mediaMetadata.id) == 0) {
                    insert(
                        PlaylistSongMap(
                            songId = mediaMetadata.id,
                            playlistId = playlist.id,
                            position = playlist.songCount,
                        ),
                    )
                    update(playlist.playlist.copy(lastUpdateTime = LocalDateTime.now()))
                } else {
                    showErrorPlaylistAddDialog = true
                }
            }
        },
        onDismiss = { showChoosePlaylistDialog = false },
    )
    if (showErrorPlaylistAddDialog) {
        ListDialog(
            onDismiss = {
                showErrorPlaylistAddDialog = false
                onDismiss()
            },
        ) {
            item {
                ListItem(
                    title = stringResource(R.string.already_in_playlist),
                    thumbnailContent = {
                        Image(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize),
                        )
                    },
                    modifier = Modifier.clickable { showErrorPlaylistAddDialog = false },
                )
            }
            item {
                ListItem(
                    title = mediaMetadata.title,
                    thumbnailContent = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(ListThumbnailSize),
                        ) {
                            AsyncImage(
                                model = mediaMetadata.thumbnailUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(ThumbnailCornerRadius)),
                            )
                        }
                    },
                    subtitle = joinByBullet(
                        mediaMetadata.artists.joinToString { it.name },
                        makeTimeString(mediaMetadata.duration * 1000L),
                    ),
                )
            }
        }
    }
    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false },
        ) {
            items(artists) { artist ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .height(ListItemHeight)
                        .clickable {
                            navController.navigate("artist/${artist.id}")
                            showSelectArtistDialog = false
                            playerBottomSheetState.collapseSoft()
                            onDismiss()
                        }
                        .padding(horizontal = 24.dp),
                ) {
                    Text(
                        text = artist.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
    if (showPitchTempoDialog) {
        TempoPitchDialog(onDismiss = { showPitchTempoDialog = false })
    }
}

@Composable
fun TempoPitchDialog(onDismiss: () -> Unit) {
    val playerConnection = LocalPlayerConnection.current ?: return
    var tempo by remember {
        mutableFloatStateOf(playerConnection.player.playbackParameters.speed)
    }
    var transposeValue by remember {
        mutableIntStateOf(round(12 * log2(playerConnection.player.playbackParameters.pitch)).toInt())
    }
    val updatePlaybackParameters = {
        playerConnection.player.playbackParameters = PlaybackParameters(tempo, 2f.pow(transposeValue.toFloat() / 12))
    }

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.tempo_and_pitch))
        },
        dismissButton = {
            TextButton(
                onClick = {
                    tempo = 1f
                    transposeValue = 0
                    updatePlaybackParameters()
                },
            ) {
                Text(stringResource(R.string.reset))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        text = {
            Column {
                ValueAdjuster(
                    icon = R.drawable.speed,
                    currentValue = tempo,
                    values = (0..35).map { round((0.25f + it * 0.05f) * 100) / 100 },
                    onValueUpdate = {
                        tempo = it
                        updatePlaybackParameters()
                    },
                    valueText = { "x$it" },
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                ValueAdjuster(
                    icon = R.drawable.discover_tune,
                    currentValue = transposeValue,
                    values = (-12..12).toList(),
                    onValueUpdate = {
                        transposeValue = it
                        updatePlaybackParameters()
                    },
                    valueText = { "${if (it > 0) "+" else ""}$it" },
                )
            }
        },
    )
}

@Composable
fun <T> ValueAdjuster(
    @DrawableRes icon: Int,
    currentValue: T,
    values: List<T>,
    onValueUpdate: (T) -> Unit,
    valueText: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )

        IconButton(
            enabled = currentValue != values.first(),
            onClick = {
                onValueUpdate(values[values.indexOf(currentValue) - 1])
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.remove),
                contentDescription = null,
            )
        }

        Text(
            text = valueText(currentValue),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(80.dp),
        )

        IconButton(
            enabled = currentValue != values.last(),
            onClick = {
                onValueUpdate(values[values.indexOf(currentValue) + 1])
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.add),
                contentDescription = null,
            )
        }
    }
} 