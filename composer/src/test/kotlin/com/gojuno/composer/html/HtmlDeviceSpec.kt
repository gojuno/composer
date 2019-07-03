package com.gojuno.composer.html

import com.gojuno.composer.Device
import com.gojuno.composer.testFile
import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class HtmlDeviceSpec : Spek({

    describe("Device.toHtmlDevice") {

        val device = Device(id = "testDevice1", logcat = testFile(), instrumentationOutput = testFile(), model = "testModel1")

        val htmlDevice = device.toHtmlDevice(testFile().parentFile)

        it("converts Device to HtmlDevice") {
            assertThat(htmlDevice).isEqualTo(HtmlDevice(
                    id = device.id,
                    model = device.model,
                    logcatPath = device.logcat.name,
                    instrumentationOutputPath = device.instrumentationOutput.name
            ))
        }
    }
})
