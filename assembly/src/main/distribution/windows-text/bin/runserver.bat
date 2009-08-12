echo off
TITLE CloudLaunch Server

REM  runserver.bat

REM  ---------------------------------------------------------------------------
REM  Usage:   runTRAgent
REM  Purpose: See the cloudlaunch documentation for the explanation
REM  ---------------------------------------------------------------------------

REM  -------- USER MUST SET THE FOLLOWING VARIABLES AFTER INSTALL --------------
REM  JAVA_EXE should indicate the path to a 1.5 (or higher) java executable
REM  ---------------------------------------------------------------------------

REM Set this to a particular JAVA_EXE if you wish
set JAVA_EXE=

REM Setup the Java Virtual Machine

goto BEGIN

:warn
    echo runserver %*
goto :END

:BEGIN

if not "%JAVA_EXE%" == "" goto :Check_JAVA_END
	set JAVA_EXE=%JAVA%
	if not "%JAVA_EXE%" == "" goto :Check_JAVA_END
    	set JAVA_EXE=java
    	if "%JAVA_HOME%" == "" call :warn JAVA_HOME not set; results may vary
    	if not "%JAVA_HOME%" == "" set JAVA_EXE=%JAVA_HOME%\bin\java.exe
    	if not exist "%JAVA_HOME%" (
        	call :warn %JAVA_HOME% does not exist
        	goto END
    	)
:Check_JAVA_END

REM ---------------YOU DO NOT NEED TO CHANGE ANYTHING BELOW --------------------

REM ---------------------------------------------------------------------------
REM CLASSES contains the classpath required by a cloudlaunch server. Relative
REM paths are relative to the cloudlaunch\bin directory.
REM ---------------------------------------------------------------------------
set CLASSES=..\lib\testrunner.jar;..\lib\rmiviajms.jar

echo ------- Starting Agent -------
echo %JAVA_EXE% -classpath %CLASSES% org.fusesource.cloudlaunch.control.Main %*
%JAVA_EXE% -classpath %CLASSES% org.fusesource.cloudlaunch.control.Main %*

:END
echo Paused to catch any errors. Press any key to continue.
pause
