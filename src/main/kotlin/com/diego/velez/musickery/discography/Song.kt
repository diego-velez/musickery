package com.diego.velez.musickery.discography

import kotlinx.serialization.Serializable
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.id3.ID3v24Tag
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File
import java.net.URL
import javax.imageio.ImageIO

class Song(fullPath: String) : File(fullPath) {
    constructor(file: File) : this(file.absolutePath)

    // The key is the type of tag (artist, album, etc.); the value map, represents the type of value
    // and the value itself (E.g. Text=2 Chainz)
    private val _tags: MutableMap<Tag.Name, MutableMap<String, String>> = mutableMapOf()
    private val audioFile: AudioFile = AudioFileIO.read(this)
    private val coverArtFile = COVER_ART_FOLDER.resolve(absolutePath.hashCode().toString())

    val tags: Map<Tag.Name, Map<String, String>>
        get() = _tags

    init {
        val tag = audioFile.tag
        for (field in tag.fields) {
            val fieldName = Tag.Name.fromId(field.id)
            _tags[fieldName] = getFieldValues(fieldName)
        }
    }

    private fun getFieldValues(field: Tag.Name): MutableMap<String, String> {
        val fieldValues = mutableMapOf<String, String>()

        // Get an easily readable version for the computer
        val tags = audioFile.tag.getFields(field.id)
            .joinToString(separator = "") { it.toString().trim() }
            .split("; ")

        // Remove quotes and ; from each tag's body
        for (value in tags) {
            val typeValue = value.split("=")
            val bodyDatatype = typeValue.first()
            val body = typeValue.last()
                .removePrefix("\"")
                .removeSuffix(";").removeSuffix("\"")
            fieldValues[bodyDatatype] = body
        }

        return fieldValues
    }

    fun changeCoverArtToImageLink(link: String): File {
        val url = URL(link)
        val image = ImageIO.read(url)
        ImageIO.write(image, "jpg", coverArtFile)

        val tag = audioFile.tag
        tag.deleteArtworkField()
        tag.addField(ArtworkFactory.createArtworkFromFile(coverArtFile))
        audioFile.commit()

        _tags[Tag.Name.COVER_ART] = getFieldValues(Tag.Name.COVER_ART)

        return coverArtFile
    }

    fun coverArtFile(): File? {
        if (coverArtFile.exists()) {
            return coverArtFile
        }

        val artwork = audioFile.tag.firstArtwork ?: return null
        coverArtFile.writeBytes(artwork.binaryData)
        return coverArtFile
    }

    fun writeTag(name: Tag.Name, value: String) {
        // Skip if there is a manual COVER_ART tag edit, if the tag has not changed or the value is blank
        if (name == Tag.Name.COVER_ART || _tags[name]?.get("Text") == value || value.isBlank()) {
            return
        }

        // BUG: Year isn't saving when empty
        val tag = audioFile.tag as ID3v24Tag

        if (tag.hasField(name.id)) {
            tag.deleteField(name.id)
        }

        tag.addField(name.fieldKey, value)
        audioFile.commit()

        if (_tags.containsKey(name)) {
            _tags[name]!!["Text"] = value
        } else {
            _tags[name] = mutableMapOf("Text" to value)
        }
    }

    fun deleteTag(name: Tag.Name) {
        val tag = audioFile.tag
        tag.deleteField(name.id)
        audioFile.commit()
        _tags.remove(name)
    }

    fun serialized(): Serialized {
        return Serialized(
            absolutePath,
            _tags[Tag.Name.ARTIST]?.getOrDefault("Text", "N/A") ?: "N/A",
            _tags[Tag.Name.TITLE]?.getOrDefault("Text", "N/A") ?: "N/A",
            _tags[Tag.Name.ALBUM]?.getOrDefault("Text", "N/A") ?: "N/A",
            _tags[Tag.Name.TRACK_NUMBER]?.getOrDefault("Text", "N/A") ?: "N/A",
            _tags.map { Tag(it.key, it.value) }.map { it.serialized() },
            absolutePath.hashCode()
        )
    }

    @Serializable
    data class Serialized(
        val absolutePath: String,
        val artist: String,
        val title: String,
        val album: String,
        val trackNumber: String,
        val tags: List<Tag.Serialized>,
        val hash: Int
    ) {
        val uniqueTitle = "$artist - $title"
    }
}
