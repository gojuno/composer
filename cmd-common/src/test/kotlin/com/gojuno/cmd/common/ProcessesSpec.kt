package com.gojuno.cmd.common

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS

class ProcessesSpec : Spek({

    describe("nanosToHumanReadableTime") {

        on("convert 1 second") {

            val result by memoized { SECONDS.toNanos(1).nanosToHumanReadableTime() }

            it("converts it to 1 second") {
                assertThat(result).isEqualTo("1 second")
            }
        }

        on("convert 59 seconds") {

            val result by memoized { SECONDS.toNanos(59).nanosToHumanReadableTime() }

            it("converts it to 59 second") {
                assertThat(result).isEqualTo("59 seconds")
            }
        }

        on("convert 60 seconds") {

            val result by memoized { SECONDS.toNanos(60).nanosToHumanReadableTime() }

            it("converts it to 1 minute 0 seconds") {
                assertThat(result).isEqualTo("1 minute 0 seconds")
            }
        }

        on("convert 61 seconds") {

            val result by memoized { SECONDS.toNanos(61).nanosToHumanReadableTime() }

            it("converts it to 1 minute 1 second") {
                assertThat(result).isEqualTo("1 minute 1 second")
            }
        }

        on("convert 62 seconds") {

            val result by memoized { SECONDS.toNanos(62).nanosToHumanReadableTime() }

            it("converts it to 1 minute 2 second") {
                assertThat(result).isEqualTo("1 minute 2 seconds")
            }
        }

        on("convert 60 minutes") {

            val result by memoized { MINUTES.toNanos(60).nanosToHumanReadableTime() }

            it("converts it to 1 hour 0 seconds") {
                assertThat(result).isEqualTo("1 hour 0 minutes 0 seconds")
            }
        }

        on("convert 61 minutes") {

            val result by memoized { MINUTES.toNanos(61).nanosToHumanReadableTime() }

            it("converts it to 1 hour 1 minute 0 seconds") {
                assertThat(result).isEqualTo("1 hour 1 minute 0 seconds")
            }
        }
    }
})
