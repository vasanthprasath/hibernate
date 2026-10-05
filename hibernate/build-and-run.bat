@echo off
setlocal
cd /d "%~dp0"
where mvn >nul 2>nul
if errorlevel 1 (
  echo Maven is not installed or not on PATH.
  exit /b 1
)
echo Building HibernateStudentApp...
mvn clean package
if errorlevel 1 exit /b 1
echo.
echo Starting application...
java -jar target\HibernateStudentApp-1.0-SNAPSHOT-executable.jar
