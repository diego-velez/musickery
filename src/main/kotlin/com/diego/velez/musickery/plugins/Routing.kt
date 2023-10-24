package com.diego.velez.musickery.plugins

import com.diego.velez.musickery.routes.discographyRoute
import com.diego.velez.musickery.routes.downloadRoute
import com.diego.velez.musickery.routes.rootRoute
import com.diego.velez.musickery.routes.songRoute
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        rootRoute()
        discographyRoute()
        songRoute()
        downloadRoute()
        staticResources("/static", "static")
    }
}
