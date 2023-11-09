package com.diego.velez.musickery.routes

import com.diego.velez.musickery.discography.Discography
import com.diego.velez.musickery.discography.Tag
import com.diego.velez.musickery.download.SongDownloader
import com.diego.velez.musickery.utils.Terminal
import io.ktor.http.*
import io.ktor.server.mustache.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.server.util.*
import io.ktor.sse.*
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.File


// Flow that emits the process of the current video download
// Each event should have the id as the song's unique key
private val downloadProcessChannel = MutableSharedFlow<ServerSentEvent>()

private data class TagsReceived(
    val link: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val imageLink: String,
) {
    fun getSongFile(): File {
        var downloadFolder = Discography.getDefaultMusicFolder().resolve(artist)
        if (album.isNotEmpty()) {
            downloadFolder = downloadFolder.resolve(album)
        }
    return downloadFolder.resolve("$artist - $title.mp3")
//        return File("$artist - $title.mp3")
    }
}

private suspend fun RoutingContext.getTags(): TagsReceived =
    call.receiveParameters().run {
        TagsReceived(
            getOrFail("link"),
            getOrFail("title"),
            getOrFail("artist"),
            getOrFail("album"),
            getOrFail("genre"),
            getOrFail("image-link"),
        )
    }

fun RootRoute.downloadRoute() {
    route("download") {
        get {
            val model = mutableMapOf<String, Any>()

            if (SongDownloader.downloadedSongs.isEmpty()) {
                model["title"] = "Musickery"
            } else {
                val title = SongDownloader.downloadedSongs.last().tags[Tag.Name.TITLE]
                    ?: SongDownloader.downloadedSongs.last().name  // Show file name instead of null

                model["title"] = "Downloaded $title"
            }

            model["songs"] = SongDownloader.downloadedSongs.map { it.serialized() }
            call.respond(MustacheContent("download/main.hbs", model))
        }

        sse("process") {
            downloadProcessChannel.collect {
                send(it)
            }
        }

        route("song") {
            post {
                // TODO: Replace song when it already exists, and apply previous song tags
                val tags = getTags()
                val resultPair = SongDownloader.download(tags.link, tags.getSongFile().absolutePath) {
                    val event = ServerSentEvent(
                        id = tags.getSongFile().absolutePath.hashCode().toString(),
                        data = it
                    )
                    downloadProcessChannel.emit(event)
                }

                if (resultPair.first is Terminal.Result.Failed) {
                    return@post call.respond(HttpStatusCode.InternalServerError)
                }

                val song = resultPair.second!!
                song.writeTag(Tag.Name.TITLE, tags.title)
                song.writeTag(Tag.Name.ARTIST, tags.artist)
                song.writeTag(Tag.Name.ALBUM, tags.album)
                song.writeTag(Tag.Name.GENRE, tags.genre)

                if (tags.imageLink.isNotBlank()) {
                    song.changeCoverArtToImageLink(tags.imageLink)
                }

                val songSerialized = song.serialized()
                val model = mapOf(
                    "hash" to songSerialized.hash,
                    "artist" to songSerialized.artist,
                    "album" to songSerialized.album,
                    "title" to songSerialized.title,
                )
                call.respond(MustacheContent("download/song_list_item.hbs", model))
            }

            post("id") {
                val tags = getTags()
                call.respond(tags.getSongFile().absolutePath.hashCode())
            }
        }
    }
}