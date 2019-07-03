package com.gojuno.composer.html

import com.gojuno.commander.android.AdbDevice
import com.gojuno.composer.AdbDeviceTest
import com.gojuno.composer.Device
import com.gojuno.composer.Suite
import com.gojuno.composer.testFile
import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit.NANOSECONDS

class HtmlFullSuiteSpec : Spek({

    describe("Suite.toHtmlFullSuite") {
        val suite = Suite(
                testPackage = "p",
                devices = listOf(
                        Device(id = "device1", logcat = testFile(), instrumentationOutput = testFile(), model = "model1"),
                        Device(id = "device2", logcat = testFile(), instrumentationOutput = testFile(), model = "model2")
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

        val htmlFullSuite = suite.toHtmlFullSuite(id = "testSuite", htmlReportDir = testFile())

        it("converts Suite to HtmlFullSuite") {
            assertThat(htmlFullSuite).isEqualTo(HtmlFullSuite(
                    id = "testSuite",
                    passedCount = suite.passedCount,
                    ignoredCount = suite.ignoredCount,
                    failedCount = suite.failedCount,
                    durationMillis = NANOSECONDS.toMillis(suite.durationNanos),
                    devices = suite.devices.map { it.toHtmlDevice(htmlReportDir = testFile()) },
                    tests = suite.tests.map { it.toHtmlFullTest(suiteId = "testSuite", htmlReportDir = testFile()).toHtmlShortTest() }
            ))
        }
    }
})
