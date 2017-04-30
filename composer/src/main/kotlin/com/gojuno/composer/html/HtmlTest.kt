package com.gojuno.composer.html

import com.gojuno.composer.AdbDeviceTest
import com.google.gson.annotations.SerializedName
import java.util.concurrent.TimeUnit.NANOSECONDS

data class HtmlTest(

        @SerializedName("packageName")
        val packageName: String,

        @SerializedName("className")
        val className: String,

        @SerializedName("name")
        val name: String,

        @SerializedName("durationMillis")
        val durationMillis: Long,

        @SerializedName("status")
        val status: Status,
        
        @SerializedName("stacktrace")
        val stacktrace: String?,

        @SerializedName("logcatPath")
        val logcatPath: String,

        @SerializedName("deviceId")
        val deviceId: String,

        @SerializedName("properties")
        val properties: Map<String, Any>,

        @SerializedName("filePaths")
        val filePaths: List<String>,

        @SerializedName("screenshotsPaths")
        val screenshotsPaths: List<String>
) {
    enum class Status {

        @SerializedName("passed")
        Passed,

        @SerializedName("failed")
        Failed,

        @SerializedName("ignored")
        Ignored
    }
}

fun AdbDeviceTest.toHtmlTest() = HtmlTest(
        packageName = className.substringBeforeLast("."),
        className = className.substringAfterLast("."),
        name = testName,
        durationMillis = NANOSECONDS.toMillis(durationNanos),
        status = when (status) {
            AdbDeviceTest.Status.Passed -> HtmlTest.Status.Passed
            AdbDeviceTest.Status.Ignored -> HtmlTest.Status.Ignored
            is AdbDeviceTest.Status.Failed -> HtmlTest.Status.Failed
        },
        stacktrace = when (status) {
            is AdbDeviceTest.Status.Failed -> status.stacktrace
            else -> null
        },
        logcatPath = logcat.path,
        deviceId = adbDevice.id,
        properties = emptyMap(), // TODO: add properties support.
        filePaths = files.map { it.path },
        screenshotsPaths = screenshots.map { it.path }
)
