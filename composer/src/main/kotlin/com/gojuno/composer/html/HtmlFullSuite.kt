package com.gojuno.composer.html

import com.gojuno.composer.Device
import com.gojuno.composer.Suite
import com.google.gson.annotations.SerializedName
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.*

data class HtmlFullSuite(
        
        @SerializedName("id")
        val id: String,
        
        @SerializedName("tests")
        val tests: List<HtmlTest>,

        @SerializedName("passedCount")
        val passedCount: Int,

        @SerializedName("ignoredCount")
        val ignoredCount: Int,

        @SerializedName("failedCount")
        val failedCount: Int,

        @SerializedName("durationMillis")
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
