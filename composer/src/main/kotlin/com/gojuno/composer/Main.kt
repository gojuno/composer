package com.gojuno.composer

import com.gojuno.cmd.common.connectedAdbDevices
import com.gojuno.cmd.common.installApk
import com.gojuno.cmd.common.log
import com.gojuno.cmd.common.nanosToHumanReadableTime
import com.gojuno.janulator.parseArgs
import rx.Observable
import rx.schedulers.Schedulers
import java.io.File

sealed class Exit(val code: Int, val message: String?) {
    object Ok : Exit(code = 0, message = null)
    object NoDevicesAvailableForTests : Exit(code = 1, message = "No devices available for tests.")
    object ThereWereFailedTests : Exit(code = 1, message = "There were failed tests.")
    object NoTests : Exit(code = 1, message = "0 tests were run.")
}

fun exit(exit: Exit) {
    if (exit.message != null) {
        log(exit.message)
    }
    System.exit(exit.code)
}

fun main(rawArgs: Array<String>) {
    val args = parseArgs(rawArgs)

    log("$args")

    val startTime = System.nanoTime()

    val testRunResults: List<TestRunResult> = connectedAdbDevices()
            .map {
                it.filter { it.online }.apply {
                    if (isEmpty()) {
                        exit(Exit.NoDevicesAvailableForTests)
                    }
                }
            }
            .doOnNext { log("${it.size} connected adb device(s): $it") }
            .flatMap { connectedAdbDevices ->
                val runTestsOnDevices: List<Observable<TestRunResult>> = connectedAdbDevices.mapIndexed { index, device ->
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
                                                outputDir = File(args.outputDirectory)
                                        )
                                        .flatMap { testRunResult ->
                                            writeJunit4Report(
                                                    testRunResult = testRunResult,
                                                    outputFile = File(File(args.outputDirectory, "junit4-reports"), "${device.id}.xml")
                                            ).toSingleDefault(testRunResult)
                                        }
                                        .subscribeOn(Schedulers.io())
                                        .toObservable()
                            }
                }

                Observable.zip(runTestsOnDevices, { results -> results.map { it as TestRunResult } })
            }
            .toBlocking()
            .first()

    val duration = (System.nanoTime() - startTime)
    val totalPassed = testRunResults.sumBy { it.passedCount }
    val totalFailed = testRunResults.sumBy { it.failedCount }

    log("Test run finished, total passed = $totalPassed, total failed = $totalFailed, took ${duration.nanosToHumanReadableTime()}.")

    when {
        totalPassed > 0 && totalFailed == 0 -> exit(Exit.Ok)
        totalPassed == 0 && totalFailed == 0 -> exit(Exit.NoTests)
        else -> exit(Exit.ThereWereFailedTests)
    }
}

