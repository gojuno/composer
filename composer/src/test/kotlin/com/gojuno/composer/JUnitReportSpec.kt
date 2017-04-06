package com.gojuno.composer

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.it
import rx.observers.TestSubscriber
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

class JUnitReportSpec : Spek({

    context("write test run result to junit4 report to file") {

        val subscriber by memoized { TestSubscriber<Unit>() }
        val outputFile by memoized { testFile() }

        perform {
            writeJunit4Report(
                    testRunResult = TestRunResult(
                            testPackageName = "com.gojuno.test",
                            tests = listOf(
                                    Test(
                                            className = "test.class.name1",
                                            testName = "test1",
                                            result = Test.Result.Passed,
                                            durationNanos = SECONDS.toNanos(2)
                                    ),
                                    Test(
                                            className = "test.class.name2",
                                            testName = "test2",
                                            result = Test.Result.Failed(stacktrace = "multi\nline\nstacktrace"),
                                            durationNanos = MILLISECONDS.toNanos(3250)
                                    ),
                                    Test(
                                            className = "test.class.name1",
                                            testName = "test1",
                                            result = Test.Result.Passed,
                                            durationNanos = SECONDS.toNanos(1)
                                    )
                            ),
                            passedCount = 2,
                            failedCount = 1,
                            durationNanos = MILLISECONDS.toNanos(6250),
                            timestampMillis = 1490200150000
                    ),
                    outputFile = outputFile
            ).subscribe(subscriber)
        }

        it("produces correct xml report") {
            assertThat(outputFile.readText()).isEqualTo("""
                <?xml version="1.0" encoding="UTF-8"?>
                <testsuite name="com.gojuno.test" tests="3" failures="1" errors="0" skipped="0" time="6.25" timestamp="2017-03-22T16:29:10" hostname="localhost" >
                <properties/>
                <testcase classname="test.class.name1" name="test1" time="2.0" />
                <testcase classname="test.class.name2" name="test2" time="3.25" >
                <failure>
                multi
                line
                stacktrace
                </failure>
                </testcase>
                <testcase classname="test.class.name1" name="test1" time="1.0" />
                </testsuite>
                """.trimIndent() + "\n"
            )
        }

        it("emits completion") {
            subscriber.assertCompleted()
        }

        it("does not emit values") {
            subscriber.assertNoValues()
        }

        it("does not emit error") {
            subscriber.assertNoErrors()
        }
    }
})
