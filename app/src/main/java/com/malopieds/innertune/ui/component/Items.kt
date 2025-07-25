@file:OptIn(ExperimentalFoundationApi::class)

package com.malopieds.innertune.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
import androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING
import androidx.media3.exoplayer.offline.Download.STATE_QUEUED
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.malopieds.innertube.YouTube
import com.malopieds.innertube.models.AlbumItem
import com.malopieds.innertube.models.ArtistItem
import com.malopieds.innertube.models.PlaylistItem
import com.malopieds.innertube.models.SongItem
import com.malopieds.innertube.models.YTItem
import com.malopieds.innertune.LocalDatabase
import com.malopieds.innertune.LocalDownloadUtil
import com.malopieds.innertune.LocalPlayerConnection
import com.malopieds.innertune.R
import com.malopieds.innertune.constants.GridThumbnailHeight
import com.malopieds.innertune.constants.ListItemHeight
import com.malopieds.innertune.constants.ListThumbnailSize
import com.malopieds.innertune.constants.SmallGridThumbnailHeight
import com.malopieds.innertune.constants.ThumbnailCornerRadius
import com.malopieds.innertune.db.entities.Album
import com.malopieds.innertune.db.entities.Artist
import com.malopieds.innertune.db.entities.Playlist
import com.malopieds.innertune.db.entities.Song
import com.malopieds.innertune.extensions.toMediaItem
import com.malopieds.innertune.models.MediaMetadata
import com.malopieds.innertune.playback.queues.ListQueue
import com.malopieds.innertune.ui.theme.extractThemeColor
import com.malopieds.innertune.utils.joinByBullet
import com.malopieds.innertune.utils.makeTimeString
import com.malopieds.innertune.utils.reportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@Composable
inline fun ListItem(
    modifier: Modifier = Modifier,
    title: String,
    noinline subtitle: (@Composable RowScope.() -> Unit)? = null,
    thumbnailContent: @Composable () -> Unit,
    trailingContent: @Composable RowScope.() -> Unit = {},
    isActive: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            if (isActive) {
                modifier
                    .height(ListItemHeight)
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color = MaterialTheme.colorScheme.secondaryContainer)
            } else {
                modifier
                    .height(ListItemHeight)
                    .padding(horizontal = 8.dp)
            },
    ) {
        Box(
            modifier = Modifier.padding(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            thumbnailContent()
        }
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .basicMarquee()
                        .fillMaxWidth(),
            )

            if (subtitle != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    subtitle()
                }
            }
        }

        trailingContent()
    }
}

@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String?,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailContent: @Composable () -> Unit,
    trailingContent: @Composable RowScope.() -> Unit = {},
    isActive: Boolean = false,
) = ListItem(
    title = title,
    subtitle = {
        badges()

        if (!subtitle.isNullOrEmpty()) {
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    },
    thumbnailContent = thumbnailContent,
    trailingContent = trailingContent,
    modifier = modifier,
    isActive = isActive,
)

@Composable
fun GridItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailContent: @Composable BoxWithConstraintsScope.() -> Unit,
    thumbnailShape: Shape,
    thumbnailRatio: Float = 1f,
    fillMaxWidth: Boolean = false,
) {
    Column(
        modifier =
            if (fillMaxWidth) {
                modifier
                    .padding(12.dp)
                    .fillMaxWidth()
            } else {
                modifier
                    .padding(12.dp)
                    .width(GridThumbnailHeight * thumbnailRatio)
            },
    ) {
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier =
                if (fillMaxWidth) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.height(GridThumbnailHeight)
                }.aspectRatio(thumbnailRatio)
                    .clip(thumbnailShape),
        ) {
            thumbnailContent()
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            modifier =
                Modifier
                    .basicMarquee()
                    .fillMaxWidth(),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            badges()

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SmallGridItem(
    modifier: Modifier = Modifier,
    title: String,
    thumbnailContent: @Composable BoxWithConstraintsScope.() -> Unit,
    thumbnailShape: Shape,
    thumbnailRatio: Float = 1f,
    isArtist: Boolean? = false,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = if (isArtist == true) Alignment.CenterHorizontally else Alignment.Start,
        modifier = modifier.padding(12.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(thumbnailRatio)
                .clip(thumbnailShape),
        ) {
            thumbnailContent()
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .basicMarquee()
                .fillMaxWidth(),
        )
    }
}

@Composable
fun SongListItem(
    song: Song,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    showLikedIcon: Boolean = true,
    showInLibraryIcon: Boolean = false,
    showDownloadIcon: Boolean = true,
    isSelected: Boolean = false,
    badges: @Composable RowScope.() -> Unit = {
        if (showLikedIcon && song.song.liked) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
        if (showInLibraryIcon && song.song.inLibrary != null) {
            Icon(
                painter = painterResource(R.drawable.library_add_check),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
        if (showDownloadIcon) {
            val download by LocalDownloadUtil.current.getDownload(song.id).collectAsState(initial = null)
            when (download?.state) {
                STATE_COMPLETED ->
                    Icon(
                        painter = painterResource(R.drawable.offline),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .size(18.dp)
                                .padding(end = 2.dp),
                    )

                STATE_QUEUED, STATE_DOWNLOADING ->
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier =
                            Modifier
                                .size(16.dp)
                                .padding(end = 2.dp),
                    )

                else -> {}
            }
        }
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val playerConnection = LocalPlayerConnection.current
    var dragOffset by remember { mutableStateOf(0f) }
    var itemWidth by remember { mutableStateOf(1f) } // avoid div by zero
    var addedToQueue by remember { mutableStateOf(false) }
    val dragThresholdPercent = 0.5f

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                itemWidth = coordinates.size.width.toFloat()
            }
            .background(Color.Transparent)
    ) {
        // Queue icon background
        if (dragOffset > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize(),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    painter = painterResource(R.drawable.queue_music),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    modifier = Modifier.size(48.dp).padding(start = 24.dp)
                )
            }
        }
        // Draggable song item
        ListItem(
            title = song.song.title,
            subtitle =
                joinByBullet(
                    song.artists.joinToString { it.name },
                    makeTimeString(song.song.duration * 1000L),
                ),
            badges = badges,
            thumbnailContent = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(ListThumbnailSize),
                ) {
                    if (albumIndex != null) {
                        AnimatedVisibility(
                            visible = !isActive,
                            enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                            exit = shrinkOut(shrinkTowards = Alignment.Center) + fadeOut(),
                        ) {
                            if (isSelected) {
                                Icon(
                                    painter = painterResource(R.drawable.done),
                                    modifier = Modifier.align(Alignment.Center),
                                    contentDescription = null,
                                )
                            } else {
                                Text(
                                    text = albumIndex.toString(),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    } else {
                        if (isSelected) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .zIndex(1000f)
                                        .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                        .background(Color.Black.copy(alpha = 0.5f)),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.done),
                                    modifier = Modifier.align(Alignment.Center),
                                    contentDescription = null,
                                )
                            }
                        }
                        AsyncImage(
                            model = song.song.thumbnailUrl,
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                        )
                    }

                    PlayingIndicatorBox(
                        isActive = isActive,
                        playWhenReady = isPlaying,
                        color = if (albumIndex != null) MaterialTheme.colorScheme.onBackground else Color.White,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(
                                    color =
                                        if (albumIndex != null) {
                                            Color.Transparent
                                        } else {
                                            Color.Black.copy(
                                                alpha = 0.4f,
                                            )
                                        },
                                    shape = RoundedCornerShape(ThumbnailCornerRadius),
                                ),
                    )
                }
            },
            trailingContent = trailingContent,
            modifier = Modifier
                .offset { IntOffset(dragOffset.roundToInt(), 0) }
                .pointerInput(song.id) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            dragOffset = (dragOffset + dragAmount).coerceAtLeast(0f)
                        },
                        onDragEnd = {
                            if (dragOffset > itemWidth * dragThresholdPercent && !addedToQueue) {
                                playerConnection?.addToQueue(song.toMediaItem())
                                addedToQueue = true
                            }
                            dragOffset = 0f
                            // Reset after a short delay to allow visual feedback
                            if (addedToQueue) {
                                GlobalScope.launch {
                                    delay(600)
                                    addedToQueue = false
                                }
                            }
                        },
                        onDragCancel = {
                            dragOffset = 0f
                        }
                    )
                },
            isActive = isActive,
        )
    }
}

@Composable
fun SongSmallGridItem(
    song: Song,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) = SmallGridItem(
    title = song.song.title,
    thumbnailContent = {
        AsyncImage(
            model = song.song.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = isActive,
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(500)),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.Black.copy(alpha = if (isPlaying) 0.4f else 0f),
                            shape = RoundedCornerShape(ThumbnailCornerRadius),
                        ),
            ) {
                if (isPlaying) {
                    PlayingIndicator(
                        color = Color.White,
                        modifier = Modifier.height(24.dp),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !(isActive && isPlaying),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(8.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
            ) {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = null,
                    tint = Color.White,
                )
            }
        }
    },
    thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
    modifier = modifier,
)

@Composable
fun ArtistListItem(
    artist: Artist,
    modifier: Modifier = Modifier,
    badges: @Composable RowScope.() -> Unit = {
        if (artist.artist.bookmarkedAt != null) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
    },
    trailingContent: @Composable RowScope.() -> Unit = {},
) = ListItem(
    title = artist.artist.name,
    subtitle = pluralStringResource(R.plurals.n_song, artist.songCount, artist.songCount),
    badges = badges,
    thumbnailContent = {
        AsyncImage(
            model = artist.artist.thumbnailUrl,
            contentDescription = null,
            modifier =
                Modifier
                    .size(ListThumbnailSize)
                    .clip(CircleShape),
        )
    },
    trailingContent = trailingContent,
    modifier = modifier,
)

@Composable
fun ArtistGridItem(
    artist: Artist,
    modifier: Modifier = Modifier,
    badges: @Composable RowScope.() -> Unit = {
        if (artist.artist.bookmarkedAt != null) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
    },
    fillMaxWidth: Boolean = false,
) = GridItem(
    title = artist.artist.name,
    subtitle = pluralStringResource(R.plurals.n_song, artist.songCount, artist.songCount),
    badges = badges,
    thumbnailContent = {
        AsyncImage(
            model = artist.artist.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    },
    thumbnailShape = CircleShape,
    fillMaxWidth = fillMaxWidth,
    modifier = modifier,
)

@Composable
fun ArtistSmallGridItem(
    artist: Artist,
    modifier: Modifier = Modifier,
) = SmallGridItem(
    title = artist.artist.name,
    thumbnailContent = {
        AsyncImage(
            model = artist.artist.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    },
    thumbnailShape = CircleShape,
    modifier = modifier,
    isArtist = true,
)

@Composable
fun AlbumListItem(
    album: Album,
    modifier: Modifier = Modifier,
    showLikedIcon: Boolean = true,
    badges: @Composable RowScope.() -> Unit = {
        val database = LocalDatabase.current
        val downloadUtil = LocalDownloadUtil.current
        var songs by remember {
            mutableStateOf(emptyList<Song>())
        }

        LaunchedEffect(Unit) {
            database.albumSongs(album.id).collect {
                songs = it
            }
        }

        var downloadState by remember {
            mutableStateOf(Download.STATE_STOPPED)
        }

        LaunchedEffect(songs) {
            if (songs.isEmpty()) return@LaunchedEffect
            downloadUtil.downloads.collect { downloads ->
                downloadState =
                    if (songs.all { downloads[it.id]?.state == STATE_COMPLETED }) {
                        STATE_COMPLETED
                    } else if (songs.all {
                            downloads[it.id]?.state == STATE_QUEUED ||
                                downloads[it.id]?.state == STATE_DOWNLOADING ||
                                downloads[it.id]?.state == STATE_COMPLETED
                        }
                    ) {
                        STATE_DOWNLOADING
                    } else {
                        Download.STATE_STOPPED
                    }
            }
        }

        if (showLikedIcon && album.album.bookmarkedAt != null) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }

        when (downloadState) {
            STATE_COMPLETED ->
                Icon(
                    painter = painterResource(R.drawable.offline),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(18.dp)
                            .padding(end = 2.dp),
                )

            STATE_DOWNLOADING ->
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier =
                        Modifier
                            .size(16.dp)
                            .padding(end = 2.dp),
                )

            else -> {}
        }
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) = ListItem(
    title = album.album.title,
    subtitle =
        joinByBullet(
            album.artists.joinToString { it.name },
            pluralStringResource(R.plurals.n_song, album.album.songCount, album.album.songCount),
            album.album.year?.toString(),
        ),
    badges = badges,
    thumbnailContent = {
        val database = LocalDatabase.current
        val coroutineScope = rememberCoroutineScope()

        AsyncImage(
            model =
                ImageRequest
                    .Builder(LocalContext.current)
                    .data(album.album.thumbnailUrl)
                    .allowHardware(false)
                    .build(),
            contentDescription = null,
            onState = { state ->
                if (album.album.themeColor == null && state is AsyncImagePainter.State.Success) {
                    coroutineScope.launch(Dispatchers.IO) {
                        state.result.drawable.toBitmapOrNull()?.extractThemeColor()?.toArgb()?.let { color ->
                            database.query {
                                update(album.album.copy(themeColor = color))
                            }
                        }
                    }
                }
            },
            modifier =
                Modifier
                    .size(ListThumbnailSize)
                    .clip(RoundedCornerShape(ThumbnailCornerRadius)),
        )

        PlayingIndicatorBox(
            isActive = isActive,
            playWhenReady = isPlaying,
            modifier =
                Modifier
                    .size(ListThumbnailSize)
                    .background(
                        color = Color.Black.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(ThumbnailCornerRadius),
                    ),
        )
    },
    trailingContent = trailingContent,
    modifier = modifier,
)

@Composable
fun AlbumGridItem(
    album: Album,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope,
    badges: @Composable RowScope.() -> Unit = {
        val database = LocalDatabase.current
        val downloadUtil = LocalDownloadUtil.current
        var songs by remember {
            mutableStateOf(emptyList<Song>())
        }

        LaunchedEffect(Unit) {
            database.albumSongs(album.id).collect {
                songs = it
            }
        }

        var downloadState by remember {
            mutableStateOf(Download.STATE_STOPPED)
        }

        LaunchedEffect(songs) {
            if (songs.isEmpty()) return@LaunchedEffect
            downloadUtil.downloads.collect { downloads ->
                downloadState =
                    if (songs.all { downloads[it.id]?.state == STATE_COMPLETED }) {
                        STATE_COMPLETED
                    } else if (songs.all {
                            downloads[it.id]?.state == STATE_QUEUED ||
                                downloads[it.id]?.state == STATE_DOWNLOADING ||
                                downloads[it.id]?.state == STATE_COMPLETED
                        }
                    ) {
                        STATE_DOWNLOADING
                    } else {
                        Download.STATE_STOPPED
                    }
            }
        }

        if (album.album.bookmarkedAt != null) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }

        when (downloadState) {
            STATE_COMPLETED ->
                Icon(
                    painter = painterResource(R.drawable.offline),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(18.dp)
                            .padding(end = 2.dp),
                )

            STATE_DOWNLOADING ->
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier =
                        Modifier
                            .size(16.dp)
                            .padding(end = 2.dp),
                )

            else -> {}
        }
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
) = GridItem(
    title = album.album.title,
    subtitle = album.artists.joinToString { it.name },
    badges = badges,
    thumbnailContent = {
        AsyncImage(
            model = album.album.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = isActive,
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(500)),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(ThumbnailCornerRadius),
                        ),
            ) {
                if (isPlaying) {
                    PlayingIndicator(
                        color = Color.White,
                        modifier = Modifier.height(24.dp),
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !isActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
        ) {
            val database = LocalDatabase.current
            val playerConnection = LocalPlayerConnection.current ?: return@AnimatedVisibility

            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable {
                            coroutineScope.launch {
                                database
                                    .albumWithSongs(album.id)
                                    .first()
                                    ?.songs
                                    ?.map { it.toMediaItem() }
                                    ?.let {
                                        playerConnection.service.getAutomixAlbum(albumId = album.id)
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = album.album.title,
                                                items = it,
                                            ),
                                        )
                                    }
                            }
                        },
            ) {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = null,
                    tint = Color.White,
                )
            }
        }
    },
    thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
    fillMaxWidth = fillMaxWidth,
    modifier = modifier,
)

@Composable
fun AlbumSmallGridItem(
    song: Song,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) = song.song.albumName?.let {
    SmallGridItem(
        title = it,
        thumbnailContent = {
            AsyncImage(
                model = song.song.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            AnimatedVisibility(
                visible = isActive,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(500)),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                color = Color.Black.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(ThumbnailCornerRadius),
                            ),
                ) {
                    if (isPlaying) {
                        PlayingIndicator(
                            color = Color.White,
                            modifier = Modifier.height(24.dp),
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                }
            }
        },
        thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
        modifier = modifier,
    )
}

@Composable
fun PlaylistListItem(
    playlist: Playlist,
    modifier: Modifier = Modifier,
    trailingContent: @Composable RowScope.() -> Unit = {},
    autoPlaylist: Boolean = false,
) = ListItem(
    title = playlist.playlist.name,
    subtitle = if (autoPlaylist) "" else pluralStringResource(R.plurals.n_song, playlist.songCount, playlist.songCount),
    thumbnailContent = {
        val painter =
            when (playlist.playlist.name) {
                stringResource(R.string.liked) -> R.drawable.favorite
                stringResource(R.string.offline) -> R.drawable.offline
                else -> {
                    if (autoPlaylist) {
                        R.drawable.trending_up
                    } else {
                        R.drawable.queue_music
                    }
                }
            }
        when (playlist.thumbnails.size) {
            0 ->
                Box(modifier = Modifier.size(ListThumbnailSize)) {
                    Icon(
                        painter = painterResource(painter),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp).align(Alignment.Center),
                    )
                }

            1 ->
                AsyncImage(
                    model = playlist.thumbnails[0],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(ListThumbnailSize)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                )

            else ->
                Box(
                    modifier =
                        Modifier
                            .size(ListThumbnailSize)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                ) {
                    listOf(
                        Alignment.TopStart,
                        Alignment.TopEnd,
                        Alignment.BottomStart,
                        Alignment.BottomEnd,
                    ).fastForEachIndexed { index, alignment ->
                        AsyncImage(
                            model = playlist.thumbnails.getOrNull(index),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier =
                                Modifier
                                    .align(alignment)
                                    .size(ListThumbnailSize / 2),
                        )
                    }
                }
        }
    },
    trailingContent = trailingContent,
    modifier = modifier,
)

@Composable
fun PlaylistGridItem(
    playlist: Playlist,
    modifier: Modifier = Modifier,
    badges: @Composable RowScope.() -> Unit = { },
    fillMaxWidth: Boolean = false,
    autoPlaylist: Boolean = false,
) = GridItem(
    title = playlist.playlist.name,
    subtitle = if (autoPlaylist) "" else pluralStringResource(R.plurals.n_song, playlist.songCount, playlist.songCount),
    badges = badges,
    thumbnailContent = {
        val painter =
            when (playlist.playlist.name) {
                stringResource(R.string.liked) -> R.drawable.favorite
                stringResource(R.string.offline) -> R.drawable.offline
                else -> {
                    if (autoPlaylist) {
                        R.drawable.trending_up
                    } else {
                        R.drawable.queue_music
                    }
                }
            }
        val width = maxWidth
        when (playlist.thumbnails.size) {
            0 ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(color = MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Icon(
                        painter = painterResource(painter),
                        contentDescription = null,
                        tint = LocalContentColor.current.copy(alpha = 0.8f),
                        modifier =
                            Modifier
                                .size(48.dp)
                                .align(Alignment.Center),
                    )
                }

            1 ->
                AsyncImage(
                    model = playlist.thumbnails[0],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(width)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                )

            else ->
                Box(
                    modifier =
                        Modifier
                            .size(width)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                ) {
                    listOf(
                        Alignment.TopStart,
                        Alignment.TopEnd,
                        Alignment.BottomStart,
                        Alignment.BottomEnd,
                    ).fastForEachIndexed { index, alignment ->
                        AsyncImage(
                            model = playlist.thumbnails.getOrNull(index),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier =
                                Modifier
                                    .align(alignment)
                                    .size(width / 2),
                        )
                    }
                }
        }
    },
    thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
    fillMaxWidth = fillMaxWidth,
    modifier = modifier,
)

@Composable
fun MediaMetadataListItem(
    mediaMetadata: MediaMetadata,
    modifier: Modifier,
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) = ListItem(
    title = mediaMetadata.title,
    subtitle =
        joinByBullet(
            mediaMetadata.artists.joinToString { it.name },
            makeTimeString(mediaMetadata.duration * 1000L),
        ),
    thumbnailContent = {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(ListThumbnailSize),
        ) {
            if (isSelected) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .zIndex(1000f)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius))
                            .background(Color.Black.copy(alpha = 0.5f)),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.done),
                        modifier = Modifier.align(Alignment.Center),
                        contentDescription = null,
                    )
                }
            }

            AsyncImage(
                model = mediaMetadata.thumbnailUrl,
                contentDescription = null,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(ThumbnailCornerRadius)),
            )

            PlayingIndicatorBox(
                isActive = isActive,
                playWhenReady = isPlaying,
                color = Color.White,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(ThumbnailCornerRadius),
                        ),
            )
        }
    },
    trailingContent = trailingContent,
    modifier = modifier,
    isActive = isActive,
)

@Composable
fun YouTubeListItem(
    item: YTItem,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    isSelected: Boolean = false,
    badges: @Composable RowScope.() -> Unit = {
        val database = LocalDatabase.current
        val song by database.song(item.id).collectAsState(initial = null)
        val album by database.album(item.id).collectAsState(initial = null)

        if (item is SongItem &&
            song?.song?.liked == true ||
            item is AlbumItem &&
            album?.album?.bookmarkedAt != null
        ) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
        if (item.explicit) {
            Icon(
                painter = painterResource(R.drawable.explicit),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
        if (item is SongItem && song?.song?.inLibrary != null) {
            Icon(
                painter = painterResource(R.drawable.library_add_check),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
        if (item is SongItem) {
            val downloads by LocalDownloadUtil.current.downloads.collectAsState()
            when (downloads[item.id]?.state) {
                STATE_COMPLETED ->
                    Icon(
                        painter = painterResource(R.drawable.offline),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .size(18.dp)
                                .padding(end = 2.dp),
                    )

                STATE_QUEUED, STATE_DOWNLOADING ->
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier =
                            Modifier
                                .size(16.dp)
                                .padding(end = 2.dp),
                    )

                else -> {}
            }
        }
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val playerConnection = LocalPlayerConnection.current
    var dragOffset by remember { mutableStateOf(0f) }
    var itemWidth by remember { mutableStateOf(1f) } // avoid div by zero
    var addedToQueue by remember { mutableStateOf(false) }
    val dragThresholdPercent = 0.5f

    val dragModifier = if (item is SongItem) {
        Modifier
            .onGloballyPositioned { coordinates ->
                itemWidth = coordinates.size.width.toFloat()
            }
            .offset { IntOffset(dragOffset.roundToInt(), 0) }
            .pointerInput(item.id) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        dragOffset = (dragOffset + dragAmount).coerceAtLeast(0f)
                    },
                    onDragEnd = {
                        if (dragOffset > itemWidth * dragThresholdPercent && !addedToQueue) {
                            playerConnection?.addToQueue(item.toMediaItem())
                            addedToQueue = true
                        }
                        dragOffset = 0f
                        // Reset after a short delay to allow visual feedback
                        if (addedToQueue) {
                            GlobalScope.launch {
                                delay(600)
                                addedToQueue = false
                            }
                        }
                    },
                    onDragCancel = {
                        dragOffset = 0f
                    }
                )
            }
    } else {
        Modifier
    }

    Box(
        modifier = modifier.then(dragModifier)
            .background(Color.Transparent)
    ) {
        // Queue icon background (shows during drag)
        if (item is SongItem && dragOffset > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize(),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    painter = painterResource(R.drawable.queue_music),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    modifier = Modifier.size(48.dp).padding(start = 24.dp)
                )
            }
        }
        // Persistent queue icon at bottom left for SongItem
        if (item is SongItem) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.BottomStart
            ) {
                Icon(
                    painter = painterResource(R.drawable.queue_music),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    modifier = Modifier.size(32.dp).padding(start = 16.dp, bottom = 8.dp)
                )
            }
        }
        // Draggable/regular item
        ListItem(
            title = item.title,
            subtitle =
                when (item) {
                    is SongItem -> joinByBullet(item.artists.joinToString { it.name }, makeTimeString(item.duration?.times(1000L)))
                    is AlbumItem -> joinByBullet(item.artists?.joinToString { it.name }, item.year?.toString())
                    is ArtistItem -> null
                    is PlaylistItem -> joinByBullet(item.author?.name, item.songCountText)
                },
            badges = badges,
            thumbnailContent = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(ListThumbnailSize),
                ) {
                    val thumbnailShape = if (item is ArtistItem) CircleShape else RoundedCornerShape(ThumbnailCornerRadius)
                    if (albumIndex != null) {
                        AnimatedVisibility(
                            visible = !isActive,
                            enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                            exit = shrinkOut(shrinkTowards = Alignment.Center) + fadeOut(),
                        ) {
                            if (isSelected) {
                                Icon(
                                    painter = painterResource(R.drawable.done),
                                    modifier = Modifier.align(Alignment.Center),
                                    contentDescription = null,
                                )
                            } else {
                                Text(
                                    text = albumIndex.toString(),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    } else {
                        if (isSelected) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .zIndex(1000f)
                                        .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                        .background(Color.Black.copy(alpha = 0.5f)),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.done),
                                    modifier = Modifier.align(Alignment.Center),
                                    contentDescription = null,
                                )
                            }
                        }
                        AsyncImage(
                            model = item.thumbnail,
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .clip(thumbnailShape),
                        )
                    }

                    PlayingIndicatorBox(
                        isActive = isActive,
                        playWhenReady = isPlaying,
                        color = if (albumIndex != null) MaterialTheme.colorScheme.onBackground else Color.White,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(
                                    color =
                                        if (albumIndex != null) {
                                            Color.Transparent
                                        } else {
                                            Color.Black.copy(
                                                alpha = 0.4f,
                                            )
                                        },
                                    shape = thumbnailShape,
                                ),
                    )
                }
            },
            trailingContent = trailingContent,
            modifier = Modifier,
            isActive = isActive,
        )
    }
}

@Composable
fun YouTubeGridItem(
    item: YTItem,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope? = null,
    badges: @Composable RowScope.() -> Unit = {
        val database = LocalDatabase.current
        val song by database.song(item.id).collectAsState(initial = null)
        val album by database.album(item.id).collectAsState(initial = null)

        if (item is SongItem &&
            song?.song?.liked == true ||
            item is AlbumItem &&
            album?.album?.bookmarkedAt != null
        ) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
        if (item is SongItem && song?.song?.inLibrary != null) {
            Icon(
                painter = painterResource(R.drawable.library_add_check),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
        if (item.explicit) {
            Icon(
                painter = painterResource(R.drawable.explicit),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
        if (item is SongItem) {
            val downloads by LocalDownloadUtil.current.downloads.collectAsState()
            when (downloads[item.id]?.state) {
                STATE_COMPLETED ->
                    Icon(
                        painter = painterResource(R.drawable.offline),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .size(18.dp)
                                .padding(end = 2.dp),
                    )

                STATE_DOWNLOADING ->
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier =
                            Modifier
                                .size(16.dp)
                                .padding(end = 2.dp),
                    )

                else -> {}
            }
        }
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
) {
    val thumbnailShape = if (item is ArtistItem) CircleShape else RoundedCornerShape(ThumbnailCornerRadius)
    val thumbnailRatio = if (item is SongItem) 16f / 9 else 1f

    Column(
        modifier =
            if (fillMaxWidth) {
                modifier
                    .padding(12.dp)
                    .fillMaxWidth()
            } else {
                modifier
                    .padding(12.dp)
                    .width(GridThumbnailHeight * thumbnailRatio)
            },
    ) {
        Box(
            modifier =
                if (fillMaxWidth) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.height(GridThumbnailHeight)
                }.aspectRatio(thumbnailRatio)
                    .clip(thumbnailShape),
        ) {
            AsyncImage(
                model = item.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = isActive,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(500)),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                color = Color.Black.copy(alpha = 0.4f),
                                shape = thumbnailShape,
                            ),
                ) {
                    if (isPlaying) {
                        PlayingIndicator(
                            color = Color.White,
                            modifier = Modifier.height(24.dp),
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = item is AlbumItem && !isActive,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
            ) {
                val database = LocalDatabase.current
                val playerConnection = LocalPlayerConnection.current ?: return@AnimatedVisibility

                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable {
                                var playlistId = ""
                                coroutineScope?.launch(Dispatchers.IO) {
                                    var songs =
                                        database
                                            .albumWithSongs(item.id)
                                            .first()
                                            ?.songs
                                            ?.map { it.toMediaItem() }
                                    if (songs == null) {
                                        YouTube
                                            .album(item.id)
                                            .onSuccess { albumPage ->
                                                playlistId = albumPage.album.playlistId
                                                database.transaction {
                                                    insert(albumPage)
                                                }
                                                songs = albumPage.songs.map { it.toMediaItem() }
                                            }.onFailure {
                                                reportException(it)
                                            }
                                    }
                                    songs?.let {
                                        withContext(Dispatchers.Main) {
                                            playerConnection.service.getAutomix(playlistId)
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = item.title,
                                                    items = it,
                                                ),
                                            )
                                        }
                                    }
                                }
                            },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (item is ArtistItem) TextAlign.Center else TextAlign.Start,
            modifier =
                Modifier
                    .basicMarquee(iterations = 3)
                    .fillMaxWidth(),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            badges()

            val subtitle =
                when (item) {
                    is SongItem -> joinByBullet(item.artists.joinToString { it.name }, makeTimeString(item.duration?.times(1000L)))
                    is AlbumItem -> joinByBullet(item.artists?.joinToString { it.name }, item.year?.toString())
                    is ArtistItem -> null
                    is PlaylistItem -> joinByBullet(item.author?.name, item.songCountText)
                }

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun YouTubeSmallGridItem(
    item: YTItem,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope? = null,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
) = SmallGridItem(
    title = item.title,
    thumbnailContent = {
        AsyncImage(
            model = item.thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth(),
        )
        if (item is SongItem) {
            AnimatedVisibility(
                visible = isActive,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(500)),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                color = Color.Black.copy(alpha = if (isPlaying) 0.4f else 0f),
                                shape = RoundedCornerShape(ThumbnailCornerRadius),
                            ),
                ) {
                    if (isPlaying) {
                        PlayingIndicator(
                            color = Color.White,
                            modifier = Modifier.height(24.dp),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !(isActive && isPlaying),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .padding(8.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }
        }
    },
    thumbnailShape =
        when (item) {
            is ArtistItem -> CircleShape
            else -> RoundedCornerShape(ThumbnailCornerRadius)
        },
    modifier = modifier,
    isArtist =
        when (item) {
            is ArtistItem -> true
            else -> false
        },
)

@Composable
fun LocalItemsGrid(
    title: String,
    subtitle: String,
    badges:
        @Composable()
        (RowScope.() -> Unit) = {},
    thumbnailUrl: String?,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    modifier: Modifier,
) = GridItem(
    title = title,
    subtitle = subtitle,
    badges = badges,
    thumbnailContent = {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = isActive,
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(500)),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(ThumbnailCornerRadius),
                        ),
            ) {
                if (isPlaying) {
                    PlayingIndicator(
                        color = Color.White,
                        modifier = Modifier.height(24.dp),
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }
        }
    },
    thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
    fillMaxWidth = fillMaxWidth,
    modifier = modifier,
)
