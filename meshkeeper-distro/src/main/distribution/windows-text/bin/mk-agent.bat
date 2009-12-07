@REM
@REM  Copyright (C) 2009 Progress Software, Inc. All rights reserved.
@REM  http://fusesource.com
@REM
@REM  Licensed under the Apache License, Version 2.0 (the "License");
@REM  you may not use this file except in compliance with the License.
@REM  You may obtain a copy of the License at
@REM
@REM         http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM  Unless required by applicable law or agreed to in writing, software
@REM  distributed under the License is distributed on an "AS IS" BASIS,
@REM  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM  See the License for the specific language governing permissions and
@REM  limitations under the License.
@REM
@echo off

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

%JAVA_EXE% %JAVA_OPTS% %OPTS% -classpath %CLASSPATH% org.fusesource.meshkeeper.launcher.Main %*

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
    set MESHKEEPER_BASE=%MESHKEEPER_HOME%\data
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
  set OPTS=
  set OPTS=%OPTS% -Dmeshkeeper.application=%~n0
  set OPTS=%OPTS% -Dmeshkeeper.home=%MESHKEEPER_HOME%
  set OPTS=%OPTS% -Dmeshkeeper.base=%MESHKEEPER_BASE%
  set OPTS=%OPTS% -Dlog4j.configuration=file:%MESHKEEPER_HOME%\etc\log4j.properties
  set OPTS=%OPTS% -Dmop.base=%MESHKEEPER_HOME%
  set OPTS=%OPTS% -Dmop.online=false
  set OPTS=%OPTS% %MESHKEEPER_OPTS%
goto :EOF

:LOCATE_CLASSPATH
  set CLASSPATH=
  set CLASSPATH=%CLASSPATH%;%MESHKEEPER_HOME%\repository\org\fusesource\meshkeeper\meshkeeper-api\${project.version}\meshkeeper-api-${project.version}.jar
  set CLASSPATH=%CLASSPATH%;%MESHKEEPER_HOME%\repository\org\fusesource\meshkeeper\meshkeeper-mop-resolver\${project.version}\meshkeeper-meshkeeper-mop-resolver-${project.version}.jar
  set CLASSPATH=%CLASSPATH%;%MESHKEEPER_HOME%\repository\commons-logging\commons-logging\${commons-logging-version}\commons-logging-${commons-logging-version}.jar
  set CLASSPATH=%CLASSPATH%;%MESHKEEPER_HOME%\repository\log4j\log4j\${log4j-version}\log4j-${log4j-version}.jar
goto :EOF
    
:ERROR
  echo Paused to catch any errors. Press any key to continue.
  errorlvl 1    
  pause
goto :END

:END
endlocal