package com.gojuno.composer

import com.gojuno.commander.android.connectedAdbDevices
import com.gojuno.commander.android.installApk
import com.gojuno.commander.os.log
import com.gojuno.commander.os.nanosToHumanReadableTime
import com.gojuno.composer.html.writeHtmlReport
import com.google.gson.Gson
import rx.Observable
import rx.schedulers.Schedulers
import java.io.File
import java.util.Date
import java.util.concurrent.TimeUnit

sealed class Exit(val code: Int, val message: String?) {
    object Ok : Exit(code = 0, message = null)
    object NoDevicesAvailableForTests : Exit(code = 1, message = "Error: No devices available for tests.")
    object ThereWereFailedTests : Exit(code = 1, message = "Error: There were failed tests.")
    object NoTests : Exit(code = 1, message = "Error: 0 tests were run.")
}

fun exit(exit: Exit) {
    if (exit.message != null) {
        log(exit.message)
    }
    System.exit(exit.code)
}

fun main(rawArgs: Array<String>) {
    val startTime = System.nanoTime()

    val args = parseArgs(rawArgs)

    if (args.verboseOutput) {
        log("$args")
    }

    val gson = Gson()

    val suites: List<Suite> = connectedAdbDevices()
            .map { devices ->
                when (args.devicePattern.isEmpty()) {
                    true -> devices
                    false -> Regex(args.devicePattern).let { regex -> devices.filter { regex.matches(it.id) } }
                }
            }
            .map {
                when (args.devices.isEmpty()) {
                    true -> it
                    false -> it.filter { args.devices.contains(it.id) }
                }
            }
            .map {
                it.filter { it.online }.apply {
                    if (isEmpty()) {
                        exit(Exit.NoDevicesAvailableForTests)
                    }
                }
            }
            .doOnNext { log("${it.size} connected adb device(s): $it") }
            .flatMap { connectedAdbDevices ->
                val runTestsOnDevices: List<Observable<AdbDeviceTestRun>> = connectedAdbDevices.mapIndexed { index, device ->
                    val installTimeout = Pair(args.installTimeoutSeconds, TimeUnit.SECONDS)
                    val installAppApk = device.installApk(pathToApk = args.appApkPath, timeout = installTimeout)
                    val installTestApk = device.installApk(pathToApk = args.testApkPath, timeout = installTimeout)

                    Observable
                            .concat(installAppApk, installTestApk)
                            // Work with each device in parallel, but install apks sequentially on a device.
                            .subscribeOn(Schedulers.io())
                            .toList()
                            .flatMap {
                                val instrumentationArguments =
                                        buildShardArguments(
                                                shardingOn = args.shard,
                                                shardIndex = index,
                                                devices = connectedAdbDevices.size
                                        ) + args.instrumentationArguments.pairArguments()

                                device
                                        .runTests(
                                                // TODO parse package name and runner class from test apk.
                                                testPackageName = args.testPackage,
                                                testRunnerClass = args.testRunner,
                                                instrumentationArguments = instrumentationArguments.formatInstrumentationArguments(),
                                                outputDir = File(args.outputDirectory),
                                                verboseOutput = args.verboseOutput,
                                                keepOutput = args.keepOutputOnExit
                                        )
                                        .flatMap { adbDeviceTestRun ->
                                            writeJunit4Report(
                                                    suite = adbDeviceTestRun.toSuite(args.testPackage),
                                                    outputFile = File(File(args.outputDirectory, "junit4-reports"), "${device.sanitizedId()}.xml")
                                            ).toSingleDefault(adbDeviceTestRun)
                                        }
                                        .subscribeOn(Schedulers.io())
                                        .toObservable()
                            }
                }
                Observable.zip(runTestsOnDevices, { results -> results.map { it as AdbDeviceTestRun } })
            }
            .map { adbDeviceTestRuns ->
                when (args.shard) {
                // In "shard=true" mode test runs from all devices combined into one suite of tests.
                    true -> listOf(Suite(
                            testPackage = args.testPackage,
                            devices = adbDeviceTestRuns.fold(emptyList()) { devices, adbDeviceTestRun ->
                                devices + Device(
                                        id = adbDeviceTestRun.adbDevice.id,
                                        logcat = adbDeviceTestRun.logcat,
                                        instrumentationOutput = adbDeviceTestRun.instrumentationOutput
                                )
                            },
                            tests = adbDeviceTestRuns.map { it.tests }.fold(emptyList()) { result, tests ->
                                result + tests
                            },
                            passedCount = adbDeviceTestRuns.sumBy { it.passedCount },
                            ignoredCount = adbDeviceTestRuns.sumBy { it.ignoredCount },
                            failedCount = adbDeviceTestRuns.sumBy { it.failedCount },
                            durationNanos = adbDeviceTestRuns.map { it.durationNanos }.max() ?: -1,
                            timestampMillis = adbDeviceTestRuns.map { it.timestampMillis }.min() ?: -1
                    ))

                // In "shard=false" mode test run from each device represented as own suite of tests.  
                    false -> adbDeviceTestRuns.map { it.toSuite(args.testPackage) }
                }
            }
            .flatMap { suites ->
                log("Generating HTML report...")
                val htmlReportStartTime = System.nanoTime()
                writeHtmlReport(gson, suites, File(args.outputDirectory, "html-report"), Date())
                        .doOnCompleted { log("HTML report generated, took ${(System.nanoTime() - htmlReportStartTime).nanosToHumanReadableTime()}.") }
                        .andThen(Observable.just(suites))
            }
            .toBlocking()
            .first()

    val duration = (System.nanoTime() - startTime)

    val totalPassed = suites.sumBy { it.passedCount }
    val totalFailed = suites.sumBy { it.failedCount }
    val totalIgnored = suites.sumBy { it.ignoredCount }

    log("Test run finished, total passed = $totalPassed, total failed = $totalFailed, total ignored = $totalIgnored, took ${duration.nanosToHumanReadableTime()}.")

    when {
        totalPassed > 0 && totalFailed == 0 -> exit(Exit.Ok)
        totalPassed == 0 && totalFailed == 0 -> exit(Exit.NoTests)
        else -> exit(Exit.ThereWereFailedTests)
    }
}

private fun List<String>.pairArguments(): List<Pair<String, String>> =
        foldIndexed(mutableListOf()) { index, accumulator, value ->
            accumulator.apply {
                if (index % 2 == 0) {
                    add(value to "")
                } else {
                    set(lastIndex, last().first to value)
                }
            }
        }

private fun buildShardArguments(shardingOn: Boolean, shardIndex: Int, devices: Int): List<Pair<String, String>> = when {
    shardingOn && devices > 1 -> listOf(
            "numShards" to "$devices",
            "shardIndex" to "$shardIndex"
    )

    else -> emptyList()
}

private fun List<Pair<String, String>>.formatInstrumentationArguments(): String = when (isEmpty()) {
    true -> ""
    false -> " " + joinToString(separator = " ") { "-e ${it.first} ${it.second}" }
}

data class Suite(
        val testPackage: String,
        val devices: List<Device>,
        val tests: List<AdbDeviceTest>, // TODO: switch to separate Test class.
        val passedCount: Int,
        val ignoredCount: Int,
        val failedCount: Int,
        val durationNanos: Long,
        val timestampMillis: Long
)

data class Device(
        val id: String,
        val logcat: File,
        val instrumentationOutput: File
){
    fun sanitizedId() = id.replace(":","-")
}

fun AdbDeviceTestRun.toSuite(testPackage: String): Suite = Suite(
        testPackage = testPackage,
        devices = listOf(Device(
                id = adbDevice.id,
                logcat = logcat,
                instrumentationOutput = instrumentationOutput
        )),
        tests = tests,
        passedCount = passedCount,
        ignoredCount = ignoredCount,
        failedCount = failedCount,
        durationNanos = durationNanos,
        timestampMillis = timestampMillis
)
