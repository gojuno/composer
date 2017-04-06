package com.gojuno.composer

import com.gojuno.cmd.common.*
import rx.Observable
import rx.Single
import rx.schedulers.Schedulers
import java.io.File

data class TestRunResult(
        val testPackageName: String,
        val tests: List<Test>,
        val passedCount: Int,
        val failedCount: Int,
        val durationNanos: Long,
        val timestampMillis: Long
)

fun AdbDevice.runTests(
        testPackageName: String,
        testRunnerClass: String,
        instrumentationArguments: List<Pair<String, String>>,
        outputDir: File,
        verboseOutput: Boolean
): Single<TestRunResult> {
    val adbDevice = this
    val logsDir = Single.fromCallable { File(File(outputDir, "logs"), adbDevice.id).apply { mkdirs() } }.cache()

    val runTests = logsDir.toObservable()
            .flatMap { logsDir ->
                process(
                        commandAndArgs = listOf(
                                adb,
                                "-s", adbDevice.id,
                                "shell", "am instrument -w -r${instrumentationArguments.formatInstrumentationOptions()} $testPackageName/$testRunnerClass"
                        ),
                        timeout = null,
                        redirectOutputTo = File(logsDir, "instrumentation.output")
                )
            }
            .share()

    val runningTests = runTests
            .ofType(Notification.Start::class.java)
            .flatMap { readInstrumentationOutput(it.output) }
            .asTests()
            .share()

    val tests = runningTests
            .doOnNext { test ->
                val passed = test.result is Test.Result.Passed
                adbDevice.log("Test ${if (passed) "passed" else "failed"} in ${test.durationNanos.nanosToHumanReadableTime()}: ${test.className}.${test.testName}")
            }
            .flatMap { test ->
                pullTestFiles(adbDevice, test, outputDir, verboseOutput)
                        .toObservable()
                        .subscribeOn(Schedulers.io())
                        .map { test }
            }
            .toList()

    val testRunFinish = runTests.ofType(Notification.Exit::class.java).cache()

    val saveLogcat = saveLogcat(adbDevice, logsDir)
            .map { Unit }
            // TODO: Stop when all expected tests were parsed from logcat and not when instrumentation finishes.
            // Logcat may be delivered with delay and that may result in missing logcat for last (n) tests.
            .takeUntil(testRunFinish)
            .startWith(Unit) // To allow zip finish normally even if no tests were run.

    return Observable
            .zip(
                    Observable.fromCallable { System.nanoTime() },
                    tests,
                    saveLogcat,
                    testRunFinish
            )
            { startTimeNanos, tests, _, _ ->
                TestRunResult(
                        testPackageName = testPackageName,
                        tests = tests,
                        passedCount = tests.count { it.result is Test.Result.Passed },
                        failedCount = tests.count { it.result is Test.Result.Failed },
                        durationNanos = System.nanoTime() - startTimeNanos,
                        timestampMillis = System.currentTimeMillis()
                )
            }
            .doOnSubscribe { adbDevice.log("Starting tests...") }
            .doOnNext { result ->
                adbDevice.log(
                        "Test run finished, ${result.passedCount} passed, ${result.failedCount} failed, took ${result.durationNanos.nanosToHumanReadableTime()}."
                )
            }
            .doOnError { adbDevice.log("Error during tests run: $it") }
            .take(1)
            .toSingle()
}

private fun List<Pair<String, String>>.formatInstrumentationOptions(): String = when (isEmpty()) {
    true -> ""
    false -> " " + joinToString(separator = " ") { "-e ${it.first} ${it.second}" }
}

private fun pullTestFiles(adbDevice: AdbDevice, test: Test, outputDir: File, verboseOutput: Boolean): Single<Boolean> = Single
        .fromCallable {
            File(File(File(outputDir, "screenshots"), adbDevice.id), test.className).apply { mkdirs() }
        }
        .flatMap { folderOnHostMachine ->
            // TODO: Collect logcat logs for separate tests.
            adbDevice
                    .pullFolder(
                            // TODO: Add support for internal storage and external storage strategies.
                            // TODO: Add support for spoon files dir.
                            folderOnDevice = "/storage/emulated/0/app_spoon-screenshots/${test.className}/${test.testName}",
                            folderOnHostMachine = folderOnHostMachine,
                            logErrors = verboseOutput
                    )
        }

private fun saveLogcat(adbDevice: AdbDevice, logsDir: Single<File>): Observable<Pair<String, String>> = logsDir.toObservable()
        .map { logsDir -> logsDir to File(logsDir, "full.logcat") }
        .flatMap { (logsDir, fullLogcatFile) -> adbDevice.redirectLogcatToFile(fullLogcatFile).toObservable().map { logsDir to fullLogcatFile } }
        .flatMap { (logsDir, fullLogcatFile) ->
            data class result(val logcat: String = "", val startedTestClassAndName: Pair<String, String>? = null, val finishedTestClassAndName: Pair<String, String>? = null)

            tail(fullLogcatFile)
                    .scan(result()) { previous, newline ->
                        val logcat = when (previous.startedTestClassAndName != null && previous.finishedTestClassAndName != null) {
                            true -> newline
                            false -> "${previous.logcat}\n$newline"
                        }

                        fun String.parseTestClassAndName(prefix: String): Pair<String, String>? = this.substringAfter(prefix, missingDelimiterValue = "").let {
                            when (it) {
                                "" -> null
                                else -> it.substringAfter("(").removeSuffix(")") to it.substringBefore("(")
                            }
                        }

                        // Implicitly expecting to see logs from `android.support.test.internal.runner.listener.LogRunListener`.
                        // Was not able to find more reliable solution to capture logcat per test.
                        val startedTest: Pair<String, String>? = newline.parseTestClassAndName("TestRunner: started: ")
                        val finishedTest: Pair<String, String>? = newline.parseTestClassAndName("TestRunner: finished: ")

                        result(
                                logcat = logcat,
                                startedTestClassAndName = startedTest ?: previous.startedTestClassAndName,
                                finishedTestClassAndName = finishedTest // Actual finished test should always overwrite previous.
                        )
                    }
                    .filter { it.startedTestClassAndName != null && it.startedTestClassAndName == it.finishedTestClassAndName }
                    .map { result ->
                        File(File(logsDir, result.startedTestClassAndName!!.first)
                                .apply { mkdirs() }, "${result.startedTestClassAndName.second}.logcat")
                                .writeText(result.logcat)

                        result.startedTestClassAndName
                    }
        }
