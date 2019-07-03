package com.gojuno.composer

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class ArgsSpec : Spek({

    val rawArgsWithOnlyRequiredFields = arrayOf(
            "--apk", "apk_path",
            "--test-apk", "test_apk_path"
    )

    describe("parse args with only required params") {

        val args by memoized { parseArgs(rawArgsWithOnlyRequiredFields) }

        it("parses passes instrumentationArguments and uses default values for other fields") {
            assertThat(args).isEqualTo(Args(
                    appApkPath = "apk_path",
                    testApkPath = "test_apk_path",
                    testRunner = "",
                    shard = true,
                    outputDirectory = "composer-output",
                    instrumentationArguments = emptyList(),
                    verboseOutput = false,
                    keepOutputOnExit = false,
                    devices = emptyList(),
                    devicePattern = "",
                    installTimeoutSeconds = 120,
                    failIfNoTests = true,
                    runWithOrchestrator = false,
                    extraApks = emptyList()
            ))
        }
    }

    describe("parse args with test runner specified") {

        val args by memoized {
            parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--test-runner", "test_runner"))
        }

        it("converts instrumentation arguments to list of key-value pairs") {
            assertThat(args.testRunner).isEqualTo("test_runner")
        }
    }

    describe("parse args with instrumentation arguments") {

        val args by memoized {
            parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--instrumentation-arguments", "key1", "value1", "key2", "value2"))
        }

        it("converts instrumentation arguments to list of key-value pairs") {
            assertThat(args.instrumentationArguments).isEqualTo(listOf("key1", "value1", "key2", "value2"))
        }
    }

    describe("parse args with instrumentation arguments with values with commas") {

        val args by memoized {
            parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--instrumentation-arguments", "key1", "value1,value2", "key2", "value3,value4"))
        }

        it("converts instrumentation arguments to list of key-value pairs") {
            assertThat(args.instrumentationArguments).isEqualTo(listOf("key1", "value1,value2", "key2", "value3,value4"))
        }
    }

    describe("parse args with explicitly passed --shard") {

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

    describe("parse args with explicitly passed --verbose-output") {

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

    describe("parse args with passed --devices") {

        val args by memoized {
            parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--devices", "emulator-5554"))
        }

        it("parses correctly device ids") {
            assertThat(args.devices).isEqualTo(listOf("emulator-5554"))
        }
    }

    describe("parse args with passed two --devices") {

        val args by memoized {
            parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--devices", "emulator-5554", "emulator-5556"))
        }

        it("parses correctly two device ids") {
            assertThat(args.devices).isEqualTo(listOf("emulator-5554", "emulator-5556"))
        }
    }

    describe("parse args with passed --device-pattern") {

        val args by memoized {
            parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--device-pattern", "[abc|def]"))
        }

        it("parses correctly device-pattern") {
            assertThat(args.devicePattern).isEqualTo("[abc|def]")
        }
    }

    describe("parse args with passed --devices and --device-pattern") {

        it("raises argument error") {
            assertThatThrownBy { parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--device-pattern", "[abc|def]") + arrayOf("--devices", "emulator-5554")) }
                    .isInstanceOf(IllegalArgumentException::class.java)
                    .hasMessageContaining("Specifying both --devices and --device-pattern is prohibited.")
        }
    }

    describe("parse args with --keep-output-on-exit") {

        val args by memoized {
            parseArgs(rawArgsWithOnlyRequiredFields + "--keep-output-on-exit")
        }

        it("parses --keep-output-on-exit correctly") {
            assertThat(args.keepOutputOnExit).isEqualTo(true)
        }
    }

    describe("parse args with passed --install-timeout") {

        val args by memoized {
            parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--install-timeout", "600"))
        }

        it("parses --install-timeout correctly") {
            assertThat(args.installTimeoutSeconds).isEqualTo(600)
        }
    }

    describe("parse args with passed --fail-if-no-tests") {

        val args by memoized {
            parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--fail-if-no-tests", "false"))
        }

        it("parses --fail-if-no-tests correctly") {
            assertThat(args.failIfNoTests).isEqualTo(false)
        }
    }

    describe("parse args with explicitly passed --fail-if-no-tests") {

        listOf(true, false).forEach { failIfNoTests ->

            context("--fail-if-no-tests $failIfNoTests") {

                val args by memoized {
                    parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--fail-if-no-tests", "$failIfNoTests"))
                }

                it("parses --fail-if-no-tests correctly") {
                    assertThat(args.failIfNoTests).isEqualTo(failIfNoTests)
                }
            }
        }
    }

    describe("parse args with --with-orchestrator") {

        val args by memoized {
            parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--with-orchestrator", "true"))
        }

        it("parses --with-orchestrator correctly") {
            assertThat(args.runWithOrchestrator).isEqualTo(true)
        }
    }

    describe("parse args with passed --extra-apks") {

        val args by memoized {
            parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--extra-apks", "apk1.apk", "apk2.apk"))
        }

        it("parses correctly two extra apks") {
            assertThat(args.extraApks).isEqualTo(listOf("apk1.apk", "apk2.apk"))
        }
    }

})
