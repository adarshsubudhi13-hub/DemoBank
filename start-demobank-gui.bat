@echo off
setlocal
if exist "%JAVA_HOME%\bin\java.exe" (
    set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVA_CMD=java"
)
if exist "demobank" cd demobank
echo Launching DemoBank Secure Client Java Swing Desktop GUI...
"%JAVA_CMD%" -cp target\demobank.jar com.demobank.gui.DemoBankSwingApp
