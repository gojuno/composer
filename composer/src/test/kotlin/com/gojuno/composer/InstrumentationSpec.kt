package com.gojuno.composer

import com.gojuno.composer.Test.Result.Failed
import com.gojuno.composer.Test.Result.Passed
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.it
import rx.observers.TestSubscriber

class InstrumentationSpec : Spek({

    context("read output with failed test") {

        val subscriber by memoized { TestSubscriber<Test>() }
        val tests by memoized { mutableListOf<Test>() }

        perform {
            readInstrumentationOutput(fileFromJarResources<InstrumentationSpec>("instrumentation-output-failed-test.txt"))
                    .asTests()
                    .subscribe(subscriber)

            subscriber.awaitTerminalEvent()
            subscriber.onNextEvents.forEach { test ->
                tests.add(test.copy(durationNanos = 0)) // We have no control over system time in tests.
            }
        }

        it("emits expected tests") {
            assertThat(tests).isEqualTo(listOf(
                    Test(
                            className = "net.juno.rd.functional_tests.tests.AddCreditCardForUserWithNoPaymentsTest",
                            testName = "on400WithUnknownErrorReceivedOnAddPaymentMethodShowsGenericServerError",
                            result = Failed(stacktrace = """java.net.UnknownHostException: Artem test
	at net.juno.rd.functional_tests.tests.AddCreditCardForUserWithNoPaymentsTest.on400WithUnknownErrorReceivedOnAddPaymentMethodShowsGenericServerError.1.invoke(AddCreditCardForUserWithNoPaymentsTest.kt:245)
	at net.juno.rd.functional_tests.tests.AddCreditCardForUserWithNoPaymentsTest.on400WithUnknownErrorReceivedOnAddPaymentMethodShowsGenericServerError.1.invoke(AddCreditCardForUserWithNoPaymentsTest.kt:44)
	at net.juno.rd.functional_tests.screens.AddCreditCardScreen.Companion.invoke(AddCreditCardScreen.kt:23)
	at net.juno.rd.functional_tests.tests.AddCreditCardForUserWithNoPaymentsTest.on400WithUnknownErrorReceivedOnAddPaymentMethodShowsGenericServerError(AddCreditCardForUserWithNoPaymentsTest.kt:238)
	at java.lang.reflect.Method.invoke(Native Method)
	at org.junit.runners.model.FrameworkMethod.1.runReflectiveCall(FrameworkMethod.java:50)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
	at org.junit.rules.ExpectedException.ExpectedExceptionStatement.evaluate(ExpectedException.java:239)
	at net.juno.rd.functional_tests.utils.LaunchAppRule.apply.1.evaluate(LaunchAppRule.kt:36)
	at com.gojuno.mockserver1.ServerImpl.apply.1.evaluate(ServerImpl.kt:112)
	at net.juno.rd.functional_tests.utils.RetryRule.runTest(RetryRule.kt:43)
	at net.juno.rd.functional_tests.utils.RetryRule.access.runTest(RetryRule.kt:14)
	at net.juno.rd.functional_tests.utils.RetryRule.apply.1.evaluate(RetryRule.kt:29)
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
                    Test(
                            className = "net.juno.rd.functional_tests.tests.AddCreditCardForUserWithNoPaymentsTest",
                            testName = "onMastercardCreditCardFirst2NumbersEnteredDisplaysMasterCardIcon",
                            result = Passed,
                            durationNanos = 0
                    ),
                    Test(
                            className = "net.juno.rd.functional_tests.tests.AddCreditCardForUserWithPaymentMethod",
                            testName = "successfullyAddsAmericanExpressCardShowsNumberForItAndItIsNotDefault",
                            result = Passed,
                            durationNanos = 0
                    ),
                    Test(
                            className = "net.juno.rd.functional_tests.tests.AddCreditCardForUserWithPaymentMethod",
                            testName = "successfullyAddsMasterCardShowsNumberForItAndItIsNotDefault",
                            result = Passed,
                            durationNanos = 0
                    ),
                    Test(
                            className = "net.juno.rd.functional_tests.tests.ConfirmationWithUserWithNoPaymentMethodTest",
                            testName = "onAddPaymentButtonTappedOpensAddCreditCardScreen",
                            result = Passed,
                            durationNanos = 0
                    ),
                    Test(
                            className = "net.juno.rd.functional_tests.tests.ForgotPasswordTest",
                            testName = "onEmailAddressFieldInvalidTextEnteredEmailMeAResetLinkButtonIsNotActive",
                            result = Passed,
                            durationNanos = 0
                    ),
                    Test(
                            className = "net.juno.rd.functional_tests.tests.ForgotPasswordWithPredefinedEmailTest",
                            testName = "tapOnResetLinkShowsAlert",
                            result = Passed,
                            durationNanos = 0
                    ),
                    Test(
                            className = "net.juno.rd.functional_tests.tests.LoginTest",
                            testName = "onEmailEnteredAndLoginButtonTappedShowsPasswordCannotBeEmpty",
                            result = Passed,
                            durationNanos = 0
                    ),
                    Test(
                            className = "net.juno.rd.functional_tests.tests.LoginTest",
                            testName = "onLoginWithUnsettledDebtDisplaysYourPaymentMethodDeclinedAlert",
                            result = Passed,
                            durationNanos = 0
                    ),
                    Test(
                            className = "net.juno.rd.functional_tests.tests.SignUpFirstNameTest",
                            testName = "onPhotoIconTappedShowsChoosePhotoOptionInAppropriatePanel",
                            result = Passed,
                            durationNanos = 0
                    ),
                    Test(
                            className = "net.juno.rd.functional_tests.tests.SignUpFirstNameTest",
                            testName = "onBackButtonTappedNavigatesToSignUpPasswordScreenWithEnteredPassword",
                            result = Passed,
                            durationNanos = 0
                    ),
                    Test(
                            className = "net.juno.rd.functional_tests.tests.SignUpLastNameTest",
                            testName = "onOkTappedClosesWeCannotVerifyYourNumberAlertAndNavigatesToSignUpPhoneScreenWithEnteredEarlierPhoneNumber",
                            result = Passed,
                            durationNanos = 0
                    ),
                    Test(
                            className = "net.juno.rd.functional_tests.tests.SignUpPhoneTest",
                            testName = "onCloseButtonTappedNavigatesToOnboardingScreen",
                            result = Passed,
                            durationNanos = 0
                    ),
                    Test(
                            className = "net.juno.rd.functional_tests.tests.SignUpPhoneTest",
                            testName = "onPhoneNumberEnteredDisplaysClearButton",
                            result = Passed,
                            durationNanos = 0
                    )
            ))
        }

        it("completes stream") {
            subscriber.assertCompleted()
        }

        it("does not emit error") {
            subscriber.assertNoErrors()
        }
    }

    context("read output with 0 tests") {

        val subscriber by memoized { TestSubscriber<Test>() }
        val tests by memoized { mutableListOf<Test>() }

        perform {
            readInstrumentationOutput(fileFromJarResources<InstrumentationSpec>("instrumentation-output-0-tests.txt"))
                    .asTests()
                    .subscribe(subscriber)

            subscriber.awaitTerminalEvent()
            subscriber.onNextEvents.forEach { test ->
                tests.add(test.copy(durationNanos = 0)) // We have no control over system time in tests.
            }
        }

        it("does not emit any value") {
            subscriber.assertNoValues()
        }

        it("completest stream") {
            subscriber.assertCompleted()
        }

        it("does not emit error") {
            subscriber.assertNoErrors()
        }
    }

    context("read unordered output") {

        val subscriber by memoized { TestSubscriber<Test>() }
        val tests by memoized { mutableListOf<Test>() }

        perform {
            readInstrumentationOutput(fileFromJarResources<InstrumentationSpec>("instrumentation-unordered-output.txt"))
                    .asTests()
                    .subscribe(subscriber)

            subscriber.awaitTerminalEvent()
            subscriber.onNextEvents.forEach { test ->
                tests.add(test.copy(durationNanos = 0)) // We have no control over system time in tests.
            }
        }

        it("emits expected tests") {
            assertThat(tests).isEqualTo(listOf(
                    Test(
                            className = "com.example.functional_tests.tests.TestClass1",
                            testName = "test1",
                            result = Passed,
                            durationNanos = 0L
                    ),
                    Test(
                            className = "com.example.functional_tests.tests.TestClass2",
                            testName = "test2",
                            result = Passed,
                            durationNanos = 0L
                    ),
                    Test(
                            className = "com.example.functional_tests.tests.TestClass2",
                            testName = "test3",
                            result = Passed,
                            durationNanos = 0L
                    )
            ))
        }
    }
})

