@echo off
echo ============================================
echo    JatsApp - Generando ejecutables JAR
echo ============================================
echo.

:: Crear carpetas de salida
if not exist "dist" mkdir dist
if not exist "dist\servidor" mkdir dist\servidor
if not exist "dist\cliente-windows" mkdir dist\cliente-windows
if not exist "dist\cliente-ubuntu" mkdir dist\cliente-ubuntu

:: Servidor
echo [1/3] Compilando JatsApp Servidor...
cd JatsApp_Servidor
call mvn clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo ERROR: Fallo al compilar servidor
    pause
    exit /b 1
)
copy target\JatsApp-Server-Console.jar ..\dist\servidor\ >nul
copy target\JatsApp-Server-GUI.jar ..\dist\servidor\ >nul
copy src\main\resources\config.properties ..\dist\servidor\ >nul
cd ..
echo       OK: JatsApp-Server-Console.jar
echo       OK: JatsApp-Server-GUI.jar
echo       OK: config.properties (servidor)

:: Cliente Windows
echo [2/3] Compilando JatsApp Cliente Windows...
cd JatsApp_Client
call mvn clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo ERROR: Fallo al compilar cliente Windows
    pause
    exit /b 1
)
copy target\JatsApp-Client-Windows.jar ..\dist\cliente-windows\ >nul
copy src\main\resources\config.properties ..\dist\cliente-windows\ >nul
cd ..
echo       OK: JatsApp-Client-Windows.jar
echo       OK: config.properties (cliente)

:: Cliente Ubuntu
echo [3/3] Compilando JatsApp Cliente Ubuntu...
cd JatsApp_Client_Ubuntu
call mvn clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo ERROR: Fallo al compilar cliente Ubuntu
    pause
    exit /b 1
)
copy target\JatsApp-Client-Ubuntu.jar ..\dist\cliente-ubuntu\ >nul
copy src\main\resources\config.properties ..\dist\cliente-ubuntu\ >nul
cd ..
echo       OK: JatsApp-Client-Ubuntu.jar
echo       OK: config.properties (cliente)

echo.
echo ============================================
echo    COMPLETADO! Archivos en carpeta: dist\
echo ============================================
echo.
echo Estructura generada:
echo   dist\
echo   +-- servidor\
echo   ^|   +-- JatsApp-Server-Console.jar
echo   ^|   +-- JatsApp-Server-GUI.jar
echo   ^|   +-- config.properties  (EDITAR: BD, email)
echo   +-- cliente-windows\
echo   ^|   +-- JatsApp-Client-Windows.jar
echo   ^|   +-- config.properties  (EDITAR: IP servidor)
echo   +-- cliente-ubuntu\
echo       +-- JatsApp-Client-Ubuntu.jar
echo       +-- config.properties  (EDITAR: IP servidor)
echo.
echo IMPORTANTE: Edita los config.properties antes de ejecutar!
echo.
pause

