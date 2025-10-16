package dev.brahmkshatriya.echo.extension

class GrooveParser {
    
    fun parseSearchResults(html: String): SearchResults {
        val albums = mutableListOf<AlbumResult>()
        val songs = mutableListOf<SongResult>()
        
        val albumsStart = html.indexOf("Albums Result")
        if (albumsStart != -1) {
            val songsStart = html.indexOf("Songs Result", albumsStart)
            val albumSection = if (songsStart != -1) {
                html.substring(albumsStart, songsStart)
            } else {
                html.substring(albumsStart, minOf(albumsStart + 10000, html.length))
            }
            
            val albumRegex = Regex("""<a href="/album/([^"]+)">\s*<div[^>]*>\s*<div[^>]*>\s*<img src="([^"]+)"[^>]*>\s*<div[^>]*>\s*<div><b>([^<]+)<\/b><\/div>\s*<div><i>([^<]+)<\/i>""")
            albumRegex.findAll(albumSection).forEach { match ->
                albums.add(AlbumResult(
                    id = match.groupValues[1],
                    title = cleanText(match.groupValues[3]),
                    category = cleanText(match.groupValues[4]),
                    coverImage = normalizeImageUrl(match.groupValues[2]),
                    url = "/album/${match.groupValues[1]}"
                ))
            }
        }
        
        val songsStart = html.indexOf("Songs Result")
        if (songsStart != -1) {
            val songSection = html.substring(songsStart, minOf(songsStart + 10000, html.length))
            
            val songRegex = Regex("""<a href="/songs/([^"]+)">\s*<div[^>]*>\s*<div[^>]*>\s*<img src="([^"]+)"[^>]*>\s*<div[^>]*>\s*<div><b>([^<]+)<\/b><br><span>([^<]*)<\/span><\/div>\s*<div><i>([^<]+)<\/i>""")
            songRegex.findAll(songSection).forEach { match ->
                songs.add(SongResult(
                    id = match.groupValues[1],
                    title = cleanText(match.groupValues[3]),
                    album = cleanText(match.groupValues[4]),
                    category = cleanText(match.groupValues[5]),
                    coverImage = normalizeImageUrl(match.groupValues[2]),
                    url = "/songs/${match.groupValues[1]}"
                ))
            }
        }
        
        return SearchResults(albums, songs)
    }
    fun parseSongDetail(html: String, songUrl: String): SongDetail? {
        try {
            val songNameMatch = Regex("""<b>Song Name:\s*</b>\s*([^<\n]+)""").find(html)
            val songName = songNameMatch?.groupValues?.get(1)?.let { cleanText(it) } ?: return null
            
            val singersMatch = Regex("""<b>Singer\(s\):\s*</b>\s*([^<\n]+)""").find(html)
            val singers = singersMatch?.groupValues?.get(1)?.let { cleanText(it) } ?: ""
            
            val starsMatch = Regex("""<b>Lead Star\(s\):\s*</b>\s*([^<\n]+)""").find(html)
            val leadStars = starsMatch?.groupValues?.get(1)?.let { cleanText(it) } ?: ""
            
            val composerMatch = Regex("""<b>Music Composer:\s*</b>\s*([^<\n]+)""").find(html)
            val composer = composerMatch?.groupValues?.get(1)?.let { cleanText(it) } ?: ""
            
            val releaseDateMatch = Regex("""<b>Released On:\s*</b>\s*([^<\n]+)""").find(html)
            val releaseDate = releaseDateMatch?.groupValues?.get(1)?.let { cleanText(it) } ?: ""
            
            val lyricistMatch = Regex("""Lyrics (?:beautifully )?penned by\s+([^<,\n]+)""").find(html)
            val lyricist = lyricistMatch?.groupValues?.get(1)?.let { cleanText(it) } ?: ""
            
            val coverMatch = Regex("""data-src="(https://pagalnew\.com/coverimages/[^"]+)"""").find(html)
                ?: Regex("""src="(https://pagalnew\.com/coverimages/[^"]+)"""").find(html)
                ?: Regex("""data-src="(\.\.?/coverimages/[^"]+)"""").find(html)
                ?: Regex("""src="(\.\.?/coverimages/[^"]+)"""").find(html)
            
            val coverImage = when {
                coverMatch == null -> ""
                coverMatch.groupValues[1].startsWith("../") -> "https://pagalnew.com/${coverMatch.groupValues[1].substring(3)}"
                coverMatch.groupValues[1].startsWith("./") -> "https://pagalnew.com${coverMatch.groupValues[1].substring(1)}"
                else -> coverMatch.groupValues[1]
            }
            val categoryMatch = Regex("""<li[^>]*><a href="[^"]*category/([^"]+)"""").find(html)
            val category = categoryMatch?.groupValues?.get(1)?.replace("-", " ")?.let { cleanText(it) } ?: ""
    
            val albumMatch = Regex("""<li[^>]*><a href="[^"]*\/album\/[^"]*">([^<]+)<\/a><\/li>""").find(html)
            val album = albumMatch?.groupValues?.get(1)?.let { cleanText(it) } ?: ""
            
            val yearMatch = Regex("""Songs\s+(\d{4})\s+year""").find(html)
            val year = yearMatch?.groupValues?.get(1) ?: ""
            
            val url128Match = Regex("""<a[^>]+href="(https://pagalnew\.com/128-downloads/[^"]+)"""").find(html)
                ?: Regex("""href="(/128-downloads/\d+)"""").find(html)
            val url128 = when {
                url128Match == null -> null
                url128Match.groupValues[1].startsWith("/") -> "https://pagalnew.com${url128Match.groupValues[1]}"
                else -> url128Match.groupValues[1]
            }
            
            val url320Match = Regex("""<a[^>]+href="(https://pagalnew\.com/320-download/[^"]+)"""").find(html)
                ?: Regex("""href="(/320-download/\d+)"""").find(html)
            val url320 = when {
                url320Match == null -> null
                url320Match.groupValues[1].startsWith("/") -> "https://pagalnew.com${url320Match.groupValues[1]}"
                else -> url320Match.groupValues[1]
            }
            
            return SongDetail(
                id = songUrl,
                title = songName,
                singers = singers,
                leadStars = leadStars,
                composer = composer,
                lyricist = lyricist,
                album = album,
                releaseDate = releaseDate,
                category = category,
                year = year,
                coverImage = coverImage,
                url128kbps = url128,
                url320kbps = url320,
                url = songUrl
            )
        } catch (e: Exception) {
            println("DEBUG: Failed to parse song detail: ${e.message}")
            return null
        }
    }
        
    fun parseAlbumDetail(html: String, albumUrl: String): AlbumDetail? {
        try {
            val albumNameMatch = Regex("""<b>\s*Album:\s*</b>\s*([^<\n]+)""").find(html)
            val albumName = albumNameMatch?.groupValues?.get(1)?.let { cleanText(it) } ?: return null
            
            val coverMatch = Regex("""data-src="(https://pagalnew\.com/coverimages/album/[^"]+)"""").find(html)
                ?: Regex("""src="(https://pagalnew\.com/coverimages/album/[^"]+)"""").find(html)
            val coverImage = coverMatch?.groupValues?.get(1) ?: ""
            
            val artistsMatch = Regex("""<b>\s*Artists?:\s*</b>\s*([^\n<]+)""").find(html)
            val artists = artistsMatch?.groupValues?.get(1)?.let { cleanText(it) } ?: ""
            
            val starcastMatch = Regex("""<b>\s*Starcast:\s*</b>\s*([^\n<]*)""").find(html)
            val starcast = starcastMatch?.groupValues?.get(1)?.let { cleanText(it) } ?: ""
            
            val composersMatch = Regex("""<b>\s*Composed by:\s*</b>\s*([^\n<]*)""").find(html)
            val composers = composersMatch?.groupValues?.get(1)?.let { cleanText(it) } ?: ""
            
            val yearMatch = Regex("""<b>\s*Year:\s*</b>\s*(\d{4})""").find(html)
            val year = yearMatch?.groupValues?.get(1) ?: ""
            
            val categoryMatch = Regex("""<li[^>]*><a href="[^"]*category/([^"/]+)""").find(html)
            val category = categoryMatch?.groupValues?.get(1)?.replace("-", " ")?.let { cleanText(it) } ?: ""
            
            val songs = mutableListOf<AlbumSong>()
            val seenUrls = mutableSetOf<String>()
            
            val songPattern1 = Regex("""<a\s+href="(https://pagalnew\.com/songs/[^"]+)"[^>]*>[\s\S]*?<div[^>]*style="color:#000000;\s*font-weight:700;">\s*([^<]+)\s*</div>[\s\S]*?<div[^>]*>\s*([^<\n]+)""")
            songPattern1.findAll(html).forEach { match ->
                val songUrl = match.groupValues[1]
                if (!seenUrls.contains(songUrl)) {
                    seenUrls.add(songUrl)
                    songs.add(AlbumSong(
                        title = cleanText(match.groupValues[2]),
                        artists = cleanText(match.groupValues[3]),
                        url = songUrl
                    ))
                }
            }
            
            val songPattern2 = Regex("""<a\s+href="(https://pagalnew\.com/songs/[^"]+)"[^>]*>\s*<div[^>]*>\s*<div[^>]*>\s*<img[^>]*>\s*</div>\s*<div[^>]*>\s*<div[^>]*>\s*([^<]+?)\s*</div>\s*<div[^>]*>\s*([^<\n]*)""")
            songPattern2.findAll(html).forEach { match ->
                val songUrl = match.groupValues[1]
                if (!seenUrls.contains(songUrl)) {
                    seenUrls.add(songUrl)
                    songs.add(AlbumSong(
                        title = cleanText(match.groupValues[2]),
                        artists = cleanText(match.groupValues[3]),
                        url = songUrl
                    ))
                }
            }
            
            if (songs.isEmpty()) {
                val songPattern3 = Regex("""<a\s+href="(https://pagalnew\.com/songs/[^"]+)"[^>]*>([\s\S]{0,500}?)</a>""")
                songPattern3.findAll(html).forEach { match ->
                    val songUrl = match.groupValues[1]
                    val content = match.groupValues[2]
                    
                    if (!seenUrls.contains(songUrl)) {
                        val titleMatch = Regex("""<div[^>]*(?:font-weight:700|class="[^"]*title[^"]*")[^>]*>\s*([^<]+)\s*</div>""").find(content)
                            ?: Regex("""<div[^>]*>\s*([^<]+?)\s*(?:Mp3 Song|Song)\s*</div>""").find(content)
                        
                        val artistMatch = Regex("""<div[^>]*>\s*([^<\n]+?)(?:<br|<p|</div>)""").find(content)
                        
                        if (titleMatch != null) {
                            seenUrls.add(songUrl)
                            songs.add(AlbumSong(
                                title = cleanText(titleMatch.groupValues[1]),
                                artists = artistMatch?.groupValues?.get(1)?.let { cleanText(it) } ?: "",
                                url = songUrl
                            ))
                        }
                    }
                }
            }
            
            val hasPagination = html.contains("""class="pagination"""") || html.contains("""class='pagination'""")
            val paginationPages = mutableListOf<Int>()
            
            if (hasPagination) {
                val pagePattern = Regex("""<a\s+href\s*=\s*["']([^"']+/(\d+))["']\s*>(\d+)\s*</a>""")
                pagePattern.findAll(html).forEach { match ->
                    val pageNum = match.groupValues[2].toIntOrNull()
                    if (pageNum != null && !paginationPages.contains(pageNum)) {
                        paginationPages.add(pageNum)
                    }
                }
            }
            
            return AlbumDetail(
                id = albumUrl,
                name = albumName,
                coverImage = coverImage,
                artists = artists,
                starcast = starcast,
                composers = composers,
                year = year,
                category = category,
                songs = songs,
                url = albumUrl,
                hasPagination = hasPagination,
                paginationPages = paginationPages.sorted()
            )
        } catch (e: Exception) {
            println("DEBUG: Failed to parse album detail: ${e.message}")
            return null
        }
    }
    fun parseSimilarSongs(html: String): List<SongResult> {
        val similarSongs = mutableListOf<SongResult>()
        
        try {
            val moreSongsMatch = Regex("""<div class="lyricname">More Songs From ([^<]+)</div>""").find(html)
            if (moreSongsMatch == null) {
                println("DEBUG: No 'More Songs From' section found in HTML")
                return emptyList()
            }
            
            val category = cleanText(moreSongsMatch.groupValues[1])
            println("DEBUG: Found 'More Songs From $category' section")
            
            val sectionStart = moreSongsMatch.range.last
            val sectionHtml = html.substring(sectionStart, minOf(sectionStart + 30000, html.length))
            
            val songBlockRegex = Regex("""<a href="(/songs/[^"]+)">\s*<img[^>]+src="([^"]+)"[^>]*>\s*([^<]+?)\s*-\s*([^<]+?)\s*</a>""")
            
            songBlockRegex.findAll(sectionHtml).forEach { match ->
                val songPath = match.groupValues[1].trim()
                val songUrl = if (songPath.startsWith("http")) songPath else "https://pagalnew.com$songPath"
                val coverImage = match.groupValues[2].trim()
                val title = cleanText(match.groupValues[3])
                val artist = cleanText(match.groupValues[4])
                
                similarSongs.add(SongResult(
                    id = songUrl,
                    title = title,
                    album = artist,
                    category = category,
                    coverImage = normalizeImageUrl(coverImage),
                    url = songUrl
                ))
            }
            
            println("DEBUG: Parsed ${similarSongs.size} similar songs from 'More Songs' section")
        } catch (e: Exception) {
            println("DEBUG: Failed to parse similar songs: ${e.message}")
            e.printStackTrace()
        }
        
        return similarSongs
    }
    
    fun parseHomeContent(html: String): HomeContent {
        val latestSongs = mutableListOf<HomeItem>()
        val recentAlbums = mutableListOf<HomeItem>()
        
        val songLinkRegex = Regex("""<a\s+href="(https://pagalnew\.com/songs/[^"]+)"""")
        songLinkRegex.findAll(html).take(12).forEach { linkMatch ->
            val songUrl = linkMatch.groupValues[1]
            val startPos = linkMatch.range.first
            val chunk = html.substring(startPos, minOf(startPos + 1000, html.length))
            
            val titleMatch = Regex("""<h2[^>]*>([^<]+)</h2>""").find(chunk)
            if (titleMatch != null) {
                val songTitle = cleanText(titleMatch.groupValues[1])
                
                val imgMatch = Regex("""data-src="([^"]+)"""").find(chunk)
                val imageUrl = imgMatch?.groupValues?.get(1) ?: ""
                
                val paragraphs = mutableListOf<String>()
                val pRegex = Regex("""<p[^>]*>([^<]+)</p>""")
                pRegex.findAll(chunk).take(3).forEach { pMatch ->
                    paragraphs.add(cleanText(pMatch.groupValues[1]))
                }
                
                if (paragraphs.size >= 3) {
                    latestSongs.add(HomeItem(
                        title = songTitle,
                        subtitle = paragraphs[0], 
                        coverImage = normalizeImageUrl(imageUrl),
                        url = songUrl,
                        type = "song"
                    ))
                }
            }
        }
        
        val albumLinkRegex = Regex("""<a\s+href="(https://pagalnew\.com/album/[^"]+)"""")
        albumLinkRegex.findAll(html).take(12).forEach { linkMatch ->
            val albumUrl = linkMatch.groupValues[1]
            val startPos = linkMatch.range.first
            val chunk = html.substring(startPos, minOf(startPos + 1000, html.length))
            
            val imgMatch = Regex("""data-src="([^"]+)"""").find(chunk)
            val imageUrl = imgMatch?.groupValues?.get(1) ?: ""
            
            val titleMatch = Regex("""color:#000000;\s*font-weight:700[^>]*>\s*([^<]+)\s*<""").find(chunk)
            
            val artistMatch = Regex("""font-weight:500[^>]*>\s*([^<]+?)(?:\s*<|$)""").find(chunk)
            
            if (titleMatch != null) {
                recentAlbums.add(HomeItem(
                    title = cleanText(titleMatch.groupValues[1]),
                    subtitle = artistMatch?.groupValues?.get(1)?.let { cleanText(it) } ?: "",
                    coverImage = normalizeImageUrl(imageUrl),
                    url = albumUrl,
                    type = "album"
                ))
            }
        }
        
        return HomeContent(latestSongs, recentAlbums)
    }
    
    fun parseArtistSongs(html: String, currentPage: Int): ArtistSongs? {
        try {
            val artistNameMatch = Regex("""<div[^>]*main_page_category_div[^>]*>Songs By ([^<]*)</div>""").find(html)
            val artistName = artistNameMatch?.groupValues?.get(1)?.let { cleanText(it) } ?: "Unknown Artist"
            
            println("DEBUG: Parsing artist songs for: $artistName")
            println("DEBUG: HTML length: ${html.length}")
            
            val songs = mutableListOf<ArtistSong>()
            
            val songBlockRegex = Regex(
                """<a href="([^"]*\/songs\/[^"]*)">[\s\S]*?<div class="col-lg-6[^"]*main_page_category_music">[\s\S]*?<img src="([^"]*)[\s\S]*?<b>\s*([^<]*?)\s*</b>[\s\S]*?<div>\s*([^<]*?)\s*</div>[\s\S]*?</div>[\s\S]*?</div>[\s\S]*?</a>"""
            )
            
            songBlockRegex.findAll(html).forEach { match ->
                val songUrl = match.groupValues[1].trim()
                val coverImage = match.groupValues[2].trim()
                val title = match.groupValues[3].trim()
                val artists = match.groupValues[4].trim()
                
                val cleanTitle = title
                    .replace(Regex("""\s*-\s*[^-]*$"""), "")
                    .trim()
                
                songs.add(ArtistSong(
                    title = if (cleanTitle.isNotEmpty()) cleanTitle else title,
                    artists = artists,
                    coverImage = normalizeImageUrl(coverImage),
                    url = if (songUrl.startsWith("http")) songUrl else "https://pagalnew.com$songUrl"
                ))
            }
            
            println("DEBUG: Found ${songs.size} songs for artist $artistName")
            
            val nextPageMatch = Regex("""<a href=['"]([^'"]*\/singer\/[^'"/]+\/(\d+))['"]>\s*Next\s*</a>""").find(html)
            val hasNextPage = nextPageMatch != null
            
            println("DEBUG: Has next page: $hasNextPage for page $currentPage")
            if (nextPageMatch != null) {
                println("DEBUG: Next page URL: ${nextPageMatch.groupValues[1]}")
            }
            
            return ArtistSongs(
                artistName = artistName,
                songs = songs,
                hasNextPage = hasNextPage,
                currentPage = currentPage
            )
        } catch (e: Exception) {
            println("DEBUG: Failed to parse artist songs: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    fun parseCategoryContent(html: String, currentPage: Int): CategoryContent {
        val items = mutableListOf<CategoryItem>()
        
        val songBlockRegex = Regex("""<div class="col-lg-6[^"]*main_page_category_music">[\s\S]*?<a href="([^"]*\/songs\/[^"]*)"[\s\S]*?<img src="([^"]*)"[\s\S]*?<div[^>]*>([^<]*)</div>[\s\S]*?<div[^>]*>([^<]*)</div>[\s\S]*?</div>[\s\S]*?</a>[\s\S]*?</div>""")
        
        songBlockRegex.findAll(html).forEach { match ->
            val songUrl = match.groupValues[1].trim()
            val coverImage = match.groupValues[2].trim()
            val titleWithYear = match.groupValues[3].trim()
            val artist = match.groupValues[4].trim()
            
            val title = titleWithYear
                .replace(Regex("""\s*\(\d{4}\)\s*$"""), "")
                .replace(Regex("""\s+Mp3 Song\s*$"""), "")
                .trim()
            
            items.add(CategoryItem(
                title = title,
                subtitle = artist,
                coverImage = normalizeImageUrl(coverImage),
                url = if (songUrl.startsWith("http")) songUrl else "https://pagalnew.com$songUrl",
                type = "song"
            ))
        }
        
        val albumBlockRegex = Regex("""<div class="col-lg-6[^"]*main_page_category_music">[\s\S]*?<a href="([^"]*\/album\/[^"]*)"[\s\S]*?<img src="([^"]*)"[\s\S]*?<div[^>]*>([^<]*)</div>[\s\S]*?<div[^>]*>([^<]*)</div>[\s\S]*?</div>[\s\S]*?</a>[\s\S]*?</div>""")
        
        albumBlockRegex.findAll(html).forEach { match ->
            val albumUrl = match.groupValues[1].trim()
            val coverImage = match.groupValues[2].trim()
            val titleWithYear = match.groupValues[3].trim()
            val artist = match.groupValues[4].trim()
            
            val title = titleWithYear
                .replace(Regex("""\s*\(\d{4}\)\s*$"""), "")
                .replace(Regex("""\s+Mp3 Songs?\s*$"""), "")
                .trim()
            
            items.add(CategoryItem(
                title = title,
                subtitle = artist,
                coverImage = normalizeImageUrl(coverImage),
                url = if (albumUrl.startsWith("http")) albumUrl else "https://pagalnew.com$albumUrl",
                type = "album"
            ))
        }
        
        val hasNextPage = html.contains("""href='[^']*/(haryanvi|bollywood|indipop|punjabi|tamil|english|dj-mix)-mp3-[^']*/${currentPage + 1}'""".toRegex())
        
        return CategoryContent(items, currentPage, hasNextPage)
    }
    
    private fun cleanText(text: String): String {
        return text
            .replace(Regex("""<[^>]*>"""), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
    
    private fun normalizeImageUrl(url: String): String {
        return when {
            url.startsWith("http") -> url
            url.startsWith("/../") -> "https://pagalnew.com${url.substring(3)}"
            url.startsWith("./") -> "https://pagalnew.com${url.substring(1)}"
            url.startsWith("/") -> "https://pagalnew.com$url"
            else -> "https://pagalnew.com/$url"
        }
    }
}

data class SearchResults(
    val albums: List<AlbumResult>,
    val songs: List<SongResult>
)

data class AlbumResult(
    val id: String,
    val title: String,
    val category: String,
    val coverImage: String,
    val url: String
)

data class SongResult(
    val id: String,
    val title: String,
    val album: String,
    val category: String,
    val coverImage: String,
    val url: String
)

data class SongDetail(
    val id: String,
    val title: String,
    val singers: String,
    val leadStars: String,
    val composer: String,
    val lyricist: String,
    val album: String,
    val releaseDate: String,
    val category: String,
    val year: String,
    val coverImage: String,
    val url128kbps: String?,
    val url320kbps: String?,
    val url: String
)

data class AlbumDetail(
    val id: String,
    val name: String,
    val coverImage: String,
    val artists: String,
    val starcast: String,
    val composers: String,
    val year: String,
    val category: String,
    val songs: List<AlbumSong>,
    val url: String,
    val hasPagination: Boolean = false,
    val paginationPages: List<Int> = emptyList()
)

data class AlbumSong(
    val title: String,
    val artists: String,
    val url: String
)

data class HomeContent(
    val latestSongs: List<HomeItem>,
    val recentAlbums: List<HomeItem>
)

data class HomeItem(
    val title: String,
    val subtitle: String,
    val coverImage: String,
    val url: String,
    val type: String 
)

data class CategoryContent(
    val items: List<CategoryItem>,
    val currentPage: Int,
    val hasNextPage: Boolean
)

data class CategoryItem(
    val title: String,
    val subtitle: String,
    val coverImage: String,
    val url: String,
    val type: String 
)

data class ArtistSongs(
    val artistName: String,
    val songs: List<ArtistSong>,
    val hasNextPage: Boolean,
    val currentPage: Int
)

data class ArtistSong(
    val title: String,
    val artists: String,
    val coverImage: String,
    val url: String
)
