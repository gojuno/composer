package com.gojuno.composer.html

import com.gojuno.composer.Device
import com.gojuno.composer.testFile
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.it

class HtmlDeviceSpec : Spek({

    context("Device.toHtmlDevice") {

        val device = Device(id = "testDevice1", logcat = testFile(), instrumentationOutput = testFile())

        val htmlDevice = device.toHtmlDevice(testFile())

        it("converts Device to HtmlDevice") {
            assertThat(htmlDevice).isEqualTo(HtmlDevice(
                    id = device.id,
                    logcatPath = "../${device.logcat.name}",
                    instrumentationOutputPath = "../${device.instrumentationOutput.name}"
            ))
        }
    }
})
