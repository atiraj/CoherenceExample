@echo off
@
@rem This will start a console application
@rem demonstrating the functionality of the Coherence(tm) API
@
setlocal

:config
@rem specify the Coherence installation directory
set coherence_home=%~dp0\..

@rem specify the lib installation directory
set lib_home=%coherence_home%\lib

:start
if not exist "%coherence_home%\lib\coherence.jar" goto instructions

set java_home=%java_home:"=%

if "%java_home%"=="" (set java_exec=java) else (set "java_exec=%java_home%\bin\java")

:launch

@rem Note that this script attempts to start the query tool in storage disabled mode, ultimately this is dependent
@rem on cache configuration being used which may override the coherence.distributed.localstorage system property.
@rem It is not recommended to run the query tool as storage enabled in a production cluster.

"%java_exec%" -server -showversion -Dcoherence.distributed.localstorage=false -cp "%coherence_home%\lib\coherence.jar;%lib_home%\jline.jar" com.tangosol.coherence.dslquery.QueryPlus %*

goto exit

:instructions

echo Usage:
echo   ^<coherence_home^>\bin\query.cmd
goto exit

:exit
endlocal
@echo on