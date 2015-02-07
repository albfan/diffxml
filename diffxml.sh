#!/bin/bash
# This script will set up the java classpath with the required libraries
# then call diffxml with the given arguments.

#First find out where we are relative to the user dir
CALLPATH=${0%/*}

if [[ -n "${CALLPATH}" ]]; then
  CALLPATH=${CALLPATH}/
fi

if [ -v JAVA_HOME ]
then
  JAVA_BIN="$JAVA_HOME/bin/java"
elif [ -v JDK_HOME ]
then
  JAVA_BIN="$JDK_HOME/jre/bin/java"
elif type java &>/dev/null
then
   JAVA_BIN=java
else
   cat <<EOF

no java executable was found. Install it or define 
variable "JAVA_HOME" or "JDK_HOME"
EOF
   exit 1
fi

"$JAVA_BIN" -cp ${CALLPATH}build:${CALLPATH}lib/diffxml.jar org.diffxml.diffxml.DiffXML "$@"
