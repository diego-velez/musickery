package com.diego.velez.musickery.routes

import com.diego.velez.musickery.discography.Discography
import com.diego.velez.musickery.discography.Song
import com.diego.velez.musickery.discography.Tag
import com.diego.velez.musickery.download.SongDownloader
import com.diego.velez.musickery.utils.Terminal
import io.ktor.server.mustache.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.server.util.*
import io.ktor.util.*
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.File


// Flow that emits the process of the current video download
// Should be sent to client via SSE
private val downloadProcessChannel = MutableSharedFlow<String>()

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
                // TODO: Apply tags
                val link: String
                val title: String
                val artist: String
                val album: String
                val genre: String
                val imageLink: String

                call.receiveParameters().run {
                    link = getOrFail("link")
                    title = getOrFail("title")
                    artist = getOrFail("artist")
                    album = getOrFail("album")
                    genre = getOrFail("genre")
                    imageLink = getOrFail("image-link")
                }

                // TODO: finish download implementation
                var downloadFolder = Discography.getDefaultMusicFolder().resolve(artist)
                if (album.isNotEmpty()) {
                    downloadFolder = downloadFolder.resolve(album)
                }
//
                val songFile = downloadFolder.resolve("$artist - $title.mp3")
//                val songFile = File("$artist - $title.mp3")

                // TODO: Handle failed downloads
                SongDownloader.download(link, songFile.absolutePath) {
                    downloadProcessChannel.emit(it)
                }

                val song = Song(songFile)
                song.writeTag(Tag.Name.TITLE, title)
                song.writeTag(Tag.Name.ARTIST, artist)
                song.writeTag(Tag.Name.ALBUM, album)
                song.writeTag(Tag.Name.GENRE, genre)
                song.changeCoverArtToImageLink(imageLink)

                // BUG: Songs are not sent with updated tags
                call.respond(SongDownloader.downloadedSongs.map { it.serialized() })
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