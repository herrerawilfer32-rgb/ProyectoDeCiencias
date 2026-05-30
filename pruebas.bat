@echo off
setlocal
cd /d "%~dp0"

set "JDK=C:\Program Files\Java\jdk-11.0.0.1"
if not exist "%JDK%\bin\java.exe" (
  echo No se encontro java en "%JDK%\bin\java.exe".
  echo Ajusta la variable JDK dentro de este archivo.
  exit /b 1
)

call "%~dp0compilar.bat"
if errorlevel 1 exit /b 1

"%JDK%\bin\java.exe" -cp build\classes test.TestMotor
