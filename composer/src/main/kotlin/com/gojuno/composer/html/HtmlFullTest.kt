package com.gojuno.composer.html

import com.gojuno.composer.AdbDeviceTest
import com.google.gson.annotations.SerializedName
import java.io.File
import java.util.concurrent.TimeUnit.NANOSECONDS

data class HtmlFullTest(

        @SerializedName("suite_id")
        val suiteId: String,

        @SerializedName("package_name")
        val packageName: String,

        @SerializedName("class_name")
        val className: String,

        @SerializedName("name")
        val name: String,

        @SerializedName("id")
        val id: String = "$packageName$className$name",

        @SerializedName("duration_millis")
        val durationMillis: Long,

        @SerializedName("status")
        val status: Status,
        
        @SerializedName("stacktrace")
        val stacktrace: String?,

        @SerializedName("logcat_path")
        val logcatPath: String,

        @SerializedName("deviceId")
        val deviceId: String,

        @SerializedName("properties")
        val properties: Map<String, Any>,

        @SerializedName("file_paths")
        val filePaths: List<String>,

        @SerializedName("screenshots_paths")
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

fun AdbDeviceTest.toHtmlFullTest(suiteId: String, htmlReportDir: File) = HtmlFullTest(
        suiteId = suiteId,
        packageName = className.substringBeforeLast("."),
        className = className.substringAfterLast("."),
        name = testName,
        durationMillis = NANOSECONDS.toMillis(durationNanos),
        status = when (status) {
            AdbDeviceTest.Status.Passed -> HtmlFullTest.Status.Passed
            AdbDeviceTest.Status.Ignored -> HtmlFullTest.Status.Ignored
            is AdbDeviceTest.Status.Failed -> HtmlFullTest.Status.Failed
        },
        stacktrace = when (status) {
            is AdbDeviceTest.Status.Failed -> status.stacktrace
            else -> null
        },
        logcatPath = logcat.relativePathTo(htmlReportDir),
        deviceId = adbDevice.id,
        properties = emptyMap(), // TODO: add properties support.
        filePaths = files.map { it.relativePathTo(htmlReportDir) },
        screenshotsPaths = screenshots.map { it.relativePathTo(htmlReportDir) }
)
