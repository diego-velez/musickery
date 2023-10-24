package com.diego.velez.musickery.routes

import io.ktor.server.response.*
import io.ktor.server.routing.*

fun RootRoutingBuilder.transferRoute() {
    route("transfer") {
        get {
            call.respondText("we in transfer baby!")
        }
    }
}
