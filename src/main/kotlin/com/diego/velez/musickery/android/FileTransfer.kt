package com.diego.velez.musickery.android

import com.diego.velez.musickery.utils.Terminal
import kotlinx.coroutines.runBlocking

/** Tendremos que usar 'adb' command en terminal para send and receive files while phone is connected to PC via USB
 * default android user dir: /storage/emulated/0
 * adb shell ls -a <dir> - list files en dir
 * adb push <pc dir> <phone dir> - transfer files de pc dir a phone dir
 * adb pull <phone dir> <pc dir> - transfer files de phone dir a pc dir
 */
fun ls(): String = runBlocking {
    return@runBlocking Terminal.run(listOf("/usr/bin/adb", "shell", "ls", "-a", "/storage/emulated/0/Music")).output
}