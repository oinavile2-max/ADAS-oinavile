# V4.5 — Correção defensiva de inicialização (NÃO VALIDADA)

A LIVE agora é construída antes dos serviços de câmera, áudio TTS e banco de dados. Falhas desses serviços são registradas em `DashCamStartup` e não devem impedir a construção da tela. Foi acrescentada uma tela de erro mínimo se o próprio layout falhar. As verificações de inicialização foram reforçadas em `onResume`, `onSurfaceTextureAvailable` e `onDestroy`.

**Não há confirmação da causa original sem `FATAL EXCEPTION` do logcat. Não houve build ou teste em dispositivos.**

## Como obter o erro exato
Com depuração USB ativada e Android Platform Tools instalado, execute no Windows:

```bat
adb devices
adb logcat -c
adb shell am force-stop br.com.dashcamadas
adb shell monkey -p br.com.dashcamadas 1
adb logcat -d -v time -s AndroidRuntime:E DashCamStartup:E > crash_dashcam.txt
```

Envie `crash_dashcam.txt`. Para K2001N sem ADB, use Android Studio conectado ou aplicativo de coleta de log com permissões apropriadas.

A correção não implementa UVC proprietária ou reconhecimento ADAS. Não é uma build comercial pronta.
