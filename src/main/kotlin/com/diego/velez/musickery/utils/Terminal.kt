package com.diego.velez.musickery.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

typealias LineOutput = suspend (String) -> Unit

object Terminal {
    /**
     * Runs a terminal command.
     *
     * @return The output of the command.
     */
    suspend fun run(command: List<String>, callback: LineOutput = {}): Result = withContext(Dispatchers.IO) {
        val processBuilder = ProcessBuilder(command)
        processBuilder.redirectErrorStream(true)
        val process = processBuilder.start()
        val reader = process.inputReader()

        val output = StringBuilder()
        val outputThread = launch {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                withContext(Dispatchers.IO) {
                    callback(line!!)
                }
                output.appendLine(line)
                System.out.flush()
            }
        }
        outputThread.start()

        val exitCode = process.waitFor()
        outputThread.join()

        // Process failed
        if (exitCode != 0) {
            return@withContext Result.Failed(output.toString(), exitCode)
        }

        return@withContext Result.Success(output.toString(), exitCode)
    }

    sealed class Result(val output: String, val exitCode: Int) {
        class Success(output: String, exitCode: Int) : Result(output.trim(), exitCode)
        class Failed(output: String, exitCode: Int) : Result(output.trim(), exitCode)
    }
}