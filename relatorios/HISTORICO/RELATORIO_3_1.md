# DashCam ADAS PRO — implementação experimental de LDW/FCW e gravação em serviço

Data: 12/10/2026. Versão 3.1.0-adas-experimental, versionCode 31, targetSdk 35 e minSdk 24. Esta entrega implementa os módulos em código; não representa validação comercial ou testes em Android físico.

## O que foi aplicado

Camera2 e MediaRecorder agora pertencem exclusivamente a DashCamForegroundService, com foregroundServiceType camera, notificação e parada explícita. A Activity recebe os quadros por binder e apenas desenha a LIVE. Ao sair da LIVE ela libera a conexão, não encerra a captura nem a gravação. Os únicos outputs da sessão são ImageReader YUV e a superfície do MediaRecorder; nenhum output depende de uma SurfaceTexture da Activity. A prévia real e a inferência são publicadas separadamente; resultados antigos não são desenhados sobre uma imagem mais recente.

O serviço inicia a partir da Activity visível e com CAMERA concedida; não tenta abrir câmera no boot ou reiniciar silenciosamente após morte do processo. Um wake lock parcial temporizado/renovado durante a captura permite continuar o processamento com a tela apagada; é liberado ao fechar a câmera. Não impede encerramento pelo sistema/OEM, perda de alimentação ou políticas de câmera. Há até três tentativas de recuperação ao reaparecer a mesma câmera. Falhas na combinação de gravação/YUV tentam restaurar a prévia; uma prévia incompatível é suspensa.

A gravação MP4/H.264 usa tamanho anunciado pelo hardware, segmentos configuráveis e orientação. A proteção manual/por evento fecha o segmento atual, move para protegidos e calcula SHA-256. Não há buffer anterior/posterior além do segmento existente. Um marcador .lock impede exclusão circular de um clipe protegido quando a movimentação falha. Eventos de serviços concorrentes são serializados. Datas/metadados são reais; não foram inventadas coordenadas ou velocidade.

RadarForegroundService continua responsável pelo GPS, radares locais e áudio, independentemente da Activity. DrivingContext compartilha apenas observações válidas: velocidade presente, precisão até 30 m, idade até 8 s e sem origem mock. GPS ausente/inadequado suspende os alertas críticos dependentes dele.

## LDW

CurveLaneDetector usa contraste local e ajuste quadrático robusto, com rejeição geométrica, cobertura vertical e suporte dos pixels. AdasEngine é um coordenador efetivo; NoOpAdasEngine foi removido. A avaliação de saída usa os limites observados, a referência de centro e as larguras informadas por câmera. Exige duas marcações com confiança suficiente, calibração habilitada e velocidade válida. Após confirmação temporal, a linha correspondente é vermelha piscante, com voz/som configuráveis e registro. Perda de confiança/quadros encerra a condição crítica; retorno à faixa sai do vermelho automaticamente.

É um detector experimental por contraste, não uma segmentação semântica validada. Pode perder marcações descontínuas/curvas ou confundir reflexos/contrastes. A calibração é uma referência manual simplificada, não uma calibração métrica completa de intrínsecos, perspectiva e distância. No vídeo recebido a cobertura foi insuficiente para validar LDW: retornou apenas candidatos isolados em dois quadros. Não anunciar LDW operacional de segurança com base nisso.

## FCW

VehicleDetector executa de verdade o SSD MobileNet v2 COCO quantizado, via TensorFlow Lite 2.17.0 em CPU com duas threads. Modelo oficial Google Coral, commit fixado, licença Apache-2.0 e checksum SHA-256 verificado na inicialização. O APK inclui os pesos/labels/licença e bibliotecas para ARM32/ARM64 (também x86/x86_64). Não depende de download ou Internet no aparelho.

O RiskEngine associa o alvo por classe/IoU, seleciona candidatos relevantes ao corredor, acompanha expansão aparente e estima TTC por regressão temporal de log da escala. Proximidade sozinha não dispara alarme. Exige confiança, histórico, consistência de expansão, calibração, GPS e confirmação temporal. Alvo correspondente fica vermelho piscante quando a estimativa entra em risco; redução de aproximação/perda do alvo encerra o estado crítico. Há antirrepetição de avisos. É estimativa monocular experimental, sem distância métrica nem certificação.

São processados carro, ônibus e caminhão. Pedestres, ciclistas, sinais e semáforos não têm alertas integrados nesta etapa. O reconhecimento geral do modelo não equivale à integração desses módulos.

## Ativação no aparelho

1. Instalar o novo APK Debug e conceder câmera/localização. Notificações tornam a sessão/ação de parada acessíveis.
2. Escolher câmera e orientação. Calibrar pelo botão ADAS com o veículo parado/alinhado e informar medidas reais. A calibração fica salva por câmera. Mudança de orientação desabilita a calibração anterior.
3. Em Configurações de áudio, os limites de LDW/TTC, confirmação, antirrepetição e piscar são ajustáveis e marcados como provisórios. Os valores padrão de desenvolvimento não foram aprovados como definitivos.
4. Iniciar REC antes de sair da LIVE. A sessão de câmera permanece no serviço; a ação Encerrar câmera na notificação fecha captura/gravação.

Os alarmes vêm desabilitados até confirmar calibração. Não force alertas com GPS falso nem marque os botões de teste históricos como detecção verdadeira.

## Testes nesta nuvem

Ver resultados definitivos em verification-summary.json, build-validacao.log, resultados-testes e lint-results-debug.*. Testes JVM exercitam confirmação/recuperação, perda de GPS/confiança/alvo, troca de objeto, ausência de FCW por proximidade estável, lacuna de frames, limites configuráveis, curvas sintéticas, conversão YUV com strides/crop, coordenadas, hash e retenção/proteção. São testes de software, não condução real.

O vídeo real Video Project 1.mp4 foi decodificado e processado por inferência SSD real; a mesma classe Kotlin AdasEngine utilizada pelo serviço foi executada em JVM para faixas/risco. Resultados: 82 quadros a 3 fps, 3 candidatos a veículos em 3 quadros e candidatos a marcação em dois quadros. GPS/calibração não existem no vídeo: os estados críticos permaneceram UNAVAILABLE. Nenhuma velocidade foi presumida.

Inferência por CPU no host: mediana 23.2 ms, percentil 95 123.8 ms. Isso não mede desempenho em Android/K2001N. Sem anotações de referência, não há precisão/recall/acurácia nem validação de LDW/FCW em trânsito. Os candidatos incluem possíveis falsos positivos.

ADB foi consultado: zero dispositivos conectados. Testes físicos de GPS, câmera, interface, gravação sob troca de apps/tela apagada, áudio FM e execução de JNI no Android NÃO foram executados. O helper de teste recusou corretamente iniciar sem dispositivo (exit 1); isso não é teste físico aprovado.

## Teste físico preparado, ainda não executado

build-tools/test_android_device.py requer Android físico autorizado por ADB, Python e ffprobe. --install e --grant-test-permissions são ações explícitas opcionais; não desinstala nem apaga dados de uma instalação existente. O helper exige ausência de gravação prévia, inicia um clipe, pressiona HOME, verifica avanço dos quadros/capturas no serviço, encerra, copia e verifica MP4/H.264 de duração >=3 s e retorna à LIVE. --require-gps exige observação real recente. A interface visual e acurácia ADAS continuam exigindo revisão manual/pista mesmo se o helper passar.

O APK Debug contém um controle de teste por arquivo privado acessível apenas ao UID do app/ADB run-as autorizado. Fica condicionado a BuildConfig.DEBUG e não está ativo em Release. Nenhuma interface de serviço foi exportada para terceiros.

Exemplo local: python3 build-tools/test_android_device.py --serial SERIAL_REAL --apk DashCam_ADAS_PRO_DEBUG.apk --install --grant-test-permissions --output resultados-aparelho

## Reprodução e pendências

Na nuvem: source /workspace/cloud-setup/env.sh; cd /workspace/DashCam_ADAS_PRO_V4_8_FAIXAS_EXPERIMENTAL_FONTE; ./gradlew --no-daemon --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
Vídeo: PYTHONPATH=/workspace/python-model-runtime python3 build-tools/offline/run_service_video.py "VIDEO.mp4" "RESULTADOS"
Windows: GERAR APK.bat, Wrapper completo, JDK 17 e SDK 35. O BAT continua sem execução em Windows neste ambiente.

Ainda pendentes: validação hardware/pista e anotações de referência; aperfeiçoamento da percepção e calibração para o desempenho exigido; backend UVC/proprietário quando Camera2 não oferecer quadros; pedestrian/cyclist/sign/light alerts, referências visuais aprovadas, backup Drive/OAuth, áudio premium e validação comercial. Esses itens não são considerados concluídos por existir menu/interface.

O repositório Git original de wallpapers permanece inalterado. O fonte DashCam é o projeto separado recebido por ZIP. Scripts de ambiente são rascunho; publicação e validação em nova máquina não foram realizadas.
