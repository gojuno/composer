package com.gojuno.cmd.common

import rx.Observable
import rx.Single
import java.io.File
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS

fun connectedAdbDevices(): Observable<Set<AdbDevice>> = process(listOf(adb, "devices"), unbufferedOutput = true)
        .ofType(Notification.Exit::class.java)
        .map { it.output.readText() }
        .map {
            when (it.contains("List of devices attached")) {
                true -> it
                false -> throw IllegalStateException("Adb output is not correct: $it.")
            }
        }
        .retry { retryCount, exception ->
            val shouldRetry = retryCount < 5 && exception is IllegalStateException
            if (shouldRetry) {
                log("runningEmulators: retrying $exception.")
            }

            shouldRetry
        }
        .map {
            it
                    .substringAfter("List of devices attached")
                    .split(System.lineSeparator())
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .filter { it.contains("online") || it.contains("device") }
                    .map {
                        AdbDevice(
                                id = it.substringBefore("\t"),
                                online = when {
                                    it.contains("offline", ignoreCase = true) -> false
                                    it.contains("device", ignoreCase = true) -> true
                                    else -> throw IllegalStateException("Unknown adb output for device: $it")
                                }
                        )
                    }
                    .toSet()
        }
        .doOnError { log("Error during getting connectedAdbDevices, error = $it") }

fun AdbDevice.log(message: String) = com.gojuno.cmd.common.log("[$id] $message")

fun AdbDevice.installApk(pathToApk: String): Observable<Unit> {
    val adbDevice = this
    val installApk = process(
            commandAndArgs = listOf(adb, "-s", adbDevice.id, "install", "-r", pathToApk),
            unbufferedOutput = true,
            timeout = 5 to MINUTES
    )

    return Observable
            .fromCallable { System.nanoTime() }
            .flatMap { startTimeNanos -> installApk.ofType(Notification.Exit::class.java).map { it to startTimeNanos } }
            .map { (exit, startTimeNanos) ->
                val success = exit
                        .output
                        .readText()
                        .split(System.lineSeparator())
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .firstOrNull { it.equals("Success", ignoreCase = true) } != null

                val duration = System.nanoTime() - startTimeNanos

                when (success) {
                    true -> {
                        adbDevice.log("Successfully installed apk in ${duration.nanosToHumanReadableTime()}, pathToApk = $pathToApk")
                    }

                    false -> {
                        adbDevice.log("Failed to install apk $pathToApk")
                        System.exit(1)
                    }
                }
            }
            .doOnSubscribe { adbDevice.log("Installing apk... pathToApk = $pathToApk") }
            .doOnError { adbDevice.log("Error during installing apk: $it, pathToApk = $pathToApk") }
}

fun AdbDevice.pullFolder(folderOnDevice: String, folderOnHostMachine: File): Single<Boolean> {
    val adbDevice = this
    val pullFiles = process(
            commandAndArgs = listOf(adb, "-s", adbDevice.id, "pull", folderOnDevice, folderOnHostMachine.absolutePath),
            timeout = 60 to SECONDS,
            unbufferedOutput = true
    )

    return pullFiles
            .ofType(Notification.Exit::class.java)
            .retry(3)
            // TODO don't print error if folder does not exist on device, this could be ok in case if test did not create any screenshots.
            .doOnError { error -> log("Failed to pull files from $folderOnDevice to $folderOnHostMachine failed: $error") }
            .map { true }
            .onErrorReturn { false }
            .toSingle()
}

fun AdbDevice.redirectLogcatToFile(file: File): Single<Process> = Observable
        .fromCallable { file.parentFile.mkdirs() }
        .flatMap { process(listOf(adb, "-s", this.id, "logcat"), redirectOutputTo = file, timeout = null) }
        .ofType(Notification.Start::class.java)
        .doOnError {
            when (it) {
                is InterruptedException -> Unit // Expected case, interrupt comes from System.exit(0).
                else -> this.log("Error during redirecting logcat to file $file, error = $it")
            }
        }
        .map { it.process }
        .take(1)
        .toSingle()
