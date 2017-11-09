package com.gojuno.composer

import com.beust.jcommander.IStringConverter
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import java.util.concurrent.TimeUnit

data class Args(
        @Parameter(
                names = arrayOf("--apk"),
                required = true,
                description = "Path to application apk that needs to be tested",
                order = 0
        )
        var appApkPath: String = "",

        @Parameter(
                names = arrayOf("--test-apk"),
                required = true,
                description = "Path to apk with tests",
                order = 1
        )
        var testApkPath: String = "",

        @Parameter(
                names = arrayOf("--test-package"),
                required = true,
                description = "Android package name of the test apk.",
                order = 2
        )
        var testPackage: String = "",

        @Parameter(
                names = arrayOf("--test-runner"),
                required = true,
                description = "Full qualified name of test runner class you're using.",
                order = 3
        )
        var testRunner: String = "",

        @Parameter(
                names = arrayOf("--shard"),
                required = false,
                arity = 1,
                description = "Either `true` or `false` to enable/disable test sharding which runs tests in parallel on available devices/emulators. `true` by default.",
                order = 4
        )
        var shard: Boolean = true,

        @Parameter(
                names = arrayOf("--output-directory"),
                required = false,
                description = "Either relative or absolute path to directory for output: reports, files from devices and so on. `composer-output` by default.",
                order = 5
        )
        var outputDirectory: String = "composer-output",

        @Parameter(
                names = arrayOf("--instrumentation-arguments"),
                required = false,
                variableArity = true,
                description = "Key-value pairs to pass to Instrumentation Runner. Usage example: `--instrumentation-arguments myKey1 myValue1 myKey2 myValue2`.",
                listConverter = InstrumentationArgumentsConverter::class,
                order = 6
        )
        var instrumentationArguments: List<String> = listOf(),

        @Parameter(
                names = arrayOf("--verbose-output"),
                required = false,
                arity = 1,
                description = "Either `true` or `false` to enable/disable verbose output for Composer. `false` by default.",
                order = 7
        )
        var verboseOutput: Boolean = false,

        @Parameter(
                names = arrayOf("--keep-output-on-exit"),
                required = false,
                description = "Keep output on exit. False by default.",
                order = 8
        )
        var keepOutputOnExit: Boolean = false,

        @Parameter(
                names = arrayOf("--devices"),
                required = false,
                variableArity = true,
                description = "Connected devices/emulators that will be used to run tests against. If not passed — tests will run on all connected devices/emulators. Specifying both `--devices` and `--device-pattern` will result in an error. Usage example: `--devices emulator-5554 emulator-5556`.",
                order = 9
        )
        var devices: List<String> = emptyList(),

        @Parameter(
                names = arrayOf("--device-pattern"),
                required = false,
                description = "Connected devices/emulators that will be used to run tests against. If not passed — tests will run on all connected devices/emulators. Specifying both `--device-pattern` and `--devices` will result in an error. Usage example: `--device-pattern \"somePatterns\"`.",
                order = 10
        )
        var devicePattern: String = "",

        @Parameter(
                names = arrayOf("--install-timeout"),
                required = false,
                description = "APK installation timeout in seconds. If not passed defaults to 120 seconds (2 minutes). Applicable to both test APK and APK under test.",
                order = 11
        )
        var installTimeoutSeconds: Int = TimeUnit.MINUTES.toSeconds(2).toInt()
)

// No way to share array both for runtime and annotation without reflection.
val PARAMETER_HELP_NAMES = setOf("--help", "-help", "help", "-h")

private fun validateArguments(args: Args) {
    if (!args.devicePattern.isEmpty() && !args.devices.isEmpty()) {
        throw IllegalArgumentException("Specifying both --devices and --device-pattern is prohibited.")
    }
}

fun parseArgs(rawArgs: Array<String>) = Args().also { args ->
    if (PARAMETER_HELP_NAMES.firstOrNull { rawArgs.contains(it) } != null) {
        JCommander(args).usage()
        exit(Exit.Ok)
    }

    JCommander.newBuilder()
            .addObject(args)
            .build()
            .parse(*rawArgs)
    validateArguments(args)
}

private class InstrumentationArgumentsConverter : IStringConverter<List<String>> {
    override fun convert(argument: String): List<String> = listOf(argument)
}