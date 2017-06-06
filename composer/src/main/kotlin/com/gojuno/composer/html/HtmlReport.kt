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
                    suites = suites.mapIndexed { index, suite -> suite.toHtmlShortSuite(id = "$index", htmlReportDir = outputDir) }
            )
    )

    File(outputDir, "index.json").writeText(htmlIndexJson)

    val suitesDir = File(outputDir, "suites").apply { mkdirs() }

    suites.mapIndexed { suiteId, suite ->
        File(suitesDir, "$suiteId.json").writeText(gson.toJson(suite.toHtmlFullSuite(id = "$suiteId", htmlReportDir = suitesDir)))

        suite
                .tests
                .map { it to File(File(suitesDir, "$suiteId"), it.adbDevice.id).apply { mkdirs() } }
                .map { (test, testDir) -> test.toHtmlFullTest(htmlReportDir = testDir) to testDir }
                .forEach { (htmlFullTest, testDir) -> File(testDir, "${htmlFullTest.id}.json").writeText(gson.toJson(htmlFullTest)) }
    }
}

/**
 * Fixed version of `toRelativeString()` from Kotlin stdlib that forces use of absolute file paths.
 * See https://youtrack.jetbrains.com/issue/KT-14056
 */
fun File.relativePathTo(base: File): String = absoluteFile.toRelativeString(base.absoluteFile)
