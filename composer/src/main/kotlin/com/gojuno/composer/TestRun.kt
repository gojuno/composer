package com.gojuno.composer

import com.gojuno.commander.android.*
import com.gojuno.commander.os.Notification
import com.gojuno.commander.os.nanosToHumanReadableTime
import com.gojuno.commander.os.process
import rx.Observable
import rx.Single
import rx.schedulers.Schedulers
import java.io.File

data class AdbDeviceTestRun(
        val adbDevice: AdbDevice,
        val tests: List<AdbDeviceTest>,
        val passedCount: Int,
        val ignoredCount: Int,
        val failedCount: Int,
        val durationNanos: Long,
        val timestampMillis: Long,
        val logcat: File,
        val instrumentationOutput: File
)

data class AdbDeviceTest(
        val adbDevice: AdbDevice,
        val className: String,
        val testName: String,
        val status: Status,
        val durationNanos: Long,
        val logcat: File,
        val files: List<File>,
        val screenshots: List<File>
) {
    sealed class Status {
        object Passed : Status()
        object Ignored : Status()
        data class Failed(val stacktrace: String) : Status()
    }
}

fun AdbDevice.runTests(
        testPackageName: String,
        testRunnerClass: String,
        instrumentationArguments: List<Pair<String, String>>,
        outputDir: File,
        verboseOutput: Boolean
): Single<AdbDeviceTestRun> {

    val adbDevice = this
    val logsDir = File(File(outputDir, "logs"), adbDevice.id)
    val instrumentationOutputFile = File(logsDir, "instrumentation.output")

    val runTests = process(
            commandAndArgs = listOf(
                    adb,
                    "-s", adbDevice.id,
                    "shell", "am instrument -w -r${instrumentationArguments.formatInstrumentationOptions()} $testPackageName/$testRunnerClass"
            ),
            timeout = null,
            redirectOutputTo = instrumentationOutputFile
    ).share()

    @Suppress("destructure")
    val runningTests = runTests
            .ofType(Notification.Start::class.java)
            .flatMap { readInstrumentationOutput(it.output) }
            .asTests()
            .doOnNext { test ->
                val status = when (test.status) {
                    is InstrumentationTest.Status.Passed -> "passed"
                    is InstrumentationTest.Status.Ignored -> "ignored"
                    is InstrumentationTest.Status.Failed -> "failed"
                }

                adbDevice.log(
                        "Test ${test.index}/${test.total} $status in " +
                        "${test.durationNanos.nanosToHumanReadableTime()}: " +
                        "${test.className}.${test.testName}"
                )
            }
            .flatMap { test ->
                pullTestFiles(adbDevice, test, outputDir, verboseOutput)
                        .toObservable()
                        .subscribeOn(Schedulers.io())
                        .map { pulledFiles -> test to pulledFiles }
            }
            .toList()

    val adbDeviceTestRun = Observable
            .zip(
                    Observable.fromCallable { System.nanoTime() },
                    runningTests,
                    { time, tests -> time to tests }
            )
            .map { (startTimeNanos, testsWithPulledFiles) ->
                val tests = testsWithPulledFiles.map { it.first }

                AdbDeviceTestRun(
                        adbDevice = adbDevice,
                        tests = testsWithPulledFiles.map { (test, pulledFiles) ->
                            AdbDeviceTest(
                                    adbDevice = adbDevice,
                                    className = test.className,
                                    testName = test.testName,
                                    status = when (test.status) {
                                        is InstrumentationTest.Status.Passed -> AdbDeviceTest.Status.Passed
                                        is InstrumentationTest.Status.Ignored -> AdbDeviceTest.Status.Ignored
                                        is InstrumentationTest.Status.Failed -> AdbDeviceTest.Status.Failed(test.status.stacktrace)
                                    },
                                    durationNanos = test.durationNanos,
                                    logcat = logcatFileForTest(logsDir, test.className, test.testName),
                                    files = pulledFiles.files.sortedBy { it.name },
                                    screenshots = pulledFiles.screenshots.sortedBy { it.name }
                            )
                        },
                        passedCount = tests.count { it.status is InstrumentationTest.Status.Passed },
                        ignoredCount = tests.count { it.status is InstrumentationTest.Status.Ignored },
                        failedCount = tests.count { it.status is InstrumentationTest.Status.Failed },
                        durationNanos = System.nanoTime() - startTimeNanos,
                        timestampMillis = System.currentTimeMillis(),
                        logcat = logcatFileForDevice(logsDir),
                        instrumentationOutput = instrumentationOutputFile
                )
            }

    val testRunFinish = runTests.ofType(Notification.Exit::class.java).cache()

    val saveLogcat = saveLogcat(adbDevice, logsDir)
            .map { Unit }
            // TODO: Stop when all expected tests were parsed from logcat and not when instrumentation finishes.
            // Logcat may be delivered with delay and that may result in missing logcat for last (n) tests (it's just a theory though).
            .takeUntil(testRunFinish)
            .startWith(Unit) // To allow zip finish normally even if no tests were run.

    return Observable
            .zip(adbDeviceTestRun, saveLogcat, testRunFinish) { suite, _, _ -> suite }
            .doOnSubscribe { adbDevice.log("Starting tests...") }
            .doOnNext { testRun ->
                adbDevice.log(
                        "Test run finished, " +
                        "${testRun.passedCount} passed, " +
                        "${testRun.failedCount} failed, took " +
                        "${testRun.durationNanos.nanosToHumanReadableTime()}."
                )
            }
            .doOnError { adbDevice.log("Error during tests run: $it") }
            .toSingle()
}

private fun List<Pair<String, String>>.formatInstrumentationOptions(): String = when (isEmpty()) {
    true -> ""
    false -> " " + joinToString(separator = " ") { "-e ${it.first} ${it.second}" }
}

data class PulledFiles(
        val files: List<File>,
        val screenshots: List<File>
)

private fun pullTestFiles(adbDevice: AdbDevice, test: InstrumentationTest, outputDir: File, verboseOutput: Boolean): Single<PulledFiles> = Single
        // TODO: Add support for spoon files dir.
        .fromCallable {
            File(File(File(outputDir, "screenshots"), adbDevice.id), test.className).apply { mkdirs() }
        }
        .flatMap { screenshotsFolderOnHostMachine ->
            adbDevice
                    .pullFolder(
                            // TODO: Add support for internal storage and external storage strategies.
                            folderOnDevice = "/storage/emulated/0/app_spoon-screenshots/${test.className}/${test.testName}",
                            folderOnHostMachine = screenshotsFolderOnHostMachine,
                            logErrors = verboseOutput
                    )
                    .map { File(screenshotsFolderOnHostMachine, test.testName) }
        }
        .map { screenshotsFolderOnHostMachine ->
            PulledFiles(
                    files = emptyList(), // TODO: Pull test files.
                    screenshots = screenshotsFolderOnHostMachine.let {
                        when (it.exists()) {
                            true -> it.listFiles().toList()
                            else -> emptyList()
                        }
                    }
            )
        }

private fun saveLogcat(adbDevice: AdbDevice, logsDir: File): Observable<Pair<String, String>> = Observable
        .just(logsDir to logcatFileForDevice(logsDir))
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
                        logcatFileForTest(logsDir, className = result.startedTestClassAndName!!.first, testName = result.startedTestClassAndName.second)
                                .apply { parentFile.mkdirs() }
                                .writeText(result.logcat)

                        result.startedTestClassAndName
                    }
        }

private fun logcatFileForDevice(logsDir: File) = File(logsDir, "full.logcat")

private fun logcatFileForTest(logsDir: File, className: String, testName: String): File = File(File(logsDir, className), "$testName.logcat")
