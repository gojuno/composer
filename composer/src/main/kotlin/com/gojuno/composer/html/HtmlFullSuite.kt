package com.gojuno.composer.html

import com.gojuno.composer.Device
import com.gojuno.composer.Suite
import com.google.gson.annotations.SerializedName
import java.util.concurrent.TimeUnit.NANOSECONDS

data class HtmlFullSuite(
        
        @SerializedName("id")
        val id: String,
        
        @SerializedName("tests")
        val tests: List<HtmlTest>,

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

fun Suite.toHtmlFullSuite(id: String) = HtmlFullSuite(
        id = id,
        tests = tests.map { it.toHtmlTest() },
        passedCount = passedCount,
        ignoredCount = ignoredCount,
        failedCount = failedCount,
        durationMillis = NANOSECONDS.toMillis(durationNanos),
        devices = devices.map(Device::toHtmlDevice)
)
