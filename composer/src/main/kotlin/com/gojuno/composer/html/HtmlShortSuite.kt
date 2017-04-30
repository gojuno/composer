package com.gojuno.composer.html

import com.gojuno.composer.Device
import com.gojuno.composer.Suite
import com.google.gson.annotations.SerializedName
import java.util.concurrent.TimeUnit.NANOSECONDS

data class HtmlShortSuite(
        
        @SerializedName("id")
        val id: String,

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

fun Suite.toHtmlShortSuite(id: String) = HtmlShortSuite(
        id = id,
        passedCount = passedCount,
        ignoredCount = ignoredCount,
        failedCount = failedCount,
        durationMillis = NANOSECONDS.toMillis(durationNanos),
        devices = devices.map(Device::toHtmlDevice)
)
