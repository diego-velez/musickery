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
import java.util.concurrent.atomic.AtomicInteger

// Flow that emits all the output from the terminal commands
// It is sent to the client via SSE
private val outputChannel = MutableSharedFlow<String>()

private val USER_MUSIC = File("/storage/emulated/0/Music")
private val skippedSongTransfersCount = AtomicInteger(0)

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
            val filesInPhone = ls {
                outputChannel.emit(it)
            }

            if (filesInPhone.isEmpty()) {
                outputChannel.emit("Folder is empty")
            }

            call.respond(HttpStatusCode.OK)
        }

        get("to-device") {
            skippedSongTransfersCount.set(0)
            Discography.getDefaultMusicFolder()
                .listFiles()!!
                .map { getAllFilesThatNeedTransfer(ls(), it) }
                .flatMap { it.await() }
                .also {
                    if (it.isEmpty()) {
                        outputChannel.emit("No songs needed to transfer.")
                    } else {
                        outputChannel.emit("Skipped ${skippedSongTransfersCount.get()} songs that were already in the phone.")
                        outputChannel.emit("Transferring ${it.size} songs.")
                    }
                }
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
 * Transfers a file on pc to the android device connected.
 * @param file The [File] on pc to transfer.
 */
private fun CoroutineScope.processTransfer(file: File): Deferred<Unit> = async {
    val phonePath = if (file.parentFile == Discography.getDefaultMusicFolder()) {
        USER_MUSIC.absolutePath
    } else {
        val relativePath = file.relativeTo(Discography.getDefaultMusicFolder()).parent
        USER_MUSIC.resolve(relativePath).absolutePath
    }
    outputChannel.emit("Transferring $file to $phonePath")
    val terminalCommand = listOf("adb", "push", file.absolutePath, phonePath)
    Terminal.run(terminalCommand) { output ->
        outputChannel.emit(output)
    }
}

/**
 * Gets all the files that need transfer within [fileInPc].
 * @param filesInPhone A [List] of each file in the phone's folder that matches
 * the relative location of [fileInPc].
 * @param fileInPc The pc file to check if it is already in the phone.
 * This should match the [filesInPhone]'s relative location on phone.
 * @return A [List] of [File] where each file represents a file object on the pc
 * that needs to be transferred to the phone.
 */
private fun CoroutineScope.getAllFilesThatNeedTransfer(
    filesInPhone: List<String>,
    fileInPc: File
): Deferred<List<File>> = async {
    if (!filesInPhone.contains(fileInPc.name)) {
        return@async listOf(fileInPc)
    }

    // Recursively check all files/folders to see if they need to be transferred
    if (fileInPc.isDirectory) {
        val relativePath = fileInPc.toRelativeString(Discography.getDefaultMusicFolder())
        return@async fileInPc
            .listFiles()!!
            .map { getAllFilesThatNeedTransfer(ls(relativePath), it) }
            .flatMap { it.await() }
    }

    val pcFileShaResult = sha1sum(fileInPc.absolutePath)
    val relativeSongPath = fileInPc.toRelativeString(Discography.getDefaultMusicFolder())
    val phoneFileShaResult = sha1sum(USER_MUSIC.resolveWithQuotes(relativeSongPath), true)

    // If the files hash does not match, then there is a tag difference, thus a transfer is needed.
    if (pcFileShaResult != phoneFileShaResult) {
        return@async listOf(fileInPc)
    }

    skippedSongTransfersCount.incrementAndGet()
    return@async emptyList()
}

/**
 * Returns a list where each item is a file in [USER_MUSIC]/[relativePath].
 * @param relativePath Relative path to [USER_MUSIC] folder to ls into.
 * Default is [USER_MUSIC] itself.
 * @param callback [LineOutput] callback for [Terminal.run].
 * @return [List] of each file.
 */
suspend fun ls(relativePath: String = "", callback: LineOutput = {}): List<String> {
    val terminalCommand = listOf("adb", "shell", "ls", "-a", USER_MUSIC.resolveWithQuotes(relativePath))
    return Terminal.run(terminalCommand, callback).output.split("\n")
}

/**
 * Check the sha1 sum of a file at [path].
 * Uses the `sha1sum` terminal command.
 * @param path The absolute path of the file to check the sha1 sum of.
 * @param onPhone Checks for the file at [path] in the phone using adb.
 * @return The sha1 sum of the file at [path].
 */
suspend fun sha1sum(path: String, onPhone: Boolean = false): String {
    val fileShaCommand = mutableListOf("sha1sum", path)

    if (onPhone) {
        fileShaCommand.add(0, "adb")
        fileShaCommand.add(1, "shell")
        println(fileShaCommand)
    }

    return Terminal.run(fileShaCommand).output.split(" ").first()
}

/**
 * Surround [File.resolve] absolute path with quotes.
 * Needed for when strings with spaces are used in terminal commands.
 */
private fun File.resolveWithQuotes(path: String): String = "\"${this.resolve(path).absolutePath}\""