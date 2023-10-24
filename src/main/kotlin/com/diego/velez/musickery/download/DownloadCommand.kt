package com.diego.velez.musickery.download

private const val PROGRAM = "yt-dlp"
private const val EXTRACT_AUDIO_FLAG = "--extract-audio"
private const val AUDIO_FORMAT_FLAG = "--audio-format"
private const val DOWNLOAD_LOCATION_FLAG = "--output"
private const val OUTPUT_JSON_FLAG = "--dump-json"
private const val SIMULATE_FLAG = "--simulate"
private const val NO_SIMULATE_FLAG = "--no-simulate"
private const val KEEP_VIDEO_FLAG = "--keep-video"

class DownloadCommand(private val link: String) {

    private val args = mutableListOf<String>()
    private var simulate = false

    fun extractAudio(): DownloadCommand {
        args.add(EXTRACT_AUDIO_FLAG)
        return this
    }

    enum class AudioFormat {
        AAC, FLAC, MP3, M4A, OPUS, VORBIS, WAV, ALAC
    }

    fun audioFormat(format: AudioFormat): DownloadCommand {
        args.add(AUDIO_FORMAT_FLAG)
        args.add(format.name.lowercase())
        return this
    }

    fun downloadLocation(fullPath: String): DownloadCommand {
        args.add(DOWNLOAD_LOCATION_FLAG)
        args.add(fullPath)
        return this
    }

    fun outputJSON(): DownloadCommand {
        args.add(OUTPUT_JSON_FLAG)
        return this
    }

    fun simulate(): DownloadCommand {
        simulate = true
        return this
    }

    fun keepVideo(): DownloadCommand {
        args.add(KEEP_VIDEO_FLAG)
        return this
    }

    /**
     * Finishes building the command, and returns a list of the command and its arguments.
     */
    fun build(): List<String> {
        val result = mutableListOf(PROGRAM)
        result.addAll(args)

        // Specify simulate
        result.add(if (simulate) SIMULATE_FLAG else NO_SIMULATE_FLAG)

        result.add(link)
        return result
    }

    override fun toString(): String = build().joinToString(separator = " ")
}