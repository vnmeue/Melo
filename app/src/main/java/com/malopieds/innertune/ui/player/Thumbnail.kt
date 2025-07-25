package com.malopieds.innertune.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.malopieds.innertune.LocalPlayerConnection
import com.malopieds.innertune.constants.PlayerHorizontalPadding
import com.malopieds.innertune.constants.ShowLyricsKey
import com.malopieds.innertune.constants.SwipeThumbnailKey
import com.malopieds.innertune.constants.ThumbnailCornerRadius
import com.malopieds.innertune.extensions.metadata
import com.malopieds.innertune.ui.component.Lyrics
import com.malopieds.innertune.utils.rememberPreference
import kotlin.math.roundToInt

@Composable
fun Thumbnail(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    changeColor: Boolean = false,
    color: Color,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentView = LocalView.current

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val error by playerConnection.error.collectAsState()

    val showLyrics by rememberPreference(ShowLyricsKey, false)
    val swipeThumbnail by rememberPreference(SwipeThumbnailKey, true)

    // Get player and indices
    val player = playerConnection.player
    val currentIndex = player.currentMediaItemIndex
    val nextIndex = if (currentIndex != -1 && currentIndex + 1 < player.mediaItemCount) currentIndex + 1 else -1
    val nextMediaItem = if (nextIndex != -1) player.getMediaItemAt(nextIndex) else null
    val nextMetadata = nextMediaItem?.metadata as? com.malopieds.innertune.models.MediaMetadata

    DisposableEffect(showLyrics) {
        currentView.keepScreenOn = showLyrics
        onDispose {
            currentView.keepScreenOn = false
        }
    }

    var offsetX by remember { mutableFloatStateOf(0f) }

    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = !showLyrics && error == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = PlayerHorizontalPadding)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragCancel = {
                                    offsetX = 0f
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    if (swipeThumbnail) {
                                        offsetX += dragAmount
                                    }
                                },
                                onDragEnd = {
                                    if (offsetX > 300) {
                                        if (playerConnection.player.previousMediaItemIndex != -1) {
                                            playerConnection.player.seekToPreviousMediaItem()
                                        }
                                    } else if (offsetX < -300) {
                                        if (playerConnection.player.nextMediaItemIndex != -1) {
                                            playerConnection.player.seekToNext()
                                        }
                                    }
                                    offsetX = 0f
                                },
                            )
                        },
            ) {
                // Show next album art as a stack behind current one only if there's a next item
                if (nextMetadata?.thumbnailUrl != null && player.mediaItemCount > 1) {
                    AsyncImage(
                        model = nextMetadata.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .offset(x = 32.dp)
                            .fillMaxWidth(0.85f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius * 2))
                            .alpha(0.6f)
                    )
                }
                // Current album art in focus
                AsyncImage(
                    model = mediaMetadata?.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier =
                        Modifier
                            .offset { 
                                if (player.mediaItemCount > 1) {
                                    IntOffset(offsetX.roundToInt() - 32, 0)
                                } else {
                                    IntOffset(offsetX.roundToInt(), 0)
                                }
                            }
                            .fillMaxWidth(if (player.mediaItemCount > 1) 1.0f else 1.05f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius * 2))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { offset ->
                                        if (offset.x < size.width / 2) {
                                            playerConnection.player.seekBack()
                                        } else {
                                            playerConnection.player.seekForward()
                                        }
                                    },
                                )
                            },
                )
            }
        }

        AnimatedVisibility(
            visible = showLyrics && error == null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Lyrics(
                sliderPositionProvider = sliderPositionProvider,
                changeColor = changeColor,
                color = color,
                modifier = Modifier.fillMaxSize()
            )
        }

        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
                Modifier
                    .padding(32.dp)
                    .align(Alignment.Center),
        ) {
            error?.let { error ->
                PlaybackError(
                    error = error,
                    retry = playerConnection.player::prepare,
                )
            }
        }
    }
}
