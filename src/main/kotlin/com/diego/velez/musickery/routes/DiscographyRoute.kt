package com.diego.velez.musickery.routes

import com.diego.velez.musickery.discography.Discography
import com.diego.velez.musickery.discography.Scanner
import io.ktor.server.mustache.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import java.io.File

fun RootRoutingBuilder.discographyRoute() {
    route("discography") {
        get {
            val model = mapOf(
                "default" to Discography.getDefaultMusicFolder().absolutePath,
                "artists" to Discography.discography
            )
            call.respond(MustacheContent("discography/main.hbs", model))
        }
        get("scan") {
            val musicFolder = File(call.request.queryParameters.getOrFail("folder"))
            Scanner.scan(musicFolder)
            val model = mapOf("artists" to Discography.discography)
            call.respond(MustacheContent("discography/artists.hbs", model))
        }
    }
}