package com.gojuno.composer

import com.gojuno.janulator.Args
import com.gojuno.janulator.parseArgs
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.it

class ArgsSpec : Spek({

    val rawArgsWithOnlyRequiredFields = arrayOf(
            "--apk", "apk_path",
            "--test-apk", "test_apk_path",
            "--test-package", "test_package",
            "--test-runner", "test_runner"
    )

    context("parse args with only required params") {

        val args by memoized { parseArgs(rawArgsWithOnlyRequiredFields) }

        it("parses passes instrumentationArguments and uses default values for other fields") {
            assertThat(args).isEqualTo(Args(
                    appApkPath = "apk_path",
                    testApkPath = "test_apk_path",
                    testPackage = "test_package",
                    testRunner = "test_runner",
                    shard = true,
                    outputDirectory = "composer-output",
                    instrumentationArguments = emptyList()
            ))
        }
    }

    context("parse args with instrumentation arguments") {

        val args by memoized {
            parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--instrumentation-arguments", "key1", "value1", "key2", "value2"))
        }

        it("converts instrumentation arguments to list of key-value pairs") {
            assertThat(args.instrumentationArguments).isEqualTo(listOf("key1" to "value1", "key2" to "value2"))
        }
    }
})