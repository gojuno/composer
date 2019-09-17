package com.gojuno.composer

import com.gojuno.composer.InstrumentationTest.Status.Failed
import com.gojuno.composer.InstrumentationTest.Status.Ignored
import com.gojuno.composer.InstrumentationTest.Status.Passed
import rx.Observable
import java.io.File

data class InstrumentationTest(
        val index: Int,
        val total: Int,
        val className: String,
        val testName: String,
        val status: Status,
        val durationNanos: Long
) {

    sealed class Status {
        object Passed : Status()
        data class Ignored(val stacktrace: String = "") : Status()
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

sealed class InstrumentationEntry {
    /**
     * Corresponds to entries starting with INSTRUMENTATION_STATUS
     */
    data class Status(
        val numTests: Int,
        val stream: String,
        val id: String,
        val test: String,
        val clazz: String,
        val current: Int,
        val stack: String,
        val statusCode: StatusCode,
        val timestampNanos: Long
    ): InstrumentationEntry()

    /**
     * Corresponds to entries starting with INSTRUMENTATION_RESULT
     */
    data class Result(
        val message: String,
        val timestampNanos: Long
    ): InstrumentationEntry()
}

private fun String.substringBetween(first: String, second: String): String {
    val indexOfFirst = indexOf(first)

    if (indexOfFirst < 0) {
        return ""
    }

    val startIndex = indexOfFirst + first.length
    val endIndex = indexOf(second, startIndex).let { if (it <= 0) length else it }

    return substring(startIndex, endIndex)
}

private fun String.parseInstrumentationResultMessage() = Regex(
    pattern = "INSTRUMENTATION_RESULT: (?:stream|shortMsg|longMsg)=(.*?)INSTRUMENTATION_CODE",
    option = RegexOption.DOT_MATCHES_ALL
)
    .find(this)
    ?.groupValues
    ?.get(1)
    ?.trim()
    ?: ""

private fun String.parseInstrumentationStatusValue(key: String): String = this
        .substringBetween("INSTRUMENTATION_STATUS: $key=", "INSTRUMENTATION_STATUS")
        .trim()

private fun String.throwIfError(output: File) = when {
    contains("INSTRUMENTATION_STATUS: Error=Unable to find instrumentation info for") -> {
        val runner = substringBetween("ComponentInfo{", "}").substringAfter("/")
        throw Exception(
                "Instrumentation was unable to run tests using runner $runner.\n" +
                "Most likely you forgot to declare test runner in AndroidManifest.xml or build.gradle.\n" +
                "Detailed log can be found in ${output.path} or Logcat output.\n" +
                "See https://github.com/gojuno/composer/issues/79 for more info."
        )
    }

    else -> this
}

private fun parseInstrumentationEntry(str: String): InstrumentationEntry = when {
    str.startsWith("INSTRUMENTATION_RESULT") -> InstrumentationEntry.Result(
        message = str.parseInstrumentationResultMessage(),
        timestampNanos = System.nanoTime()
    )
    else -> InstrumentationEntry.Status(
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
                    null -> throw IllegalStateException("Unknown test status code [$statusCode], please report that to Composer maintainers $str")
                    else -> statusCode
                }
            },
        timestampNanos = System.nanoTime()
    )
}

// Reads stream in "tail -f" mode.
fun readInstrumentationOutput(output: File): Observable<InstrumentationEntry> {
    data class Result(val buffer: String = "", val readyForProcessing: Boolean = false)

    return tail(output)
            .map(String::trim)
            .map { it.throwIfError(output) }
            .takeUntil {
                // `INSTRUMENTATION_CODE: <code>` is the last line printed by instrumentation, even if 0 tests were run.
                it.startsWith("INSTRUMENTATION_CODE")
            }
            .scan(Result()) { previousResult, newLine ->
                val buffer = when (previousResult.readyForProcessing) {
                    true -> newLine
                    false -> "${previousResult.buffer}${System.lineSeparator()}$newLine"
                }

                Result(
                    buffer = buffer,
                    readyForProcessing = newLine.startsWith("INSTRUMENTATION_STATUS_CODE") || newLine.startsWith("INSTRUMENTATION_CODE")
                )
            }
            .filter { it.readyForProcessing }
            .map { it.buffer.trim() }
            .map(::parseInstrumentationEntry)
}

fun Observable<InstrumentationEntry>.asTests(): Observable<InstrumentationTest> = buffer(2)
    .filter { it.size == 2 }
    .map { (first, second) ->
        if (first !is InstrumentationEntry.Status) {
            throw IllegalStateException(
                "Unexpected order of instrumentation entries: encountered Result before a Status? " +
                    "This should never happen, please report this to Composer maintainers ($first, $second)"
            )
        }

        val secondNormalized = second.normalize()

        InstrumentationTest(
            index = first.current,
            total = first.numTests,
            className = first.clazz,
            testName = first.test,
            status = when (secondNormalized.statusCode) {
                StatusCode.Ok -> Passed
                StatusCode.Ignored  -> Ignored()
                StatusCode.AssumptionFailure -> Ignored(stacktrace = secondNormalized.stack)
                StatusCode.Failure -> Failed(stacktrace = secondNormalized.stack)
                StatusCode.Start -> throw IllegalStateException(
                    "Unexpected status code [Start] in second entry, " +
                        "please report that to Composer maintainers ($first, $second)"
                )
            },
            durationNanos = secondNormalized.timestampNanos - first.timestampNanos
        )
    }

private data class NormalizedInstrumentationEntry(
    val statusCode: StatusCode,
    val stack: String,
    val timestampNanos: Long
)

/**
 * @return a normalized set of data for this [InstrumentationEntry]
 */
private fun InstrumentationEntry.normalize() = when (this) {
    is InstrumentationEntry.Status -> NormalizedInstrumentationEntry(
        statusCode = statusCode,
        stack = stack,
        timestampNanos = timestampNanos
    )
    is InstrumentationEntry.Result -> NormalizedInstrumentationEntry(
        statusCode = StatusCode.Failure, /* if, after starting, our 2nd instrumentation entry is a Result instead of Status..
                                            we know the test execution ended prematurely and thus can be considered as a Failure */
        stack = message,
        timestampNanos = timestampNanos
    )
}
