package com.malopieds.innertune.ui.player

import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_READY
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.malopieds.innertune.LocalDatabase
import com.malopieds.innertune.LocalDownloadUtil
import com.malopieds.innertune.LocalPlayerConnection
import com.malopieds.innertune.R
import com.malopieds.innertune.constants.DarkModeKey
import com.malopieds.innertune.constants.ListThumbnailSize
import com.malopieds.innertune.constants.PlayerBackgroundStyle
import com.malopieds.innertune.constants.PlayerBackgroundStyleKey
import com.malopieds.innertune.constants.PlayerHorizontalPadding
import com.malopieds.innertune.constants.PlayerTextAlignmentKey
import com.malopieds.innertune.constants.PureBlackKey
import com.malopieds.innertune.constants.QueuePeekHeight
import com.malopieds.innertune.constants.ShowLyricsKey
import com.malopieds.innertune.constants.SliderStyle
import com.malopieds.innertune.constants.SliderStyleKey
import com.malopieds.innertune.constants.ThumbnailCornerRadius
import com.malopieds.innertune.constants.ShowHeadsetNameKey
import com.malopieds.innertune.db.entities.PlaylistSongMap
import com.malopieds.innertune.extensions.togglePlayPause
import com.malopieds.innertune.models.MediaMetadata
import com.malopieds.innertune.playback.ExoDownloadService
import com.malopieds.innertune.playback.PlayerConnection
import com.malopieds.innertune.ui.component.BottomSheet
import com.malopieds.innertune.ui.component.BottomSheetState
import com.malopieds.innertune.ui.component.ListDialog
import com.malopieds.innertune.ui.component.ListItem
import com.malopieds.innertune.ui.component.LocalMenuState
import com.malopieds.innertune.ui.component.ResizableIconButton
import com.malopieds.innertune.ui.component.rememberBottomSheetState
import com.malopieds.innertune.ui.menu.AddToPlaylistDialog
import com.malopieds.innertune.ui.menu.PlayerMenu
import com.malopieds.innertune.ui.screens.settings.DarkMode
import com.malopieds.innertune.ui.screens.settings.PlayerTextAlignment
import com.malopieds.innertune.ui.theme.extractGradientColors
import com.malopieds.innertune.utils.BluetoothHeadsetManager
import com.malopieds.innertune.utils.BluetoothPermissionHandler
import com.malopieds.innertune.utils.joinByBullet
import com.malopieds.innertune.utils.makeTimeString
import com.malopieds.innertune.utils.rememberEnumPreference
import com.malopieds.innertune.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import me.saket.squiggles.SquigglySlider
import java.time.LocalDateTime
import kotlin.math.roundToInt
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.runtime.rememberUpdatedState
import java.util.concurrent.ConcurrentHashMap
import androidx.compose.ui.zIndex

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerProgressBar(
    playerConnection: PlayerConnection, // pass the connection
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    sliderStyle: SliderStyle,
    onBackgroundColor: Color
) {
    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    var sliderPosition by remember { mutableStateOf<Long?>(null) }
    var position by remember { mutableLongStateOf(playerConnection.player.currentPosition) }
    val durationState by rememberUpdatedState(duration)

    // Only this composable updates position every 100ms
    LaunchedEffect(playbackState) {
        if (playbackState == STATE_READY) {
            while (isActive) {
                delay(250)
                position = playerConnection.player.currentPosition
            }
        }
    }

    when (sliderStyle) {
        SliderStyle.SQUIGGLY -> {
            SquigglySlider(
                value = (sliderPosition ?: position).toFloat(),
                valueRange = 0f..(if (durationState == C.TIME_UNSET) 0f else durationState.toFloat()),
                onValueChange = {
                    sliderPosition = it.toLong()
                },
                onValueChangeFinished = {
                    sliderPosition?.let {
                        onSeek(it)
                    }
                    sliderPosition = null
                },
                modifier = modifier
                    .padding(horizontal = PlayerHorizontalPadding / 2)
                    .height(12.dp),
                squigglesSpec =
                    SquigglySlider.SquigglesSpec(
                        amplitude = if (isPlaying) (4.dp).coerceAtLeast(4.dp) else 0.dp,
                        strokeWidth = 6.dp,
                    ),
            )
        }
        SliderStyle.DEFAULT -> {
            Slider(
                value = (sliderPosition ?: position).toFloat(),
                valueRange = 0f..(if (durationState == C.TIME_UNSET) 0f else durationState.toFloat()),
                onValueChange = {
                    sliderPosition = it.toLong()
                },
                onValueChangeFinished = {
                    sliderPosition?.let {
                        onSeek(it)
                    }
                    sliderPosition = null
                },
                modifier = modifier
                    .padding(horizontal = PlayerHorizontalPadding / 2)
                    .height(12.dp),
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = PlayerHorizontalPadding + 4.dp),
    ) {
        Text(
            text = makeTimeString(sliderPosition ?: position),
            style = MaterialTheme.typography.labelMedium,
            color = onBackgroundColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (durationState != C.TIME_UNSET) makeTimeString(durationState) else "",
            style = MaterialTheme.typography.labelMedium,
            color = onBackgroundColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val bluetoothHeadsetManager = remember { BluetoothHeadsetManager(context) }
    val isHeadsetConnected by bluetoothHeadsetManager.isHeadsetConnected.collectAsState()

    val clipboardManager = LocalClipboardManager.current

    val playerConnection = LocalPlayerConnection.current ?: return

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = true)
    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.GRADIENT)
    val useBlackBackground =
        remember(isSystemInDarkTheme, darkTheme, pureBlack, playerBackground) {
            val useDarkTheme = if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            val usePureBlackPlayerBackground = (playerBackground == PlayerBackgroundStyle.DEFAULT)
            useDarkTheme && pureBlack && usePureBlackPlayerBackground
        }

    val playerTextAlignment by rememberEnumPreference(PlayerTextAlignmentKey, PlayerTextAlignment.SIDED)

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val automix by playerConnection.service.automixItems.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    var showLyrics by rememberPreference(ShowLyricsKey, defaultValue = false)

    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.SQUIGGLY)

    var duration by rememberSaveable(playerConnection.player.duration) {
        mutableLongStateOf(playerConnection.player.duration)
    }

    var gradientColors by remember {
        mutableStateOf<List<Color>>(emptyList())
    }

    var changeColor by remember {
        mutableStateOf(false)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            // Permissions granted, initialize Bluetooth
            bluetoothHeadsetManager.openBluetoothSettings()
        }
    }

    if (!canSkipNext && automix.isNotEmpty()) {
        playerConnection.service.addToQueueAutomix(automix[0], 0)
    }

    val gradientCache = remember { ConcurrentHashMap<String, List<Color>>() }
    val thumbnailUrl = mediaMetadata?.thumbnailUrl

    LaunchedEffect(thumbnailUrl, playerBackground, darkTheme, useBlackBackground) {
        if (useBlackBackground) {
            gradientColors = listOf(Color.Black, Color.Black)
        } else if (playerBackground == PlayerBackgroundStyle.GRADIENT && thumbnailUrl != null) {
            val cached = gradientCache[thumbnailUrl]
            if (cached != null) {
                gradientColors = cached
            } else {
                withContext(Dispatchers.IO) {
                    val result =
                        (
                            ImageLoader(context)
                                .execute(
                                    ImageRequest
                                        .Builder(context)
                                        .data(thumbnailUrl)
                                        .allowHardware(false)
                                        .build(),
                                ).drawable as? BitmapDrawable
                        )?.bitmap?.extractGradientColors(
                            darkTheme =
                                darkTheme == DarkMode.ON || (darkTheme == DarkMode.AUTO && isSystemInDarkTheme),
                        )
                    result?.let {
                        gradientCache[thumbnailUrl] = it
                        gradientColors = it
                    }
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    val changeBound = state.expandedBound / 3

    val hasGradientColours : Boolean = gradientColors.size >= 2
    val whiteContrastThreshold = 2.0
    val blackContrastThreshold = 2.0

    val whiteGradientContrast : Double = when {
        hasGradientColours -> {
            ColorUtils.calculateContrast(
                gradientColors.first().toArgb(),
                Color.White.toArgb(),
            )
        }
        else -> whiteContrastThreshold
    }

    val blackGradientContrast : Double = when {
        hasGradientColours -> {
            ColorUtils.calculateContrast(
                gradientColors.last().toArgb(),
                Color.Black.toArgb(),
            )
        }
        else -> blackContrastThreshold
    }

    val onBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
            else -> {
                when {
                    hasGradientColours &&
                            whiteGradientContrast < whiteContrastThreshold &&
                            blackGradientContrast > blackContrastThreshold -> {
                        changeColor = true
                        Color.Black
                    }
                    hasGradientColours &&
                            whiteGradientContrast > whiteContrastThreshold &&
                            blackGradientContrast < blackContrastThreshold -> {
                        changeColor = true
                        Color.White
                    }
                    else -> {
                        changeColor = false
                        MaterialTheme.colorScheme.onSurface
                    }
                }
            }
        }

    val download by LocalDownloadUtil.current.getDownload(mediaMetadata?.id ?: "").collectAsState(initial = null)

    val sleepTimerEnabled =
        remember(playerConnection.service.sleepTimer.triggerTime, playerConnection.service.sleepTimer.pauseWhenSongEnd) {
            playerConnection.service.sleepTimer.isActive
        }

    var sleepTimerTimeLeft by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft =
                    if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                        playerConnection.player.duration - playerConnection.player.currentPosition
                    } else {
                        playerConnection.service.sleepTimer.triggerTime - System.currentTimeMillis()
                    }
                delay(1000L)
            }
        }
    }

    var showSleepTimerDialog by remember {
        mutableStateOf(false)
    }

    var sleepTimerValue by remember {
        mutableFloatStateOf(30f)
    }
    if (showSleepTimerDialog) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showSleepTimerDialog = false },
            icon = { Icon(painter = painterResource(R.drawable.bedtime), contentDescription = null) },
            title = { Text(stringResource(R.string.sleep_timer)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSleepTimerDialog = false
                        playerConnection.service.sleepTimer.start(sleepTimerValue.roundToInt())
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSleepTimerDialog = false },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pluralStringResource(R.plurals.minute, sleepTimerValue.roundToInt(), sleepTimerValue.roundToInt()),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Slider(
                        value = sleepTimerValue,
                        onValueChange = { sleepTimerValue = it },
                        valueRange = 5f..120f,
                        steps = (120 - 5) / 5 - 1,
                    )

                    OutlinedButton(
                        onClick = {
                            showSleepTimerDialog = false
                            playerConnection.service.sleepTimer.start(-1)
                        },
                    ) {
                        Text(stringResource(R.string.end_of_song))
                    }
                }
            },
        )
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showErrorPlaylistAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onAdd = { playlist ->
            database.transaction {
                mediaMetadata?.let {
                    insert(it)
                    if (checkInPlaylist(playlist.id, it.id) == 0) {
                        insert(
                            PlaylistSongMap(
                                songId = it.id,
                                playlistId = playlist.id,
                                position = playlist.songCount,
                            ),
                        )
                        update(playlist.playlist.copy(lastUpdateTime = LocalDateTime.now()))
                    } else {
                        showErrorPlaylistAddDialog = true
                    }
                }
            }
        },
        onDismiss = {
            showChoosePlaylistDialog = false
        },
    )

    if (showErrorPlaylistAddDialog && mediaMetadata != null) {
        ListDialog(
            onDismiss = {
                showErrorPlaylistAddDialog = false
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
                    modifier =
                        Modifier
                            .clickable { showErrorPlaylistAddDialog = false },
                )
            }

            items(listOf(mediaMetadata)) { song ->
                ListItem(
                    title = song!!.title,
                    thumbnailContent = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(ListThumbnailSize),
                        ) {
                            AsyncImage(
                                model = song.thumbnailUrl,
                                contentDescription = null,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                            )
                        }
                    },
                    subtitle =
                        joinByBullet(
                            song.artists.joinToString { it.name },
                            makeTimeString(song.duration * 1000L),
                        ),
                )
            }
        }
    }

    val currentFormat by playerConnection.currentFormat.collectAsState(initial = null)

    var showDetailsDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showDetailsDialog) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showDetailsDialog = false },
            containerColor = if (useBlackBackground) Color.Black else AlertDialogDefaults.containerColor,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.info),
                    contentDescription = null,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showDetailsDialog = false },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            text = {
                Column(
                    modifier =
                        Modifier
                            .sizeIn(minWidth = 280.dp, maxWidth = 560.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    listOf(
                        stringResource(R.string.song_title) to mediaMetadata?.title,
                        stringResource(R.string.song_artists) to mediaMetadata?.artists?.joinToString { it.name },
                        stringResource(R.string.media_id) to mediaMetadata?.id,
                        "Itag" to currentFormat?.itag?.toString(),
                        stringResource(R.string.mime_type) to currentFormat?.mimeType,
                        stringResource(R.string.codecs) to currentFormat?.codecs,
                        stringResource(R.string.bitrate) to currentFormat?.bitrate?.let { "${it / 1000} Kbps" },
                        stringResource(R.string.sample_rate) to currentFormat?.sampleRate?.let { "$it Hz" },
                        stringResource(R.string.loudness) to currentFormat?.loudnessDb?.let { "$it dB" },
                        stringResource(R.string.volume) to "${(playerConnection.player.volume * 100).toInt()}%",
                        stringResource(R.string.file_size) to
                            currentFormat?.contentLength?.let { Formatter.formatShortFileSize(context, it) },
                    ).forEach { (label, text) ->
                        val displayText = text ?: stringResource(R.string.unknown)
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.titleMedium,
                            modifier =
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(displayText))
                                        Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                                    },
                                ),
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            },
        )
    }

    val queueSheetState =
        rememberBottomSheetState(
            dismissedBound = QueuePeekHeight + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
            expandedBound = state.expandedBound,
        )

    var showPlayerMenu by remember { mutableStateOf(false) }

    var showShareDialog by remember { mutableStateOf(false) }
    if (showShareDialog) {
        if (mediaMetadata?.id != null) {
            ShareSongDialog(
                mediaMetadata = mediaMetadata!!,
                albumArt = mediaMetadata!!.thumbnailUrl,
                onDismiss = { showShareDialog = false },
                shareLink = "https://music.youtube.com/watch?v=${mediaMetadata!!.id}",
                gradientColors = gradientColors
            )
        }
    }

    val showHeadsetName by rememberPreference(ShowHeadsetNameKey, defaultValue = false)

    BottomSheet(
        state = state,
        modifier = modifier,
        brushBackgroundColor =
            if (hasGradientColours &&
                state.value > changeBound
            ) {
                Brush.verticalGradient(gradientColors)
            } else {
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainer,
                        MaterialTheme.colorScheme.surfaceContainer,
                    ),
                )
            }
        ,
        onDismiss = {
            playerConnection.service.clearAutomix()
            playerConnection.player.stop()
            playerConnection.player.clearMediaItems()
        },
        collapsedContent = {
            MiniPlayer(
                position = playerConnection.player.currentPosition,
                duration = playerConnection.player.duration,
            )
        },
    ) {
        val controlsContent: @Composable ColumnScope.(MediaMetadata, songTitleFontSize: TextUnit) -> Unit = { mediaMetadata, songTitleFontSize ->
            val isPlaying by playerConnection.isPlaying.collectAsState()
            val playbackState by playerConnection.playbackState.collectAsState()
            val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
            val canSkipNext by playerConnection.canSkipNext.collectAsState()

            val playPauseRoundness by animateDpAsState(
                targetValue = if (isPlaying) 24.dp else 36.dp,
                animationSpec = tween(durationMillis = 90, easing = LinearEasing),
                label = "playPauseRoundness",
            )

            // Title row (remove explicit icon from here)
            Row(
                horizontalArrangement =
                    when (playerTextAlignment) {
                        PlayerTextAlignment.SIDED -> Arrangement.Start
                        PlayerTextAlignment.CENTER -> Arrangement.Center
                    },
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = PlayerHorizontalPadding, end = PlayerHorizontalPadding / 2),
            ) {
                AnimatedContent(
                    targetState = mediaMetadata.title to mediaMetadata.id,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "",
                ) { (title, _) ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = songTitleFontSize),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = onBackgroundColor,
                        modifier =
                            Modifier
                                .basicMarquee()
                                .clickable(enabled = mediaMetadata.album != null) {
                                    navController.navigate("album/${mediaMetadata.album!!.id}")
                                    state.collapseSoft()
                                },
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Artist row (add explicit icon here)
            Row(
                horizontalArrangement =
                    when (playerTextAlignment) {
                        PlayerTextAlignment.SIDED -> Arrangement.Start
                        PlayerTextAlignment.CENTER -> Arrangement.Center
                    },
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = PlayerHorizontalPadding, end = PlayerHorizontalPadding / 2),
            ) {
                if (mediaMetadata.explicit) {
                    Icon(
                        painter = painterResource(R.drawable.explicit),
                        contentDescription = "Explicit",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp).align(Alignment.CenterVertically)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                mediaMetadata.artists.fastForEachIndexed { index, artist ->
                    AnimatedContent(
                        targetState = artist.name to (artist.id ?: artist.name),
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "",
                    ) { (name, _) ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            color = onBackgroundColor,
                            maxLines = 1,
                            modifier =
                                Modifier.clickable(enabled = artist.id != null) {
                                    navController.navigate("artist/${artist.id}")
                                    state.collapseSoft()
                                },
                        )
                    }

                    if (index != mediaMetadata.artists.lastIndex) {
                        AnimatedContent(
                            targetState = ", ",
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "",
                        ) { comma ->
                            Text(
                                text = comma,
                                style = MaterialTheme.typography.titleMedium,
                                color = onBackgroundColor,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                // Only menu button remains here
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(42.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable {
                            menuState.show {
                                PlayerMenu(
                                    mediaMetadata = mediaMetadata,
                                    navController = navController,
                                    playerBottomSheetState = state,
                                    onShowDetailsDialog = { showDetailsDialog = true },
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                ) {
                    Image(
                        painter = painterResource(R.drawable.more_horiz),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSecondaryContainer),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Timeline (slider/squiggly) - make thicker and wider
            PlayerProgressBar(
                playerConnection = playerConnection,
                duration = duration,
                onSeek = { playerConnection.player.seekTo(it) },
                sliderStyle = sliderStyle,
                onBackgroundColor = onBackgroundColor
            )

            Spacer(Modifier.height(6.dp))

            // Remove headphone, share, and like buttons from here
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding),
            ) {
                ResizableIconButton(
                    icon = R.drawable.skip_previous,
                    enabled = canSkipPrevious,
                    color = onBackgroundColor,
                    modifier = Modifier.size(56.dp),
                    onClick = playerConnection::seekToPrevious,
                )
                Spacer(Modifier.width(32.dp))
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(playPauseRoundness))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable {
                            if (playbackState == STATE_ENDED) {
                                playerConnection.player.seekTo(0, 0)
                                playerConnection.player.playWhenReady = true
                            } else {
                                playerConnection.player.togglePlayPause()
                            }
                        },
                ) {
                    Image(
                        painter =
                            painterResource(
                                if (playbackState ==
                                    STATE_ENDED
                                ) {
                                    R.drawable.replay
                                } else if (isPlaying) {
                                    R.drawable.pause
                                } else {
                                    R.drawable.play
                                },
                            ),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSecondaryContainer),
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .size(40.dp),
                    )
                }
                Spacer(Modifier.width(32.dp))
                ResizableIconButton(
                    icon = R.drawable.skip_next,
                    enabled = canSkipNext,
                    color = onBackgroundColor,
                    modifier = Modifier.size(56.dp),
                    onClick = playerConnection::seekToNext,
                )
            }
        }

        when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                Row(
                    modifier =
                        Modifier
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                            .padding(bottom = queueSheetState.collapsedBound),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f),
                    ) {
                        Thumbnail(
                            sliderPositionProvider = { playerConnection.player.currentPosition },
                            modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection),
                            changeColor = changeColor,
                            color = onBackgroundColor,
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier =
                            Modifier
                                .weight(1f)
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)),
                    ) {
                        Spacer(Modifier.weight(1f))

                        mediaMetadata?.let {
                            controlsContent(it, 30.sp)
                        }

                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                        Modifier
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                            .padding(bottom = queueSheetState.collapsedBound),
                ) {
                    Spacer(Modifier.height(40.dp))
                    // Drag bar (Apple Music style)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.7f))
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    // Overlay art/lyric pill above thumbnail/lyrics area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.dp) // No height, just overlays
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp)
                                .zIndex(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                val selectedArt = !showLyrics
                                val selectedLyrics = showLyrics
                                Text(
                                    text = "Art",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (selectedArt) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (selectedArt) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { showLyrics = false }
                                        .padding(horizontal = 20.dp, vertical = 8.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = "Lyrics",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (selectedLyrics) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (selectedLyrics) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { showLyrics = true }
                                        .padding(horizontal = 20.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    // Main area for thumbnail/lyrics and floating pill
                    val artWeight = if (playerConnection.player.mediaItemCount > 1) 1.3f else 1.7f
                    Box(
                        modifier = Modifier
                            .weight(artWeight)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        // Floating art/lyric pill
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp)
                                .zIndex(1f)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                val selectedArt = !showLyrics
                                val selectedLyrics = showLyrics
                                Text(
                                    text = "Art",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (selectedArt) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (selectedArt) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { showLyrics = false }
                                        .padding(horizontal = 20.dp, vertical = 8.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = "Lyrics",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (selectedLyrics) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (selectedLyrics) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { showLyrics = true }
                                        .padding(horizontal = 20.dp, vertical = 8.dp)
                                )
                            }
                        }
                        // Thumbnail or Lyrics area
                        Thumbnail(
                            sliderPositionProvider = { playerConnection.player.currentPosition },
                            modifier = Modifier
                                .fillMaxWidth(1.0f)
                                .padding(top = 48.dp), // Add top padding so pill doesn't overlap content
                            changeColor = changeColor,
                            color = onBackgroundColor,
                        )
                    }
                    // Move song name and artist name up by reducing vertical gap
                    Spacer(Modifier.height(24.dp))
                    mediaMetadata?.let {
                        controlsContent(it, 30.sp) // Pass larger font size
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        Queue(
            state = queueSheetState,
            playerBottomSheetState = state,
            navController = navController,
            backgroundColor =
                if (useBlackBackground) {
                    Color.Black
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            onBackgroundColor = onBackgroundColor,
        )
    }
}
