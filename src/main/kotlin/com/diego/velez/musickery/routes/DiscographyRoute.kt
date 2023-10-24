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
            call.respond(getDiscographyHTML(Discography.getDefaultMusicFolder()))
        }
        get("scan") {
            val musicFolder = File(call.request.queryParameters.getOrFail("folder"))
            // BUG: Crash if spammed
            Scanner.scan(musicFolder)
            call.respond(getDiscographyHTML(musicFolder))
        }
    }
}

fun getDiscographyHTML(musicFolder: File): MustacheContent {
    val model = mapOf(
        "default" to musicFolder.absolutePath,
        "artists" to Discography.discography
    )
    return MustacheContent("discography/main.hbs", model)
}