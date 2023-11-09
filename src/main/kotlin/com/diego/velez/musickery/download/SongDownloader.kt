package com.diego.velez.musickery.download

import com.diego.velez.musickery.discography.Discography
import com.diego.velez.musickery.discography.Song
import com.diego.velez.musickery.utils.LineOutput
import com.diego.velez.musickery.utils.Logging
import com.diego.velez.musickery.utils.Terminal
import java.io.File

private val DOWNLOADS_FILE = File("logs/downloads.csv")

/**
 * Handles everything related to downloading.
 * Depends on [Discography]
 */
object SongDownloader {

    private val logger = Logging.getLogger(this)

    private var checkedDownloadFolder = false
    private val _downloadedSongs = mutableListOf<Int>()

    val downloadedSongs: List<Song>
        get() {
            // TODO: Maybe there is a better way to do this, checkedDownloadFolder needs
            // to be checked first, otherwise there will be an infinite loop
            if (!checkedDownloadFolder) {
                checkedDownloadFolder = true
                checkPreviousDownloads()
            }

            return Discography.allSongs
                .filter { _downloadedSongs.contains(it.key) }
                .values
                .toList()
        }

    /**
     * Downloads a song from a given url.
     *
     * @param link The link of the song to download.
     * @param fullPath The absolute path to where the song should be downloaded.
     * @param callback Callback for every yt-dlp line output.
     * @return A pair of the [Terminal.Result] and [Song] downloaded.
     * Song can be null if the [Terminal.Result] is [Terminal.Result.Failed].
     */
    suspend fun download(link: String, fullPath: String, callback: LineOutput): Pair<Terminal.Result, Song?> {
        logger.info("Downloading $link audio to $fullPath")
        val command = DownloadCommand(link)
            .extractAudio()
//            .keepVideo()
//            .simulate() // TODO: Come up with a step by step plan to test song downloads
            .audioFormat(DownloadCommand.AudioFormat.MP3)
            .downloadLocation(fullPath)
            .build()
        val result = Terminal.run(command, callback)

        var song: Song? = null
        when (result) {
            is Terminal.Result.Success -> {
                logger.info("Download successful")
                song = addDownload(fullPath)
            }

            is Terminal.Result.Failed -> {
                logger.severe("Download failed with ${result.exitCode} exit code")
            }
        }

        return Pair(result, song)
    }

    /**
     * Gets the previously downloaded songs by reading the logs/downloads.csv file.
     */
    private fun checkPreviousDownloads() {
        // Fix FileNotFoundException
        if (!DOWNLOADS_FILE.exists()) {
            DOWNLOADS_FILE.createNewFile()
        }

        var previouslyDownloadedSongs = 0
        DOWNLOADS_FILE.readLines().forEach {
            addSong(it)
            previouslyDownloadedSongs++
        }
        logger.info("Found $previouslyDownloadedSongs songs that were previously downloaded")
    }

    /**
     * Add song to [_downloadedSongs] and [Discography].
     */
    private fun addSong(fullPath: String): Song {
        // TODO: Handle FileNotFoundException when song file doesn't exist (happens when using simulate)
        val song = Song(fullPath)
        _downloadedSongs.add(song.absolutePath.hashCode())
        Discography.addSong(song)
        return song
    }

    /**
     * Calls [addSong] and writes it to the logs/downloads.csv file.
     */
    private fun addDownload(fullPath: String): Song {
        DOWNLOADS_FILE.appendText(fullPath + "\n")
        return addSong(fullPath)
    }
}