#!/bin/sh
#  runagent.sh

#  ---------------------------------------------------------------------------
#  Usage:   runagent.sh 
#  Purpose: See the cloudlaunch documentation for the explanation
#  ---------------------------------------------------------------------------

#  -------- USER MUST SET THE FOLLOWING VARIABLES AFTER INSTALL --------------
#  JAVA_EXE should indicate the path to a 1.5 (or higher) java executable
#  ---------------------------------------------------------------------------

JAVA_EXE=


# ---------------YOU DO NOT NEED TO CHANGE ANYTHING BELOW --------------------

# ----------------------------------------------------------------------------
# CLASSES contains the classpath required by a testrunner agent. Relative
# paths are relative to the testrunner/bin directory.
# ----------------------------------------------------------------------------
CLASSES=../lib/testrunner.jar:../lib/rmiviajms.jar

echo ------- Starting Agent -------
echo $JAVA_EXE -classpath $CLASSES org.fusesource.cloudlaunch.launcher.Main %*
$JAVA_EXE  -classpath $CLASSES org.fusesource.cloudlaunch.launcher.Main %*

echo Paused to catch any errors. Press any key to continue.
read ans

