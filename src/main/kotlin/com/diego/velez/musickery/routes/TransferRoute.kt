package com.diego.velez.musickery.routes

import com.diego.velez.musickery.android.ls
import io.ktor.http.*
import io.ktor.server.mustache.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun RootRoutingBuilder.transferRoute() {
    route("transfer") {
        get {
//            call.respond(MustacheContent("transfer/main.html", null))
            call.respondTemplate("transfer/main.html")
        }

        get("ls") {
            call.respondText(ls(), ContentType.Text.Plain)
        }
    }
}
