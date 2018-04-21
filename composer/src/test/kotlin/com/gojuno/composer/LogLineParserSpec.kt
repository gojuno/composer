package com.gojuno.composer

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.it

class LogLineParserSpec : Spek({

    context("parse TestRunner log line with long prefix") {

        context("parse started log") {
            val args by memoized {
                "04-06 00:25:49.747 28632 28650 I TestRunner: started: someTestMethod(com.example.SampleClass)".parseTestClassAndName()
            }

            it("extracts test class and method") {
                assertThat(args).isEqualTo("com.example.SampleClass" to "someTestMethod")
            }
        }

        context("parse finished log") {
            val args by memoized {
                "04-06 00:25:49.747 28632 28650 I TestRunner: finished: someTestMethod(com.example.SampleClass)".parseTestClassAndName()
            }

            it("extracts test class and method") {
                assertThat(args).isEqualTo("com.example.SampleClass" to "someTestMethod")
            }
        }
    }

    context("parse TestRunner log line with short prefix") {

        context("parse started log") {

            val args by memoized {
                "I/TestRunner( 123): started: someTestMethod(com.example.SampleClass)".parseTestClassAndName()
            }

            it("extracts test class and method") {
                assertThat(args).isEqualTo("com.example.SampleClass" to "someTestMethod")
            }
        }

        context("parse finished log") {

            val args by memoized {
                "I/TestRunner( 123): finished: someTestMethod(com.example.SampleClass)".parseTestClassAndName()
            }

            it("extracts test class and method") {
                assertThat(args).isEqualTo("com.example.SampleClass" to "someTestMethod")
            }
        }
    }

    context("parse non TestRunner started/finished logs") {

        it("does not parse empty log") {
            assertThat("".parseTestClassAndName()).isNull()
        }

        it("does not parse TestRunner logs without started/finished") {
            assertThat("I/TestRunner( 123):".parseTestClassAndName()).isNull()
            assertThat("04-06 00:25:49.747 28632 28650 I TestRunner:".parseTestClassAndName()).isNull()
        }
    }
})