package com.gojuno.composer.html

import com.gojuno.composer.Device
import com.google.gson.annotations.SerializedName

data class HtmlDevice(

        @SerializedName("id")
        val id: String,

        @SerializedName("logcat_path")
        val logcatPath: String,

        @SerializedName("instrumentation_output_path")
        val instrumentationOutputPath: String
)

fun Device.toHtmlDevice() = HtmlDevice(
        id = id,
        logcatPath = logcat.path,
        instrumentationOutputPath = instrumentationOutput.path
)
