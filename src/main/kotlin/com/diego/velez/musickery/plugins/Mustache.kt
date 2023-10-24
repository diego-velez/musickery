package com.diego.velez.musickery.plugins

import com.github.mustachejava.DefaultMustacheFactory
import io.ktor.server.application.*
import io.ktor.server.mustache.*

fun Application.configureMustache() {
    install(Mustache) {
        mustacheFactory = DefaultMustacheFactory("templates")
    }
}