package com.gojuno.composer

import com.gojuno.composer.InstrumentationTest.Status.*
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.it
import rx.observers.TestSubscriber
import java.util.concurrent.TimeUnit.SECONDS

class InstrumentationSpec : Spek({

    context("read output with failed test") {

        val entries by memoized { readInstrumentationOutput(fileFromJarResources<InstrumentationSpec>("instrumentation-output-failed-test.txt")) }
        val entriesSubscriber by memoized { TestSubscriber<InstrumentationEntry>() }

        perform {
            entries.subscribe(entriesSubscriber)
            entriesSubscriber.awaitTerminalEvent(30, SECONDS)
        }

        it("emits expected entries") {
            // We have no control over system time in tests.
            assertThat(entriesSubscriber.onNextEvents.map { it.copy(timestampNanos = 0) }).isEqualTo(listOf(
                    InstrumentationEntry(
                            numTests = 4,
                            stream = "com.example.test.TestClass:",
                            id = "AndroidJUnitRunner",
                            test = "test1",
                            clazz = "com.example.test.TestClass",
                            current = 1,
                            stack = "",
                            statusCode = StatusCode.Start,
                            timestampNanos = 0
                    ),
                    InstrumentationEntry(
                            numTests = 4,
                            stream = """Error in test1(com.example.test.TestClass):
java.net.UnknownHostException: Test Exception
at com.example.test.TestClass.test1.1.invoke(TestClass.kt:245)
at com.example.test.TestClass.test1.1.invoke(TestClass.kt:44)
at com.example.test.TestClass.test1(TestClass.kt:238)
at java.lang.reflect.Method.invoke(Native Method)
at org.junit.runners.model.FrameworkMethod.1.runReflectiveCall(FrameworkMethod.java:50)
at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)
at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
at org.junit.rules.ExpectedException.ExpectedExceptionStatement.evaluate(ExpectedException.java:239)
at com.example.test.utils.LaunchAppRule.apply.1.evaluate(LaunchAppRule.kt:36)
at com.example.test.utils.RetryRule.runTest(RetryRule.kt:43)
at com.example.test.utils.RetryRule.access.runTest(RetryRule.kt:14)
at com.example.test.utils.RetryRule.apply.1.evaluate(RetryRule.kt:29)
at org.junit.rules.RunRules.evaluate(RunRules.java:20)
at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:78)
at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:57)
at org.junit.runners.ParentRunner.3.run(ParentRunner.java:290)
at org.junit.runners.ParentRunner.1.schedule(ParentRunner.java:71)
at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
at org.junit.runners.ParentRunner.access.000(ParentRunner.java:58)
at org.junit.runners.ParentRunner.2.evaluate(ParentRunner.java:268)
at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
at org.junit.runners.Suite.runChild(Suite.java:128)
at org.junit.runners.Suite.runChild(Suite.java:27)
at org.junit.runners.ParentRunner.3.run(ParentRunner.java:290)
at org.junit.runners.ParentRunner.1.schedule(ParentRunner.java:71)
at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
at org.junit.runners.ParentRunner.access.000(ParentRunner.java:58)
at org.junit.runners.ParentRunner.2.evaluate(ParentRunner.java:268)
at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
at org.junit.runner.JUnitCore.run(JUnitCore.java:115)
at android.support.test.internal.runner.TestExecutor.execute(TestExecutor.java:59)
at android.support.test.runner.JunoAndroidRunner.onStart(JunoAndroidRunner.kt:107)
at android.app.Instrumentation.InstrumentationThread.run(Instrumentation.java:1932)""",
                            id = "AndroidJUnitRunner",
                            test = "test1",
                            clazz = "com.example.test.TestClass",
                            current = 1,
                            stack = """java.net.UnknownHostException: Test Exception
at com.example.test.TestClass.test1.1.invoke(TestClass.kt:245)
at com.example.test.TestClass.test1.1.invoke(TestClass.kt:44)
at com.example.test.TestClass.test1(TestClass.kt:238)
at java.lang.reflect.Method.invoke(Native Method)
at org.junit.runners.model.FrameworkMethod.1.runReflectiveCall(FrameworkMethod.java:50)
at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)
at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
at org.junit.rules.ExpectedException.ExpectedExceptionStatement.evaluate(ExpectedException.java:239)
at com.example.test.utils.LaunchAppRule.apply.1.evaluate(LaunchAppRule.kt:36)
at com.example.test.utils.RetryRule.runTest(RetryRule.kt:43)
at com.example.test.utils.RetryRule.access.runTest(RetryRule.kt:14)
at com.example.test.utils.RetryRule.apply.1.evaluate(RetryRule.kt:29)
at org.junit.rules.RunRules.evaluate(RunRules.java:20)
at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:78)
at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:57)
at org.junit.runners.ParentRunner.3.run(ParentRunner.java:290)
at org.junit.runners.ParentRunner.1.schedule(ParentRunner.java:71)
at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
at org.junit.runners.ParentRunner.access.000(ParentRunner.java:58)
at org.junit.runners.ParentRunner.2.evaluate(ParentRunner.java:268)
at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
at org.junit.runners.Suite.runChild(Suite.java:128)
at org.junit.runners.Suite.runChild(Suite.java:27)
at org.junit.runners.ParentRunner.3.run(ParentRunner.java:290)
at org.junit.runners.ParentRunner.1.schedule(ParentRunner.java:71)
at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
at org.junit.runners.ParentRunner.access.000(ParentRunner.java:58)
at org.junit.runners.ParentRunner.2.evaluate(ParentRunner.java:268)
at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
at org.junit.runner.JUnitCore.run(JUnitCore.java:115)
at android.support.test.internal.runner.TestExecutor.execute(TestExecutor.java:59)
at android.support.test.runner.JunoAndroidRunner.onStart(JunoAndroidRunner.kt:107)
at android.app.Instrumentation.InstrumentationThread.run(Instrumentation.java:1932)""",
                            statusCode = StatusCode.Failure,
                            timestampNanos = 0
                    ),
                    InstrumentationEntry(
                            numTests = 4,
                            stream = "",
                            id = "AndroidJUnitRunner",
                            test = "test2",
                            clazz = "com.example.test.TestClass",
                            current = 2,
                            stack = "",
                            statusCode = StatusCode.Start,
                            timestampNanos = 0
                    ),
                    InstrumentationEntry(
                            numTests = 4,
                            stream = ".",
                            id = "AndroidJUnitRunner",
                            test = "test2",
                            clazz = "com.example.test.TestClass",
                            current = 2,
                            stack = "",
                            statusCode = StatusCode.Ok,
                            timestampNanos = 0
                    ),
                    InstrumentationEntry(
                            numTests = 4,
                            stream = "com.example.test.TestClass:",
                            id = "AndroidJUnitRunner",
                            test = "test3",
                            clazz = "com.example.test.TestClass",
                            current = 3,
                            stack = "",
                            statusCode = StatusCode.Start,
                            timestampNanos = 0
                    ),
                    InstrumentationEntry(
                            numTests = 4,
                            stream = ".",
                            id = "AndroidJUnitRunner",
                            test = "test3",
                            clazz = "com.example.test.TestClass",
                            current = 3,
                            stack = "",
                            statusCode = StatusCode.Ok,
                            timestampNanos = 0
                    ),
                    InstrumentationEntry(
                            numTests = 4,
                            stream = "",
                            id = "AndroidJUnitRunner",
                            test = "test4",
                            clazz = "com.example.test.TestClass",
                            current = 4,
                            stack = "",
                            statusCode = StatusCode.Start,
                            timestampNanos = 0
                    ),
                    InstrumentationEntry(
                            numTests = 4,
                            stream = ".",
                            id = "AndroidJUnitRunner",
                            test = "test4",
                            clazz = "com.example.test.TestClass",
                            current = 4,
                            stack = "",
                            statusCode = StatusCode.Ok,
                            timestampNanos = 0
                    )
            ))
        }

        it("completest stream") {
            entriesSubscriber.assertCompleted()
        }

        it("does not emit error") {
            entriesSubscriber.assertNoErrors()
        }

        context("as tests") {

            val testsSubscriber by memoized { TestSubscriber<InstrumentationTest>() }

            perform {
                entries.asTests().subscribe(testsSubscriber)
                testsSubscriber.awaitTerminalEvent(30, SECONDS)
            }

            it("emits expected tests") {
                // We have no control over system time in tests.
                assertThat(testsSubscriber.onNextEvents.map { it.copy(durationNanos = 0) }).isEqualTo(listOf(
                        InstrumentationTest(
                                className = "com.example.test.TestClass",
                                testName = "test1",
                                status = Failed(stacktrace = """java.net.UnknownHostException: Test Exception
at com.example.test.TestClass.test1.1.invoke(TestClass.kt:245)
at com.example.test.TestClass.test1.1.invoke(TestClass.kt:44)
at com.example.test.TestClass.test1(TestClass.kt:238)
at java.lang.reflect.Method.invoke(Native Method)
at org.junit.runners.model.FrameworkMethod.1.runReflectiveCall(FrameworkMethod.java:50)
at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)
at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
at org.junit.rules.ExpectedException.ExpectedExceptionStatement.evaluate(ExpectedException.java:239)
at com.example.test.utils.LaunchAppRule.apply.1.evaluate(LaunchAppRule.kt:36)
at com.example.test.utils.RetryRule.runTest(RetryRule.kt:43)
at com.example.test.utils.RetryRule.access.runTest(RetryRule.kt:14)
at com.example.test.utils.RetryRule.apply.1.evaluate(RetryRule.kt:29)
at org.junit.rules.RunRules.evaluate(RunRules.java:20)
at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:78)
at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:57)
at org.junit.runners.ParentRunner.3.run(ParentRunner.java:290)
at org.junit.runners.ParentRunner.1.schedule(ParentRunner.java:71)
at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
at org.junit.runners.ParentRunner.access.000(ParentRunner.java:58)
at org.junit.runners.ParentRunner.2.evaluate(ParentRunner.java:268)
at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
at org.junit.runners.Suite.runChild(Suite.java:128)
at org.junit.runners.Suite.runChild(Suite.java:27)
at org.junit.runners.ParentRunner.3.run(ParentRunner.java:290)
at org.junit.runners.ParentRunner.1.schedule(ParentRunner.java:71)
at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
at org.junit.runners.ParentRunner.access.000(ParentRunner.java:58)
at org.junit.runners.ParentRunner.2.evaluate(ParentRunner.java:268)
at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
at org.junit.runner.JUnitCore.run(JUnitCore.java:115)
at android.support.test.internal.runner.TestExecutor.execute(TestExecutor.java:59)
at android.support.test.runner.JunoAndroidRunner.onStart(JunoAndroidRunner.kt:107)
at android.app.Instrumentation.InstrumentationThread.run(Instrumentation.java:1932)"""),
                                durationNanos = 0
                        ),
                        InstrumentationTest(
                                className = "com.example.test.TestClass",
                                testName = "test2",
                                status = Passed,
                                durationNanos = 0
                        ),
                        InstrumentationTest(
                                className = "com.example.test.TestClass",
                                testName = "test3",
                                status = Passed,
                                durationNanos = 0
                        ),
                        InstrumentationTest(
                                className = "com.example.test.TestClass",
                                testName = "test4",
                                status = Passed,
                                durationNanos = 0
                        )
                ))
            }

            it("completes stream") {
                testsSubscriber.assertCompleted()
            }

            it("does not emit error") {
                testsSubscriber.assertNoErrors()
            }
        }
    }

    context("read output with 0 tests") {

        val entries by memoized { readInstrumentationOutput(fileFromJarResources<InstrumentationSpec>("instrumentation-output-0-tests.txt")) }
        val entriesSubscriber by memoized { TestSubscriber<InstrumentationEntry>() }

        perform {
            entries.subscribe(entriesSubscriber)
            entriesSubscriber.awaitTerminalEvent(30, SECONDS)
        }

        it("does not emit any entry") {
            entriesSubscriber.assertNoValues()
        }

        it("completest stream") {
            entriesSubscriber.assertCompleted()
        }

        it("does not emit error") {
            entriesSubscriber.assertNoErrors()
        }

        context("as tests") {

            val testsSubscriber by memoized { TestSubscriber<InstrumentationTest>() }

            perform {
                entries.asTests().subscribe(testsSubscriber)
                testsSubscriber.awaitTerminalEvent(30, SECONDS)
            }

            it("does not emit any test") {
                testsSubscriber.assertNoValues()
            }

            it("completest stream") {
                testsSubscriber.assertCompleted()
            }

            it("does not emit error") {
                testsSubscriber.assertNoErrors()
            }
        }
    }

    context("read unordered output") {

        val entries by memoized { readInstrumentationOutput(fileFromJarResources<InstrumentationSpec>("instrumentation-unordered-output.txt")) }
        val entriesSubscriber by memoized { TestSubscriber<InstrumentationEntry>() }

        perform {
            entries.subscribe(entriesSubscriber)
            entriesSubscriber.awaitTerminalEvent(30, SECONDS)
        }

        it("emits expected entries") {
            // We have no control over system time in tests.
            assertThat(entriesSubscriber.onNextEvents.map { it.copy(timestampNanos = 0) }).isEqualTo(listOf(
                    InstrumentationEntry(
                            numTests = 3,
                            stream = "com.example.test.TestClass:",
                            id = "AndroidJUnitRunner",
                            test = "test1",
                            clazz = "com.example.test.TestClass",
                            current = 1,
                            stack = "",
                            statusCode = StatusCode.Start,
                            timestampNanos = 0
                    ),
                    InstrumentationEntry(
                            numTests = 3,
                            stream = ".",
                            id = "AndroidJUnitRunner",
                            test = "test1",
                            clazz = "com.example.test.TestClass",
                            current = 1,
                            stack = "",
                            statusCode = StatusCode.Ok,
                            timestampNanos = 0
                    ),
                    InstrumentationEntry(
                            numTests = 3,
                            stream = "com.example.test.TestClass:",
                            id = "AndroidJUnitRunner",
                            test = "test2",
                            clazz = "com.example.test.TestClass",
                            current = 2,
                            stack = "",
                            statusCode = StatusCode.Start,
                            timestampNanos = 0
                    ),
                    InstrumentationEntry(
                            numTests = 3,
                            stream = "",
                            id = "AndroidJUnitRunner",
                            test = "test3",
                            clazz = "com.example.test.TestClass",
                            current = 3,
                            stack = "",
                            statusCode = StatusCode.Start,
                            timestampNanos = 0
                    ),
                    InstrumentationEntry(
                            numTests = 3,
                            stream = ".",
                            id = "AndroidJUnitRunner",
                            test = "test2",
                            clazz = "com.example.test.TestClass",
                            current = 2,
                            stack = "",
                            statusCode = StatusCode.Ok,
                            timestampNanos = 0
                    ),
                    InstrumentationEntry(
                            numTests = 3,
                            stream = ".",
                            id = "AndroidJUnitRunner",
                            test = "test3",
                            clazz = "com.example.test.TestClass",
                            current = 3,
                            stack = "",
                            statusCode = StatusCode.Ok,
                            timestampNanos = 0
                    ))
            )
        }

        it("completest stream") {
            entriesSubscriber.assertCompleted()
        }

        it("does not emit error") {
            entriesSubscriber.assertNoErrors()
        }

        context("as tests") {

            val testsSubscriber by memoized { TestSubscriber<InstrumentationTest>() }

            perform {
                entries.asTests().subscribe(testsSubscriber)
                testsSubscriber.awaitTerminalEvent(30, SECONDS)
            }

            it("emits expected tests") {
                assertThat(testsSubscriber.onNextEvents.map { it.copy(durationNanos = 0) }).isEqualTo(listOf(
                        InstrumentationTest(
                                className = "com.example.test.TestClass",
                                testName = "test1",
                                status = Passed,
                                durationNanos = 0L
                        ),
                        InstrumentationTest(
                                className = "com.example.test.TestClass",
                                testName = "test2",
                                status = Passed,
                                durationNanos = 0L
                        ),
                        InstrumentationTest(
                                className = "com.example.test.TestClass",
                                testName = "test3",
                                status = Passed,
                                durationNanos = 0L
                        )
                ))
            }

            it("completest stream") {
                testsSubscriber.assertCompleted()
            }

            it("does not emit error") {
                testsSubscriber.assertNoErrors()
            }
        }
    }

    context("read output with ignored test") {

        val entries by memoized { readInstrumentationOutput(fileFromJarResources<InstrumentationSpec>("instrumentation-output-ignored-test.txt")) }
        val entriesSubscriber by memoized { TestSubscriber<InstrumentationEntry>() }

        perform {
            entries.subscribe(entriesSubscriber)
            entriesSubscriber.awaitTerminalEvent(30, SECONDS)
        }

        it("emits expected entries") {
            // We have no control over system time in tests.
            assertThat(entriesSubscriber.onNextEvents.map { it.copy(timestampNanos = 0) }).isEqualTo(listOf(
                    InstrumentationEntry(
                            numTests = 2,
                            stream = "com.example.test.TestClass:",
                            id = "AndroidJUnitRunner",
                            test = "test1",
                            clazz = "com.example.test.TestClass",
                            current = 1,
                            stack = "",
                            statusCode = StatusCode.Start,
                            timestampNanos = 0
                    ),
                    InstrumentationEntry(
                            numTests = 2,
                            stream = ".",
                            id = "AndroidJUnitRunner",
                            test = "test1",
                            clazz = "com.example.test.TestClass",
                            current = 1,
                            stack = "",
                            statusCode = StatusCode.Ok,
                            timestampNanos = 0
                    ),
                    InstrumentationEntry(
                            numTests = 2,
                            stream = "",
                            id = "AndroidJUnitRunner",
                            test = "test2",
                            clazz = "com.example.test.TestClass",
                            current = 2,
                            stack = "",
                            statusCode = StatusCode.Start,
                            timestampNanos = 0
                    ),
                    InstrumentationEntry(
                            numTests = 2,
                            stream = "",
                            id = "AndroidJUnitRunner",
                            test = "test2",
                            clazz = "com.example.test.TestClass",
                            current = 2,
                            stack = "",
                            statusCode = StatusCode.Ignored,
                            timestampNanos = 0
                    ))
            )
        }

        it("completest stream") {
            entriesSubscriber.assertCompleted()
        }

        it("does not emit error") {
            entriesSubscriber.assertNoErrors()
        }

        context("as tests") {

            val testsSubscriber by memoized { TestSubscriber<InstrumentationTest>() }

            perform {
                entries.asTests().subscribe(testsSubscriber)
                testsSubscriber.awaitTerminalEvent(30, SECONDS)
            }

            it("emits expected tests") {
                assertThat(testsSubscriber.onNextEvents.map { it.copy(durationNanos = 0) }).isEqualTo(listOf(
                        InstrumentationTest(
                                className = "com.example.test.TestClass",
                                testName = "test1",
                                status = Passed,
                                durationNanos = 0L
                        ),
                        InstrumentationTest(
                                className = "com.example.test.TestClass",
                                testName = "test2",
                                status = Ignored,
                                durationNanos = 0L
                        )
                ))
            }
        }
    }
})

