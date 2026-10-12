# DashCam ADAS PRO — 3.3 neural experimental

**Branch de prévia visual, sem build:** a interface Android foi atualizada a partir das imagens, mas não foi compilada nesta etapa. Veja [prévia dos popups](design/preview/LEIA-ME.md). Os APKs e o ZIP de fonte nos downloads abaixo pertencem à entrega 3.3 anterior à alteração visual; os 73 testes citados também correspondem àquela entrega.

Redes neurais locais para **faixas/área dirigível** (YOLOP) e **veículos** (SSD MobileNet). Processamento no Android, sem internet e sem aprendizagem automática durante a condução.

## Downloads

- [APK NWD K2001N ARM32 — assinaturas V1 + V2](https://github.com/oinavile2-max/ADAS-oinavile/raw/refs/heads/main/APKS/DashCam_ADAS_PRO_3_3_NWD_K2001N_ARM32_V1_V2_DEBUG.apk)
- [APK genérico — celular/tablet/emulador](https://github.com/oinavile2-max/ADAS-oinavile/raw/refs/heads/main/APKS/DashCam_ADAS_PRO_3_3_NEURAL_GENERIC_DEBUG.apk)
- [Código-fonte completo](https://github.com/oinavile2-max/ADAS-oinavile/raw/refs/heads/main/DashCam_Fonte.zip)

A identificação correta da central é **NWD K2001N, Android 7.1.1 (API 25)**. Os arquivos antigos e a opção de compilação `k2000Native` permanecem como identificadores históricos para preservar os downloads existentes. CPU/ABI e câmera ainda precisam de confirmação física.

APKs debug assinados. minSdk 24, targetSdk/compileSdk 35. Driver do APK fornecido habilitado somente no perfil ARM32 e Android 7/8 (API 24–27); compatibilidade real ainda precisa de teste na NWD K2001N, informada pelo usuário como Android 7.1.1 (API 25).

O APK genérico exige páginas de memória de 4 KB: a ponte JNI ARM64 do ONNX Runtime 1.20.0 não suporta dispositivos configurados com páginas de 16 KB. Essa restrição não se aplica ao perfil ARM32 da NWD K2001N Android 7.1.1.

## Usar

1. Na NWD K2001N, instale o APK ARM32. Abra **CONFIG → CONFIG → AUTORIZAR USB — ABRIR LIVE COM IA**, selecione a câmera e conceda a autorização USB do Android. Também pode selecionar a fonte no botão CÂMERA da LIVE. O APK genérico usa Camera2.
2. Confira a orientação em **GIRAR / ESPELHAR**. A rede exige imagem da estrada orientada horizontalmente.
3. Com o veículo parado, calibre pelo botão **ADAS**, usando medidas reais. Detecções podem aparecer sem GPS; os alertas LDW/FCW exigem GPS válido, calibração e resultados recentes.
4. A indicação **IA ativa** corresponde a inferências realmente executadas. Se a análise exceder 500 ms, o app avisa e suspende os alertas dependentes desse resultado.
5. Use **REC** para gravar. O serviço mantém captura, análise e codificação ao sair da Activity. Continuidade em segundo plano/tela apagada depende do sistema e do hardware e ainda precisa de teste físico.
6. Em áudio, permanecem os seis padrões de bip, frequência, duração e volume ajustáveis, com prévia antes de salvar.

## Melhorias

YOLOP produz máscaras neurais de faixas e área dirigível; curvas são ajustadas aos pixels observados, com confirmação temporal e suporte de estrada ao lado. Pontos de cada borda são escolhidos separadamente, preservando marcações tracejadas quando falta um trecho próximo da câmera. Trechos fora do quadro não são extrapolados para gerar alertas. Veículos fora da área dirigível têm a associação de risco restringida.

USB autorizado agora alimenta **o mesmo serviço** usado pela LIVE e pelos modelos. O driver entrega quadros RGBA a um ImageReader independente da tela. Um renderizador EGL do app envia esses quadros reais ao MediaRecorder/H.264; a gravação não depende de um modo de codificação não documentado do driver. Perda de quadros USB finaliza a gravação e suspende ADAS. Nenhuma dessas combinações EGL/driver/encoder foi validada fisicamente nesta nuvem.

Resultados da IA são desenhados sobre o próprio quadro analisado, evitando caixas/faixas de um quadro antigo sobre outra imagem. Quadros uniformes e saídas inválidas não fornecem evidência de faixas. Os pesos têm checksum verificado no carregamento.

## Compilar e verificar

JDK 17, Gradle 8.9, Android Platform 35 e Build Tools 35.0.0.

- Genérico: `./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
- ARM32: acrescente `-Pk2000Native=true` ao comando.
- Windows: `GERAR APK.bat` ou `GERAR APK K2000N.bat`. Execução Windows não realizada nesta nuvem.

Os testes JVM usam ONNX Runtime para computador apenas em testRuntimeOnly. O APK inclui exclusivamente o runtime Android. Bibliotecas JNI são comprimidas e extraídas na instalação, preservando ARM32/ARM64/x86/x86_64 no APK genérico.

## Evidências

73 testes passaram, incluindo inferência real do modelo pelo mesmo RoadNeuralSession usado no app. Vídeo recebido: 82 execuções neurais, 66 quadros com candidatos a curvas e sete pares confirmados pelo critério interno; a versão 3.2 teve zero pares. Isso **não é medida de precisão**: não há marcações de referência, GPS ou calibração nesse vídeo. Nenhum risco crítico foi produzido sem esses dados.

Veja [RELATORIO_TECNICO.md](RELATORIO_TECNICO.md), `relatorios/3_3`, [proveniência YOLOP](app/src/main/assets/MODEL_PROVENANCE_YOLOP.md) e [referência do driver](referencias_nativas/PROVENIENCIA.md). GPS, câmera, áudio/FM, interface, driver USB e gravação continuam sem validação física. Não houve aparelho conectado à nuvem.

Teste físico preparado: `python3 build-tools/test_android_device.py --help`. Para USB, autorize a fonte na interface antes do teste e use --native-usb; --require-realtime exige o limite de 500 ms. O teste verifica avanço das duas redes em segundo plano e o MP4 real.

YOLOP: licença MIT preservada. SSD: licença Apache-2.0 preservada. A licença comercial da biblioteca do APK fornecido não foi verificada. Placas, semáforos, pedestres e ciclistas com alertas completos continuam fora desta revisão.

## Instalação na NWD K2001N

Após relato de erro de instalação, foi preparado um APK ARM32 com assinaturas V1 e V2, mantendo o mesmo applicationId, versionCode e certificado da versão 3.3 anterior. O APK anterior tinha somente V2, válido para Android 7.1.1; a assinatura adicional amplia a compatibilidade com instaladores antigos, mas a causa do erro relatado ainda não foi identificada. O aparelho não está conectado à nuvem.

A versão 3.2 publicada neste repositório e a 3.3 usam o mesmo certificado. Versões compiladas em outro computador podem ter certificado diferente. Para diagnosticar, informe a mensagem exata, o nome do APK e se existe versão anterior instalada. Se houver ADB disponível, `adb install -r arquivo.apk` fornece o código detalhado da falha. `adb shell getprop ro.build.version.sdk` e `adb shell getprop ro.product.cpu.abilist` conferem API e ABIs reais da central.
