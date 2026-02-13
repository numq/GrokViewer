@echo off
REM Point this to your JDK 17+ install (e.g. Java 25). Use forward slashes or escaped backslashes.
REM Common locations:
REM   Microsoft:    C:/Program Files/Microsoft/jdk-25.0.x
REM   Eclipse:      C:/Program Files/Eclipse Adoptium/jdk-25.x.x-hotspot
REM   Oracle:       C:/Program Files/Java/jdk-25
set "JDK_PATH=C:/Program Files/Microsoft/jdk-25.0.1"

if defined JAVA_HOME (
  echo Using JAVA_HOME=%JAVA_HOME%
) else (
  set "JAVA_HOME=%JDK_PATH%"
  echo Using JDK at %JAVA_HOME%
)

call gradlew.bat :desktop:run %*
