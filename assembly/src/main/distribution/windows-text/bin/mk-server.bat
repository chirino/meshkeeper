echo off
setlocal
TITLE MeshKeeper Server

REM  runagent.bat

REM  ---------------------------------------------------------------------------
REM  Usage:   runagent.bat
REM  Purpose: See the meshkeeper documentation for the explanation
REM  ---------------------------------------------------------------------------

REM  -------- USER MUST SET THE FOLLOWING VARIABLES AFTER INSTALL --------------
REM  JAVA_EXE should indicate the path to a 1.5 (or higher) java executable
REM  ---------------------------------------------------------------------------

REM Set this to a particular JAVA_EXE if you wish
set JAVA_EXE=

REM Setup the Java Virtual Machine

goto BEGIN

:warn
    echo runagent %*
goto :END

:BEGIN

CALL setenv.bat
if ERRORLEVEL 1 goto warn

echo ------- Starting Agent -------
%JAVA_EXE% %OPTS% -classpath %CLASSPATH% org.fusesource.meshkeeper.control.Main -dataDir "%MESHKEEPER_HOME%\data" %*

:END
echo Paused to catch any errors. Press any key to continue.
endlocal
pause
