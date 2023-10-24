package com.diego.velez.musickery.discography

import com.diego.velez.musickery.discography.Tag.Serialized.TagMap.Companion.toTagMap
import kotlinx.serialization.Serializable
import org.jaudiotagger.tag.FieldKey

/**
 * The ids map represents all the available tags for a song under a single tag id, where the key is the type of data
 * and the value is the data itself.
 *
 * TODO: Consider making this a custom map
 */
data class Tag(val name: Name, val ids: MutableMap<String, String>) {

    constructor(id: String, values: MutableMap<String, String>) : this(Name.fromId(id), values)

    enum class Name(private var id: String, val fieldKey: FieldKey) {
        TITLE("TIT2", FieldKey.TITLE),
        ARTIST("TPE1", FieldKey.ARTIST),
        ALBUM("TALB", FieldKey.ALBUM),
        GENRE("TCON", FieldKey.GENRE),
        TRACK_NUMBER("TRCK", FieldKey.TRACK),
        YEAR("TYER", FieldKey.YEAR),
        COVER_ART("APIC", FieldKey.COVER_ART),
        UNKNOWN("UNKNOWN", FieldKey.KEY);

        override fun toString(): String {
            return name.lowercase().replaceFirstChar { it.uppercaseChar() }.replace("_", " ")
        }

        companion object {
            /**
             * Get the [Name] from the tag [id], or return [UNKNOWN] if it is not supported
             */
            fun fromId(id: String): Name {
                return entries.find { it.id == id } ?: UNKNOWN.apply { this.id = id }
            }
        }
    }

    fun serialized(): Serialized {
        return Serialized(name.name, ids.map { it.toTagMap(name) })
    }

    @Serializable
    data class Serialized(val name: String, val ids: List<TagMap>) {

        @Serializable
        /**
         * The editable tag means if the editable class should apply to that tag, meaning if that tag should be allowed to
         * be edited.
         */
        data class TagMap(val type: String, val value: String, val editable: Boolean = true) {
            companion object {
                fun Map.Entry<String, String>.toTagMap(name: Name): TagMap {
                    // The cover art tag shouldn't be modified because those tags are metadata about the cover art image
                    // itself, instead the cover art should be changed
                    return TagMap(key, value, name != Name.COVER_ART)
                }
            }
        }
    }
}
