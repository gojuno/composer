package com.gojuno.composer

import com.gojuno.composer.InstrumentationTest.Status.*
import rx.Observable
import java.io.File

data class InstrumentationTest(
        val className: String,
        val testName: String,
        val status: Status,
        val durationNanos: Long
) {

    sealed class Status {
        object Passed : Status()
        object Ignored : Status()
        data class Failed(val stacktrace: String) : Status()
    }
}

/**
 * @see android.support.test.internal.runner.listener.InstrumentationResultPrinter
 */
enum class StatusCode(val code: Int) {
    Start(1),
    Ok(0),
    Failure(-2),
    Ignored(-3),
    AssumptionFailure(-4)
}

data class InstrumentationEntry(
        val numTests: Int,
        val stream: String,
        val id: String,
        val test: String,
        val clazz: String,
        val current: Int,
        val stack: String,
        val statusCode: StatusCode,
        val timestampNanos: Long
)

private fun String.substringBetween(first: String, second: String): String {
    val indexOfFirst = indexOf(first)

    if (indexOfFirst < 0) {
        return ""
    }

    val startIndex = indexOfFirst + first.length
    val endIndex = indexOf(second, startIndex).let { if (it <= 0) length else it }

    return substring(startIndex, endIndex)
}

private fun String.parseInstrumentationStatusValue(key: String): String = substringBetween("INSTRUMENTATION_STATUS: $key=", "INSTRUMENTATION_STATUS")
        .trim()

private fun parseInstrumentationEntry(str: String): InstrumentationEntry =
        InstrumentationEntry(
                numTests = str.parseInstrumentationStatusValue("numtests").toInt(),
                stream = str.parseInstrumentationStatusValue("stream"),
                stack = str.parseInstrumentationStatusValue("stack"),
                id = str.parseInstrumentationStatusValue("id"),
                test = str.parseInstrumentationStatusValue("test"),
                clazz = str.parseInstrumentationStatusValue("class"),
                current = str.parseInstrumentationStatusValue("current").toInt(),
                statusCode = str.substringBetween("INSTRUMENTATION_STATUS_CODE: ", "INSTRUMENTATION_STATUS")
                        .trim()
                        .toInt()
                        .let { code ->
                            StatusCode.values().firstOrNull { it.code == code }
                        }
                        .let { statusCode ->
                            when (statusCode) {
                                null -> throw IllegalStateException("Unknown test status status code [$statusCode], please report that to Composer maintainers $str")
                                else -> statusCode
                            }
                        },
                timestampNanos = System.nanoTime()
        )

// Reads stream in "tail -f" mode.
fun readInstrumentationOutput(output: File): Observable<InstrumentationEntry> {
    data class result(val buffer: String = "", val readyForProcessing: Boolean = false)

    return tail(output)
            .map { it.trim() }
            // `INSTRUMENTATION_CODE: -1` is last line printed by instrumentation, even if 0 tests were run.
            .takeWhile { !it.startsWith("INSTRUMENTATION_CODE") }
            .scan(result()) { previousResult, newLine ->
                val buffer = when (previousResult.readyForProcessing) {
                    true -> newLine
                    false -> "${previousResult.buffer}${System.lineSeparator()}$newLine"
                }

                result(buffer = buffer, readyForProcessing = newLine.startsWith("INSTRUMENTATION_STATUS_CODE"))
            }
            .filter { it.readyForProcessing }
            .map { parseInstrumentationEntry(it.buffer) }
}

fun Observable<InstrumentationEntry>.asTests(): Observable<InstrumentationTest> {
    data class result(val entries: List<InstrumentationEntry> = emptyList(), val tests: List<InstrumentationTest> = emptyList(), val totalTestsCount: Int = 0)

    return this
            .scan(result()) { previousResult, newEntry ->
                val entries = previousResult.entries + newEntry
                val tests: List<InstrumentationTest> = entries
                        .mapIndexed { index, first ->
                            val second = entries
                                    .subList(index + 1, entries.size)
                                    .firstOrNull {
                                        first.clazz == it.clazz
                                                &&
                                                first.test == it.test
                                                &&
                                                first.current == it.current
                                                &&
                                                first.statusCode != it.statusCode
                                    }

                            if (second == null) null else first to second
                        }
                        .filterNotNull()
                        .map { (first, second) ->
                            InstrumentationTest(
                                    className = first.clazz,
                                    testName = first.test,
                                    status = when (second.statusCode) {
                                        StatusCode.Ok -> Passed
                                        StatusCode.Ignored -> Ignored
                                        StatusCode.Failure, StatusCode.AssumptionFailure -> Failed(stacktrace = second.stack)
                                        StatusCode.Start -> throw IllegalStateException("Unexpected status code [${second.statusCode}] in second entry, please report that to Composer maintainers ($first, $second)")
                                    },
                                    durationNanos = second.timestampNanos - first.timestampNanos
                            )
                        }

                result(
                        entries = entries.filter { entry -> tests.firstOrNull { it.className == entry.clazz && it.testName == entry.test } == null },
                        tests = tests,
                        totalTestsCount = previousResult.totalTestsCount + tests.size
                )
            }
            .takeUntil {
                if (it.entries.count { it.current == it.numTests } == 2) {
                    if (it.totalTestsCount < it.entries.first().numTests) {
                        throw IllegalStateException("Less tests were emitted than Instrumentation reported: $it")
                    }

                    true
                } else {
                    false
                }
            }
            .filter { it.tests.isNotEmpty() }
            .flatMap { Observable.from(it.tests) }
}
