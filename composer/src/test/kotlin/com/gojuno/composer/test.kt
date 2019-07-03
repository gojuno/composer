package com.gojuno.composer

import org.spekframework.spek2.style.specification.Suite
import java.io.File

inline fun <reified C> fileFromJarResources(name: String) = File(C::class.java.classLoader.getResource(name).file)

fun testFile(): File = createTempFile().apply { deleteOnExit() }

fun Suite.perform(body: () -> Unit) = beforeEachTest(body)

fun Suite.cleanup(body: () -> Unit) = afterEachTest(body)

/** Make a Unix-formatted String compliant with current operating system's newline format */
fun normalizeLinefeed(str: String): String = str.replace("\n", System.getProperty("line.separator"));
