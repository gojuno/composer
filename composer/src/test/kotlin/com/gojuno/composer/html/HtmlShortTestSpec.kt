package com.gojuno.composer.html

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.it

class HtmlShortTestSpec : Spek({

    context("HtmlFullTest.toHtmlShortTest") {

        val htmlFullTest = HtmlFullTest(
                packageName = "com.gojuno.example",
                className = "TestClass",
                name = "test1",
                status = HtmlFullTest.Status.Passed,
                durationMillis = 1234,
                stacktrace = null,
                logcatPath = "testLogcatPath",
                filePaths = listOf("testFilePath1", "testFilePath2"),
                screenshotsPaths = listOf("testScreenshotPath1", "testScreenshotPath2"),
                deviceId = "test-device-id",
                properties = mapOf("key1" to "value1", "key2" to "value2")
        )

        val htmlShortTest = htmlFullTest.toHtmlShortTest()

        it("converts HtmlFullTest to HtmlShortTest") {
            assertThat(htmlShortTest).isEqualTo(HtmlShortTest(
                    id = htmlFullTest.id,
                    packageName = "com.gojuno.example",
                    className = "TestClass",
                    name = "test1",
                    status = HtmlFullTest.Status.Passed,
                    durationMillis = 1234,
                    deviceId = "test-device-id",
                    properties = mapOf("key1" to "value1", "key2" to "value2")
            ))
        }
    }
})
