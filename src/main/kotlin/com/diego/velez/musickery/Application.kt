package com.diego.velez.musickery

import com.diego.velez.musickery.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import java.util.logging.Level
import java.util.logging.Logger

fun main(args: Array<String>) {
    // Disable all jaudiotagger library logging to console
    val jAudioTaggerLogger = Logger.getLogger("org.jaudiotagger")
    jAudioTaggerLogger.level = Level.OFF
    EngineMain.main(args)
}

fun Application.module() {
    configureSSE()
    configureStatusPages()
    configureMustache()
    configureContentNegotiation()
    configureRouting()
}
