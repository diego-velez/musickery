package com.diego.velez.musickery.plugins

import com.diego.velez.musickery.routes.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        rootRoute()
        discographyRoute()
        songRoute()
        downloadRoute()
        transferRoute()
        staticResources("/static", "static")
    }
}
