package com.gojuno.composer.html

import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class HtmlShortTestSpec : Spek({

    describe("HtmlFullTest.toHtmlShortTest") {

        val htmlFullTest by memoized {
            HtmlFullTest(
                    suiteId = "testSuite",
                    packageName = "com.gojuno.example",
                    className = "TestClass",
                    name = "test1",
                    deviceModel = "test-device-model",
                    status = HtmlFullTest.Status.Passed,
                    durationMillis = 1234,
                    stacktrace = null,
                    logcatPath = "testLogcatPath",
                    filePaths = listOf("testFilePath1", "testFilePath2"),
                    screenshots = listOf(HtmlFullTest.Screenshot(path = "testScreenshotPath1", title = "testScreenshot1"), HtmlFullTest.Screenshot(path = "testScreenshotPath2", title = "testScreenshot2")),
                    deviceId = "test-device-id",
                    properties = mapOf("key1" to "value1", "key2" to "value2")
            )
        }

        val htmlShortTest by memoized { htmlFullTest.toHtmlShortTest() }

        it("converts HtmlFullTest to HtmlShortTest") {
            assertThat(htmlShortTest).isEqualTo(HtmlShortTest(
                    id = htmlFullTest.id,
                    packageName = "com.gojuno.example",
                    className = "TestClass",
                    name = "test1",
                    status = HtmlFullTest.Status.Passed,
                    durationMillis = 1234,
                    deviceId = "test-device-id",
                    deviceModel = "test-device-model",
                    properties = mapOf("key1" to "value1", "key2" to "value2")
            ))
        }
    }
})
