package com.gojuno.composer

import com.gojuno.commander.android.AdbDevice
import com.gojuno.composer.AdbDeviceTest.Status.*
import io.reactivex.observers.TestObserver
import io.reactivex.subscribers.TestSubscriber
import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

class JUnitReportSpec : Spek({

    val LF = System.getProperty("line.separator")

    describe("write test run result as junit4 report to file") {

        val adbDevice by memoized { AdbDevice(id = "testDevice", online = true) }
        val subscriber by memoized { TestObserver<Unit>() }
        val outputFile by memoized { testFile() }

        perform {
            writeJunit4Report(
                    suite = Suite(
                            testPackage = "com.gojuno.test",
                            devices = listOf(Device(
                                    id = adbDevice.id,
                                    logcat = testFile(),
                                    instrumentationOutput = testFile(),
                                    model = adbDevice.model
                            )),
                            tests = listOf(
                                    AdbDeviceTest(
                                            adbDevice = adbDevice,
                                            className = "test.class.name1",
                                            testName = "test1",
                                            status = Passed,
                                            durationNanos = SECONDS.toNanos(2),
                                            logcat = testFile(),
                                            files = emptyList(),
                                            screenshots = emptyList()
                                    ),
                                    AdbDeviceTest(
                                            adbDevice = adbDevice,
                                            className = "test.class.name2",
                                            testName = "test2",
                                            status = Failed(stacktrace = "multi${LF}line${LF}stacktrace"),
                                            durationNanos = MILLISECONDS.toNanos(3250),
                                            logcat = testFile(),
                                            files = emptyList(),
                                            screenshots = emptyList()
                                    ),
                                    AdbDeviceTest(
                                            adbDevice = adbDevice,
                                            className = "test.class.name3",
                                            testName = "test3",
                                            status = Passed,
                                            durationNanos = SECONDS.toNanos(1),
                                            logcat = testFile(),
                                            files = emptyList(),
                                            screenshots = emptyList()
                                    ),
                                    AdbDeviceTest(
                                            adbDevice = adbDevice,
                                            className = "test.class.name4",
                                            testName = "test4",
                                            status = Ignored(""),
                                            durationNanos = SECONDS.toNanos(0),
                                            logcat = testFile(),
                                            files = emptyList(),
                                            screenshots = emptyList()
                                    ),
                                    AdbDeviceTest(
                                            adbDevice = adbDevice,
                                            className = "test.class.name5",
                                            testName = "test5",
                                            status = Ignored("multi${LF}line${LF}stacktrace"),
                                            durationNanos = SECONDS.toNanos(0),
                                            logcat = testFile(),
                                            files = emptyList(),
                                            screenshots = emptyList()
                                    )
                            ),
                            passedCount = 2,
                            ignoredCount = 2,
                            failedCount = 1,
                            durationNanos = MILLISECONDS.toNanos(6250),
                            timestampMillis = 1490200150000
                    ),
                    outputFile = outputFile
            ).subscribe(subscriber)
        }

        it("produces correct xml report") {
            var expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <testsuite name="com.gojuno.test" tests="5" failures="1" errors="0" skipped="2" time="6.25" timestamp="2017-03-22T16:29:10" hostname="localhost">
                <properties/>
                <testcase classname="test.class.name1" name="test1" time="2.0"/>
                <testcase classname="test.class.name2" name="test2" time="3.25">
                <failure>
                multi
                line
                stacktrace
                </failure>
                </testcase>
                <testcase classname="test.class.name3" name="test3" time="1.0"/>
                <testcase classname="test.class.name4" name="test4" time="0.0">
                <skipped/>
                </testcase>
                <testcase classname="test.class.name5" name="test5" time="0.0">
                <skipped>
                multi
                line
                stacktrace
                </skipped>
                </testcase>
                </testsuite>
                """.trimIndent() + "\n"
            expected = normalizeLinefeed(expected)
            val actual = outputFile.readText()
            assertThat(actual).isEqualTo(expected)
        }

        it("emits completion") {
            subscriber.assertComplete()
        }

        it("does not emit values") {
            subscriber.assertNoValues()
        }

        it("does not emit error") {
            subscriber.assertNoErrors()
        }
    }
})
