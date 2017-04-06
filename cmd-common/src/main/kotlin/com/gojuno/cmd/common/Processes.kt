package com.gojuno.cmd.common

import com.gojuno.cmd.common.Os.Linux
import com.gojuno.cmd.common.Os.Mac
import rx.Emitter.BackpressureMode
import rx.Observable
import rx.schedulers.Schedulers.io
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.TimeoutException

val home: String = System.getenv("HOME")
val androidHome: String = System.getenv("ANDROID_HOME")
val adb: String = "$androidHome/platform-tools/adb"

fun log(message: String): Unit = println("[${Date()}]: $message")

sealed class Notification {
    data class Start(val process: Process, val output: File) : Notification()
    data class Exit(val output: File) : Notification()
}

fun process(
        commandAndArgs: List<String>,
        timeout: Pair<Int, TimeUnit>? = 30 to SECONDS,
        redirectOutputTo: File? = null,
        unbufferedOutput: Boolean = false,
        print: Boolean = false,
        destroyOnUnsubscribe: Boolean = false
): Observable<Notification> = Observable.create<Notification>(
        { emitter ->
            if (print) {
                log("\nRun: $commandAndArgs")
            }

            val outputFile = when (redirectOutputTo) {
                null -> prepareOutputFile(commandAndArgs)
                else -> redirectOutputTo
            }

            outputFile.apply { parentFile?.mkdirs() }

            if (print) {
                log("$commandAndArgs\n, outputFile = $outputFile")
            }

            val command: List<String>

            when (unbufferedOutput) {
                false -> command = commandAndArgs
                true -> command = when (os()) {
                // Some programs, in particular "emulator" do not always flush output
                // after printing so we have to force unbuffered mode to make sure
                // that output will be available for consuming.
                    Linux -> listOf("script", outputFile.absolutePath, "--flush", "-c", commandAndArgs.joinToString(separator = " "))
                    Mac -> listOf("script", "-F", outputFile.absolutePath, *commandAndArgs.toTypedArray())
                }
            }

            val process: Process = ProcessBuilder(command)
                    .let {
                        when (unbufferedOutput) {
                            true -> it
                            else -> it.redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.to(outputFile))
                        }
                    }
                    .start()

            if (destroyOnUnsubscribe) {
                emitter.setCancellation {
                    process.destroy()
                }
            }

            emitter.onNext(Notification.Start(process, outputFile))

            if (timeout == null) {
                process.waitFor()
            } else {
                if (process.waitFor(timeout.first.toLong(), timeout.second).not()) {
                    throw TimeoutException("Process $command timed out ${timeout.first} ${timeout.second} waiting for exit code ${outputFile.readText()}")
                }
            }

            val exitCode = process.exitValue()

            if (print) {
                log("Exit code $exitCode: $commandAndArgs,\noutput = \n${outputFile.readText()}")
            }

            when (exitCode) {
                0 -> {
                    emitter.onNext(Notification.Exit(outputFile))
                    emitter.onCompleted()
                }
                else -> {
                    emitter.onError(IllegalStateException("Process $command exited with non-zero code $exitCode ${outputFile.readText()}"))
                }
            }
        }, BackpressureMode.LATEST
)
        .subscribeOn(io()) // Prevent subscriber thread from unnecessary blocking.
        .observeOn(io())   // Allow to wait for process exit code.

private fun prepareOutputFile(commandAndArgs: List<String>): File = Random()
        .nextInt()
        .let { System.nanoTime() + it }
        .let { name ->
            File("$name.output").apply {
                createNewFile()
                deleteOnExit()
            }
        }

enum class Os {
    Linux,
    Mac
}

private fun os(): Os {
    val os = System.getProperty("os.name", "unknown").toLowerCase(Locale.ENGLISH)

    if (os.contains("mac") || os.contains("darwin")) {
        return Mac
    } else if (os.contains("linux")) {
        return Linux
    } else {
        throw IllegalStateException("Unsupported os $os, only ${Os.values()} are supported.")
    }
}

fun Long.nanosToHumanReadableTime(): String {
    var seconds: Long = TimeUnit.NANOSECONDS.toSeconds(this)
    var minutes: Long = (seconds / 60).apply {
        seconds -= this * 60
    }
    val hours: Long = (minutes / 60).apply {
        minutes -= this * 60
    }

    return buildString {
        if (hours != 0L) {
            append("$hours hour")

            if (hours > 1) {
                append("s")
            }

            append(" ")
        }

        if (minutes != 0L || hours > 0) {
            append("$minutes minute")

            if (minutes != 1L) {
                append("s")
            }

            append(" ")
        }

        append("$seconds second")

        if (seconds != 1L) {
            append("s")
        }
    }
}
