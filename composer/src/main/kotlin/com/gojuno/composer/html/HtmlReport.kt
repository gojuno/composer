package com.gojuno.composer.html

import com.gojuno.composer.Suite
import com.google.gson.Gson
import rx.Completable
import java.io.File

fun writeHtmlReport(gson: Gson, suites: List<Suite>, outputDir: File): Completable = Completable.fromCallable {
    outputDir.mkdirs()

    val htmlIndexJson = gson.toJson(
            HtmlIndex(
                    suites = suites.mapIndexed { index, suite -> suite.toHtmlShortSuite(id = "$index") }
            )
    )

    File(outputDir, "index.json").writeText(htmlIndexJson)

    val suitesDir = File(outputDir, "suites").apply { mkdirs() }

    suites.mapIndexed { index, suite ->
        val suiteJson = gson.toJson(suite.toHtmlFullSuite(id = "$index"))
        File(suitesDir, "$index.json").writeText(suiteJson)
    }
}
