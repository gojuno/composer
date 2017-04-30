package com.gojuno.composer.html

import com.google.gson.annotations.SerializedName

data class HtmlIndex(
        
        @SerializedName("suites")
        val suites: List<HtmlShortSuite>
)
