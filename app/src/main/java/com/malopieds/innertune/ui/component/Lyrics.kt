package com.malopieds.innertune.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malopieds.innertune.BuildConfig
import com.malopieds.innertune.LocalPlayerConnection
import com.malopieds.innertune.R
import com.malopieds.innertune.constants.LyricsClickKey
import com.malopieds.innertune.constants.LyricsTextPositionKey
import com.malopieds.innertune.constants.PlayerBackgroundStyle
import com.malopieds.innertune.constants.PlayerBackgroundStyleKey
import com.malopieds.innertune.constants.PlayerTextAlignmentKey
import com.malopieds.innertune.constants.TranslateLyricsKey
import com.malopieds.innertune.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.malopieds.innertune.lyrics.LyricsEntry
import com.malopieds.innertune.lyrics.LyricsEntry.Companion.HEAD_LYRICS_ENTRY
import com.malopieds.innertune.lyrics.LyricsUtils
import com.malopieds.innertune.lyrics.LyricsUtils.findCurrentLineIndex
import com.malopieds.innertune.lyrics.LyricsUtils.parseLyrics
import com.malopieds.innertune.ui.component.shimmer.ShimmerHost
import com.malopieds.innertune.ui.component.shimmer.TextPlaceholder
import com.malopieds.innertune.ui.menu.LyricsMenu
import com.malopieds.innertune.ui.screens.settings.LyricsPosition
import com.malopieds.innertune.ui.screens.settings.PlayerTextAlignment
import com.malopieds.innertune.ui.utils.fadingEdge
import com.malopieds.innertune.utils.rememberEnumPreference
import com.malopieds.innertune.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.seconds
import com.malopieds.innertune.ui.player.ShareLyricsDialog
import coil.ImageLoader
import coil.request.ImageRequest
import android.graphics.drawable.BitmapDrawable
import com.malopieds.innertune.ui.theme.extractGradientColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun Lyrics(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    changeColor: Boolean,
    color: Color,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val density = LocalDensity.current

    val lyricsTextPosition by rememberEnumPreference(LyricsTextPositionKey, LyricsPosition.CENTER)
    val playerTextAlignment by rememberEnumPreference(PlayerTextAlignmentKey, PlayerTextAlignment.SIDED)
    var translationEnabled by rememberPreference(TranslateLyricsKey, false)
    val changeLyrics by rememberPreference(LyricsClickKey, true)

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val translating by playerConnection.translating.collectAsState()
    val lyricsEntity by playerConnection.currentLyrics.collectAsState(initial = null)
    val lyrics =
        remember(lyricsEntity, translating) {
            if (translating) {
                null
            } else {
                lyricsEntity?.lyrics
            }
        }

    val lines =
        remember(lyrics) {
            if (lyrics == null || lyrics == LYRICS_NOT_FOUND) {
                emptyList()
            } else if (lyrics.startsWith("[")) {
                listOf(HEAD_LYRICS_ENTRY) + parseLyrics(lyrics)
            } else {
                lyrics.lines().mapIndexed { index, line -> LyricsEntry(index * 100L, line) }
            }
        }
    val isSynced =
        remember(lyrics) {
            !lyrics.isNullOrEmpty() && lyrics.startsWith("[")
        }

    var currentLineIndex by remember {
        mutableIntStateOf(-1)
    }
    // Because LaunchedEffect has delay, which leads to inconsistent with current line color and scroll animation,
    // we use deferredCurrentLineIndex when user is scrolling
    var deferredCurrentLineIndex by rememberSaveable {
        mutableIntStateOf(0)
    }

    var lastPreviewTime by rememberSaveable {
        mutableLongStateOf(0L)
    }
    var isSeeking by remember {
        mutableStateOf(false)
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val isSystemInDarkThemeValue = androidx.compose.foundation.isSystemInDarkTheme()
    val themePrimary = MaterialTheme.colorScheme.primary
    val themeSecondary = MaterialTheme.colorScheme.secondary
    var gradientColors by remember { mutableStateOf<List<Color>>(listOf(themePrimary, themeSecondary)) }
    val darkTheme by rememberEnumPreference(com.malopieds.innertune.constants.DarkModeKey, defaultValue = com.malopieds.innertune.ui.screens.settings.DarkMode.AUTO)
    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.GRADIENT)
    val currentMediaMetadata = mediaMetadata
    LaunchedEffect(currentMediaMetadata, playerBackground, darkTheme, isSystemInDarkThemeValue, themePrimary, themeSecondary) {
        if (playerBackground == PlayerBackgroundStyle.GRADIENT && currentMediaMetadata?.thumbnailUrl != null) {
            val result = withContext(Dispatchers.IO) {
                val drawable = ImageLoader(context)
                    .execute(
                        ImageRequest.Builder(context)
                            .data(currentMediaMetadata.thumbnailUrl)
                            .allowHardware(false)
                            .build()
                    ).drawable as? BitmapDrawable
                drawable?.bitmap?.extractGradientColors(
                    darkTheme == com.malopieds.innertune.ui.screens.settings.DarkMode.ON || (darkTheme == com.malopieds.innertune.ui.screens.settings.DarkMode.AUTO && isSystemInDarkThemeValue)
                )
            }
            if (!result.isNullOrEmpty()) {
                gradientColors = result
            }
        } else {
            gradientColors = listOf(themePrimary, themeSecondary)
        }
    }

    LaunchedEffect(lyrics) {
        if (lyrics.isNullOrEmpty() || !lyrics.startsWith("[")) {
            currentLineIndex = -1
            return@LaunchedEffect
        }
        while (isActive) {
            delay(150)
            val sliderPosition = sliderPositionProvider()
            isSeeking = sliderPosition != null
            currentLineIndex = findCurrentLineIndex(lines, sliderPosition ?: playerConnection.player.currentPosition)
        }
    }

    LaunchedEffect(isSeeking, lastPreviewTime) {
        if (isSeeking) {
            lastPreviewTime = 0L
        } else if (lastPreviewTime != 0L) {
            delay(LyricsPreviewTime)
            lastPreviewTime = 0L
        }
    }

    val lazyListState = rememberLazyListState()

    LaunchedEffect(currentLineIndex, lastPreviewTime) {
        if (!isSynced) return@LaunchedEffect
        if (currentLineIndex != -1) {
            deferredCurrentLineIndex = currentLineIndex
            if (lastPreviewTime == 0L) {
                if (isSeeking) {
                    lazyListState.scrollToItem(currentLineIndex, with(density) { 36.dp.toPx().toInt() })
                } else {
                    lazyListState.animateScrollToItem(currentLineIndex, with(density) { 36.dp.toPx().toInt() })
                }
            }
        }
    }

    val currentLine =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.primary
            else ->
                if (changeColor) {
                    color
                } else {
                    MaterialTheme.colorScheme.primary
                }
        }

    val outLines =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary
            else ->
                if (changeColor) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.secondary
                }
        }

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .fillMaxSize()
                .padding(bottom = 12.dp),
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding =
                WindowInsets.systemBars
                    .only(WindowInsetsSides.Top)
                    .add(WindowInsets(top = maxHeight / 2, bottom = maxHeight / 2))
                    .asPaddingValues(),
            modifier =
                Modifier
                    .fadingEdge(vertical = 64.dp)
                    .nestedScroll(
                        remember {
                            object : NestedScrollConnection {
                                override fun onPostScroll(
                                    consumed: Offset,
                                    available: Offset,
                                    source: NestedScrollSource,
                                ): Offset {
                                    lastPreviewTime = System.currentTimeMillis()
                                    return super.onPostScroll(consumed, available, source)
                                }

                                override suspend fun onPostFling(
                                    consumed: Velocity,
                                    available: Velocity,
                                ): Velocity {
                                    lastPreviewTime = System.currentTimeMillis()
                                    return super.onPostFling(consumed, available)
                                }
                            }
                        },
                    ),
        ) {
            val displayedCurrentLineIndex = if (isSeeking) deferredCurrentLineIndex else currentLineIndex

            if (lyrics == null || translating) {
                item {
                    ShimmerHost {
                        repeat(10) {
                            Box(
                                contentAlignment =
                                    when (lyricsTextPosition) {
                                        LyricsPosition.LEFT -> Alignment.CenterStart
                                        LyricsPosition.CENTER -> Alignment.Center
                                        LyricsPosition.RIGHT -> Alignment.CenterEnd
                                    },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 4.dp),
                            ) {
                                TextPlaceholder()
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = lines,
                ) { index, item ->
                    val isCurrent = index == displayedCurrentLineIndex
                    Text(
                        text = item.text,
                        fontSize = if (isCurrent) 32.sp else 28.sp,
                        lineHeight = if (isCurrent) 36.sp else 32.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = if (isCurrent) currentLine else outLines.copy(alpha = 0.5f),
                        textAlign = TextAlign.Left,
                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isSynced && changeLyrics) {
                                playerConnection.player.seekTo(item.time)
                                lastPreviewTime = 0L
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                            .alpha(1f),
                    )
                }
            }
        }

        if (lyrics == LYRICS_NOT_FOUND) {
            Text(
                text = stringResource(R.string.lyrics_not_found),
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Left,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .alpha(0.5f),
            )
        }

        mediaMetadata?.let { mediaMetadata ->
            var showShareLyricsDialog by remember { mutableStateOf(false) }
            if (showShareLyricsDialog && !lyrics.isNullOrEmpty()) {
                val shareableLyrics = if (lyrics.startsWith("[")) {
                    LyricsUtils.parseLyrics(lyrics).joinToString("\n") { it.text }
                } else {
                    lyrics
                }
                ShareLyricsDialog(
                    lyrics = shareableLyrics,
                    mediaMetadata = mediaMetadata,
                    onDismiss = { showShareLyricsDialog = false },
                    gradientColors = gradientColors
                )
            }
            Row(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp),
            ) {
                if (BuildConfig.FLAVOR != "foss") {
                    IconButton(
                        onClick = {
                            translationEnabled = !translationEnabled
                        },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.translate),
                            contentDescription = null,
                            tint = LocalContentColor.current.copy(alpha = if (translationEnabled) 1f else 0.3f),
                        )
                    }
                }
                if (!lyrics.isNullOrEmpty()) {
                    IconButton(
                        onClick = { showShareLyricsDialog = true },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.share),
                            contentDescription = "Share Lyrics",
                        )
                    }
                }
                IconButton(
                    onClick = {
                        menuState.show {
                            LyricsMenu(
                                lyricsProvider = { lyricsEntity },
                                mediaMetadataProvider = { mediaMetadata },
                                onDismiss = menuState::dismiss,
                            )
                        }
                    },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.more_horiz),
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

const val ANIMATE_SCROLL_DURATION = 300L
val LyricsPreviewTime = 4.seconds
