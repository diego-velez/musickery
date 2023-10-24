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
     *
     * Automatically adds all songs found to the [Discography]
     */
    suspend fun scan(musicFolder: File) =
        coroutineScope {
            logger.info("Scanning $musicFolder for songs...")

            val asyncJobs = musicFolder.listFiles()!!.map { file ->
                processFile(file)
            }

            asyncJobs.forEach { it.await() }

            if (jobsLeft.get() == 0) {
                logger.info("Found $songsFound songs")
            }
        }

    /**
     * Processes each [File] object withing the music folder.
     *
     * If the [File] is a folder, then it'll go through each [File] object within, assigning
     * a new [processFile] task to each folder or calling [foundSong] to each file.
     */
    private fun CoroutineScope.processFile(file: File): Deferred<Unit> = async {
        jobsLeft.incrementAndGet()

        if (file.isDirectory) {
            file.listFiles()!!
                .forEach {
                    if (it.isFile) {
                        foundSong(it)
                    } else {
                        processFile(it).start()
                    }
                }
        } else {
            foundSong(file)
        }

        jobsLeft.decrementAndGet()
    }

    /**
     * Checks if [songFile] is a valid [Song], then adds it to the [Discography].
     */
    private fun foundSong(songFile: File) {
        if (songFile.extension == "mp3" || songFile.extension == "ogg") {
            val song = Song(songFile)
            Discography.addSong(song)
            songsFound.incrementAndGet()
        }
    }
}
