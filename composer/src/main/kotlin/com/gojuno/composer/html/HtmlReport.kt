package com.gojuno.composer.html

import com.gojuno.composer.Suite
import com.google.gson.Gson
import org.apache.commons.lang3.StringEscapeUtils
import io.reactivex.Completable
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Following file tree structure will be created:
 * - index.json
 * - suites/suiteId.json
 * - suites/deviceId/testId.json
 */
fun writeHtmlReport(gson: Gson, suites: List<Suite>, outputDir: File, date: Date): Completable = Completable.fromCallable {
    outputDir.mkdirs()

    val htmlIndexJson = gson.toJson(
            HtmlIndex(
                    suites = suites.mapIndexed { index, suite -> suite.toHtmlShortSuite(id = "$index", htmlReportDir = outputDir) }
            )
    )

    val formattedDate = SimpleDateFormat("HH:mm:ss z, MMM d yyyy").apply { timeZone = TimeZone.getTimeZone("UTC") }.format(date)

    val appJs = File(outputDir, "app.min.js")
    inputStreamFromResources("html-report/app.min.js").copyTo(appJs.outputStream())

    val appCss = File(outputDir, "app.min.css")
    inputStreamFromResources("html-report/app.min.css").copyTo(appCss.outputStream())

    // index.html is a page that can render all kinds of inner pages: Index, Suite, Test.
    val indexHtml = inputStreamFromResources("html-report/index.html").reader().readText()

    val indexHtmlFile = File(outputDir, "index.html")

    fun File.relativePathToHtmlDir(): String = outputDir.relativePathTo(this.parentFile).let { relativePath ->
        when (relativePath) {
            "" -> relativePath
            else -> "$relativePath/"
        }
    }

    indexHtmlFile.writeText(indexHtml
            .replace("\${relative_path}", indexHtmlFile.relativePathToHtmlDir())
            .replace("\${data_json}", "window.mainData = $htmlIndexJson")
            .replace("\${date}", formattedDate)
            .replace("\${log}", "")
    )

    val suitesDir = File(outputDir, "suites").apply { mkdirs() }

    suites.mapIndexed { suiteId, suite ->
        val suiteJson = gson.toJson(suite.toHtmlFullSuite(id = "$suiteId", htmlReportDir = suitesDir))
        val suiteHtmlFile = File(suitesDir, "$suiteId.html")

        suiteHtmlFile.writeText(indexHtml
                .replace("\${relative_path}", suiteHtmlFile.relativePathToHtmlDir())
                .replace("\${data_json}", "window.suite = $suiteJson")
                .replace("\${date}", formattedDate)
                .replace("\${log}", "")
        )

        suite
                .tests
                .map { it to File(File(suitesDir, "$suiteId"), it.adbDevice.id).apply { mkdirs() } }
                .map { (test, testDir) -> Triple(test, test.toHtmlFullTest(suiteId = "$suiteId", htmlReportDir = testDir), testDir) }
                .forEach { (test, htmlTest, testDir) ->
                    val testJson = gson.toJson(htmlTest)
                    val testHtmlFile = File(testDir, "${htmlTest.id}.html")

                    testHtmlFile.writeText(indexHtml
                            .replace("\${relative_path}", testHtmlFile.relativePathToHtmlDir())
                            .replace("\${data_json}", "window.test = $testJson")
                            .replace("\${date}", formattedDate)
                            .replace("\${log}", generateLogcatHtml(test.logcat))
                    )
                }
    }
}

/**
 * Fixed version of `toRelativeString()` from Kotlin stdlib that forces use of absolute file paths.
 * See https://youtrack.jetbrains.com/issue/KT-14056
 */
fun File.relativePathTo(base: File): String = absoluteFile.toRelativeString(base.absoluteFile)

fun inputStreamFromResources(path: String): InputStream = Suite::class.java.classLoader.getResourceAsStream(path)

fun generateLogcatHtml(logcatOutput: File): String = when (logcatOutput.exists()) {
    false -> ""
    true -> logcatOutput
            .readLines()
            .map { line -> """<div class="log__${cssClassForLogcatLine(line)}">${StringEscapeUtils.escapeXml11(line)}</div>""" }
            .fold(StringBuilder("""<div class="content"><div class="card log">""")) { stringBuilder, line ->
                stringBuilder.appendln(line)
            }
            .appendln("""</div></div>""")
            .toString()
}

fun cssClassForLogcatLine(logcatLine: String): String {
    // Logcat line example: `06-07 16:55:14.490  2100  2100 I MicroDetectionWorker: #onError(false)`
    // First letter is Logcat level.
    return when (logcatLine.firstOrNull { it.isLetter() }) {
        'V' -> "verbose"
        'D' -> "debug"
        'I' -> "info"
        'W' -> "warning"
        'E' -> "error"
        'A' -> "assert"
        else -> "default"
    }
}
