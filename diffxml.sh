#!/bin/bash
# This script will set up the java classpath with the required libraries
# then call diffxml with the given arguments.
# You may need to edit this file to reflect your own setup.

java -cp ./lib/xpp3-1.1.3.4.C:./build org.diffxml.diffxml.DiffXML "$@"
