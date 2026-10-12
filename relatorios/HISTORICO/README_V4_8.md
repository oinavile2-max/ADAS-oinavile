# DashCam ADAS PRO — Premium Luxury V3.0 (projeto Android)

## O que este projeto entrega

- **Menu inicial fiel à imagem aprovada** (`premium_menu.jpg`): arte 3D original fornecida pelo usuário, com 8 áreas de toque e barra inferior funcional; a imagem é fixa e escala para a tela horizontal.
- **Android 7.1 / API 25**: `minSdk 24`, orientação horizontal, projeto Kotlin sem dependências de UI de terceiros.
- **Tablets**: câmera traseira integrada por Camera2; seleção manual entre câmeras que o Android disponibiliza; câmera USB-C **somente se o sistema a expuser via Camera2**.
- **K2001N**: descoberta Camera2, vídeo LIVE e gravação H.264 720p quando a câmera aceitar esse formato; status real de falha quando indisponível.
- **Gravação**: arquivos MP4 privados do app, segmentos configuráveis de 1/3/5 minutos, proteção manual do clipe atual.
- **Radares GPS**: banco local manual e alertas geográficos aproximados, sem detecção por radiofrequência.
- **Luxury Audio**: TTS pt-BR (dependente do motor instalado), modo MIX sem solicitar AudioFocus, efeitos sintetizados de teste a 65%, voz 80%; não são masters sonoros premium finalizados.
- **Eventos**: registros locais de radares e proteção de clipes; demonstrações ADAS explicitamente marcadas como testes.
- **Diagnóstico**: lista de câmeras Camera2, VID/PID de dispositivos USB Host, indicação clara de limitações.

## Ainda NÃO implementado / NÃO validado

1. Protocolo USB proprietário do **UCarDVR (`com.ankai.cardvr`)** e sua `libmycamera.so`: a biblioteca do APK original não foi copiada ou incorporada. Não presumir funcionamento da câmera original sem investigação e teste na K2001N.
2. Detecção real por visão computacional: colisão, pedestres, ciclistas, faixas, semáforos e placas; **os botões destas seções são testes manuais de popups/voz, não ADAS automático**.
3. Reconhecimento de infrações, processamento inteligente de eventos, anexação automática de imagem/vídeo, modelo de IA e calibração geométrica.
4. Som Luxury Elegance definitivo, animações de bordas pulsantes e reprodução de rádio FM MIX verificada.
5. UVC por acesso USB Host direto para tablets cujos drivers não expõem a câmera no Camera2.
6. Persistência da gravação em segundo plano, gerenciamento automático de espaço, PiP, suporte amplo a resoluções/FPS.
7. Tela LIVE com fidelidade total à referência cinematográfica: **o menu é visualmente fiel, mas a tela LIVE é uma interface nativa funcional de bancada**.

## Build

Abrir a pasta no **Android Studio**, instalar Android SDK Platform 35, Build Tools e JDK 17. Sincronizar Gradle e executar `assembleDebug`. APK esperado em `app/build/outputs/apk/debug/app-debug.apk` **somente após build bem-sucedido**.

Sem SDK/Gradle Android disponíveis neste ambiente, o projeto foi empacotado como **código-fonte, não APK**. A compilação e testes em hardware continuam pendentes.

## Hardware de referência

- Central K2001N NWD, Android 7.1, 2 GB RAM / 32 GB.
- APK UCarDVR enviado como referência para análise de hardware; **não redistribuído** dentro deste projeto.
- Tablet Android com câmera traseira; USB-C/OTG condicionado a driver e protocolo.

## Privacidade e segurança

Vídeos e eventos permanecem no armazenamento privado do aplicativo. Os avisos são assistivos, não substituem a atenção do motorista nem são prova legal de infração. Não usar os botões de teste enquanto dirige.
