package com.diego.velez.musickery.utils

import com.diego.velez.musickery.discography.Tag
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import java.io.File
import java.net.URLDecoder
import java.util.logging.Level
import java.util.logging.Logger

fun disableLoggers() {
    val jAudioTaggerLogger = Logger.getLogger("org.jaudiotagger")
    jAudioTaggerLogger.level = Level.OFF
}

fun AudioFile.getAllTags(): MutableSet<Tag> {
    val allTags = mutableSetOf<Tag>()

    for (field in tag.fields) {
        // Get an easily readable version for the computer
        val tags = tag.getFields(field.id)
            .joinToString(separator = "") { it.toString().trim() }
            .split("; ")

        // Remove quotes and ; from each tag's body
        val tagMap = mutableMapOf<String, String>()
        for (value in tags) {
            val typeValue = value.split("=")
            val bodyDatatype = typeValue.first()
            val body = typeValue.last()
                .removePrefix("\"")
                .removeSuffix(";").removeSuffix("\"")
            tagMap[bodyDatatype] = body
        }
        allTags.add(Tag(field.id, tagMap))
    }

    return allTags
}

//fun AudioFile.notSupportedTags(): List<String> {
//    val result = mutableListOf<String>()
//    val tags = getAllTags()
//
//    for (tag in tags) {
//        val supportedTags = Tag.values().map { it.id }
//        if (!supportedTags.contains(tag.key)) {
//            result.add(tag.key)
//        }
//    }
//
//    return result
//}

fun AudioFile.deleteTag(tag: String): Boolean {
    this.tag.deleteField(tag)
    commit()

    // Read file again to make sure that the tag was deleted
    val audioFile = AudioFileIO.read(this.file)
    return !audioFile.tag.hasField(tag)
}

fun needsURLEncoding(input: String): Boolean {
    val encodedCharacters = setOf(' ', '<', '>', '#', '%', '"', '{', '}', '|', '\\', '^', '~', '[', ']', '`', '/')

    for (char in input) {
        if (char in encodedCharacters) {
            return true
        }
    }

    return false
}

fun isOk(file: File): Boolean {
    val decoded = try {
        URLDecoder.decode(file.name, "UTF-8")
    } catch (e: IllegalArgumentException) {
        println("Illegal for $file")
        return true
    }
    return decoded.equals(file.name)
}

fun main() {
    File("/home/dvt/Music").walkTopDown().forEach { file ->
        if (!isOk(file)) {
            println("Fixing ${file.absolutePath}")
            val newName = URLDecoder.decode(file.name, "UTF-8")
            val newFile = file.toPath().toAbsolutePath().parent.resolve(newName).toFile()

            if (newName.contains("/")) {
                println("oh no")
            }
            println("From '${file.name}' to '${newFile.name}'\n")
//            file.renameTo(newFile)
        }
    }
}