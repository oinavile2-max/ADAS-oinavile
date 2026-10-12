# DashCam ADAS PRO — build 3.4 neon neural NWD K2001N

12/10/2026. Build autorizado pelo usuário após a revisão visual. Android 7.1.1/API 25, perfil armeabi-v7a; versionCode 34, minSdk 24, targetSdk 35. Build debug com o mesmo certificado das entregas anteriores.

## Alterações entregues

LIVE com posições proporcionais às referências; HUD dinâmico; banners, diálogos, listas e avisos do app com neon vermelho. Corrigida a chamada de configuração do fundo do Spinner para `setPopupBackgroundDrawable`, verificada pelo compilador Kotlin. A tela direita usa o mesmo bitmap analisado e recorte limitado ao alvo; mapeamento compartilhado evita desenhar a caixa em outro quadro. Riscos vencidos removem o destaque. Prévias HTML/SVG não são capturas Android.

Assistente de faixas desenha curvas observadas, corredor/seta ciano e marcador azul/vermelho do veículo. Sensibilidade LDW 0–100% e espessura 1–12 dp são persistidas, com acesso no menu da câmera. Confiança, GPS, calibração e confirmação continuam obrigatórios.

O SSD existente separa pessoas (classe COCO 0) de veículos (2/5/7) em uma inferência. Aviso experimental de pedestre exige confiança >=0,6, geometria à frente, suporte da máscara de área dirigível, GPS >=3 m/s, calibração e confirmação temporal; mudança de alvo reinicia confirmação. Não estima distância métrica do pedestre. Semáforos, placas e ciclistas com alertas completos permanecem pendentes.

YOLOP e SSD pré-treinados são executados localmente. Hashes são conferidos no carregamento e pela tarefa `verifyNeuralModelAssets`. Empacotamento/assemble dependem dessa tarefa e de `testDebugUnitTest`, incluindo inferência real YOLOP. O driver ARM32, serviço USB/RGBA, EGL/MediaRecorder e gravação em serviço da 3.3 são preservados. Implementação e ensaios anteriores estão em [relatório histórico 3.3](relatorios/HISTORICO/RELATORIO_3_3.md).

## Verificação desta entrega

Comando: `./gradlew --no-daemon -Pk2000Native=true :app:assembleDebug :app:lintDebug`.

- Compilação/empacotamento concluídos com sucesso.
- 81 testes JVM: zero falhas, erros ou testes ignorados. Oito adicionais verificam decoder pessoa/veículo, confirmação/perda/troca de alvo, pré-condições GPS/calibração/máscara, sensibilidade e projeção com recorte/espelhamento.
- Inferência real YOLOP em CPU executada no teste compartilhado; testes geométricos não medem precisão em estrada.
- Lint: zero erros, 79 avisos; relatório completo preservado.
- ABI somente armeabi-v7a; 25 métodos JNI e 23 callbacks conferidos. Hashes dos dois modelos e do driver original verificados, sem execução física do driver.
- Assinaturas V1 e V2 e alinhamento ZIP conferidos separadamente; V1 é exercitada pelo verificador forçado, sem alterar minSdk 24. Certificado preservado.

Logs, XML de testes/lint e verificações do pacote: `relatorios/3_4`. O ensaio SSD adicional no vídeo teve 27 inferências reais no host e um candidato pessoa de confiança 0,5, abaixo do limiar. Isso não comprova precisão nem alerta Android. Quadros originais do vídeo recebido não são publicados.

## Validação física pendente

ADB não possui aparelho conectado. Instalação, identidade visual pixel a pixel, GPS, imagem USB, latência/RAM/temperatura ARM32, áudio/FM e gravação com tela apagada não foram testados na NWD K2001N. O erro de instalação anterior continua sem mensagem diagnóstica; assinaturas válidas não identificam sua causa. O helper `build-tools/test_android_device.py` está preparado para verificar inferências em segundo plano e MP4/H.264 real.
