package com.gojuno.composer.html

import com.gojuno.commander.android.AdbDevice
import com.gojuno.composer.AdbDeviceTest
import com.gojuno.composer.Device
import com.gojuno.composer.Suite
import com.gojuno.composer.perform
import com.google.gson.Gson
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.util.Files
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

class HtmlReportSpec : Spek({

    describe("writeHtmlReport") {

        val outputDir by memoized { Files.newTemporaryFolder() }

        val adbDevice1 = AdbDevice(
                id = "device1",
                online = true,
                model = "model1"
        )

        val suites by memoized {
            listOf(
                    Suite(
                            testPackage = "com.gojuno.example1",
                            devices = listOf(
                                Device(
                                    id = "device1",
                                    logcat = File(outputDir, "device1.logcat"),
                                    instrumentationOutput = File(outputDir, "device1.instrumentation"),
                                    model = "model1")
                            ),
                            tests = listOf(
                                    AdbDeviceTest(
                                            adbDevice = adbDevice1,
                                            className = "com.gojuno.example1.TestClass",
                                            testName = "test1",
                                            durationNanos = MILLISECONDS.toNanos(1234),
                                            status = AdbDeviceTest.Status.Passed,
                                            logcat = File(File(outputDir, "com.gojuno.example1.TestClass"), "test1.logcat"),
                                            files = listOf(File(File(outputDir, "com.gojuno.example1.TestClass.test1"), "file1"), File(File(outputDir, "com.gojuno.example1.TestClass.test1"), "file2")),
                                            screenshots = listOf(File(File(outputDir, "com.gojuno.example1.TestClass.test1"), "screenshot1"), File(File(outputDir, "com.gojuno.example1.TestClass.test1"), "screenshot2"))
                                    ),
                                    AdbDeviceTest(
                                            adbDevice = adbDevice1,
                                            className = "com.gojuno.example1.TestClass",
                                            testName = "test2",
                                            durationNanos = MILLISECONDS.toNanos(1234),
                                            status = AdbDeviceTest.Status.Failed(stacktrace = "abc"),
                                            logcat = File(File(outputDir, "com.gojuno.example1.TestClass"), "test2.logcat"),
                                            files = listOf(File(File(outputDir, "com.gojuno.example1.TestClass.test2"), "file1"), File(File(outputDir, "com.gojuno.example1.TestClass.test2"), "file2")),
                                            screenshots = listOf(File(File(outputDir, "com.gojuno.example1.TestClass.test2"), "screenshot1"), File(File(outputDir, "com.gojuno.example1.TestClass.test2"), "screenshot2"))
                                    )
                            ),
                            passedCount = 2,
                            ignoredCount = 0,
                            failedCount = 1,
                            durationNanos = MILLISECONDS.toNanos(1234 * 2),
                            timestampMillis = 1805
                    )
            )
        }

        fun File.deleteOnExitRecursively() {
            when (isDirectory) {
                false -> deleteOnExit()
                true -> listFiles()?.forEach { inner -> inner.deleteOnExitRecursively() }
            }
        }

        val date by memoized { Date(1496848677000) }

        val subscriber by memoized {
            writeHtmlReport(Gson(), suites, outputDir, date).test()
        }

        perform {
            subscriber.awaitTerminalEvent(5, SECONDS)
            outputDir.deleteOnExitRecursively()
        }

        it("completes") {
            subscriber.assertComplete()
        }

        it("does not emit error") {
            subscriber.assertNoErrors()
        }

        fun String.removeEmptyLines() = lines().filter { it.trim() != "" }.joinToString(separator = "\n") { it }

        it("creates index html") {
            assertThat(File(outputDir, "index.html").readText().removeEmptyLines()).isEqualTo(
                    """
                    <!doctype html>
                    <html lang="en">
                      <head>
                        <meta name=viewport content="width=device-width, initial-scale=1, maximum-scale=1">
                        <meta charset="utf-8">
                        <title>Composer</title>
                        <link href="app.min.css" rel="stylesheet">
                        <script>
                          window.mainData = {"suites":[{"id":"0","passed_count":2,"ignored_count":0,"failed_count":1,"duration_millis":2468,"devices":[{"id":"device1","model":"model1","logcat_path":"device1.logcat","instrumentation_output_path":"device1.instrumentation"}]}]}
                          // window.mainData / window.suite / window.test={ json }
                        </script>
                      </head>
                      <body>
                        <div id="root"></div>
                        <script type="text/javascript" src="app.min.js"></script>

                        <div class="copy content">Generated with&nbsp;❤️&nbsp;&nbsp;by Juno at 15:17:57 UTC, Jun 7 2017</div>
                      </body>
                    </html>
                    """.removeEmptyLines().trimIndent()
            )
        }

        it("creates suite html") {
            assertThat(File(File(outputDir, "suites"), "0.html").readText().removeEmptyLines()).isEqualTo(
                    """
                            <!doctype html>
                            <html lang="en">
                              <head>
                                <meta name=viewport content="width=device-width, initial-scale=1, maximum-scale=1">
                                <meta charset="utf-8">
                                <title>Composer</title>
                                <link href="../app.min.css" rel="stylesheet">
                                <script>
                                  window.suite = {"id":"0","tests":[{"id":"com.gojuno.example1TestClasstest1","package_name":"com.gojuno.example1","class_name":"TestClass","name":"test1","duration_millis":1234,"status":"passed","deviceId":"device1","deviceModel":"model1","properties":{}},{"id":"com.gojuno.example1TestClasstest2","package_name":"com.gojuno.example1","class_name":"TestClass","name":"test2","duration_millis":1234,"status":"failed","deviceId":"device1","deviceModel":"model1","properties":{}}],"passed_count":2,"ignored_count":0,"failed_count":1,"duration_millis":2468,"devices":[{"id":"device1","model":"model1","logcat_path":"../device1.logcat","instrumentation_output_path":"../device1.instrumentation"}]}
                                  // window.mainData / window.suite / window.test={ json }
                                </script>
                              </head>
                              <body>
                                <div id="root"></div>
                                <script type="text/javascript" src="../app.min.js"></script>
                                <div class="copy content">Generated with&nbsp;❤️&nbsp;&nbsp;by Juno at 15:17:57 UTC, Jun 7 2017</div>
                              </body>
                            </html>
                            """.removeEmptyLines().trimIndent()
            )
        }

        it("creates html for 1st test") {
            assertThat(File(File(File(File(outputDir, "suites"), "0"), "device1"), "com.gojuno.example1TestClasstest1.html").readText().removeEmptyLines()).isEqualTo(
                    """
                            <!doctype html>
                            <html lang="en">
                              <head>
                                <meta name=viewport content="width=device-width, initial-scale=1, maximum-scale=1">
                                <meta charset="utf-8">
                                <title>Composer</title>
                                <link href="../../../app.min.css" rel="stylesheet">
                                <script>
                                  window.test = {"suite_id":"0","package_name":"com.gojuno.example1","class_name":"TestClass","name":"test1","id":"com.gojuno.example1TestClasstest1","duration_millis":1234,"status":"passed","logcat_path":"../../../com.gojuno.example1.TestClass/test1.logcat","deviceId":"device1","deviceModel":"model1","properties":{},"file_paths":["../../../com.gojuno.example1.TestClass.test1/file1","../../../com.gojuno.example1.TestClass.test1/file2"],"screenshots":[{"path":"../../../com.gojuno.example1.TestClass.test1/screenshot1","title":"screenshot1"},{"path":"../../../com.gojuno.example1.TestClass.test1/screenshot2","title":"screenshot2"}]}
                                  // window.mainData / window.suite / window.test={ json }
                                </script>
                              </head>
                              <body>
                                <div id="root"></div>
                                <script type="text/javascript" src="../../../app.min.js"></script>

                                <div class="copy content">Generated with&nbsp;❤️&nbsp;&nbsp;by Juno at 15:17:57 UTC, Jun 7 2017</div>
                              </body>
                            </html>
                            """.removeEmptyLines().trimIndent()
            )
        }

        it("creates html for 2nd test") {
            assertThat(File(File(File(File(outputDir, "suites"), "0"), "device1"), "com.gojuno.example1TestClasstest2.html").readText().removeEmptyLines()).isEqualTo(
                    """
                            <!doctype html>
                            <html lang="en">
                              <head>
                                <meta name=viewport content="width=device-width, initial-scale=1, maximum-scale=1">
                                <meta charset="utf-8">
                                <title>Composer</title>
                                <link href="../../../app.min.css" rel="stylesheet">
                                <script>
                                  window.test = {"suite_id":"0","package_name":"com.gojuno.example1","class_name":"TestClass","name":"test2","id":"com.gojuno.example1TestClasstest2","duration_millis":1234,"status":"failed","stacktrace":"abc","logcat_path":"../../../com.gojuno.example1.TestClass/test2.logcat","deviceId":"device1","deviceModel":"model1","properties":{},"file_paths":["../../../com.gojuno.example1.TestClass.test2/file1","../../../com.gojuno.example1.TestClass.test2/file2"],"screenshots":[{"path":"../../../com.gojuno.example1.TestClass.test2/screenshot1","title":"screenshot1"},{"path":"../../../com.gojuno.example1.TestClass.test2/screenshot2","title":"screenshot2"}]}
                                  // window.mainData / window.suite / window.test={ json }
                                </script>
                              </head>
                              <body>
                                <div id="root"></div>
                                <script type="text/javascript" src="../../../app.min.js"></script>

                                <div class="copy content">Generated with&nbsp;❤️&nbsp;&nbsp;by Juno at 15:17:57 UTC, Jun 7 2017</div>
                              </body>
                            </html>
                            """.removeEmptyLines().trimIndent()
            )
        }
    }

    describe("cssClassForLogcatLine") {

        context("verbose") {

            val cssClass by memoized { cssClassForLogcatLine("06-07 16:55:14.490  2100  2100 V MicroDetectionWorker: #onError(false)") }

            it("is verbose") {
                assertThat(cssClass).isEqualTo("verbose")
            }
        }

        context("debug") {

            val cssClass by memoized { cssClassForLogcatLine("06-07 16:55:14.490  2100  2100 D MicroDetectionWorker: #onError(false)") }

            it("is debug") {
                assertThat(cssClass).isEqualTo("debug")
            }
        }

        context("info") {

            val cssClass by memoized { cssClassForLogcatLine("06-07 16:55:14.490  2100  2100 I MicroDetectionWorker: #onError(false)") }

            it("is info") {
                assertThat(cssClass).isEqualTo("info")
            }
        }

        context("warning") {

            val cssClass by memoized { cssClassForLogcatLine("06-07 16:55:14.490  2100  2100 W MicroDetectionWorker: #onError(false)") }

            it("is warning") {
                assertThat(cssClass).isEqualTo("warning")
            }
        }

        context("error") {

            val cssClass by memoized { cssClassForLogcatLine("06-07 16:55:14.490  2100  2100 E MicroDetectionWorker: #onError(false)") }

            it("is error") {
                assertThat(cssClass).isEqualTo("error")
            }
        }

        context("assert") {

            val cssClass by memoized { cssClassForLogcatLine("06-07 16:55:14.490  2100  2100 A MicroDetectionWorker: #onError(false)") }

            it("is assert") {
                assertThat(cssClass).isEqualTo("assert")
            }
        }

        context("default") {

            val cssClass by memoized { cssClassForLogcatLine("06-07 16:55:14.490  2100  2100 U MicroDetectionWorker: #onError(false)") }

            it("is default") {
                assertThat(cssClass).isEqualTo("default")
            }
        }
    }
})
