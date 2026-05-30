@echo off
setlocal
cd /d "%~dp0"

set "JDK=C:\Program Files\Java\jdk-11.0.0.1"
if not exist "%JDK%\bin\javac.exe" (
  echo No se encontro javac en "%JDK%\bin\javac.exe".
  echo Ajusta la variable JDK dentro de este archivo.
  exit /b 1
)

if not exist build\classes mkdir build\classes

dir /s /b src\*.java > build\sources.txt
"%JDK%\bin\javac.exe" -encoding UTF-8 -d build\classes -sourcepath src @build\sources.txt
if errorlevel 1 exit /b 1

echo.
echo Compilacion completada.
