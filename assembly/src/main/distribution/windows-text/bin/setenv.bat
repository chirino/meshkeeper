REM Set this to a particular JAVA_EXE if you wish
set JAVA_EXE=

REM Setup the Java Virtual Machine

goto BEGIN

:warn
	errorlvl 1    
goto :END

:BEGIN

REM Set MESHKEEPER_HOME
cd %~dp0%
cd ..
set MESHKEEPER_HOME=%cd%

if not exist "%MESHKEEPER_HOME%" (
    call :warn MeshKeeper home directory is not valid: %MESHKEEPER_HOME%
    goto END
)

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

REM Setup the classpath
set CLASSPATH=
pushd "%MESHKEEPER_HOME%\lib"
for %%G in (*.*) do call:APPEND_TO_CLASSPATH %%G
popd
goto CLASSPATH_END

: APPEND_TO_CLASSPATH
set filename=%~1
set suffix=%filename:~-4%
if %suffix% equ .jar set CLASSPATH=%CLASSPATH%;%MESHKEEPER_HOME%\lib\%filename%
goto :EOF

:CLASSPATH_END

REM setup of defaults
set OPTS=-Dlog4j.configuration=file:%MESHKEEPER_HOME%\etc\log4j.properties

cd %MESHKEEPER_HOME%

:END
