echo off
REM ######################################################################################
REM # Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    #
REM # http://fusesource.com                                                              #
REM # ---------------------------------------------------------------------------------- #
REM # The software in this package is published under the terms of the AGPL license      #
REM # a copy of which has been included with this distribution in the license.txt file.  #
REM ######################################################################################
setlocal
TITLE MeshKeeper Agent

REM =====================================================================
REM User customization area.  You can set your defaults here.
REM =====================================================================

REM Set this to a particular JAVA_EXE if you wish
set JAVA_EXE=

JAVA_MIN_MEM=16m
JAVA_MAX_MEM=32m

goto BEGIN

REM =====================================================================
REM This section holds MeshKeeper environment setup routines
REM =====================================================================

REM =====================
REM When an error happens
REM =====================
:ERROR
  echo Paused to catch any errors. Press any key to continue.
  errorlvl 1    
  pause
goto :END

REM ===================
REM Set MESHKEEPER_HOME
REM ===================

cd %~dp0%
cd ..
set MESHKEEPER_HOME=%cd%
if not exist "%MESHKEEPER_HOME%" (
    echo MESHKEEPER_HOME directory is not valid: %MESHKEEPER_HOME%
    goto :ERROR
)

REM ==============
REM Setup JAVA_EXE
REM ==============
if not "%JAVA_EXE%" == "" goto :CHECK_JAVA_END
	set JAVA_EXE=%JAVA%
	if not "%JAVA_EXE%" == "" goto :CHECK_JAVA_END
    	set JAVA_EXE=java
:CHECK_JAVA_END

REM ===============
REM Setup CLASSPATH
REM ===============
CLASSPATH=%MESHKEEPER_HOME%\repository\org\fusesource\meshkeeper\meshkeeper-api\${project.version}\meshkeeper-api-${project.version}.jar
CLASSPATH=%CLASSPATH%;%MESHKEEPER_HOME%\repository\org\fusesource\mop\mop-core\${mop-version}\mop-core-${mop-version}.jar

REM ==========
REM Setup OPTS
REM ==========
set OPTS=-Dlog4j.configuration=file:%MESHKEEPER_HOME%\etc\log4j.properties
set OPTS=%OPTS% -Dmop.base=$MESHKEEPER_HOME
set OPTS=%OPTS% -Dmop.online=false

REM =====================================================================
REM Execute the Launch Agent
REM =====================================================================

cd %MESHKEEPER_HOME%
%JAVA_EXE% %OPTS% -classpath %CLASSPATH% org.fusesource.meshkeeper.launcher.Main --directory "%MESHKEEPER_HOME%\data" %*
if ERRORLEVEL 1 GOTO ERROR

:END
endlocal