#!/bin/bash
# This script will set up the java classpath with the required libraries
# then call diffxml with the given arguments

export DIFFXML_HOME=$HOME/diffxml
export DIFFXML_LIB=$DIFFXML_HOME/lib
export DIFFXML_BUILD=$DIFFXML_HOME/build
 
export CLASSPATH=$CLASSPATH:$DIFFXML_LIB/xalan.jar:$DIFFXML_LIB/xercesImpl.jar:$DIFFXML_LIB/xml-apis.jar:$DIFFXML_LIB/xmlParserAPIs.jar:$DIFFXML_LIB/xpp3_1_0_8a.jar:$DIFFXML_LIB/xsltc.jar:$DIFFXML_BUILD/diffxml.jar
 
java diffxml.diffxml "$@"
