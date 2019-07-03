package com.gojuno.composer

import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class ApkSpec : Spek({

    describe("parse package from apk") {

        val testApkPath by memoized { fileFromJarResources<InstrumentationSpec>("instrumentation-test.apk").absolutePath }

        it("parses test runner correctly") {
            assertThat(parseTestRunner(testApkPath)).isEqualTo(TestRunner.Valid("android.support.test.runner.AndroidJUnitRunner"))
        }

        it("parses test package correctly") {
            assertThat(parseTestPackage(testApkPath)).isEqualTo(TestPackage.Valid("test.test.myapplication.test"))
        }

        it("parses tests list correctly") {
            assertThat(parseTests(testApkPath)).isEqualTo(listOf(
                    TestMethod("test.test.myapplication.ExampleInstrumentedTest#useAppContext",
                            listOf("dalvik.annotation.Throws", "org.junit.Test", "org.junit.runner.RunWith")
                    )
            ))
        }
    }
})