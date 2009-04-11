#!/bin/bash
# This script will set up the java classpath with the required libraries
# then call diffxml with the given arguments.

java -cp ./lib/xpp3-1.1.3.4.C:./build:./lib/diffxml.jar org.diffxml.diffxml.DiffXML "$@"
