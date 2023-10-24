package com.diego.velez.musickery.routes

import com.diego.velez.musickery.utils.Logging
import io.ktor.server.mustache.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun RootRoutingBuilder.rootRoute() {
    route("") {
        get {
            call.respond(MustacheContent("main.hbs", null))
        }

        get("logs") {
            call.respond(Logging.getLogs())
        }
    }
}