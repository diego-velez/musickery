package com.diego.velez.musickery.discography

import com.diego.velez.musickery.utils.Logging
import java.io.File
import java.util.*

val COVER_ART_FOLDER = File("cover_art")

/**
 * Everything related to handling songs goes through here.
 */
object Discography {
    private val logger = Logging.getLogger(this)

    // Key is each song's absolute path's hash code
    private val _allSongs = Collections.synchronizedMap(mutableMapOf<Int, Song>())
    private val artistMap = Collections.synchronizedMap(mutableMapOf<String, Artist>())
    private val albumMap = Collections.synchronizedMap(mutableMapOf<String, Album>())

    val allSongs: Map<Int, Song>
        get() = _allSongs

    val discography: List<Artist.Serialized>
        get() = artistMap.values.map { it.serialized() }

    private var defaultMusicFolder: File? = null

    init {
        if (!COVER_ART_FOLDER.exists()) {
            COVER_ART_FOLDER.mkdir()
            logger.info("Created cover art folder at ${COVER_ART_FOLDER.absolutePath}")
        }
    }

    fun getDefaultMusicFolder(): File {
        if (defaultMusicFolder != null) {
            return defaultMusicFolder!!
        }

        val home = System.getProperty("user.home")
        val musicFolder = "$home/Music"
        logger.info("Default music folder: $musicFolder")
        defaultMusicFolder = File(musicFolder)
        return defaultMusicFolder!!
    }

    /**
     * Add [Song] to discography.
     */
    fun addSong(song: Song) {
        _allSongs[song.absolutePath.hashCode()] = song
        addToAlbum(song)
    }

    /**
     * Add [song] to its tagged [Album].
     */
    private fun addToAlbum(song: Song) {
        val songAlbum = song.tags[Tag.Name.ALBUM]?.getOrDefault("Text", "N/A") ?: "N/A"
        var album = albumMap[songAlbum]

        if (album == null) {
            album = Album(songAlbum)
            albumMap[songAlbum] = album
            addToArtist(song, album)
        }
        album.addSong(song)
    }

    /**
     * Add [album] to the [song]'s tagged [Artist].
     */
    private fun addToArtist(song: Song, album: Album) {
        val songArtist = song.tags[Tag.Name.ARTIST]?.getOrDefault("Text", "N/A") ?: "N/A"
        var artist = artistMap[songArtist]

        if (artist == null) {
            artist = Artist(songArtist)
            artistMap[songArtist] = artist
        }
        artist.addAlbum(album)
    }
}