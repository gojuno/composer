package com.gojuno.composer.html

import com.gojuno.commander.android.AdbDevice
import com.gojuno.composer.AdbDeviceTest
import com.gojuno.composer.Device
import com.gojuno.composer.Suite
import com.gojuno.composer.testFile
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.it
import java.util.concurrent.TimeUnit.NANOSECONDS

class HtmlShortSuiteSpec : Spek({

    context("Suite.toHtmlShortSuite") {
        val suite by memoized {
            Suite(
                    testPackage = "p",
                    devices = listOf(
                            Device(id = "device1", logcat = testFile(), instrumentationOutput = testFile()),
                            Device(id = "device2", logcat = testFile(), instrumentationOutput = testFile())
                    ),
                    tests = listOf(
                            AdbDeviceTest(
                                    adbDevice = AdbDevice(id = "device1", online = true),
                                    className = "c",
                                    testName = "t1",
                                    status = AdbDeviceTest.Status.Passed,
                                    durationNanos = 200000,
                                    logcat = testFile(),
                                    files = listOf(testFile(), testFile()),
                                    screenshots = listOf(testFile(), testFile())
                            ),
                            AdbDeviceTest(
                                    adbDevice = AdbDevice(id = "device2", online = true),
                                    className = "c",
                                    testName = "t2",
                                    status = AdbDeviceTest.Status.Passed,
                                    durationNanos = 300000,
                                    logcat = testFile(),
                                    files = listOf(testFile(), testFile()),
                                    screenshots = listOf(testFile(), testFile())
                            )
                    ),
                    passedCount = 2,
                    ignoredCount = 0,
                    failedCount = 0,
                    durationNanos = 500000,
                    timestampMillis = 123
            )
        }

        val htmlShortSuite by memoized { suite.toHtmlShortSuite(id = "testSuite", htmlReportDir = testFile().parentFile) }

        it("converts Suite to HtmlShortSuite") {
            assertThat(htmlShortSuite).isEqualTo(HtmlShortSuite(
                    id = "testSuite",
                    passedCount = suite.passedCount,
                    ignoredCount = suite.ignoredCount,
                    failedCount = suite.failedCount,
                    durationMillis = NANOSECONDS.toMillis(suite.durationNanos),
                    devices = suite.devices.map { it.toHtmlDevice(htmlReportDir = testFile().parentFile) }
            ))
        }
    }
})
