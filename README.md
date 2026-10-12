# DashCam ADAS PRO — 3.4 neon neural experimental

**Branch de aparência comercial:** refinamento de controles e [prévia das configurações](design/comercial/LEIA-ME.md), ainda sem novo APK. Os downloads e os 81 testes abaixo correspondem ao build 3.4 anterior a esse refinamento.

Build para **NWD K2001N, Android 7.1.1 (API 25), ARM32**. Interface LIVE atualizada segundo as referências, popups com neon vermelho, foco no alvo real na tela direita e controles de sensibilidade/espessura das faixas. Redes locais YOLOP (faixas/área dirigível) e SSD MobileNet (veículos/pessoas), sem internet ou treinamento durante a condução.

## Downloads

- [APK 3.4 NWD K2001N ARM32 — V1 + V2](https://github.com/oinavile2-max/ADAS-oinavile/raw/refs/heads/main/APKS/DashCam_ADAS_PRO_3_4_NWD_K2001N_ARM32_NEON_DEBUG.apk)
- [Código-fonte completo 3.4](https://github.com/oinavile2-max/ADAS-oinavile/raw/refs/heads/main/DashCam_Fonte.zip)
- [APK genérico 3.3 histórico, anterior à nova interface](https://github.com/oinavile2-max/ADAS-oinavile/raw/refs/heads/main/APKS/DashCam_ADAS_PRO_3_3_NEURAL_GENERIC_DEBUG.apk)

APK debug, versionCode 34, minSdk 24, targetSdk/compileSdk 35. O perfil ARM32 inclui o driver do APK fornecido, habilitado em API 24–27. O identificador de compilação `k2000Native` é histórico. CPU/ABI e câmera da central ainda precisam de confirmação física. O APK genérico histórico não suporta páginas de memória de 16 KB.

## Usar

1. Instale o APK ARM32. Na LIVE, abra o ícone de câmera lateral para selecionar a fonte ou use CONFIG → CONFIG → AUTORIZAR USB — ABRIR LIVE COM IA. Conceda a permissão USB solicitada pelo Android.
2. No menu da câmera, ajuste rotação e espelhamento para mostrar a estrada horizontalmente. **Sensibilidade e espessura das faixas** abre os controles: 0–100% (padrão 50%) e 1–12 dp (padrão 4). A espessura altera o desenho; a sensibilidade ajusta a margem LDW sem dispensar confiança/GPS/calibração.
3. Com o veículo parado, calibre em ADAS usando medidas reais. Alertas LDW/FCW/pedestre exigem GPS válido, calibração, confiança, confirmação temporal e resultados recentes.
4. **IA ativa** depende de inferências executadas. Análises acima de 500 ms suspendem os alertas dependentes desses resultados. Sem dados, o HUD exibe `--`; resolução e sensores refletem a fonte real.
5. Toque no botão superior esquerdo para gravar; toque longo protege o clipe. REC também está no menu da câmera. Captura, análise e gravação pertencem ao serviço foreground; continuidade em segundo plano/tela apagada ainda precisa de teste na central.
6. Áudio oferece seis padrões de bip, frequência, duração, volume e prévia.

A tela direita apresenta o mesmo quadro real analisado com foco no alvo, não uma segunda câmera. Os destaques vermelhos correspondem a risco confirmado e desaparecem ao perder dados recentes. Faixas/corredor/seta usam curvas observadas. Não há bordas de estrada fictícias ou distância métrica inventada. O aviso de pedestre é experimental; semáforos, placas e ciclistas com alertas completos continuam pendentes.

## Compilar e verificar

JDK 17, Gradle 8.9, SDK Platform 35 e Build Tools 35.0.0:

```sh
./gradlew --no-daemon -Pk2000Native=true :app:assembleDebug :app:lintDebug
```

Hashes dos dois modelos e `testDebugUnitTest` são dependências obrigatórias do empacotamento. O teste da rede de faixas executa o modelo real em ONNX Runtime CPU. Runtime de computador permanece apenas no escopo dos testes; APK contém runtime Android ARM32. No Windows: `GERAR APK K2000N.bat`; execução Windows não validada nesta nuvem.

## Evidências e instalação

**81 testes passaram**, sem falhas/erros/skips; lint sem erros, 79 avisos. Modelos e contrato JNI conferidos no APK, com assinaturas V1/V2 e certificado igual às entregas 3.2/3.3. [Relatório técnico](RELATORIO_TECNICO.md) e `relatorios/3_4` registram os resultados. [Prévias](design/preview/LEIA-ME.md) são maquetes HTML/SVG, não capturas Android; identidade pixel a pixel ainda não comprovada.

O vídeo recebido foi analisado no host; não há anotações para medir precisão. O ensaio SSD encontrou um candidato pessoa com confiança 0,5, abaixo do limiar de aviso 0,6. Testes de inferência no computador não comprovam latência em ARM32.

Nenhum Android conectado à nuvem: instalação, interface, GPS, USB, áudio/FM e gravação em segundo plano ainda precisam de teste físico. `python3 build-tools/test_android_device.py --help` descreve o teste preparado. O erro de instalação anterior não teve causa identificada; V1/V2 não comprova a solução no aparelho. `adb install -r arquivo.apk` fornece o erro detalhado. Versões compiladas em outro computador podem usar certificado diferente.

YOLOP preserva licença MIT; SSD, Apache-2.0. Proveniências estão em `app/src/main/assets` e `referencias_nativas/PROVENIENCIA.md`. A licença comercial do driver fornecido continua sem confirmação.
