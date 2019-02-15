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
        data class Ignored(val stacktrace: String) : Status()
        data class Failed(val stacktrace: String) : Status()
    }
}

fun AdbDevice.runTests(
        testPackageName: String,
        testRunnerClass: String,
        instrumentationArguments: String,
        outputDir: File,
        verboseOutput: Boolean,
        keepOutput: Boolean,
        useTestServices: Boolean
): Single<AdbDeviceTestRun> {

    val adbDevice = this
    val logsDir = File(File(outputDir, "logs"), adbDevice.id)
    val instrumentationOutputFile = File(logsDir, "instrumentation.output")
    val commandPrefix = if (useTestServices) {
        "CLASSPATH=$(pm path android.support.test.services) app_process / android.support.test.services.shellexecutor.ShellMain "
    } else ""

    val runTests = process(
            commandAndArgs = listOf(
                    adb,
                    "-s", adbDevice.id,
                    "shell", "${commandPrefix}am instrument -w -r $instrumentationArguments $testPackageName/$testRunnerClass"
            ),
            timeout = null,
            redirectOutputTo = instrumentationOutputFile,
            keepOutputOnExit = keepOutput
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
                                        is InstrumentationTest.Status.Ignored -> AdbDeviceTest.Status.Ignored(test.status.stacktrace)
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
                    .externalStorage()
                    .flatMap { externalStorageFolder ->
                        adbDevice.pullFolder(
                            // TODO: Add support for internal storage and external storage strategies.
                            folderOnDevice = "$externalStorageFolder/app_spoon-screenshots/${test.className}/${test.testName}",
                            folderOnHostMachine = screenshotsFolderOnHostMachine,
                            logErrors = verboseOutput
                        )
                    }
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

internal fun String.parseTestClassAndName(): Pair<String, String>? {
    val index = indexOf("TestRunner")
    if (index < 0) return null

    val tokens = substring(index, length).split(':')
    if (tokens.size != 3) return null

    val startedOrFinished = tokens[1].trimStart()
    if (startedOrFinished == "started" || startedOrFinished == "finished") {
        return tokens[2].substringAfter("(").removeSuffix(")") to tokens[2].substringBefore("(").trim()
    }
    return null
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

                        // Implicitly expecting to see logs from `android.support.test.internal.runner.listener.LogRunListener`.
                        // Was not able to find more reliable solution to capture logcat per test.
                        val startedTest: Pair<String, String>? = newline.parseTestClassAndName()
                        val finishedTest: Pair<String, String>? = newline.parseTestClassAndName()

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
