package com.malopieds.innertune.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.malopieds.innertune.LocalPlayerConnection
import com.malopieds.innertune.R
import com.malopieds.innertune.constants.MiniPlayerHeight
import com.malopieds.innertune.constants.ThumbnailCornerRadius
import com.malopieds.innertune.extensions.togglePlayPause
import com.malopieds.innertune.models.MediaMetadata
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.graphicsLayer

@Composable
private fun AnimatedProgressBar(
    progress: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val playerConnection = LocalPlayerConnection.current ?: return

    Box(
        modifier = modifier
            .height(6.dp)
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val percentage = offset.x / size.width
                    val newPosition = (playerConnection.player.duration * percentage).toLong()
                    playerConnection.player.seekTo(newPosition)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    val percentage = (change.position.x / size.width).coerceIn(0f, 1f)
                    val newPosition = (playerConnection.player.duration * percentage).toLong()
                    playerConnection.player.seekTo(newPosition)
                }
            }
    ) {
        // Draw background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
        )

        // Draw progress
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(colorScheme.primary)
        )
    }
}

@Composable
fun MiniPlayer(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val error by playerConnection.error.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()

    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 75.dp.toPx() }
    val animatableOffsetX = remember { Animatable(0f) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier =
            modifier
                .fillMaxWidth()
                .height(MiniPlayerHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
        ) {
            AnimatedProgressBar(
                progress = if (duration == C.TIME_UNSET) 0f else (position.toFloat() / duration),
                isPlaying = isPlaying,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 12.dp)
                        .graphicsLayer(translationX = animatableOffsetX.value)
                        .pointerInput(canSkipNext, canSkipPrevious) {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    dragOffsetX = 0f
                                    coroutineScope.launch { animatableOffsetX.stop() }
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetX += dragAmount
                                    coroutineScope.launch {
                                        animatableOffsetX.snapTo(dragOffsetX)
                                    }
                                },
                                onDragEnd = {
                                    when {
                                        dragOffsetX > swipeThresholdPx && canSkipPrevious -> {
                                            coroutineScope.launch {
                                                animatableOffsetX.animateTo(
                                                    targetValue = 500f,
                                                    animationSpec = tween(durationMillis = 150)
                                                )
                                                animatableOffsetX.snapTo(-500f)
                                                playerConnection.seekToPrevious()
                                                animatableOffsetX.animateTo(0f, animationSpec = tween(durationMillis = 150))
                                            }
                                        }
                                        dragOffsetX < -swipeThresholdPx && canSkipNext -> {
                                            coroutineScope.launch {
                                                animatableOffsetX.animateTo(
                                                    targetValue = -500f,
                                                    animationSpec = tween(durationMillis = 150)
                                                )
                                                animatableOffsetX.snapTo(500f)
                                                playerConnection.seekToNext()
                                                animatableOffsetX.animateTo(0f, animationSpec = tween(durationMillis = 150))
                                            }
                                        }
                                        else -> {
                                            coroutineScope.launch {
                                                animatableOffsetX.animateTo(0f, animationSpec = tween(durationMillis = 200))
                                            }
                                        }
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        animatableOffsetX.animateTo(0f, animationSpec = tween(durationMillis = 200))
                                    }
                                },
                            )
                        },
                ) {
                    Box(Modifier.weight(1f)) {
                        mediaMetadata?.let {
                            MiniMediaInfo(
                                mediaMetadata = it,
                                error = error,
                                modifier = Modifier.padding(horizontal = 6.dp),
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (playbackState == Player.STATE_ENDED) {
                                playerConnection.player.seekTo(0, 0)
                                playerConnection.player.playWhenReady = true
                            } else {
                                playerConnection.player.togglePlayPause()
                            }
                        },
                    ) {
                        Icon(
                            painter =
                                painterResource(
                                    if (playbackState ==
                                        Player.STATE_ENDED
                                    ) {
                                        R.drawable.replay
                                    } else if (isPlaying) {
                                        R.drawable.pause
                                    } else {
                                        R.drawable.play
                                    },
                                ),
                            contentDescription = null,
                        )
                    }

                    IconButton(
                        enabled = canSkipNext,
                        onClick = playerConnection::seekToNext,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MiniMediaInfo(
    mediaMetadata: MediaMetadata,
    error: PlaybackException?,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.padding(6.dp)) {
            AsyncImage(
                model = mediaMetadata.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(48.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(ThumbnailCornerRadius)),
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = error != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(ThumbnailCornerRadius),
                        ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier =
                            Modifier
                                .align(Alignment.Center),
                    )
                }
            }
        }

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
        ) {
            AnimatedContent(
                targetState = mediaMetadata.title,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "",
            ) { title ->
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(),
                )
            }

            AnimatedContent(
                targetState = mediaMetadata.artists.joinToString { it.name },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "",
            ) { artists ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (mediaMetadata.explicit) {
                        Icon(
                            painter = painterResource(R.drawable.explicit),
                            contentDescription = "Explicit",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp).align(Alignment.CenterVertically)
                        )
                        Spacer(Modifier.width(2.dp))
                    }
                    Text(
                        text = artists,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee(),
                    )
                }
            }
        }
    }
}
