# Terms

* Test is a single functionality check.
    * Properties:
        * name;
        * package name;
        * class name;
        * duration;
        * log;
        * device name;
        * list of properties — property is a key-value pair;
        * list of files;
        * list of screenshots.
* Test suite is a combination of all tests available.
    * Properties:
        * list of tests;
        * success tests count;
        * failure tests count;
        * ignored tests count;
        * log;
        * duration.
    * Test suite can be run different ways.
        * Single device — just a single run of all tests in a test suite.
        * Multiple devices.
            * Sharding — all tests in a test suite are being split evenly between all devices.
            * Duplicating — all tests in a test suite are being run the same way on each device available.

# Report Pages

## Device List

This page should be skipped if sharding was used to run the test suite.

Contains a list of items with following properties:

* device name;
* test suite success tests count;
* test suite failure tests count;
* test suite ignored tests count.

Available actions:

* click on the list item opens the Test Suite page.

## Test Suite

Contains following blocks from top to bottom.

* Summary with following properties:
    * success tests count;
    * failure tests count;
    * ignored tests count;
    * duration.
* Search input.
* List of tests with following properties:
    * package name;
    * class name;
    * name;
    * device name.
    
Available actions:

* click on the list item opens the Test page;
* changing search input contents changes list of tests content.

## Test

Contains following blocks from top to bottom.

* Summary:
    * package name;
    * class name;
    * name;
    * device name;
    * duration.
* Properties:
    * list of key-value pairs.
* Files:
    * list of file links.
* Screenshots:
    * list of screenshot images.
* Log.

Available actions:

* click on a file starts its downloading;
* click on a screenshot opens in a new tab full-size.
