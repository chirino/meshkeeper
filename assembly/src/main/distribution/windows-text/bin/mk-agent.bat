@echo off
REM ######################################################################################
REM # Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    #
REM # http://fusesource.com                                                              #
REM # ---------------------------------------------------------------------------------- #
REM # The software in this package is published under the terms of the AGPL license      #
REM # a copy of which has been included with this distribution in the license.txt file.  #
REM ######################################################################################

setlocal
TITLE MeshKeeper Launch Agent

REM =====================================================================
REM User customization area.  You can set your defaults here.
REM =====================================================================

set JAVA_EXE=
set JAVA_MIN_MEM=16m
set JAVA_MAX_MEM=32m
set MESHKEEPER_OPTS=

REM =====================================================================
REM Execute the Launch Agent
REM =====================================================================

call :SETUP_DEFAULTS

%JAVA_EXE% %JAVA_OPTS% %OPTS% -classpath %CLASSPATH% org.fusesource.meshkeeper.launcher.Main --directory "%MESHKEEPER_HOME%\data" %*

if ERRORLEVEL 1 goto ERROR
goto END

REM =====================================================================
REM Environment setup helper routines
REM =====================================================================

:SETUP_DEFAULTS
  call :LOCATE_MESHKEEPER_HOME
  call :LOCATE_MESHKEEPER_BASE
  call :LOCATE_JAVA_EXE
  call :LOCATE_JAVA_OPTS
  call :LOCATE_OPTS
  call :LOCATE_CLASSPATH
goto :EOF

:LOCATE_MESHKEEPER_HOME
  cd %~dp0%
  cd ..
  set MESHKEEPER_HOME=%cd%
  if not exist "%MESHKEEPER_HOME%" (
    echo MESHKEEPER_HOME directory is not valid: %MESHKEEPER_HOME%
    goto :ERROR
  )
goto :EOF

:LOCATE_MESHKEEPER_BASE
  if "%MESHKEEPER_BASE%" == "" (
    set MESHKEEPER_BASE=%MESHKEEPER_HOME%
  )
goto :EOF

:LOCATE_JAVA_EXE
  if "%JAVA_EXE%" == "" (
    set JAVA_EXE=%JAVA%
  )
	if "%JAVA_EXE%" == "" (
    set JAVA_EXE=java
	)
goto :EOF
    
:LOCATE_JAVA_OPTS
  if "%JAVA_OPTS%" == "" (
    set JAVA_OPTS=-Xms%JAVA_MIN_MEM% -Xmx%JAVA_MIN_MEM%
  )
  if "%MESHKEEPER_DEBUG%" == "" goto :END_MESHKEEPER_DEBUG
  
    if "%JAVA_DEBUG_OPTS%" == "" (
      set JAVA_DEBUG_OPTS=-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005
    )
  
    echo Enabling Java debug options: %JAVA_DEBUG_OPTS%
    set JAVA_OPTS=%JAVA_DEBUG_OPTS% %JAVA_OPTS%
  :END_MESHKEEPER_DEBUG  
goto :EOF
    
:LOCATE_OPTS
  set OPTS=-Dlog4j.configuration=file:%MESHKEEPER_HOME%\etc\log4j.properties
  set OPTS=%OPTS% -Dmop.base=%MESHKEEPER_HOME%
  set OPTS=%OPTS% -Dmop.online=false
  set OPTS=%OPTS% %MESHKEEPER_OPTS%
goto :EOF

:LOCATE_CLASSPATH
  set CLASSPATH=%MESHKEEPER_HOME%\repository\org\fusesource\meshkeeper\meshkeeper-api\${project.version}\meshkeeper-api-${project.version}.jar
  set CLASSPATH=%CLASSPATH%;%MESHKEEPER_HOME%\repository\org\fusesource\mop\mop-core\${mop-version}\mop-core-${mop-version}.jar
goto :EOF
    
:ERROR
  echo Paused to catch any errors. Press any key to continue.
  errorlvl 1    
  pause
goto :END

:END
endlocal