package com.diego.velez.musickery.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*

fun Application.configureStatusPages() {
    install(StatusPages) {
        status(HttpStatusCode.NotFound) { call, status ->
            call.respondText("Route ${call.request.uri} doesn't exist", status = status)
        }
        exception<Throwable> { call, throwable ->
            call.respondText(
                "Internal Server Error (500)\n${throwable.stackTraceToString()}",
                status = HttpStatusCode.InternalServerError
            )
        }
    }
}