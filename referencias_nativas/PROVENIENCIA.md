# Referência nativa fornecida pelo usuário

APK: `apkfiles.com_618718_ucardvr.apk`, pacote `com.ankai.cardvr`, versão `20250515.3`.

SHA256 APK: `5669b6b81f591b872c227e831a0a3a4fa693cc557ae2d7365b0ac6024bdb5499`.

SHA256 libmycamera.so: `fa9d9a7936f2ae12ca8f6540f214bb581c0b26ac5222c1b76384dcf4f2795bd9`.

Biblioteca copiada sem alteração, ELF32 ARM, ABI armeabi-v7a; depende somente das bibliotecas de sistema listadas no ELF (android, log, EGL, GLESv2, OpenSLES, stdc++, m, c, dl). Somente a compilação `-Pk2000Native=true` inclui o binário e restringe o APK a ARM32. O APK genérico preserva as demais ABIs e não inclui esta biblioteca.

`MyCamera.java` reproduz os 25 descritores JNI e callbacks necessários ao RegisterNatives. O adaptador é escrito neste projeto; nenhum classes.dex do outro aplicativo foi incorporado. Os comandos de prévia 6/7 e configuração 18 foram confirmados na enumeração original; API completa em jni-api.json.

Prévia USB experimental disponível na tela de teste; não integra ainda gravação/LDW/FCW ao fluxo USB deste driver. A gravação/ADAS existentes usam Camera2. Não há teste físico da K2000N nem comprovação de compatibilidade com toda câmera UVC. O APK fornecido não contém licença geral de redistribuição da biblioteca; os direitos comerciais não foram verificados.
