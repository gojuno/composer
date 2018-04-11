package com.gojuno.composer

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.it

class LogLineParserSpec : Spek({

    val parser = LogLineParser()

    context("parse test class and name") {

        it("parses old TestRunner start logs") {
            assertThat(parser.parseTestClassAndName("I/TestRunner( 123): started: someTestMethod(com.example.SampleClass)"))
                .isEqualTo(Pair("com.example.SampleClass", "someTestMethod"))
        }

        it("parses old TestRunner finish logs") {
            assertThat(parser.parseTestClassAndName("I/TestRunner( 123): finished: someTestMethod(com.example.SampleClass)"))
                .isEqualTo(Pair("com.example.SampleClass", "someTestMethod"))
        }

        it("parses new TestRunner start logs") {
            assertThat(parser.parseTestClassAndName("TestRunner: started: someTestMethod(com.example.SampleClass)"))
                .isEqualTo(Pair("com.example.SampleClass", "someTestMethod"))
        }

        it("parses new TestRunner finish logs") {
            assertThat(parser.parseTestClassAndName("TestRunner: finished: someTestMethod(com.example.SampleClass)"))
                .isEqualTo(Pair("com.example.SampleClass", "someTestMethod"))
        }

        it("does not parse other logs") {
            assertThat(parser.parseTestClassAndName("")).isNull()
            assertThat(parser.parseTestClassAndName("blah: blah: blah")).isNull()
            assertThat(parser.parseTestClassAndName("aldkjf ;aldkj ffha a;ldjfoioihfads")).isNull()
            assertThat(parser.parseTestClassAndName("I/TestRunner( 123):")).isNull()
            assertThat(parser.parseTestClassAndName("TestRunner")).isNull()
        }
    }
})