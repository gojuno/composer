package com.gojuno.composer.html

import com.gojuno.composer.Suite
import com.google.gson.annotations.SerializedName
import java.io.File
import java.util.concurrent.TimeUnit.NANOSECONDS

data class HtmlFullSuite(

        @SerializedName("id")
        val id: String,

        @SerializedName("tests")
        val tests: List<HtmlShortTest>,

        @SerializedName("passed_count")
        val passedCount: Int,

        @SerializedName("ignored_count")
        val ignoredCount: Int,

        @SerializedName("failed_count")
        val failedCount: Int,

        @SerializedName("duration_millis")
        val durationMillis: Long,

        @SerializedName("devices")
        val devices: List<HtmlDevice>
)

fun Suite.toHtmlFullSuite(id: String, htmlReportDir: File) = HtmlFullSuite(
        id = id,
        tests = tests.map { it.toHtmlFullTest(suiteId = id, htmlReportDir = htmlReportDir).toHtmlShortTest() },
        passedCount = passedCount,
        ignoredCount = ignoredCount,
        failedCount = failedCount,
        durationMillis = NANOSECONDS.toMillis(durationNanos),
        devices = devices.map { it.toHtmlDevice(htmlReportDir) }
)
