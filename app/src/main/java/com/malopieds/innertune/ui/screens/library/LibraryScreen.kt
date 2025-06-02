package com.malopieds.innertune.ui.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.malopieds.innertune.R
import com.malopieds.innertune.constants.ChipSortTypeKey
import com.malopieds.innertune.constants.LibraryFilter
import com.malopieds.innertune.ui.component.ChipsRow
import com.malopieds.innertune.ui.component.NavigationTile
import com.malopieds.innertune.utils.rememberEnumPreference

@Composable
fun LibraryScreen(navController: NavController) {
    var filterType by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)

    val filterContent = @Composable {
        Row {
            ChipsRow(
                chips =
                    listOf(
                        LibraryFilter.PLAYLISTS to stringResource(R.string.filter_playlists),
                        LibraryFilter.SONGS to stringResource(R.string.filter_songs),
                        LibraryFilter.ALBUMS to stringResource(R.string.filter_albums),
                        LibraryFilter.ARTISTS to stringResource(R.string.filter_artists),
                    ),
                currentValue = filterType,
                onValueUpdate = {
                    filterType =
                        if (filterType == it) {
                            LibraryFilter.LIBRARY
                        } else {
                            it
                        }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        when (filterType) {
            LibraryFilter.LIBRARY -> LibraryMixScreen(navController = navController, filterContent = filterContent)
            LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(navController = navController, filterContent = filterContent)
            LibraryFilter.SONGS -> LibrarySongsScreen(navController = navController, onDeselect = { filterType = LibraryFilter.LIBRARY })
            LibraryFilter.ALBUMS -> LibraryAlbumsScreen(navController = navController, onDeselect = { filterType = LibraryFilter.LIBRARY })
            LibraryFilter.ARTISTS -> LibraryArtistsScreen(navController = navController, onDeselect = { filterType = LibraryFilter.LIBRARY })
        }
    }
}
