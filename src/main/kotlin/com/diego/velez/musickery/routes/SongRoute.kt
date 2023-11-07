package com.diego.velez.musickery.routes

import com.diego.velez.musickery.discography.Discography
import com.diego.velez.musickery.discography.Scanner
import com.diego.velez.musickery.discography.Tag
import com.diego.velez.musickery.download.SongDownloader
import io.ktor.http.*
import io.ktor.server.mustache.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun RootRoute.songRoute() {
    route("song") {
        get("{songHash}") {
            // Server restarted and page refresh, so no songs have been scanned yet
            if (Discography.allSongs.isEmpty()) {
                Scanner.scan(Discography.getDefaultMusicFolder())
                SongDownloader.downloadedSongs
            }

            val songHash = call.parameters.getOrFail("songHash").toInt()
            val song = Discography.allSongs[songHash]!!.serialized()

            call.respond(MustacheContent("song/main.hbs", mapOf("song" to song)))
        }

        route("cover_art") {
            get("{songHash}") {
                val songHash = call.parameters.getOrFail("songHash").toInt()
                val song = Discography.allSongs[songHash]!!
                if (!song.coverArtFile.exists()) {
                    return@get call.respond(HttpStatusCode.NotFound)
                }
                call.respondBytes(song.coverArtFile.readBytes(), ContentType.Image.JPEG)
            }
            post("new/{songHash}") {
                val songHash = call.parameters.getOrFail("songHash").toInt()
                val imageLink = call.receiveText()
                val song = Discography.allSongs[songHash]!!
                call.respondBytes(song.changeCoverArtToImageLink(imageLink).readBytes(), ContentType.Image.JPEG)
            }
        }

        route("tags") {
            /**
             * Gets the tags that the song doesn't have yet in order to populate the add tags popup menu
             */
            get("{songHash}") {
                val songHash = call.parameters.getOrFail("songHash").toInt()
                val song = Discography.allSongs[songHash]!!
                val tags = Tag.Name.entries.filterNot { tagName ->
                    song.tags.containsKey(tagName) || tagName == Tag.Name.UNKNOWN
                }
                call.respondText(Json.encodeToString(tags), ContentType.Application.Json)
            }

            /**
             * Delete a specific tag from a song
             */
            delete("{songHash}") {
                // BUG: Reload seems like it didn't delete
                val tagName = call.receiveText()
                val tag = Tag.Name.entries.find { it.name == tagName }!!
                val songHash = call.parameters.getOrFail("songHash").toInt()
                val song = Discography.allSongs[songHash]!!
                song.deleteTag(tag)
                call.respond(HttpStatusCode.OK)
            }

            /**
             * Write/edit a specific tag from a song
             */
            put("{songHash}") {
                // BUG: Doesn't seem to write tag
                val songHash = call.parameters.getOrFail("songHash").toInt()
                val data = call.receive<Tag.Serialized.TagMap>()
                val song = Discography.allSongs[songHash]!!
                val tagName = Tag.Name.entries.find { it.name == data.type }
                    ?: return@put call.respond(HttpStatusCode.NotAcceptable)
                song.writeTag(tagName, data.value)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}