package com.gojuno.composer

import org.apache.commons.lang3.StringEscapeUtils
import rx.Completable
import rx.Single
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit.NANOSECONDS

fun writeJunit4Report(testRunResult: TestRunResult, outputFile: File): Completable = Single
        .fromCallable { outputFile.parentFile.mkdirs() }
        .map {
            fun Long.toJunitSeconds(): String = (NANOSECONDS.toMillis(this) / 1000.0).toString()

            buildString(capacity = testRunResult.tests.size * 150) {
                appendln("""<?xml version="1.0" encoding="UTF-8"?>""")

                append("<testsuite ")
                apply {
                    append("""name="${testRunResult.testPackageName}" """)
                    append("""tests="${testRunResult.tests.size}" """)
                    append("""failures="${testRunResult.failedCount}" """)

                    // We can try to parse logcat output to get this info. See `android.support.test.internal.runner.listener.LogRunListener`.
                    append("""errors="0" """)
                    append("""skipped="0" """)

                    append("""time="${testRunResult.durationNanos.toJunitSeconds()}" """)
                    append("""timestamp="${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(testRunResult.timestampMillis))}" """)
                    append("""hostname="localhost" """)
                }
                appendln(">")

                apply {
                    appendln("<properties/>")
                    testRunResult.tests.forEach { test ->
                        append("<testcase ")
                        append("""classname="${test.className}" """)
                        append("""name="${test.testName}" """)
                        append("""time="${test.durationNanos.toJunitSeconds()}" """)

                        when (test.result) {
                            Test.Result.Passed -> {
                                appendln("/>")
                            }
                            is Test.Result.Failed -> {
                                appendln(">")

                                appendln("<failure>")
                                appendln(StringEscapeUtils.escapeXml10(test.result.stacktrace))
                                appendln("</failure>")

                                appendln("</testcase>")
                            }
                        }
                    }
                }

                appendln("</testsuite>")
            }
        }
        .map { xml -> outputFile.writeText(xml) }
        .toCompletable()
