package com.diego.velez.musickery.routes

import com.diego.velez.musickery.discography.Discography
import com.diego.velez.musickery.utils.LineOutput
import com.diego.velez.musickery.utils.Terminal
import io.ktor.http.*
import io.ktor.server.mustache.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.File

// Flow that emits all the output from the terminal commands
// It is sent to the client via SSE
private val outputChannel = MutableSharedFlow<String>()

/** Tendremos que usar 'adb' command en terminal para send and receive files while phone is connected to PC via USB
 * default android user dir: /storage/emulated/0
 * adb shell ls -a <dir> - list files en dir
 * adb push <pc dir> <phone dir> - transfer files de pc dir a phone dir
 * adb pull <phone dir> <pc dir> - transfer files de phone dir a pc dir
 */
fun RootRoute.transferRoute() {
    route("transfer") {
        get {
            call.respondTemplate("transfer/main.html")
        }

        get("ls") {
            val result = ls {
                outputChannel.emit(it)
            }

            if (result.output.isBlank()) {
                outputChannel.emit("Folder is empty")
            }

            call.respond(HttpStatusCode.OK)
        }

        get("to-device") {
            val lsOutput = ls().output
            val filesInPhone = lsOutput.split("\n")
            Discography.getDefaultMusicFolder()
                .listFiles()!!
                .filterNotWithCallback(
                    { filesInPhone.contains(it.name) },
                    { outputChannel.emit("${it.name} already in phone, skipping.") })
                .map { processTransfer(it) }
                .forEach { it.await() }
            call.respond(HttpStatusCode.OK)
        }

        get("devices") {
            val devicesOutput = Terminal.run(listOf("adb", "devices")).output
            val outputLines = devicesOutput.split("\n")

            // Skip the output header
            val rawDevicesOutput = outputLines.subList(1, outputLines.size)
            val formattedOutput = rawDevicesOutput.map {
                it.replace("\t", " (") + ")"
            }
            call.respond(formattedOutput)
        }

        sse("output") {
            outputChannel.collect {
                send(it)
            }
        }
    }
}

/**
 * Same as [filterNot] but has a [callback] that is called when [predicate] is true.
 */
private suspend fun <T> Array<out T>.filterNotWithCallback(
    predicate: (T) -> Boolean,
    callback: suspend (T) -> Unit
): List<T> {
    val destination = ArrayList<T>()
    for (element in this) {
        if (!predicate(element)) {
            destination.add(element)
        } else {
            callback(element)
        }
    }
    return destination
}

/**
 * Transfers [file] to the android device.
 */
private fun CoroutineScope.processTransfer(file: File): Deferred<Unit> = async {
    val terminalCommand = listOf("adb", "push", file.absolutePath, "/storage/emulated/0/Music")
    outputChannel.emit("Transferring ${file.absolutePath}")
    Terminal.run(terminalCommand) { output ->
        outputChannel.emit(output)
    }
}

suspend fun ls(callback: LineOutput = {}): Terminal.Result {
    val terminalCommand = "adb shell ls -a /storage/emulated/0/Music".split(" ")
    return Terminal.run(terminalCommand, callback)
}