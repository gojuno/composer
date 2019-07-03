package com.gojuno.composer

import com.gojuno.composer.AdbDeviceTest.Status.*
import io.reactivex.Completable
import io.reactivex.Single
import org.apache.commons.text.StringEscapeUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit.NANOSECONDS

fun writeJunit4Report(suite: Suite, outputFile: File): Completable = Single
        .fromCallable { outputFile.parentFile.mkdirs() }
        .map {
            fun Long.toJunitSeconds(): String = (NANOSECONDS.toMillis(this) / 1000.0).toString()

            buildString(capacity = suite.tests.size * 150) {
                appendln("""<?xml version="1.0" encoding="UTF-8"?>""")

                append("<testsuite ")
                apply {
                    append("""name="${suite.testPackage}" """)
                    append("""tests="${suite.tests.size}" """)
                    append("""failures="${suite.failedCount}" """)

                    // We can try to parse logcat output to get this info. See `android.support.test.internal.runner.listener.LogRunListener`.
                    append("""errors="0" """)
                    append("""skipped="${suite.ignoredCount}" """)

                    append("""time="${suite.durationNanos.toJunitSeconds()}" """)
                    append("""timestamp="${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(suite.timestampMillis))}" """)
                    append("""hostname="localhost"""")
                }
                appendln(">")

                apply {
                    appendln("<properties/>")
                    suite.tests.forEach { test ->
                        append("<testcase ")
                        append("""classname="${test.className}" """)
                        append("""name="${test.testName}" """)
                        append("""time="${test.durationNanos.toJunitSeconds()}"""")

                        when (test.status) {
                            Passed -> {
                                appendln("/>")
                            }
                            is Ignored -> {
                                appendln(">")
                                if (test.status.stacktrace.isEmpty()) {
                                    appendln("<skipped/>")
                                } else {
                                    appendln("<skipped>")
                                    appendln(StringEscapeUtils.escapeXml10(test.status.stacktrace))
                                    appendln("</skipped>")
                                }
                                appendln("</testcase>")
                            }
                            is Failed -> {
                                appendln(">")

                                appendln("<failure>")
                                appendln(StringEscapeUtils.escapeXml10(test.status.stacktrace))
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
        .ignoreElement()
