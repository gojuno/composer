package com.gojuno.composer

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.it

class ApkSpec : Spek({

    context("parse package from apk") {

        val testApkPath by memoized { fileFromJarResources<InstrumentationSpec>("instrumentation-test.apk").absolutePath }

        it("parses test runner correctly") {
            assertThat(parseTestRunner(testApkPath)).isEqualTo(TestRunner.Valid("android.support.test.runner.AndroidJUnitRunner"))
        }

        it("parses test package correctly") {
            assertThat(parseTestPackage(testApkPath)).isEqualTo(TestPackage.Valid("test.test.myapplication.test"))
        }
    }
})
