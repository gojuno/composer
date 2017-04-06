## Composer — Reactive Android Instrumentation Test Runner.

Composer is a modern reactive replacement for [square/spoon][spoon] with following feature set:

* Parallel test execution on multiple emulators/devices with [test sharding][test sharding] support.
* Logcat output capturing per test and for whole test run as well.
* Screenshots and files pulling for each test reactively (with support for [square/spoon][spoon] folder structure).
* JUnit4 report generation. 

### Why we've decided to replace [square/spoon][spoon]
 
**Problem 1:** Our UI tests are stable, but we saw a lot of UI tests build failures. About ~50% of our CI builds were failing. All such failures of UI tests came from Spoon not being able to run tests on one or more emulators (device is red in the report and error message is `…work/emulator-5554/result.json (No such file or directory)`, basically it timed out on installing the apk on a device, increasing adb timeout did not help, all emulators responded to adb commands and mouse/keyboard interactions, we suppose problem is in in ddmlib used by Spoon.

**Solution:** Composer does not use ddmlib and talks to emulators/devices by invoking `adb` binary.  

**Problem 2:** Pretty often when test run finished, Spoon freezed on moving screenshots from one of the emulators/devices. Again, we blame ddmlib used in Spoon for that.

**Solution:** Composer invokes `adb` binary to pull files from emulators/devices, we haven't seen problems with that in more than 700 builds on CI.

**Problem 3:** Spoon pulled screenshots/files *after* finish of the whole test run on a device which slows down builds: `test_run_time + pull_files_time`.

**Solution:** Composer pulls screenshots/files *reactively* after each test which basically leads to: `~test_run_time`.

**Problem 4:** If test sharding is enabled (which we do all the time), Spoon HTML report is very hard to look at, especially if you want to find some particular test(s) and it's not failed. You have to either hover mouse over each test to find out its name or go into html/xml source and find on which emulator/device test was sharded in order to click on correct device and then find test by CMD+F on the page.

**Solution:** ~~Composer does not generate HTML report~~ HTML report that we have in mind will be easier to look at and inspect.
  
**Problem 5:** Html report can be very slow to load if you have lots of screenshots (which we do) since it displays all the screenshots of tests that were run on a particular device on a single page — it can take up to minutes to finish while you effectively unable to scroll page since scroll is jumping up and down each time new screenshot loaded.

**Solution:** ~~Composer does not generate HTML report~~ HTML report that we have in mind will not display screenshots until you explicitly ask it for that → fast page load.

>With Composer we were able to make UI tests required part of CI for Pull Requests.
>It's fast, reliable and uses RxJava which means that it's relatively easy to add more features combining complex async transformations. 

### HTML Report

Composer already have all the info to generate it, we're working on it with our Web Frontend team.

For now you can inspect test run results by looking at build log, screenshots/files, captured logcat output (it's not hard, but HTML report would be very nice).  

## Usage

Composer shipped as jar, to run it you need JVM 1.8+: java -jar composer-latest-version.jar options. 

#### Supported options

##### Required

* `--apk`
  * Path to application apk that needs to be tested.
* `--test-apk`
  * Path to apk with tests.
* `--test-package`
  * Android package name of the test apk (Could be parsed from `--test-apk`, PR welcome).
* `--test-runner`
  * Full qualified name of test runner class you're using (Could be parsed from `--test-apk`, PR welcome).

##### Optional

* `--help, -help, help, -h`
  * Print help and exit.
* `--shard`
  * Either `true` or `false` to enable/disable [test sharding][test sharding] which runs shards tests to available devices/emulators. True by default.
* `--output-directory`
  * Either relative or absolute path to directory for output: reports, files from devices and so on. `composer-output` by default.
* `--instrumentation-arguments`
  * Key-value pairs to pass to Instrumentation Runner. Usage example: `--instrumentation-arguments myKey1 myValue1 myKey2 myValue2`.
* `--verbose-output`
  * Either `true` or `false` to enable/disable verbose output for Swarmer. `false` by default.

##### Example

```console
java -jar composer-latest-version.jar \
--apk app/build/outputs/apk/example-debug.apk \
--test-apk app/build/outputs/apk/example-debug-androidTest.apk \
--test-package com.example.test \
--test-runner com.example.test.ExampleTestRunner \
--output-directory artifacts/composer-output \
--instrumentation-arguments key1 value1 key2 value2 \
--verbose-output false
```

### How to build

Dependencies: you only need `docker` and `bash` installed on your machine.

```console
bash ci/build.sh
```

## License

```
Copyright 2017 Juno, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

[spoon]: https://github.com/square/spoon
[test sharding]: https://developer.android.com/topic/libraries/testing-support-library/index.html#ajur-sharding
