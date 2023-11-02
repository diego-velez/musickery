package com.diego.velez.musickery.discography

import com.diego.velez.musickery.utils.Logging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * Handlers everything related to scanning the hard drive for songs.
 * Depends on [Discography]
 */
object Scanner {
    private val logger = Logging.getLogger(this)

    private val songsFound = AtomicInteger(0)
    private val jobsLeft = AtomicInteger(0)

    /**
     * Scans [musicFolder] recursively for all song files.
     * Automatically adds all songs found to the [Discography].
     * @param musicFolder The absolute path of the music folder to scan the songs of.
     * @return The amount of songs that the scan found.
     */
    suspend fun scan(musicFolder: File): Int =
        coroutineScope {
            logger.info("Scanning $musicFolder for songs...")
            songsFound.set(0)

            val asyncJobs = musicFolder.listFiles()!!.map { processFile(it) }
            asyncJobs.forEach { it.await() }

            logger.info("Found $songsFound songs")
            return@coroutineScope songsFound.get()
        }

    /**
     * Processes each [File] object within the music folder.
     *
     * If the [File] is a folder, then it'll go through each [File] object within, assigning
     * a new [processFile] task to each folder or calling [foundSong] to each file.
     */
    private fun CoroutineScope.processFile(file: File): Deferred<Unit> = async {
        jobsLeft.incrementAndGet()

        if (file.isFile) {
            foundSong(file)
        } else {
            file.listFiles()!!.forEach { processFile(it).start() }
        }

        jobsLeft.decrementAndGet()
    }

    /**
     * Checks if [songFile] is a valid [Song], then adds it to the [Discography].
     */
    private fun foundSong(songFile: File) {
        // Do not add song if it was already added
        if (Discography.allSongs.containsKey(songFile.absolutePath.hashCode())) {
            return
        }

        if (songFile.extension == "mp3" || songFile.extension == "ogg") {
            val song = Song(songFile)
            Discography.addSong(song)
            songsFound.incrementAndGet()
        }
    }
}
