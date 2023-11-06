package com.diego.velez.musickery.routes

import com.diego.velez.musickery.discography.Discography
import com.diego.velez.musickery.utils.Terminal
import io.ktor.http.*
import io.ktor.server.mustache.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import kotlinx.coroutines.flow.MutableSharedFlow

// Flow that emits all the output from the terminal commands
// It is sent to the client via SSE
private val outputChannel = MutableSharedFlow<String>()

/** Tendremos que usar 'adb' command en terminal para send and receive files while phone is connected to PC via USB
 * default android user dir: /storage/emulated/0
 * adb shell ls -a <dir> - list files en dir
 * adb push <pc dir> <phone dir> - transfer files de pc dir a phone dir
 * adb pull <phone dir> <pc dir> - transfer files de phone dir a pc dir
 */
fun RootRoutingBuilder.transferRoute() {
    route("transfer") {
        get {
            call.respondTemplate("transfer/main.html")
        }

        get("ls") {
            val terminalCommand = "adb shell ls -a /storage/emulated/0/Music".split(" ")
            val terminalResult = Terminal.run(terminalCommand) {
                outputChannel.emit(it)
            }
            call.respond(HttpStatusCode.OK)
        }

        get("to-device") {
            // TODO: Run each transfer asynchronously
            Discography.getDefaultMusicFolder()
                .listFiles()!!
                .forEach {
                    val terminalCommand = "adb push ${it.absolutePath} /storage/emulated/0/Music".split(" ")
                    outputChannel.emit("Transferring ${it.absolutePath}")
                    Terminal.run(terminalCommand) { output ->
                        outputChannel.emit(output)
                    }
                }

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