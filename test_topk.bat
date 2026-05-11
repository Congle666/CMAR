@echo off
cd d:\CMAR
javac -d out src\*.java
if %errorlevel% neq 0 (
    echo Compilation FAILED
    exit /b 1
)
echo Compilation OK
timeout /t 5
java -cp out BenchmarkTopK
