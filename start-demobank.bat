@echo off
if exist "%JAVA_HOME%\bin\java.exe" (
    "%JAVA_HOME%\bin\java.exe" -jar target\demobank.jar
) else (
    java -jar target\demobank.jar
)
