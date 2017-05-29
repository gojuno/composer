package com.gojuno.composer.html

import com.gojuno.composer.Suite
import com.google.gson.Gson
import rx.Completable
import java.io.File

/**
 * Following file tree structure will be created:
 * - index.json
 * - suites/suiteId.json
 * - suites/deviceId/testId.json
 */
fun writeHtmlReport(gson: Gson, suites: List<Suite>, outputDir: File): Completable = Completable.fromCallable {
    outputDir.mkdirs()

    val htmlIndexJson = gson.toJson(
            HtmlIndex(
                    suites = suites.mapIndexed { index, suite -> suite.toHtmlShortSuite(id = "$index") }
            )
    )

    File(outputDir, "index.json").writeText(htmlIndexJson)

    val suitesDir = File(outputDir, "suites").apply { mkdirs() }

    suites.mapIndexed { suiteId, suite ->
        File(suitesDir, "$suiteId.json").writeText(gson.toJson(suite.toHtmlFullSuite(id = "$suiteId")))

        suite.tests.map { it.toHtmlFullTest() }.forEach { htmlFullTest ->
            File(File(File(suitesDir, "$suiteId"), htmlFullTest.deviceId).apply { mkdirs() }, "${htmlFullTest.id}.json").writeText(gson.toJson(htmlFullTest))
        }
    }
}
