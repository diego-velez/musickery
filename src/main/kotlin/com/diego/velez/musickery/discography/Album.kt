package com.diego.velez.musickery.discography

import io.ktor.server.plugins.*
import kotlinx.serialization.Serializable

class Album(var name: String) {

//        private val songs = Collections.synchronizedList(mutableListOf<Song>())
    private val songs = mutableListOf<Song>()

    val s: List<Song>
        get() = songs

    fun addSong(song: Song) {
        songs.add(song)
    }

    fun getSong(name: String): Song {
        return songs.find { it.name == name } ?: throw NotFoundException("Could not find song: $name")
    }

    fun serialized(): Serialized {
        val l = mutableListOf<Song.Serialized>()
        for (song in songs) {
            l.add(song.serialized())
        }
        return Serialized(name, l)
    }

    @Serializable
    data class Serialized(val album: String, val songs: List<Song.Serialized>) {
        /**
         * Check if the songs on this album have a track number by checking to see how many songs have a track number,
         * if there is any song with a track number then it is ordered, else it is not.
         */
        val ordered: Boolean
            get() = songs.count { it.trackNumber != "N/A" } > 0
    }
}