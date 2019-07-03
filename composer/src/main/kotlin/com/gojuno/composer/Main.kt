package com.gojuno.composer

import com.gojuno.commander.android.connectedAdbDevices
import com.gojuno.commander.android.installApk
import com.gojuno.commander.os.log
import com.gojuno.commander.os.nanosToHumanReadableTime
import com.gojuno.composer.html.writeHtmlReport
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

sealed class Exit(val code: Int, val message: String?) {
    object Ok : Exit(code = 0, message = null)
    object NoDevicesAvailableForTests : Exit(code = 1, message = "Error: No devices available for tests.")
    class TestRunnerNotFound(message: String) : Exit(code = 1, message = message)
    class TestPackageNotFound(message: String) : Exit(code = 1, message = message)
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

    val testPackage: TestPackage.Valid = parseTestPackage(args.testApkPath).let {
        when (it) {
            is TestPackage.Valid -> it
            is TestPackage.ParseError -> {
                exit(Exit.TestPackageNotFound(message = it.error))
                return
            }
        }
    }

    val testRunner: TestRunner.Valid =
            if (!args.testRunner.isEmpty()) {
                TestRunner.Valid(args.testRunner)
            } else {
                parseTestRunner(args.testApkPath).let {
                    when (it) {
                        is TestRunner.Valid -> it
                        is TestRunner.ParseError -> {
                            exit(Exit.TestRunnerNotFound(message = it.error))
                            return
                        }
                    }
                }
            }

    val suites = runAllTests(args, testPackage, testRunner)

    val duration = (System.nanoTime() - startTime)

    val totalPassed = suites.sumBy { it.passedCount }
    val totalFailed = suites.sumBy { it.failedCount }
    val totalIgnored = suites.sumBy { it.ignoredCount }

    log("Test run finished, total passed = $totalPassed, total failed = $totalFailed, total ignored = $totalIgnored, took ${duration.nanosToHumanReadableTime()}.")

    when {
        totalPassed > 0 && totalFailed == 0 -> exit(Exit.Ok)
        totalPassed == 0 && totalFailed == 0 -> if(args.failIfNoTests) exit(Exit.NoTests) else exit(Exit.Ok)
        else -> exit(Exit.ThereWereFailedTests)
    }

    log("Test run finished took ${duration.nanosToHumanReadableTime()}.")
    exit(Exit.Ok)
}

private fun runAllTests(args: Args, testPackage: TestPackage.Valid, testRunner: TestRunner.Valid): List<Suite> {
    val gson = Gson()

    return connectedAdbDevices()
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
            .doOnSuccess { log("${it.size} connected adb device(s): $it") }
            .flatMap { connectedAdbDevices ->
                val runTestsOnDevices: List<Single<AdbDeviceTestRun>> = connectedAdbDevices.mapIndexed { index, device ->
                    val installTimeout = Pair(args.installTimeoutSeconds, TimeUnit.SECONDS)
                    val installAppApk = device.installApk(pathToApk = args.appApkPath, timeout = installTimeout)
                    val installTestApk = device.installApk(pathToApk = args.testApkPath, timeout = installTimeout)
                    val installApks = mutableListOf(installAppApk, installTestApk)
                    installApks.addAll(args.extraApks.map {
                      device.installApk(pathToApk = it, timeout = installTimeout)
                    })

                    Observable
                            .concat(installApks)
                            // Work with each device in parallel, but install apks sequentially on a device.
                            .subscribeOn(Schedulers.io())
                            .toList()
                            .flatMap {
                                val targetInstrumentation: List<Pair<String, String>>
                                val testPackageName: String
                                val testRunnerClass: String

                                if (args.runWithOrchestrator) {
                                    targetInstrumentation = listOf("targetInstrumentation" to "${testPackage.value}/${testRunner.value}")
                                    testPackageName = "androidx.test.orchestrator"
                                    testRunnerClass = "androidx.test.orchestrator.AndroidTestOrchestrator"
                                } else {
                                    targetInstrumentation = emptyList()
                                    testPackageName = testPackage.value
                                    testRunnerClass = testRunner.value
                                }

                                val instrumentationArguments =
                                        buildShardArguments(
                                                shardingOn = args.shard,
                                                shardIndex = index,
                                                devices = connectedAdbDevices.size
                                        ) + args.instrumentationArguments.pairArguments() + targetInstrumentation

                                device
                                        .runTests(
                                                testPackageName = testPackageName,
                                                testRunnerClass = testRunnerClass,
                                                instrumentationArguments = instrumentationArguments.formatInstrumentationArguments(),
                                                outputDir = File(args.outputDirectory),
                                                verboseOutput = args.verboseOutput,
                                                keepOutput = args.keepOutputOnExit,
                                                useTestServices = args.runWithOrchestrator
                                        )
                                        .flatMap { adbDeviceTestRun ->
                                            writeJunit4Report(
                                                    suite = adbDeviceTestRun.toSuite(testPackage.value),
                                                    outputFile = File(File(args.outputDirectory, "junit4-reports"), "${device.id}.xml")
                                            ).toSingleDefault(adbDeviceTestRun)
                                        }
                                        .subscribeOn(Schedulers.io())
                            }
                }
                Single.zip(runTestsOnDevices, { results: Array<Any> -> results.map { it as AdbDeviceTestRun } })
            }
            .map { adbDeviceTestRuns ->
                when (args.shard) {
                true -> {// In "shard=true" mode test runs from all devices arecombined into one suite of tests.
                     listOf(Suite(
                            testPackage = testPackage.value,
                            devices = adbDeviceTestRuns.fold(emptyList()) { devices, adbDeviceTestRun ->
                                devices + Device(
                                        id = adbDeviceTestRun.adbDevice.id,
                                        model = adbDeviceTestRun.adbDevice.model,logcat = adbDeviceTestRun.logcat,
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
                    ))}

                    false -> {
                        // In "shard=false" mode test run from each device is represented as own suite of tests.
                        adbDeviceTestRuns.map {
                            it.toSuite(testPackage.value)
                        }
                    }
                }
            }
            .flatMap { suites ->
                log("Generating HTML report...")
                val htmlReportStartTime = System.nanoTime()
                writeHtmlReport(gson, suites, File(args.outputDirectory, "html-report"), Date())
                        .doOnComplete { log("HTML report generated, took ${(System.nanoTime() - htmlReportStartTime).nanosToHumanReadableTime()}.") }
                        .andThen(Single.just(suites))
            }
            .blockingGet()
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

private fun buildSingleTestArguments(testMethod : String) : List<Pair<String,String>> =
        listOf("class" to testMethod)

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
        val model:String,
        val logcat: File,
        val instrumentationOutput: File
)

fun AdbDeviceTestRun.toSuite(testPackage: String): Suite = Suite(
        testPackage = testPackage,
        devices = listOf(Device(
                id = adbDevice.id,
                model = adbDevice.model,
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
