# Compilar no Android Studio

1. Instale Android Studio, JDK 17 e Android SDK Platform 35.
2. Abra a pasta `DashCam_ADAS_PRO_V3_Projeto`.
3. Aguarde a sincronização do Gradle (requer internet para baixar plugins).
4. Menu **Build > Build APK(s)**, ou execute `gradle assembleDebug` com Gradle 8.9+ configurado.
5. APK gerado em `app/build/outputs/apk/debug/app-debug.apk` após compilação bem-sucedida.
6. Instale primeiro em um tablet de teste e depois na K2001N. Se Camera2 não mostrar a dashcam original, será necessário investigar o protocolo do UCarDVR.

**Não há APK compilado neste pacote.** Nenhuma compilação ou execução no dispositivo foi validada neste ambiente.
