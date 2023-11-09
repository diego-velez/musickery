package com.diego.velez.musickery.discography

import kotlinx.serialization.Serializable

class Artist(var name: String) {
    private val albums = mutableListOf<Album>()

    fun addAlbum(album: Album) {
        albums.add(album)
    }

    fun getAlbum(name: String): Album? {
        return albums.find { it.name == name }
    }

    fun serialized(): Serialized {
        return Serialized(name, albums.map { it.serialized() })
    }

    @Serializable
    data class Serialized(val artist: String, val albums: List<Album.Serialized>)
}