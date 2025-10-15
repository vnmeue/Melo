package com.malopieds.innertune.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.malopieds.innertube.YouTube.SearchFilter.Companion.FILTER_ALBUM
import com.malopieds.innertube.YouTube.SearchFilter.Companion.FILTER_ARTIST
import com.malopieds.innertube.YouTube.SearchFilter.Companion.FILTER_COMMUNITY_PLAYLIST
import com.malopieds.innertube.YouTube.SearchFilter.Companion.FILTER_FEATURED_PLAYLIST
import com.malopieds.innertube.YouTube.SearchFilter.Companion.FILTER_SONG
import com.malopieds.innertube.YouTube.SearchFilter.Companion.FILTER_VIDEO
import com.malopieds.innertube.models.AlbumItem
import com.malopieds.innertube.models.ArtistItem
import com.malopieds.innertube.models.PlaylistItem
import com.malopieds.innertube.models.SongItem
import com.malopieds.innertube.models.WatchEndpoint
import com.malopieds.innertube.models.YTItem
import com.malopieds.innertune.LocalPlayerAwareWindowInsets
import com.malopieds.innertune.LocalPlayerConnection
import com.malopieds.innertune.R
import com.malopieds.innertune.constants.AppBarHeight
import com.malopieds.innertune.constants.SearchFilterHeight
import com.malopieds.innertune.extensions.togglePlayPause
import com.malopieds.innertune.models.toMediaMetadata
import com.malopieds.innertune.playback.queues.YouTubeQueue
import com.malopieds.innertune.ui.component.ChipsRow
import com.malopieds.innertune.ui.component.EmptyPlaceholder
import com.malopieds.innertune.ui.component.LocalMenuState
import com.malopieds.innertune.ui.component.NavigationTitle
import com.malopieds.innertune.ui.component.YouTubeListItem
import com.malopieds.innertune.ui.component.shimmer.ListItemPlaceHolder
import com.malopieds.innertune.ui.component.shimmer.ShimmerHost
import com.malopieds.innertune.ui.menu.YouTubeAlbumMenu
import com.malopieds.innertune.ui.menu.YouTubeArtistMenu
import com.malopieds.innertune.ui.menu.YouTubePlaylistMenu
import com.malopieds.innertune.ui.menu.YouTubeSongMenu
import com.malopieds.innertune.viewmodels.OnlineSearchViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnlineSearchResult(
    navController: NavController,
    viewModel: OnlineSearchViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current
    val haptic = LocalHapticFeedback.current
    val isPlaying by (playerConnection?.isPlaying ?: flowOf(false)).collectAsState(initial = false)
    val mediaMetadata by (playerConnection?.mediaMetadata ?: flowOf(null)).collectAsState(initial = null)

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val searchFilter by viewModel.filter.collectAsState()
    val searchSummary = viewModel.summaryPage
    val itemsPage by remember(searchFilter) {
        derivedStateOf {
            searchFilter?.value?.let {
                viewModel.viewStateMap[it]
            }
        }
    }

    val songCount = remember(searchSummary) {
        if (searchSummary == null) 0 else searchSummary.summaries.filter { it.title.contains("song", ignoreCase = true) }.sumOf { it.items.size }
    }
    val videoCount = remember(searchSummary) {
        if (searchSummary == null) 0 else searchSummary.summaries.filter { it.title.contains("video", ignoreCase = true) }.sumOf { it.items.size }
    }
    val albumCount = remember(searchSummary) {
        if (searchSummary == null) 0 else searchSummary.summaries.filter { it.title.contains("album", ignoreCase = true) }.sumOf { it.items.size }
    }
    val artistCount = remember(searchSummary) {
        if (searchSummary == null) 0 else searchSummary.summaries.filter { it.title.contains("artist", ignoreCase = true) }.sumOf { it.items.size }
    }
    val playlistCount = remember(searchSummary) {
        if (searchSummary == null) 0 else searchSummary.summaries.filter { it.title.contains("playlist", ignoreCase = true) }.sumOf { it.items.size }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            lazyListState.layoutInfo.visibleItemsInfo.any { it.key == "loading" }
        }.collect { shouldLoadMore ->
            if (!shouldLoadMore) return@collect
            viewModel.loadMore()
        }
    }

    val ytItemContent: @Composable LazyItemScope.(YTItem) -> Unit = { item: YTItem ->
        val longClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            menuState.show {
                when (item) {
                    is SongItem ->
                        YouTubeSongMenu(
                            song = item,
                            navController = navController,
                            onDismiss = menuState::dismiss,
                        )

                    is AlbumItem ->
                        YouTubeAlbumMenu(
                            albumItem = item,
                            navController = navController,
                            onDismiss = menuState::dismiss,
                        )

                    is ArtistItem ->
                        YouTubeArtistMenu(
                            artist = item,
                            onDismiss = menuState::dismiss,
                        )

                    is PlaylistItem ->
                        YouTubePlaylistMenu(
                            playlist = item,
                            coroutineScope = coroutineScope,
                            onDismiss = menuState::dismiss,
                        )
                }
            }
        }
        YouTubeListItem(
            item = item,
            isActive =
                when (item) {
                    is SongItem -> mediaMetadata?.id == item.id
                    is AlbumItem -> mediaMetadata?.album?.id == item.id
                    else -> false
                },
            isPlaying = isPlaying,
            trailingContent = {
                IconButton(
                    onClick = longClick,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = null,
                    )
                }
            },
            modifier =
                Modifier
                    .combinedClickable(
                        onClick = {
            when (item) {
                                is SongItem -> {
                    if (playerConnection != null) {
                        if (item.id == mediaMetadata?.id) {
                            playerConnection.player.togglePlayPause()
                        } else {
                            playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id), item.toMediaMetadata()))
                        }
                    }
                                }

                                is AlbumItem -> navController.navigate("album/${item.id}")
                                is ArtistItem -> navController.navigate("artist/${item.id}")
                                is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                            }
                        },
                        onLongClick = longClick,
                    ).animateItemPlacement(),
        )
    }

    LazyColumn(
        state = lazyListState,
        contentPadding =
            LocalPlayerAwareWindowInsets.current
                .add(WindowInsets(top = 0))
                .asPaddingValues(),
    ) {
        if (searchFilter == null) {
            // When no filter, fall back to showing only songs
            items(
                items = searchSummary?.summaries?.flatMap { it.items }?.filterIsInstance<SongItem>().orEmpty(),
                key = { it.id },
                itemContent = ytItemContent,
            )
        } else {
            items(
                items = itemsPage?.items.orEmpty(),
                key = { it.id },
                itemContent = ytItemContent,
            )

            if (itemsPage?.continuation != null) {
                item(key = "loading") {
                    ShimmerHost {
                        repeat(3) {
                            ListItemPlaceHolder()
                        }
                    }
                }
            }

            if (itemsPage?.items?.isEmpty() == true) {
                item {
                    EmptyPlaceholder(
                        icon = R.drawable.search,
                        text = stringResource(R.string.no_results_found),
                    )
                }
            }
        }

        if (searchFilter == null && searchSummary == null || searchFilter != null && itemsPage == null) {
            item {
                ShimmerHost {
                    repeat(8) {
                        ListItemPlaceHolder()
                    }
                }
            }
        }
    }

    // Removed top chips row to show songs-only view after search
}
