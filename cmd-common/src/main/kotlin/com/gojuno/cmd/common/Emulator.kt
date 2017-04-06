package com.gojuno.cmd.common

data class AdbDevice(
        val id: String,
        val online: Boolean
) {
    val isEmulator = id.startsWith("emulator-")
}
