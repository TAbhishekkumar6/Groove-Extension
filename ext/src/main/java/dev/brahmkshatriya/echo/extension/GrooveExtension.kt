package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.*
import dev.brahmkshatriya.echo.common.helpers.Page
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.*
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeedData
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.common.models.NetworkRequest.Companion.toGetRequest
import dev.brahmkshatriya.echo.common.providers.MessageFlowProvider
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class GrooveExtension : ExtensionClient,
    QuickSearchClient, SearchFeedClient, HomeFeedClient,
    TrackClient, AlbumClient, ArtistClient, RadioClient, LibraryFeedClient, MessageFlowProvider {

    private val api by lazy { GrooveApi() }
    private val parser by lazy { GrooveParser() }
    
    private lateinit var settings: Settings
    private lateinit var messageFlow: MutableSharedFlow<Message>
    
    override suspend fun getSettingItems(): List<Setting> {
        return emptyList()
    }
    
    override fun setSettings(settings: Settings) {
        this.settings = settings
    }

     override fun setMessageFlow(messageFlow: MutableSharedFlow<Message>) {
        this.messageFlow = messageFlow
        
        //notification
        CoroutineScope(Dispatchers.Main).launch {
            messageFlow.emit(Message(
                message = "403 Error Means Source Dead / Content Removed Try Different Quality Options"
            ))
        }
    }

    // HOME FEED
    
    override suspend fun loadHomeFeed(): Feed<Shelf> {
        val tabs = listOf(
            Tab(id = "home", title = "Home"),
            Tab(id = "english", title = "English"),
            Tab(id = "haryanvi", title = "Haryanvi"),
            Tab(id = "bollywood", title = "Bollywood"),
            Tab(id = "punjabi", title = "Punjabi"),
            Tab(id = "indipop", title = "Indipop"),
            Tab(id = "tamil", title = "Tamil")
        )
        
        return Feed(tabs) { tab ->
            try {
                when (tab?.id) {
                    "home" -> loadHomeTab()
                    "english" -> loadCategoryTab("/category/english-mp3-tracks", "English")
                    "haryanvi" -> loadCategoryTab("/category/haryanvi-mp3-tracks", "Haryanvi")
                    "bollywood" -> loadCategoryTab("/category/bollywood-tracks", "Bollywood")
                    "punjabi" -> loadCategoryTab("/category/punjabi-mp3-tracks", "Punjabi")
                    "indipop" -> loadCategoryTab("/category/indipop-mp3-tracks", "Indipop")
                    "tamil" -> loadCategoryTab("/category/tamil-mp3-tracks", "Tamil")
                    else -> loadHomeTab()
                }
            } catch (e: Exception) {
                println("DEBUG: Failed to load home feed tab ${tab?.id}: ${e.message}")
                e.printStackTrace()
                emptyList<Shelf>().toFeedData()
            }
        }
    }
    
    private suspend fun loadHomeTab(): Feed.Data<Shelf> {
        val html = api.getHomePage()
        val homeContent = parser.parseHomeContent(html)
        
        val shelves = mutableListOf<Shelf>()
        
        if (homeContent.latestSongs.isNotEmpty()) {
            shelves.add(Shelf.Lists.Items(
                id = "latest_songs",
                title = "Latest Release Songs",
                list = homeContent.latestSongs.map { homeItemToMedia(it) },
                subtitle = "Recently released tracks"
            ))
        }
        
        if (homeContent.recentAlbums.isNotEmpty()) {
            shelves.add(Shelf.Lists.Items(
                id = "recent_albums",
                title = "Recent Bollywood Albums",
                list = homeContent.recentAlbums.map { homeItemToMedia(it) },
                subtitle = "Latest album releases"
            ))
        }
        
        return shelves.toFeedData()
    }
    
    private suspend fun loadCategoryTab(categoryUrl: String, categoryName: String): Feed.Data<Shelf> {
        return PagedData.Continuous { continuation ->
            val page = (continuation as? String)?.toIntOrNull() ?: 1
            
            try {
                val html = api.getCategory(categoryUrl, page)
                val categoryContent = parser.parseCategoryContent(html, page)
                
                if (categoryContent.items.isEmpty()) {
                    Page(emptyList<Shelf>(), null)
                } else {
                    val shelf = Shelf.Lists.Items(
                        id = "category_${categoryName.lowercase()}_page_$page",
                        title = if (page == 1) categoryName else "$categoryName - Page $page",
                        list = categoryContent.items.map { categoryItemToMedia(it) },
                        subtitle = null
                    )
                    
                    Page(
                        listOf(shelf),
                        if (categoryContent.hasNextPage) (page + 1).toString() else null
                    )
                }
            } catch (e: Exception) {
                println("DEBUG: Failed to load category $categoryName page $page: ${e.message}")
                e.printStackTrace()
                Page(emptyList<Shelf>(), null)
            }
        }.toFeedData()
    }

    // QUICK SEARCH 
    
    override suspend fun quickSearch(query: String): List<QuickSearchItem> {
        if (query.isBlank()) return emptyList()
        
        return try {
            val html = api.search(query)
            val results = parser.parseSearchResults(html)
            
            val items = mutableListOf<QuickSearchItem>()
            
            results.songs.take(5).forEach { song ->
                items.add(QuickSearchItem.Media(songResultToTrack(song), false))
            }
            
            results.albums.take(5).forEach { album ->
                items.add(QuickSearchItem.Media(albumResultToAlbum(album), false))
            }
            
            items
        } catch (e: Exception) {
            println("DEBUG: Quick search failed: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    override suspend fun deleteQuickSearch(item: QuickSearchItem) {
        // Not implemented
    }

    // SEARCH FEED
    
    override suspend fun loadSearchFeed(query: String): Feed<Shelf> {
        if (query.isBlank()) {
            return emptyList<Shelf>().toFeed()
        }
        
        return try {
            val html = api.search(query)
            val results = parser.parseSearchResults(html)
            
            val shelves = mutableListOf<Shelf>()
            
            if (results.songs.isNotEmpty()) {
                shelves.add(Shelf.Lists.Tracks(
                    id = "search_songs",
                    title = "Songs",
                    list = results.songs.map { songResultToTrack(it) }
                ))
            }
            
            if (results.albums.isNotEmpty()) {
                shelves.add(Shelf.Lists.Items(
                    id = "search_albums",
                    title = "Albums",
                    list = results.albums.map { albumResultToAlbum(it) }
                ))
            }
            
            shelves.toFeed()
        } catch (e: Exception) {
            println("DEBUG: Search feed failed: ${e.message}")
            e.printStackTrace()
            emptyList<Shelf>().toFeed()
        }
    }

    // TRACK CLIENT
    
    override suspend fun loadTrack(track: Track, isDownload: Boolean): Track {
        return try {
            println("DEBUG: Loading track with URL: ${track.id}")
            val html = api.getSongDetails(track.id)
            val songDetail = parser.parseSongDetail(html, track.id)
                ?: throw Exception("Failed to parse song details")
            
            songDetailToTrack(songDetail)
        } catch (e: Exception) {
            println("DEBUG: Failed to load track ${track.id}: ${e.message}")
            e.printStackTrace()
            throw Exception("Failed to load track: ${e.message}")
        }
    }
    
    override suspend fun loadStreamableMedia(
        streamable: Streamable,
        isDownload: Boolean
    ): Streamable.Media {
        return when (streamable.type) {
            Streamable.MediaType.Server -> {
                println("DEBUG: Loading streamable media")
                
                val url = streamable.extras["url"]
                    ?: throw Exception("No stream URL found")
                
                val quality = streamable.extras["quality"]?.toIntOrNull() ?: 128
                
                val source = Streamable.Source.Http(
                    request = url.toGetRequest(),
                    type = Streamable.SourceType.Progressive,
                    quality = quality,
                    title = "${quality}kbps"
                )
                
                Streamable.Media.Server(listOf(source), false)
            }
            Streamable.MediaType.Background -> {
                throw Exception("Background streamables not supported")
            }
            Streamable.MediaType.Subtitle -> {
                throw Exception("Subtitles not supported")
            }
        }
    }
    
    override suspend fun loadFeed(track: Track): Feed<Shelf> {
        // No related tracks feed for now
        return emptyList<Shelf>().toFeed()
    }

    // ARTIST CLIENT
    
    override suspend fun loadArtist(artist: Artist): Artist {
        return try {
            val artistSlug = when {
                artist.extras["slug"] != null -> (artist.extras["slug"] as String).removeSuffix(".html")
                artist.id.startsWith("http") -> artist.id
                artist.id.startsWith("/singer/") -> artist.id.removePrefix("/singer/").removeSuffix(".html")
                artist.id.endsWith(".html") -> artist.id.removeSuffix(".html")
                else -> {
                    artist.name.trim()
                        .lowercase()
                        .replace(Regex("""[^a-z0-9\\s-]"""), "") 
                        .replace(Regex("""\\s+"""), "-") 
                        .replace(Regex("""-+"""), "-") 
                        .trim('-')
                }
            }
            
            val artistUrl = when {
                artistSlug.startsWith("http") -> artistSlug
                artistSlug.startsWith("/") -> "https://pagalnew.com$artistSlug"
                else -> "https://pagalnew.com/singer/$artistSlug.html"
            }
            
            println("DEBUG: Loading artist '${artist.name}' with slug: $artistSlug")
            println("DEBUG: Artist URL: $artistUrl")
            
            val html = api.getArtistSongs(artistUrl, 1)
            val artistData = parser.parseArtistSongs(html, 1)
                ?: throw Exception("Failed to parse artist page")
            
            artist.copy(
                name = artistData.artistName,
                subtitle = "${artistData.songs.size} songs available",
                extras = artist.extras + mapOf(
                    "url" to artistUrl,
                    "slug" to artistSlug,
                    "songCount" to artistData.songs.size.toString()
                )
            )
        } catch (e: Exception) {
            println("DEBUG: Failed to load artist: ${e.message}")
            e.printStackTrace()
            artist.copy(
                subtitle = "Failed to load artist"
            )
        }
    }
    
    override suspend fun loadFeed(artist: Artist): Feed<Shelf> {
        return try {
            val artistUrl = artist.extras["url"] as? String ?: artist.id
            val shelves = mutableListOf<Shelf>()
            val allSongs = mutableListOf<Track>()
            
            for (page in 1..3) {
                val html = api.getArtistSongs(artistUrl, page)
                val artistData = parser.parseArtistSongs(html, page)
                
                if (artistData != null && artistData.songs.isNotEmpty()) {
                    val tracks = artistData.songs.map { song ->
                        artistSongToTrack(song, artistData.artistName)
                    }
                    allSongs.addAll(tracks)
                    
                    if (!artistData.hasNextPage) break
                } else {
                    break
                }
            }
            
            if (allSongs.isNotEmpty()) {
                shelves.add(Shelf.Lists.Tracks(
                    id = "artist_songs",
                    title = "Popular Songs",
                    list = allSongs
                ))
            }
            
            shelves.toFeed()
        } catch (e: Exception) {
            println("DEBUG: Failed to load artist feed: ${e.message}")
            emptyList<Shelf>().toFeed()
        }
    }
    
    // RADIO CLIENT 
    
    override suspend fun radio(item: EchoMediaItem, context: EchoMediaItem?): Radio {
        return when (item) {
            is Track -> createRadioFromTrack(item)
            is Album -> createRadioFromAlbum(item)
            is Artist -> createRadioFromArtist(item)
            else -> throw Exception("Radio not supported for this item type")
        }
    }
    
    override suspend fun loadTracks(radio: Radio): Feed<Track> {
        val trackUrlsJson = radio.extras["trackUrls"] as? String
        val categoryUrl = radio.extras["categoryUrl"] as? String
    
        if (trackUrlsJson.isNullOrBlank()) {
            println("DEBUG: Radio has no tracks")
            return emptyList<Track>().toFeed() as Feed<Track>
        }
        
        val initialTrackUrls = trackUrlsJson.split(",").filter { it.isNotBlank() }
        
        return PagedData.Continuous { continuation ->
            try {
                val parts = if (continuation != null) {
                    continuation.split(":", limit = 2)
                } else {
                    listOf("0", trackUrlsJson)
                }
                
                val pageIndex = parts[0].toIntOrNull() ?: 0
                val currentUrls = if (parts.size > 1) {
                    parts[1].split(",").filter { it.isNotBlank() }
                } else {
                    initialTrackUrls
                }
                
                val pageSize = 20 
                val startIndex = pageIndex * pageSize
                
                println("DEBUG: Loading radio page $pageIndex, starting at index $startIndex of ${currentUrls.size} total")
                
                var updatedUrls = currentUrls.toMutableList()
                
                if (startIndex >= updatedUrls.size) {
                    println("DEBUG: Reached end of tracks (${updatedUrls.size}), attempting to fetch more similar songs")
                    
                    val lastTrackUrl = updatedUrls.lastOrNull()
                    if (lastTrackUrl != null) {
                        try {
                            val html = api.getSongDetails(lastTrackUrl)
                            val moreSimilarSongs = parser.parseSimilarSongs(html)
                            
                            if (moreSimilarSongs.isNotEmpty()) {
                                val newUrls = moreSimilarSongs.map { it.url }.filter { !updatedUrls.contains(it) }
                                updatedUrls.addAll(newUrls)
                                println("DEBUG: Found ${newUrls.size} new unique similar songs, total now: ${updatedUrls.size}")
                            } else if (!categoryUrl.isNullOrBlank()) {
                                println("DEBUG: No more similar songs, trying category fallback")
                                val categoryPage = (pageIndex / 2) + 2 
                                val categoryHtml = api.getCategory(categoryUrl, categoryPage)
                                val categoryContent = parser.parseCategoryContent(categoryHtml, categoryPage)
                                
                                val newUrls = categoryContent.items
                                    .filter { it.type == "song" && !updatedUrls.contains(it.url) }
                                    .map { it.url }
                                
                                if (newUrls.isNotEmpty()) {
                                    updatedUrls.addAll(newUrls)
                                    println("DEBUG: Added ${newUrls.size} tracks from category page $categoryPage, total now: ${updatedUrls.size}")
                                }
                            }
                        } catch (e: Exception) {
                            println("DEBUG: Failed to fetch more tracks: ${e.message}")
                        }
                    }
                }
                
                if (startIndex >= updatedUrls.size) {
                    println("DEBUG: No more tracks available")
                    return@Continuous Page(emptyList(), null)
                }
                
                val endIndex = minOf(startIndex + pageSize, updatedUrls.size)
                val pageUrls = updatedUrls.subList(startIndex, endIndex)
                
                val tracks = mutableListOf<Track>()
                
                for (url in pageUrls) {
                    try {
                        val html = api.getSongDetails(url)
                        val songDetail = parser.parseSongDetail(html, url)
                        if (songDetail != null) {
                            tracks.add(songDetailToTrack(songDetail))
                        }
                    } catch (e: Exception) {
                        println("DEBUG: Failed to load track $url for radio: ${e.message}")
                    }
                }
                
                println("DEBUG: Loaded ${tracks.size} tracks for radio page $pageIndex")
                
                val hasNext = endIndex < updatedUrls.size || tracks.isNotEmpty()
                val nextContinuation = if (hasNext) {
                    "${pageIndex + 1}:${updatedUrls.joinToString(",")}"
                } else null
                
                Page(tracks, nextContinuation)
            } catch (e: Exception) {
                println("DEBUG: Failed to load radio tracks page: ${e.message}")
                e.printStackTrace()
                Page(emptyList(), null)
            }
        }.toFeed() as Feed<Track>
    }
    
    override suspend fun loadRadio(radio: Radio): Radio = radio
    
    private suspend fun createRadioFromTrack(track: Track): Radio {
        return try {
            println("DEBUG: Creating radio from track: ${track.title}")
            val trackUrls = mutableListOf<String>()
            
            try {
                println("DEBUG: Fetching track detail page for similar songs")
                val html = api.getSongDetails(track.id)
                val similarSongs = parser.parseSimilarSongs(html)
                
                if (similarSongs.isNotEmpty()) {
                    println("DEBUG: Found ${similarSongs.size} similar songs from 'More Songs' section")
                    trackUrls.addAll(similarSongs.map { it.url })
                }
            } catch (e: Exception) {
                println("DEBUG: Failed to get similar songs from track page: ${e.message}")
            }
            
            if (trackUrls.isEmpty()) {
                val trackAlbum = track.album
                if (trackAlbum != null) {
                    try {
                        println("DEBUG: No similar songs found, fetching album tracks for radio")
                        val albumUrl = trackAlbum.extras["url"] as? String ?: trackAlbum.id
                        val html = api.getAlbumDetails(albumUrl)
                        val albumDetail = parser.parseAlbumDetail(html, albumUrl)
                        
                        if (albumDetail != null && albumDetail.songs.isNotEmpty()) {
                            println("DEBUG: Found ${albumDetail.songs.size} songs from album")
                            trackUrls.addAll(albumDetail.songs.map { it.url })
                        }
                    } catch (e: Exception) {
                        println("DEBUG: Failed to get album tracks: ${e.message}")
                    }
                }
            }
            
            var categoryUrl: String? = null
            if (trackUrls.isEmpty()) {
                val category = track.extras["category"] as? String
                if (!category.isNullOrBlank()) {
                    try {
                        println("DEBUG: No album found, fetching category tracks for radio: $category")
                        categoryUrl = "/category/${category.lowercase().replace(" ", "-")}-mp3-tracks"
                        val html = api.getCategory(categoryUrl, 1)
                        val categoryContent = parser.parseCategoryContent(html, 1)
                        
                        if (categoryContent.items.isNotEmpty()) {
                            val songItems = categoryContent.items.filter { it.type == "song" }
                            println("DEBUG: Found ${songItems.size} songs from category")
                            trackUrls.addAll(songItems.map { it.url })
                        }
                    } catch (e: Exception) {
                        println("DEBUG: Failed to get category tracks: ${e.message}")
                    }
                }
            } else {
                val category = track.extras["category"] as? String
                if (!category.isNullOrBlank()) {
                    categoryUrl = "/category/${category.lowercase().replace(" ", "-")}-mp3-tracks"
                }
            }
            
            if (trackUrls.isEmpty()) {
                println("DEBUG: No similar tracks found for radio, returning empty radio")
                return Radio(
                    id = "radio_${track.id}",
                    title = "${track.title} Radio",
                    subtitle = "No similar tracks available",
                    cover = track.cover,
                    extras = mapOf("trackUrls" to "")
                )
            }
            
            println("DEBUG: Radio created successfully with ${trackUrls.size} tracks")
            
            Radio(
                id = "radio_${track.id}",
                title = "${track.title} Radio",
                subtitle = "Similar tracks (${trackUrls.size}+ songs)",
                cover = track.cover,
                extras = mapOf(
                    "trackUrls" to trackUrls.joinToString(","),
                    "categoryUrl" to (categoryUrl ?: "")
                )
            )
        } catch (e: Exception) {
            println("DEBUG: Failed to create radio from track: ${e.message}")
            Radio(
                id = "radio_${track.id}",
                title = "${track.title} Radio",
                subtitle = "Radio unavailable",
                cover = track.cover,
                extras = mapOf("trackUrls" to "")
            )
        }
    }
    
    private suspend fun createRadioFromAlbum(album: Album): Radio {
        return try {
            val albumUrl = album.extras["url"] as? String ?: album.id
            val html = api.getAlbumDetails(albumUrl)
            val albumDetail = parser.parseAlbumDetail(html, albumUrl)
                ?: throw Exception("Album not found")
            
            val trackUrls = albumDetail.songs.map { it.url }
            
            val category = albumDetail.category
            val categoryUrl = if (category.isNotBlank()) {
                "/category/${category.lowercase().replace(" ", "-")}-mp3-tracks"
            } else ""
            
            Radio(
                id = "radio_${album.id}",
                title = "${album.title} Radio",
                subtitle = "Songs from ${album.title} (${trackUrls.size}+ songs)",
                cover = album.cover,
                extras = mapOf(
                    "trackUrls" to trackUrls.joinToString(","),
                    "categoryUrl" to categoryUrl
                )
            )
        } catch (e: Exception) {
            println("DEBUG: Failed to create radio from album: ${e.message}")
            Radio(
                id = "radio_${album.id}",
                title = "${album.title} Radio",
                subtitle = "Radio unavailable",
                cover = album.cover,
                extras = mapOf("trackUrls" to "")
            )
        }
    }
    
    private suspend fun createRadioFromArtist(artist: Artist): Radio {
        return try {
            val artistUrl = artist.extras["url"] as? String ?: artist.id
            val trackUrls = mutableListOf<String>()
            
            for (page in 1..2) {
                val html = api.getArtistSongs(artistUrl, page)
                val artistData = parser.parseArtistSongs(html, page)
                
                if (artistData != null && artistData.songs.isNotEmpty()) {
                    trackUrls.addAll(artistData.songs.map { it.url })
                    
                    if (!artistData.hasNextPage) break
                } else {
                    break
                }
            }
            
            Radio(
                id = "radio_${artist.id}",
                title = "${artist.name} Radio",
                subtitle = "Songs by ${artist.name} (${trackUrls.size}+ songs)",
                cover = artist.cover,
                extras = mapOf(
                    "trackUrls" to trackUrls.joinToString(","),
                    "categoryUrl" to "" 
                )
            )
        } catch (e: Exception) {
            println("DEBUG: Failed to create radio from artist: ${e.message}")
            Radio(
                id = "radio_${artist.id}",
                title = "${artist.name} Radio",
                subtitle = "Radio unavailable",
                cover = artist.cover,
                extras = mapOf("trackUrls" to "")
            )
        }
    }
    
    // ALBUM CLIENT 
    
    override suspend fun loadAlbum(album: Album): Album {
        return try {
            println("DEBUG: Loading album with URL: ${album.id}")
            val html = api.getAlbumDetails(album.id)
            val albumDetail = parser.parseAlbumDetail(html, album.id)
                ?: throw Exception("Failed to parse album details")
            
            var totalTracks = albumDetail.songs.size.toLong()
            
            if (albumDetail.hasPagination && albumDetail.paginationPages.isNotEmpty()) {
                totalTracks = (albumDetail.songs.size * albumDetail.paginationPages.size).toLong()
            }
            
            val convertedAlbum = albumDetailToAlbum(albumDetail)
            
            convertedAlbum.copy(
                trackCount = totalTracks,
                subtitle = if (albumDetail.hasPagination) {
                    "${albumDetail.category} • ${albumDetail.year} • ${totalTracks}+ tracks"
                } else {
                    "${albumDetail.category} • ${albumDetail.year} • $totalTracks tracks"
                }
            )
        } catch (e: Exception) {
            println("DEBUG: Failed to load album ${album.id}: ${e.message}")
            e.printStackTrace()
            throw Exception("Failed to load album: ${e.message}")
        }
    }
    
    override suspend fun loadTracks(album: Album): Feed<Track>? {
        return try {
            println("DEBUG: Loading tracks for album: ${album.id}")
            val html = api.getAlbumDetails(album.id)
            val albumDetail = parser.parseAlbumDetail(html, album.id)
                ?: return null
            
            val allSongs = albumDetail.songs.toMutableList()
            
            if (albumDetail.hasPagination && albumDetail.paginationPages.isNotEmpty()) {
                println("DEBUG: Album has pagination with pages: ${albumDetail.paginationPages}")
                
                for (pageNum in albumDetail.paginationPages) {
                    if (pageNum == 1) continue 
                    
                    try {
                        val pageUrl = if (album.id.contains(".html")) {
                            album.id.replace(".html", "/$pageNum")
                        } else {
                            "${album.id}/$pageNum"
                        }
                        
                        println("DEBUG: Fetching album page $pageNum from: $pageUrl")
                        val pageHtml = api.getAlbumDetails(pageUrl)
                        val pageDetail = parser.parseAlbumDetail(pageHtml, pageUrl)
                        
                        if (pageDetail != null) {
                            allSongs.addAll(pageDetail.songs)
                            println("DEBUG: Added ${pageDetail.songs.size} songs from page $pageNum")
                        }
                    } catch (e: Exception) {
                        println("DEBUG: Failed to fetch album page $pageNum: ${e.message}")
                    }
                }
            }
            
            println("DEBUG: Total songs in album: ${allSongs.size}")
            
            val tracks = allSongs.map { albumSong ->
                Track(
                    id = albumSong.url,
                    title = albumSong.title,
                    type = Track.Type.Song,
                    cover = album.cover,
                    artists = albumSong.artists.split(",").map { name ->
                        Artist(
                            id = "",
                            name = name.trim(),
                            cover = null,
                            bio = null,
                            background = null,
                            banners = emptyList(),
                            subtitle = null,
                            extras = emptyMap()
                        )
                    },
                    album = album,
                    duration = null,
                    playedDuration = null,
                    plays = null,
                    releaseDate = null,
                    description = null,
                    background = album.cover,
                    genres = emptyList(),
                    isrc = null,
                    albumOrderNumber = null,
                    albumDiscNumber = null,
                    playlistAddedDate = null,
                    isExplicit = false,
                    subtitle = albumSong.artists,
                    extras = mapOf("url" to albumSong.url),
                    isPlayable = Track.Playable.Yes,
                    streamables = emptyList()
                )
            }
            
            tracks.toFeed() as Feed<Track>
        } catch (e: Exception) {
            println("DEBUG: Failed to load album tracks: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    override suspend fun loadFeed(album: Album): Feed<Shelf>? {
        return null
    }
    
    // LIBRARY FEED
    
    override suspend fun loadLibraryFeed(): Feed<Shelf> {
        val shelves = listOf(
            Shelf.Lists.Items(
                id = "playlists",
                title = "Playlists",
                subtitle = "Your saved playlists",
                list = emptyList<Playlist>()
            ),
            Shelf.Lists.Items(
                id = "liked_songs",
                title = "Liked Songs",
                subtitle = "Your favorite tracks",
                list = emptyList<Track>()
            ),
            Shelf.Lists.Items(
                id = "downloaded",
                title = "Downloaded",
                subtitle = "Offline available tracks",
                list = emptyList<Track>()
            )
        )
        
        return shelves.toFeed()
    }
}
