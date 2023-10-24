package com.diego.velez.musickery.utils

import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.*
import java.util.logging.Formatter

/**
 * Handles all logging logic.
 */
object Logging {
    private val MAIN_LOG_FILE = File("logs/main.log")

    // Formats date like so: 23MAR2266 (FRIDAY) @ 00:58
    private val DATE_FORMAT = SimpleDateFormat("ddMMMYYYY (EEEE) @ HH:mm")

    private val FILE_HANDLER = FileHandler(MAIN_LOG_FILE.absolutePath, true).apply {
        level = Level.ALL
        formatter = object : Formatter() {
            override fun format(record: LogRecord): String {
                return StringBuilder().apply {
                    val date = Date(record.millis)
                    append(DATE_FORMAT.format(date).uppercase())

                    append(" - ")

                    append("[").append(record.level).append("] ")

                    append(record.sourceClassName)

                    append(" - ")

                    append(formatMessage(record))

                    appendLine()
                }.toString()
            }
        }
    }

    fun getLogger(cls: Any): Logger = Logger.getLogger(cls.javaClass.name).apply {
        addHandler(FILE_HANDLER)
    }

    fun getLogs(): String {
        if (!MAIN_LOG_FILE.exists()) {
            MAIN_LOG_FILE.createNewFile()
        }
        return MAIN_LOG_FILE.readText()
    }
}
