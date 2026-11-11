@echo off
REM Starts the application on Windows. Edit the values below (or set them once in System Environment Variables).
REM Do NOT commit your real password to GitHub.
if "%DB_USERNAME%"=="" set DB_USERNAME=root
if "%DB_PASSWORD%"=="" (
  echo Please set DB_PASSWORD first, for example:  set DB_PASSWORD=your_mysql_password
  exit /b 1
)
mvn spring-boot:run
