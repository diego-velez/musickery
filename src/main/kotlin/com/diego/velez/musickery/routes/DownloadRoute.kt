package com.diego.velez.musickery.routes

import com.diego.velez.musickery.discography.Discography
import com.diego.velez.musickery.discography.Song
import com.diego.velez.musickery.discography.Tag
import com.diego.velez.musickery.download.SongDownloader
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
                val tags = getTags()
                // TODO: Handle failed downloads
                SongDownloader.download(tags.link, tags.getSongFile().absolutePath) {
                    val event = ServerSentEvent(
                        id = tags.getSongFile().absolutePath.hashCode().toString(),
                        data = it
                    )
                    downloadProcessChannel.emit(event)
                }

                val song = Song(tags.getSongFile())
                song.writeTag(Tag.Name.TITLE, tags.title)
                song.writeTag(Tag.Name.ARTIST, tags.artist)
                song.writeTag(Tag.Name.ALBUM, tags.album)
                song.writeTag(Tag.Name.GENRE, tags.genre)

                if (tags.imageLink.isNotBlank()) {
                    song.changeCoverArtToImageLink(tags.imageLink)
                }

                // BUG: Songs are not sent with updated tags
                call.respond(SongDownloader.downloadedSongs.map { it.serialized() })
            }

            post("id") {
                val tags = getTags()
                call.respond(tags.getSongFile().absolutePath.hashCode())
            }

            // TODO: Maybe instead of sending the HTML of the list with all the downloaded songs,
            // We could return the JSong from downloadSong and just add that list item to the list.
            get("list") {
                val model = mapOf("songs" to SongDownloader.downloadedSongs.map { it.serialized() })
                call.respond(MustacheContent("download/song_list.hbs", model))
            }
        }
    }
}