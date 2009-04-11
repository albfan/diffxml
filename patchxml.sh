#!/bin/bash
# This script will set up the java classpath with the required libraries
# then call diffxml with the given arguments

java -cp ./build:./lib/diffxml.jar org.diffxml.patchxml.PatchXML "$@"

