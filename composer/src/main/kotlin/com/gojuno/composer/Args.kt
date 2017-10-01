package com.gojuno.janulator

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.gojuno.composer.Exit
import com.gojuno.composer.exit

data class Args(
        val appApkPath: String,
        val testApkPath: String,
        val testPackage: String,
        val testRunner: String,
        val shard: Boolean,
        val outputDirectory: String,
        val instrumentationArguments: List<Pair<String, String>>,
        val verboseOutput: Boolean,
        val devices: List<String>,
        val devicePattern: String
)

// No way to share array both for runtime and annotation without reflection.
private val PARAMETER_HELP_NAMES = setOf("--help", "-help", "help", "-h")

private class JCommanderArgs {

    @Parameter(
            names = arrayOf("--help", "-help", "help", "-h"),
            help = true,
            description = "Print help and exit."
    )
    var help: Boolean? = null

    @Parameter(
            names = arrayOf("--apk"),
            required = true,
            description = "Path to application apk that needs to be tested"
    )
    lateinit var appApkPath: String

    @Parameter(
            names = arrayOf("--test-apk"),
            required = true,
            description = "Path to apk with tests"
    )
    lateinit var testApkPath: String

    @Parameter(
            names = arrayOf("--test-package"),
            required = true,
            description = "Android package name of the test apk."
    )
    lateinit var testPackage: String

    @Parameter(
            names = arrayOf("--test-runner"),
            required = true,
            description = "Full qualified name of test runner class you're using."
    )
    lateinit var testRunner: String

    @Parameter(
            names = arrayOf("--shard"),
            required = false,
            arity = 1,
            description = "Either `true` or `false` to enable/disable test sharding which runs tests in parallel on available devices/emulators. `true` by default."
    )
    var shard: Boolean? = null

    @Parameter(
            names = arrayOf("--output-directory"),
            required = false,
            description = "Either relative or absolute path to directory for output: reports, files from devices and so on. `composer-output` by default."
    )
    var outputDirectory: String? = null

    @Parameter(
            names = arrayOf("--instrumentation-arguments"),
            required = false,
            variableArity = true,
            description = "Key-value pairs to pass to Instrumentation Runner. Usage example: `--instrumentation-arguments myKey1 myValue1 myKey2 myValue2`."
    )
    var instrumentationArguments: List<String>? = null

    @Parameter(
            names = arrayOf("--verbose-output"),
            required = false,
            arity = 1,
            description = "Either `true` or `false` to enable/disable verbose output for Swarmer. `false` by default."
    )
    var verboseOutput: Boolean? = null

    @Parameter(
            names = arrayOf("--devices"),
            required = false,
            variableArity = true,
            description = "Connected devices/emulators that will be used to run tests against. If not passed — tests will run on all connected devices/emulators. Specifying both `--devices` and `--device-pattern` will result in an error. Usage example: `--devices emulator-5554 emulator-5556`."
    )
    var devices: List<String>? = null

    @Parameter(
            names = arrayOf("--device-pattern"),
            required = false,
            description = "Connected devices/emulators that will be used to run tests against. If not passed — tests will run on all connected devices/emulators. Specifying both `--device-pattern` and `--devices` will result in an error. Usage example: `--device-pattern \"somePatterns\"`."
    )
    var devicePattern: String? = null
}

private fun validateArguments(args: Args) {
    if(!args.devicePattern.isEmpty() && !args.devices.isEmpty()) {
        throw IllegalArgumentException("Specifying both --devices and --device-pattern is prohibited.")
    }
}

fun parseArgs(rawArgs: Array<String>): Args {
    if (PARAMETER_HELP_NAMES.firstOrNull { rawArgs.contains(it) } != null) {
        JCommander(JCommanderArgs()).usage()
        exit(Exit.Ok)
    }

    val jCommanderArgs = JCommanderArgs()
    val jCommander = JCommander.newBuilder().addObject(jCommanderArgs).build()
    jCommander.parse(*rawArgs)

    return jCommanderArgs.let {
        Args(
                appApkPath = jCommanderArgs.appApkPath,
                testApkPath = jCommanderArgs.testApkPath,
                testPackage = jCommanderArgs.testPackage,
                testRunner = jCommanderArgs.testRunner,
                shard = jCommanderArgs.shard ?: true,
                outputDirectory = jCommanderArgs.outputDirectory ?: "composer-output",
                instrumentationArguments = mutableListOf<Pair<String, String>>().apply {
                    var pairIndex = 0

                    jCommanderArgs.instrumentationArguments?.forEachIndexed { index, arg ->
                        when (index % 2 == 0) {
                            true -> this += arg to ""
                            false -> {
                                this[pairIndex] = this[pairIndex].copy(second = arg)
                                pairIndex += 1
                            }
                        }
                    }
                },
                verboseOutput = jCommanderArgs.verboseOutput ?: false,
                devices = jCommanderArgs.devices ?: emptyList(),
                devicePattern = jCommanderArgs.devicePattern ?: ""
        )
    }.apply { validateArguments(this) }
}
