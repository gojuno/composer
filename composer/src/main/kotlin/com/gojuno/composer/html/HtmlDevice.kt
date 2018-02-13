package com.gojuno.composer.html

import com.gojuno.composer.Device
import com.google.gson.annotations.SerializedName
import java.io.File

data class HtmlDevice(

        @SerializedName("id")
        val id: String,

        @SerializedName("logcat_path")
        val logcatPath: String,

        @SerializedName("instrumentation_output_path")
        val instrumentationOutputPath: String
)

fun Device.toHtmlDevice(htmlReportDir: File) = HtmlDevice(
        id = sanitizedId(),
        logcatPath = logcat.relativePathTo(htmlReportDir),
        instrumentationOutputPath = instrumentationOutput.relativePathTo(htmlReportDir)
)
