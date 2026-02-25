@echo off

IF NOT EXIST "\Windows\Desktop\Kokotmetr.lnk" copy "\Application\kokotmetr\Kokotmetr.lnk" "\Windows\Desktop\Kokotmetr.lnk"

\Windows\CrEme\bin\CrEme.exe -jar -aa 1 \Application\kokotmetr\Kokotmetr.jar
echo.
echo Aplikace skoncila, stiskni libovolnou klavesu...
pause >nul
