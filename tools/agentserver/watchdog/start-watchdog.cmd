@echo off
REM Start the Microbot watchdog in a new PowerShell window.
REM Double-click this file or run from cmd.
REM Stop the watchdog with Ctrl+C in its window.

set "SCRIPT_DIR=%~dp0"
powershell.exe -NoExit -ExecutionPolicy Bypass -File "%SCRIPT_DIR%watchdog.ps1"
