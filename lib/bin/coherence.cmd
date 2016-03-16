@echo off
@
@rem This will start a console application
@rem demonstrating the functionality of the Coherence(tm) API
@
setlocal

:config
@rem specify the Coherence installation directory
set coherence_home=%~dp0\..

:start
if not exist "%coherence_home%\lib\coherence.jar" goto instructions

set java_home=%java_home:"=%

if "%java_home%"=="" (set java_exec=java) else (set "java_exec=%java_home%\bin\java")

:launch

if "%1"=="-jmx" (
	set jmxproperties=-Dtangosol.coherence.management=all -Dtangosol.coherence.management.remote=true
	shift
)

@rem Note that this script attempts to start the console in storage disabled mode, ultimately this is dependent
@rem on cache configuration being used which may override the coherence.distributed.localstorage system property.
@rem It is not recommended to run the console as storage enabled in a production cluster.

set java_opts=-Dcoherence.distributed.localstorage=false %jmxproperties%

"%java_exec%" -server -showversion %java_opts% -cp "%coherence_home%\lib\coherence.jar" com.tangosol.net.CacheFactory %1

goto exit

:instructions

echo Usage:
echo   ^<coherence_home^>\bin\coherence.cmd
goto exit

:exit
endlocal
@echo on
