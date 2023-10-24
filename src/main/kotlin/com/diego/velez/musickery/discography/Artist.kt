package com.diego.velez.musickery.discography

import io.ktor.server.plugins.*
import kotlinx.serialization.Serializable


class Artist(var name: String) {

//        private val albums = Collections.synchronizedList(mutableListOf<Album>())
    private val albums = mutableListOf<Album>()

    fun addAlbum(album: Album) {
        albums.add(album)
    }

    fun getAlbum(name: String): Album {
        return albums.find { it.name == name } ?: throw NotFoundException("Could not find album: $name")
    }

    fun serialized(): Serialized {
        return Serialized(name, albums.map { it.serialized() })
    }

    @Serializable
    data class Serialized(val artist: String, val albums: List<Album.Serialized>)
}