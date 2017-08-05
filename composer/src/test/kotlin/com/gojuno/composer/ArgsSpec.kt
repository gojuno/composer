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
                    instrumentationArguments = emptyList(),
                    verboseOutput = false,
                    devices = emptyList(),
                    devicePattern = ""
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

    context("parse args with explicitly passed --shard") {

        listOf(true, false).forEach { shard ->

            context("--shard $shard") {

                val args by memoized {
                    parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--shard", "$shard"))
                }

                it("parses --shard correctly") {
                    assertThat(args.shard).isEqualTo(shard)
                }
            }
        }
    }

    context("parse args with explicitly passed --verbose-output") {

        listOf(true, false).forEach { verboseOutput ->

            context("--verbose--output $verboseOutput") {

                val args by memoized {
                    parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--verbose-output", "$verboseOutput"))
                }

                it("parses --verbose-output correctly") {
                    assertThat(args.verboseOutput).isEqualTo(verboseOutput)
                }
            }
        }
    }

    context("parse args with passed --devices") {

        val args by memoized {
            parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--devices", "emulator-5554"))
        }

        it("parses correctly device ids") {
            assertThat(args.devices).isEqualTo(listOf("emulator-5554"))
        }

    }

    context("parse args with passed two --devices") {

        val args by memoized {
            parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--devices", "emulator-5554", "emulator-5556"))
        }

        it("parses correctly two device ids") {
            assertThat(args.devices).isEqualTo(listOf("emulator-5554", "emulator-5556"))
        }

    }

    context("parse args with passed --device-pattern") {

        val args by memoized {
            parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--device-pattern", "[abc|def]"))
        }

        it("parses correctly device-pattern") {
            assertThat(args.devicePattern).isEqualTo("[abc|def]")
        }
    }
})
