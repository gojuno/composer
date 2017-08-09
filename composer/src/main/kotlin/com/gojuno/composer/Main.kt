package com.gojuno.composer

import com.gojuno.commander.android.connectedAdbDevices
import com.gojuno.commander.android.installApk
import com.gojuno.commander.os.log
import com.gojuno.commander.os.nanosToHumanReadableTime
import com.gojuno.composer.html.writeHtmlReport
import com.gojuno.janulator.parseArgs
import com.google.gson.Gson
import rx.Observable
import rx.schedulers.Schedulers
import java.io.File
import java.util.*

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
                when (args.devices.isEmpty()) {
                    true -> when (args.devicePattern.isEmpty()) {
                                true -> devices
                                false -> Regex(args.devicePattern).let { regex -> devices.filter { regex.matches(it.id) }}
                            }
                    false -> devices.filter { args.devices.contains(it.id) }
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
                    val installAppApk = device.installApk(pathToApk = args.appApkPath)
                    val installTestApk = device.installApk(pathToApk = args.testApkPath)

                    Observable
                            .concat(installAppApk, installTestApk)
                            // Work with each device in parallel, but install apks sequentially on a device.
                            .subscribeOn(Schedulers.io())
                            .toList()
                            .flatMap {
                                val shardOptions = when {
                                    args.shard && connectedAdbDevices.size > 1 -> listOf(
                                            "numShards" to "${connectedAdbDevices.size}",
                                            "shardIndex" to "$index"
                                    )

                                    else -> emptyList()
                                }

                                device
                                        .runTests(
                                                // TODO parse package name and runner class from test apk.
                                                testPackageName = args.testPackage,
                                                testRunnerClass = args.testRunner,
                                                instrumentationArguments = shardOptions + args.instrumentationArguments,
                                                outputDir = File(args.outputDirectory),
                                                verboseOutput = args.verboseOutput
                                        )
                                        .flatMap { adbDeviceTestRun ->
                                            writeJunit4Report(
                                                    suite = adbDeviceTestRun.toSuite(args.testPackage),
                                                    outputFile = File(File(args.outputDirectory, "junit4-reports"), "${device.id}.xml")
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
)

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
