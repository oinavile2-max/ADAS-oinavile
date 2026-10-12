@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title DashCam ADAS PRO - Build Debug
set "EXITCODE=1"
if not exist "app\build.gradle.kts" goto MISSING_PROJECT
if not defined JAVA_HOME if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\javac.exe" set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"
where java >nul 2>&1
if errorlevel 1 goto NO_JDK
where javac >nul 2>&1
if errorlevel 1 goto NO_JDK
java -version
javac -version
if not defined ANDROID_HOME if defined ANDROID_SDK_ROOT set "ANDROID_HOME=%ANDROID_SDK_ROOT%"
if not defined ANDROID_HOME if exist "%LOCALAPPDATA%\Android\Sdk\platforms" set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
if not defined ANDROID_HOME goto NO_SDK
if not exist "%ANDROID_HOME%\platforms\android-35\android.jar" goto NO_SDK
if not exist "%ANDROID_HOME%\build-tools\35.0.0\aapt2.exe" goto NO_SDK
if not exist "gradlew.bat" goto NO_WRAPPER
if not exist "gradle\wrapper\gradle-wrapper.jar" goto NO_WRAPPER
if not exist "gradle\wrapper\gradle-wrapper.properties" goto NO_WRAPPER
if exist "APK_GERADO\DashCam_ADAS_PRO_DEBUG.apk" del "APK_GERADO\DashCam_ADAS_PRO_DEBUG.apk"
echo Compilando. Aguarde; log: build-debug.log
call gradlew.bat --no-daemon clean :app:assembleDebug --stacktrace > build-debug.log 2>&1
set "BUILD_EXIT=%ERRORLEVEL%"
type build-debug.log
if not "%BUILD_EXIT%"=="0" goto BUILD_FAILED
if not exist "app\build\outputs\apk\debug\app-debug.apk" goto BUILD_FAILED
if not exist "APK_GERADO" mkdir "APK_GERADO"
copy /Y "app\build\outputs\apk\debug\app-debug.apk" "APK_GERADO\DashCam_ADAS_PRO_DEBUG.apk" >nul
if errorlevel 1 goto BUILD_FAILED
echo [SUCESSO] %CD%\APK_GERADO\DashCam_ADAS_PRO_DEBUG.apk
set "EXITCODE=0"
goto END
:MISSING_PROJECT
echo [ERRO] Extraia o ZIP completo e execute na raiz do projeto.
goto END
:NO_JDK
echo [ERRO] Instale um JDK completo. JDK 17 recomendado; configure JAVA_HOME.
goto END
:NO_SDK
echo [ERRO] Configure ANDROID_HOME e instale SDK Platform 35 e Build Tools 35.0.0.
goto END
:NO_WRAPPER
echo [ERRO] Gradle Wrapper incompleto. Extraia novamente o ZIP integral.
goto END
:BUILD_FAILED
echo [ERRO] Build ou copia do APK falhou. Consulte build-debug.log acima.
goto END
:END
if not defined CI pause
exit /b %EXITCODE%
