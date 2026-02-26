@echo off
java -jar "%~dp0out\Kokotmetr.jar"
if %ERRORLEVEL% neq 0 (
    echo.
    echo Chyba pri spusteni. Mas nainstalovanou Javu?
    pause
)
