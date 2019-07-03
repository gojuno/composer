package com.gojuno.composer.html

import com.gojuno.commander.android.AdbDevice
import com.gojuno.composer.AdbDeviceTest
import com.gojuno.composer.testFile
import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit.NANOSECONDS

class HtmlFullTestSpec : Spek({

    describe("AdbDeviceTest.toHtmlTest") {

        val adbDeviceTest = AdbDeviceTest(
                adbDevice = AdbDevice(id = "testDevice", online = true, model = "testModel"),
                className = "com.gojuno.example.TestClass",
                testName = "test1",
                status = AdbDeviceTest.Status.Passed,
                durationNanos = 23000,
                logcat = testFile(),
                files = listOf(testFile(), testFile()),
                screenshots = listOf(testFile(), testFile())
        )

        val htmlTest = adbDeviceTest.toHtmlFullTest(suiteId = "testSuite", htmlReportDir = testFile().parentFile)

        it("converts AdbDeviceTest to HtmlFullTest") {
            assertThat(htmlTest).isEqualTo(HtmlFullTest(
                    suiteId = "testSuite",
                    packageName = "com.gojuno.example",
                    className = "TestClass",
                    name = adbDeviceTest.testName,
                    deviceModel = "testModel",
                    status = HtmlFullTest.Status.Passed,
                    durationMillis = NANOSECONDS.toMillis(adbDeviceTest.durationNanos),
                    stacktrace = null,
                    logcatPath = adbDeviceTest.logcat.name,
                    filePaths = adbDeviceTest.files.map { it.name },
                    screenshots = adbDeviceTest.screenshots.map { HtmlFullTest.Screenshot(path = it.name, title = it.nameWithoutExtension) },
                    deviceId = adbDeviceTest.adbDevice.id,
                    properties = emptyMap()
            ))
        }
    }
})
