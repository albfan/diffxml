#!/bin/bash
# This script will set up the java classpath with the required libraries
# then call diffxml with the given arguments

export DIFFXML_HOME=$HOME/diffxml/cvs/diffxml
export DIFFXML_LIB=$DIFFXML_HOME/lib
export DIFFXML_BUILD=$DIFFXML_HOME/build
 
export CLASSPATH=$DIFFXML_LIB/dom3-xercesImpl.jar:$DIFFXML_LIB/dom3-xml-apis.jar:$DIFFXML_LIB/xpp3_1_0_8a.jar:$DIFFXML_BUILD:$DIFFXML_BUILD/diffxml.jar
 
java org.diffxml.diffxml.DiffXML "$@"
