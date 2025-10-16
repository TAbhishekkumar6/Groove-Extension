package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.models.*
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder


fun artistNameToSlug(name: String): String {
    return name.trim()
        .lowercase()
        .replace(Regex("""[^a-z0-9\\s-]"""), "") 
        .replace(Regex("""\\s+"""), "-")
        .replace(Regex("""-+"""), "-") 
        .trim('-')
}
fun songResultToTrack(song: SongResult): Track {
    val artists = if (song.album.isNotEmpty()) {
        listOf(Artist(
            id = "",
            name = song.album,
            cover = null,
            bio = null,
            background = null,
            banners = emptyList(),
            subtitle = null,
            extras = emptyMap()
        ))
    } else emptyList()
    
    return Track(
        id = song.url,
        title = song.title,
        type = Track.Type.Song,
        cover = song.coverImage.toImageHolder(),
        artists = artists,
        album = if (song.album.isNotEmpty()) {
            Album(
                id = "",
                title = song.album,
                type = null,
                cover = song.coverImage.toImageHolder(),
                artists = emptyList(),
                trackCount = null,
                duration = null,
                releaseDate = null,
                description = null,
                background = null,
                label = null,
                isExplicit = false,
                subtitle = song.category,
                extras = emptyMap()
            )
        } else null,
        duration = null,
        playedDuration = null,
        plays = null,
        releaseDate = null,
        description = null,
        background = song.coverImage.toImageHolder(),
        genres = emptyList(),
        isrc = null,
        albumOrderNumber = null,
        albumDiscNumber = null,
        playlistAddedDate = null,
        isExplicit = false,
        subtitle = song.category,
        extras = mapOf(
            "url" to song.url,
            "category" to song.category
        ),
        isPlayable = Track.Playable.Yes,
        streamables = emptyList()
    )
}
fun songDetailToTrack(song: SongDetail): Track {
    val artistsList = song.singers.split(",").map { name ->
        val cleanName = name.trim()
        Artist(
            id = artistNameToSlug(cleanName),
            name = cleanName,
            cover = null,
            bio = null,
            background = null,
            banners = emptyList(),
            subtitle = null,
            extras = mapOf("slug" to artistNameToSlug(cleanName))
        )
    }
    
    val streamables = mutableListOf<Streamable>()
    
    if (song.url320kbps != null) {
        streamables.add(
            Streamable.server(
                id = "320kbps_${song.id}",
                quality = 320,
                title = "320kbps",
                extras = mapOf(
                    "url" to song.url320kbps,
                    "quality" to "320"
                )
            )
        )
    }
    if (song.url128kbps != null) {
        streamables.add(
            Streamable.server(
                id = "128kbps_${song.id}",
                quality = 128,
                title = "128kbps",
                extras = mapOf(
                    "url" to song.url128kbps,
                    "quality" to "128"
                )
            )
        )
    }
    return Track(
        id = song.url,
        title = song.title,
        type = Track.Type.Song,
        cover = song.coverImage.toImageHolder(),
        artists = artistsList,
        album = if (song.album.isNotEmpty()) {
            Album(
                id = "",
                title = song.album,
                type = null,
                cover = song.coverImage.toImageHolder(),
                artists = emptyList(),
                trackCount = null,
                duration = null,
                releaseDate = null,
                description = null,
                background = null,
                label = song.composer,
                isExplicit = false,
                subtitle = song.category,
                extras = emptyMap()
            )
        } else null,
        duration = null,
        playedDuration = null,
        plays = null,
        releaseDate = null,
        description = "${song.singers}\nComposer: ${song.composer}\nLyricist: ${song.lyricist}",
        background = song.coverImage.toImageHolder(),
        genres = listOf(song.category),
        isrc = null,
        albumOrderNumber = null,
        albumDiscNumber = null,
        playlistAddedDate = null,
        isExplicit = false,
        subtitle = song.singers,
        extras = mapOf(
            "url" to song.url,
            "singers" to song.singers,
            "composer" to song.composer,
            "lyricist" to song.lyricist,
            "category" to song.category,
            "year" to song.year,
            "releaseDate" to song.releaseDate,
            "leadStars" to song.leadStars
        ),
        isPlayable = Track.Playable.Yes,
        streamables = streamables
    )
}
fun albumResultToAlbum(album: AlbumResult): Album {
    return Album(
        id = album.url,
        title = album.title,
        type = null,
        cover = album.coverImage.toImageHolder(),
        artists = emptyList(),
        trackCount = null,
        duration = null,
        releaseDate = null,
        description = null,
        background = album.coverImage.toImageHolder(),
        label = null,
        isExplicit = false,
        subtitle = album.category,
        extras = mapOf(
            "url" to album.url,
            "category" to album.category
        )
    )
}
fun albumDetailToAlbum(album: AlbumDetail): Album {
    val artistsList = album.artists.split(",").map { name ->
        val cleanName = name.trim()
        Artist(
            id = artistNameToSlug(cleanName),
            name = cleanName,
            cover = null,
            bio = null,
            background = null,
            banners = emptyList(),
            subtitle = null,
            extras = mapOf("slug" to artistNameToSlug(cleanName))
        )
    }
    
    return Album(
        id = album.url,
        title = album.name,
        type = null,
        cover = album.coverImage.toImageHolder(),
        artists = artistsList,
        trackCount = album.songs.size.toLong(),
        duration = null,
        releaseDate = null,
        description = "Artists: ${album.artists}\nStarcast: ${album.starcast}\nComposers: ${album.composers}",
        background = album.coverImage.toImageHolder(),
        label = album.composers,
        isExplicit = false,
        subtitle = "${album.category} • ${album.year}",
        extras = mapOf(
            "url" to album.url,
            "artists" to album.artists,
            "starcast" to album.starcast,
            "composers" to album.composers,
            "year" to album.year,
            "category" to album.category
        )
    )
}
fun homeItemToMedia(item: HomeItem): EchoMediaItem {
    return if (item.type == "song") {
        Track(
            id = item.url,
            title = item.title,
            type = Track.Type.Song,
            cover = item.coverImage.toImageHolder(),
            artists = item.subtitle.split(",").map { name ->
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
            album = null,
            duration = null,
            playedDuration = null,
            plays = null,
            releaseDate = null,
            description = null,
            background = item.coverImage.toImageHolder(),
            genres = emptyList(),
            isrc = null,
            albumOrderNumber = null,
            albumDiscNumber = null,
            playlistAddedDate = null,
            isExplicit = false,
            subtitle = item.subtitle,
            extras = mapOf("url" to item.url),
            isPlayable = Track.Playable.Yes,
            streamables = emptyList()
        )
    } else {
        Album(
            id = item.url,
            title = item.title,
            type = null,
            cover = item.coverImage.toImageHolder(),
            artists = item.subtitle.split(",").map { name ->
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
            trackCount = null,
            duration = null,
            releaseDate = null,
            description = null,
            background = item.coverImage.toImageHolder(),
            label = null,
            isExplicit = false,
            subtitle = item.subtitle,
            extras = mapOf("url" to item.url)
        )
    }
}
fun artistSongToTrack(song: ArtistSong, artistName: String): Track {
    val artistsList = song.artists.split(",").map { name ->
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
    }
    
    return Track(
        id = song.url,
        title = song.title,
        type = Track.Type.Song,
        cover = song.coverImage.toImageHolder(),
        artists = artistsList,
        album = null,
        duration = null,
        playedDuration = null,
        plays = null,
        releaseDate = null,
        description = null,
        background = song.coverImage.toImageHolder(),
        genres = emptyList(),
        isrc = null,
        albumOrderNumber = null,
        albumDiscNumber = null,
        playlistAddedDate = null,
        isExplicit = false,
        subtitle = song.artists,
        extras = mapOf(
            "url" to song.url,
            "artistName" to artistName
        ),
        isPlayable = Track.Playable.Yes,
        streamables = emptyList()
    )
}
fun categoryItemToMedia(item: CategoryItem): EchoMediaItem {
    return if (item.type == "song") {
        Track(
            id = item.url,
            title = item.title,
            type = Track.Type.Song,
            cover = item.coverImage.toImageHolder(),
            artists = item.subtitle.split(",").map { name ->
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
            album = null,
            duration = null,
            playedDuration = null,
            plays = null,
            releaseDate = null,
            description = null,
            background = item.coverImage.toImageHolder(),
            genres = emptyList(),
            isrc = null,
            albumOrderNumber = null,
            albumDiscNumber = null,
            playlistAddedDate = null,
            isExplicit = false,
            subtitle = item.subtitle,
            extras = mapOf("url" to item.url),
            isPlayable = Track.Playable.Yes,
            streamables = emptyList()
        )
    } else {
        Album(
            id = item.url,
            title = item.title,
            type = null,
            cover = item.coverImage.toImageHolder(),
            artists = item.subtitle.split(",").map { name ->
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
            trackCount = null,
            duration = null,
            releaseDate = null,
            description = null,
            background = item.coverImage.toImageHolder(),
            label = null,
            isExplicit = false,
            subtitle = item.subtitle,
            extras = mapOf("url" to item.url)
        )
    }
}
