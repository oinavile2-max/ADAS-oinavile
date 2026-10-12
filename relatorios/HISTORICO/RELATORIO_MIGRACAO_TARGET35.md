# Migração para targetSdk 35 e teste com vídeo real

Entrega de desenvolvimento parcial. A aceitação das sugestões autorizou migrar o alvo do Android e adaptar o aplicativo. minSdk permanece 24 (Android 7.0); isto mantém a elegibilidade de instalação no Android 7.1, mas não comprova funcionamento na K2001N.

## Mudanças

- targetSdk 35, compileSdk 35 e minSdk 24.
- Permissões fine/coarse solicitadas juntas quando nenhuma localização foi concedida. Localização aproximada é aceita sem insistir na precisa. POST_NOTIFICATIONS é solicitado a partir da API 33; sua negação não impede usar a câmera nem elimina os requisitos do serviço de primeiro plano.
- RadarForegroundService declarado como location, com FOREGROUND_SERVICE e FOREGROUND_SERVICE_LOCATION. O serviço inicia a partir da Activity visível, assume GPS/radares/áudio/eventos, tem notificação persistente e ação Encerrar GPS, trata posição desatualizada e não tenta reiniciar silenciosamente a partir do background. A Activity se conecta por binder e libera sua referência ao sair. Isso constitui implementação de GPS independente da LIVE, **não validação Android de funcionamento em segundo plano**.
- A câmera e a gravação ainda pertencem à Activity e são fechadas ao pausar a LIVE. Não foi declarada uma permissão/tipo camera de foreground service sem uma captura pertencente a esse serviço. Gravação/ADAS em segundo plano continuam pendentes; um serviço apenas nominal não resolveria essa limitação.
- FileProvider com URI content:// e permissão temporária de leitura permite abrir MP4 em reprodutor externo. Os caminhos expostos são limitados às pastas de clipes comuns e protegidos; nenhum diretório externo genérico foi liberado.
- Insets de barras e recortes são aplicados às quatro telas para o edge-to-edge do Android 15. Ajustes existentes de orientação, FIT/FILL, espelhamento, FULL, armazenamento e checksum foram preservados.
- LaneGeometry foi separado do Bitmap e é usado tanto pelo Android quanto pelo teste offline Java. Um lado detectado pode ser retornado sem duplicar nem inventar o lado ausente. Um teste e uma verificação geométrica conservadora rejeitam contraste vertical sem perspectiva coerente. O detector continua experimental e limitado a ajuste linear; a nova rejeição pode também omitir marcações verdadeiras, especialmente em curvas.

Dependência runtime adicionada: androidx.core:core:1.15.0 para FileProvider/insets, licença Apache-2.0. Nenhum modelo de IA foi adicionado. JUnit 4.13.2 continua restrito aos testes.
Arquivos alterados nesta etapa: app/build.gradle.kts, AndroidManifest.xml, MainActivity.kt, PremiumSectionActivity.kt, PremiumMenuActivity.kt, SettingsActivity.kt, LaneAssistView.kt e documentação. Novos: RadarForegroundService.kt, SafeInsets.kt, LaneGeometry.kt, LaneGeometryTest.kt, shared_clips.xml, VideoLaneProbe.java e run_video.py.

## Vídeo real

Video Project 1.mp4: H.264, 1280×720, 30 fps, duração 27,2 segundos; áudio AAC. O input e seus metadados têm SHA-256 registrado. Foram decodificados 82 quadros amostrados a 3 fps, em luminância 320×180. Foi executada a mesma classe Kotlin de detecção utilizada pelo APK, em JVM; não foi feita uma reimplementação aproximada em Python.

A versão que exigia dois lados produziu zero pares. A versão com candidatos de um lado, antes da rejeição de contraste vertical, produziu candidatos em dois quadros (6,000 e 6,333 segundos), ambos sem par. São linhas verticais em x=0,600 e x=0,625: esse resultado não fornece evidência de faixas válidas. A verificação geométrica conservadora foi acrescentada para rejeitar esse caso; seu resultado final está em offline/final/detector.log e lane-candidates.csv.

Não há marcações de referência anotadas. Contagens de candidatos **não são taxa de acerto**; nenhum recall, precisão, LDW, FCW ou desempenho no hardware foi validado. O teste real evidencia que o baseline V4.8 continua insuficiente para o ADAS solicitado.

## Verificação e reprodução

Consulte build-validacao.log, resultados-testes e lint-results-debug.txt desta entrega para resultados finais. Os testes JVM exercitam coordenadas, preservação/exclusão de arquivos, hash e rejeição/retorno dos candidatos. Não são testes de lifecycle Android, service, GPS, câmera, FileProvider ou layout em dispositivo.

Ativação da nuvem: source /workspace/cloud-setup/env.sh
Build: cd /workspace/DashCam_ADAS_PRO_V4_8_FAIXAS_EXPERIMENTAL_FONTE; ./gradlew --no-daemon --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
Vídeo: python3 build-tools/offline/run_video.py "CAMINHO/Video Project 1.mp4" "PASTA_RESULTADOS"
A ferramenta offline usa ffmpeg/ffprobe, JDK e Kotlin stdlib 2.0.21 do cache Gradle, e aceita vídeo horizontal 16:9. Compilar primeiro :app:compileDebugKotlin. O CSV registra índice do quadro e tempo nominal da amostragem (índice/3), não uma medição de alerta ou o timestamp do sensor Android.

No Windows, extraia o ZIP e use GERAR APK.bat com JDK 17 e SDK 35; o BAT continua não executado em Windows nesta nuvem.

## Ainda necessário

Validar em Android 7.1 e Android recente: permissão precisa/aproximada/negada, notificações negadas, início/parada do GPS, rádio FM, compartilhamento de vídeo, barras/recortes, rotação e gravação. Nenhum dispositivo ou emulador foi testado aqui.
Para o pedido integral ainda faltam captura/gravação em serviço próprio, modelos licenciados, rastreamento e avaliação de risco LDW/FCW, placas/semáforos/pedestres/ciclistas, UVC/protocolo proprietário, Drive/OAuth e validação comercial. O vídeo recebido permite iniciar validação offline; faltam anotações de referência e os demais materiais aprovados. A exigência do targetSdk foi resolvida no código; isso não constitui aprovação no Google Play nem prontidão comercial.

O repositório Git de wallpapers permanece separado e inalterado. O rascunho de ambiente deve ser publicado na interface para ativar o snapshot; salvar instruções não publica nem valida uma nova máquina.

## Resultados finais desta entrega

- Build :app:assembleDebug: passou.
- 15 testes JVM: passaram; zero falhas, erros ou ignorados.
- :app:lintDebug: passou; zero erros e 34 avisos. Nenhum erro foi desabilitado.
- apksigner: assinatura APK v2 válida. aapt: minSdk 24 e targetSdk 35.
- Vídeo real final: 82 quadros, zero candidatos e zero pares, conforme o CSV e log. Isso evidencia limitação do detector, não comprova que a via esteja sem marcações.
- Runtime Android, GPS/service, notificações, FileProvider, layout e câmera: não testados em dispositivo.

| Detector | Quadros analisados | Quadros com candidato | Pares |
|---|---:|---:|---:|
| Baseline V4.8, exige dois lados | 82 | 0 | 0 |
| Retorno de um lado, antes da rejeição vertical | 82 | 2 | 0 |
| Final com rejeição de contraste vertical | 82 | 0 | 0 |

Essas contagens não medem acurácia. Os CSVs anteriores ficam preservados para comparação e não representam a versão final do APK.
