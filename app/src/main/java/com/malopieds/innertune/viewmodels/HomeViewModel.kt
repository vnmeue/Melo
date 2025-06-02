package com.malopieds.innertune.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malopieds.innertube.YouTube
import com.malopieds.innertube.models.PlaylistItem
import com.malopieds.innertube.models.WatchEndpoint
import com.malopieds.innertube.models.YTItem
import com.malopieds.innertube.pages.AlbumUtils
import com.malopieds.innertube.pages.ExplorePage
import com.malopieds.innertube.pages.HomeAlbumRecommendation
import com.malopieds.innertube.pages.HomeArtistRecommendation
import com.malopieds.innertube.pages.HomePlayList
import com.malopieds.innertune.constants.QuickPicks
import com.malopieds.innertune.constants.QuickPicksKey
import com.malopieds.innertune.db.MusicDatabase
import com.malopieds.innertune.db.entities.Artist
import com.malopieds.innertune.db.entities.Song
import com.malopieds.innertune.extensions.toEnum
import com.malopieds.innertune.utils.dataStore
import com.malopieds.innertune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        @ApplicationContext context: Context,
        val database: MusicDatabase,
    ) : ViewModel() {
        val isRefreshing = MutableStateFlow(false)
        private val quickPicksEnum =
            context.dataStore.data
                .map {
                    it[QuickPicksKey].toEnum(QuickPicks.QUICK_PICKS)
                }.distinctUntilChanged()

        private val _quickPicks = MutableStateFlow<List<Song>?>(null)
        val quickPicks: StateFlow<List<Song>?> = _quickPicks.asStateFlow()

        private val _explorePage = MutableStateFlow<ExplorePage?>(null)
        val explorePage: StateFlow<ExplorePage?> = _explorePage.asStateFlow()

        private val _forgottenFavorite = MutableStateFlow<List<Song>?>(null)
        val forgottenFavorite: StateFlow<List<Song>?> = _forgottenFavorite.asStateFlow()

        private val _home = MutableStateFlow<List<HomePlayList>?>(null)
        val home: StateFlow<List<HomePlayList>?> = _home.asStateFlow()

        private val _keepListeningSongs = MutableStateFlow<List<Song>?>(null)
        val keepListeningSongs: StateFlow<List<Song>?> = _keepListeningSongs.asStateFlow()

        private val _keepListeningAlbums = MutableStateFlow<List<Song>?>(null)
        val keepListeningAlbums: StateFlow<List<Song>?> = _keepListeningAlbums.asStateFlow()

        private val _keepListeningArtists = MutableStateFlow<List<Artist>?>(null)
        val keepListeningArtists: StateFlow<List<Artist>?> = _keepListeningArtists.asStateFlow()

        private val _keepListening = MutableStateFlow<List<Int>?>(null)
        val keepListening: StateFlow<List<Int>?> = _keepListening.asStateFlow()

        private val _continuation = MutableStateFlow<String?>(null)
        val continuation: StateFlow<String?> = _continuation.asStateFlow()

        private val _homeFirstContinuation = MutableStateFlow<List<HomePlayList>?>(null)
        val homeFirstContinuation: StateFlow<List<HomePlayList>?> = _homeFirstContinuation.asStateFlow()

        private val _homeSecondContinuation = MutableStateFlow<List<HomePlayList>?>(null)
        val homeSecondContinuation: StateFlow<List<HomePlayList>?> = _homeSecondContinuation.asStateFlow()

        private val _homeThirdContinuation = MutableStateFlow<List<HomePlayList>?>(null)
        val homeThirdContinuation: StateFlow<List<HomePlayList>?> = _homeThirdContinuation.asStateFlow()

        private val _songsAlbumRecommendation = MutableStateFlow<List<Song>?>(null)
        val songsAlbumRecommendation: StateFlow<List<Song>?> = _songsAlbumRecommendation.asStateFlow()

        private val _homeFirstAlbumRecommendation = MutableStateFlow<HomeAlbumRecommendation?>(null)
        val homeFirstAlbumRecommendation: StateFlow<HomeAlbumRecommendation?> = _homeFirstAlbumRecommendation.asStateFlow()

        private val _homeSecondAlbumRecommendation = MutableStateFlow<HomeAlbumRecommendation?>(null)
        val homeSecondAlbumRecommendation: StateFlow<HomeAlbumRecommendation?> = _homeSecondAlbumRecommendation.asStateFlow()

        private val _artistRecommendation = MutableStateFlow<List<Artist>?>(null)
        val artistRecommendation: StateFlow<List<Artist>?> = _artistRecommendation.asStateFlow()

        private val _homeFirstArtistRecommendation = MutableStateFlow<HomeArtistRecommendation?>(null)
        val homeFirstArtistRecommendation: StateFlow<HomeArtistRecommendation?> = _homeFirstArtistRecommendation.asStateFlow()

        private val _homeSecondArtistRecommendation = MutableStateFlow<HomeArtistRecommendation?>(null)
        val homeSecondArtistRecommendation: StateFlow<HomeArtistRecommendation?> = _homeSecondArtistRecommendation.asStateFlow()

        private val _homeThirdArtistRecommendation = MutableStateFlow<HomeArtistRecommendation?>(null)
        val homeThirdArtistRecommendation: StateFlow<HomeArtistRecommendation?> = _homeThirdArtistRecommendation.asStateFlow()

        private val _youtubePlaylists = MutableStateFlow<List<PlaylistItem>?>(null)
        val youtubePlaylists: StateFlow<List<PlaylistItem>?> = _youtubePlaylists.asStateFlow()

        private val _exception = MutableStateFlow<Throwable?>(null)
        val exception: StateFlow<Throwable?> = _exception.asStateFlow()

        private suspend fun getQuickPicks() {
            when (quickPicksEnum.first()) {
                QuickPicks.QUICK_PICKS ->
                    _quickPicks.value =
                        database
                            .quickPicks()
                            .first()
                            .shuffled()
                            .take(20)
                QuickPicks.LAST_LISTEN -> songLoad()
            }
        }

        private suspend fun load() {
            getQuickPicks()
            val artists =
                database
                    .mostPlayedArtists(System.currentTimeMillis() - 86400000 * 7 * 2)
                    .first()
                    .shuffled()
                    .take(5)
            val filteredArtists = mutableListOf<Artist>()
            artists.forEach {
                if (it.artist.isYouTubeArtist) {
                    filteredArtists.add(it)
                }
            }
            _keepListeningArtists.value = filteredArtists
            _keepListeningAlbums.value =
                database
                    .getRecommendationAlbum(limit = 8, offset = 2)
                    .first()
                    .shuffled()
                    .take(5)
            _keepListeningSongs.value =
                database
                    .mostPlayedSongs(System.currentTimeMillis() - 86400000 * 7 * 2, limit = 15, offset = 5)
                    .first()
                    .shuffled()
                    .take(10)
            val listenAgainBuilder = mutableListOf<Int>()
            var index = 0
            _keepListeningArtists.value?.forEach { _ ->
                listenAgainBuilder.add(index)
                index += 1
            }
            index = 5
            _keepListeningAlbums.value?.forEach { _ ->
                listenAgainBuilder.add(index)
                index += 1
            }
            index = 10
            _keepListeningSongs.value?.forEach { _ ->
                listenAgainBuilder.add(index)
                index += 1
            }
            _keepListening.value = listenAgainBuilder.shuffled()
            _songsAlbumRecommendation.value =
                database
                    .getRecommendationAlbum(limit = 10)
                    .first()
                    .shuffled()
                    .take(2)

            _artistRecommendation.value =
                database
                    .mostPlayedArtists(System.currentTimeMillis() - 86400000 * 7, limit = 10)
                    .first()
                    .shuffled()
                    .take(3)

            viewModelScope.launch {
                YouTube
                    .likedPlaylists()
                    .onSuccess {
                        _youtubePlaylists.value = it
                    }.onFailure {
                        _exception.value = it
                    }
            }
        }

        private suspend fun homeLoad() {
            YouTube
                .home()
                .onSuccess { res ->
                    res.getOrNull(1)?.continuation?.let {
                        _continuation.value = it
                    }
                    _home.value = res
                }.onFailure {
                    _exception.value = it
                }
            continuationsLoad()
        }

        private suspend fun continuation(
            continuationVal: String?,
            next: MutableStateFlow<List<HomePlayList>?>,
        ) {
            continuationVal?.run {
                YouTube
                    .browseContinuation(this)
                    .onSuccess { res ->
                        res.firstOrNull()?.continuation?.let {
                            _continuation.value = it
                        }
                        next.value = res
                    }.onFailure {
                        _exception.value = it
                    }
            }
        }

        private suspend fun continuationsLoad() {
            artistLoad(_artistRecommendation.value?.getOrNull(0), _homeFirstArtistRecommendation)
            _forgottenFavorite.value =
                database
                    .forgottenFavorites()
                    .first()
                    .shuffled()
                    .take(20)
            _continuation.value?.run {
                continuation(this, _homeFirstContinuation)
            }
            albumLoad(_songsAlbumRecommendation.value?.getOrNull(0), _homeFirstAlbumRecommendation)

            _continuation.value?.run {
                continuation(this, _homeSecondContinuation)
            }
            artistLoad(_artistRecommendation.value?.getOrNull(1), _homeSecondArtistRecommendation)

            _continuation.value?.run {
                continuation(this, _homeThirdContinuation)
            }
            albumLoad(_songsAlbumRecommendation.value?.getOrNull(1), _homeSecondAlbumRecommendation)

            artistLoad(_artistRecommendation.value?.getOrNull(2), _homeThirdArtistRecommendation)
        }

        private suspend fun songLoad() {
            val song =
                database
                    .events()
                    .first()
                    .firstOrNull()
                    ?.song
            if (song != null) {
                if (database.hasRelatedSongs(song.id)) {
                    val relatedSongs =
                        database
                            .getRelatedSongs(song.id)
                            .first()
                            .shuffled()
                            .take(20)
                    _quickPicks.value = relatedSongs
                }
            }
        }

        private suspend fun albumLoad(
            song: Song?,
            next: MutableStateFlow<HomeAlbumRecommendation?>,
        ) {
            val albumUtils = AlbumUtils(song?.song?.albumName, song?.song?.thumbnailUrl)
            YouTube.next(WatchEndpoint(videoId = song?.id)).onSuccess { res ->
                YouTube
                    .recommendAlbum(res.relatedEndpoint!!.browseId, albumUtils)
                    .onSuccess { page ->
                        next.value =
                            page.copy(
                                albums = page.albums,
                            )
                    }.onFailure {
                        _exception.value = it
                    }
            }
        }

        private suspend fun artistLoad(
            artist: Artist?,
            next: MutableStateFlow<HomeArtistRecommendation?>,
        ) {
            val listItem = mutableListOf<YTItem>()
            artist?.id?.let {
                YouTube.artist(it).onSuccess { res ->
                    res.sections.getOrNull(res.sections.size - 2)?.items?.forEach { item ->
                        listItem.add(item)
                    }
                    res.sections.lastOrNull()?.items?.forEach { item ->
                        listItem.add(item)
                    }
                }
            }
            if (artist != null) {
                next.value =
                    HomeArtistRecommendation(
                        listItem = listItem.shuffled().take(9),
                        artistName = artist.artist.name,
                    )
            }
        }

        fun refresh() {
            if (isRefreshing.value) return
            viewModelScope.launch(Dispatchers.IO) {
                isRefreshing.value = true
                load()
                isRefreshing.value = false
            }
            viewModelScope.launch(Dispatchers.IO) {
                homeLoad()
            }
        }

        init {
            viewModelScope.launch(Dispatchers.IO) {
                val mostPlayedArtists = database.mostPlayedArtists(System.currentTimeMillis() - 86400000 * 7 * 2)
                viewModelScope.launch {
                    mostPlayedArtists.collect { artists ->
                        artists
                            .map { it.artist }
                            .filter {
                                it.thumbnailUrl == null
                            }.forEach { artist ->
                                YouTube.artist(artist.id).onSuccess { artistPage ->
                                    database.query {
                                        update(artist, artistPage)
                                    }
                                }
                            }
                    }
                }
            }
            viewModelScope.launch(Dispatchers.IO) {
                isRefreshing.value = true
                load()
                isRefreshing.value = false
            }
            viewModelScope.launch(Dispatchers.IO) {
                homeLoad()
            }
        }
    }
