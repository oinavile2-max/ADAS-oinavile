# DashCam ADAS PRO — 3.3: rede neural ativa e serviço USB

Data: 12/10/2026. versionCode 33; minSdk 24, targetSdk 35. Android 7.1 informado para a K2000N; CPU, VID/PID e combinação driver/encoder ainda não foram medidos no hardware.

## Rede neural de faixas

Foi integrado o modelo oficial YOLOP 320×320, com pesos pré-treinados e licença MIT. O grafo de área dirigível e faixas foi extraído do checkpoint ONNX oficial, removendo a cabeça de detecção de objetos não utilizada; a comparação com o modelo completo deu diferença máxima zero no ensaio registrado. O SSD MobileNet existente continua responsável por carros/ônibus/caminhões. São dois modelos efetivamente executados em CPU, sem rede, chatbot ou treinamento online.

RoadNeuralSession compartilha o processamento entre Android, testes JVM e reprodução de vídeo: RGB, letterbox com preservação de proporção, padding RGB114, normalização ImageNet, tensor float32 NCHW, inferência ONNX Runtime 1.20.0 e remapeamento das duas saídas ao quadro. São verificadas integridade, nomes, tipos, dimensões e valores finitos. Uma ultrapassagem de um ULP acima de 1 apareceu em dois quadros reais no backend de referência; a validação agora permite apenas a tolerância numérica de 1e-6, com clipping para [0,1]. Valores realmente inválidos continuam rejeitados.

As marcações neurais alimentam o ajuste geométrico, sem simular uma borda ausente. Âncoras de cada lado são selecionadas separadamente, corrigindo a perda de faixa tracejada quando os últimos pontos disponíveis pertenciam somente à outra borda. Limites geométricos usam a região observada; a calibração só estima centro automaticamente quando a referência inferior possui suporte. A área dirigível dá suporte às marcações e restringe a associação de veículos fora da estrada. Observações uniformes não sustentam faixas. Os limiares são critérios de desenvolvimento, não probabilidades de precisão certificadas.

A LIVE exibe o bitmap analisado e seus resultados em conjunto. Resultados com mais de 1200 ms não são apresentados; os alertas usam limite mais restrito de 500 ms, medido após inferência e ajuste das curvas. A mensagem IA ativa depende de execução, e IA lenta informa suspensão dos alertas. Perda de confiança/câmera/quadros limpa os riscos. GPS e calibração são obrigatórios para alertas críticos.

## USB e gravação em serviço

NativeUsbActivity agora autoriza e seleciona a fonte; não possui câmera ou encoder. O serviço abre o USB autorizado, carrega o binário ARM32 original e recebe o render do driver em ImageReader RGBA independente da Activity. Camera2 permanece opcional para esta fonte. São tratados strides/crop, geração de sessão, timeout de inicialização/primeiro quadro e desconexão. O driver não é ativado silenciosamente em aparelhos/versões incompatíveis.

RgbaVideoSink cria um contexto EGL recordable do app, envia os quadros RGBA reais à superfície MediaRecorder e usa timestamps monotônicos. Evita depender de uma hipótese sobre o modo de encoder do SDK proprietário. Não encerra o display EGL compartilhado enquanto a prévia do driver o utiliza. O contador de gravação USB avança somente após envio de quadro ao encoder; o teste físico também exige MP4/H.264 válido para demonstrar codificação. Segmentos, proteção e hashes anteriores são preservados.

Captura, duas redes e gravação pertencem ao serviço foreground, sem superfície da Activity. Perda de quadros USB fecha o clipe e suspende ADAS. Há wake lock temporizado; não garante sobrevivência a políticas OEM, morte do processo ou falta de energia. Não há teste físico de compatibilidade ImageReader/EGL/MediaRecorder com esta biblioteca.

## Validação

73 testes JVM, sem falhas/erros/skips. Incluem execução do modelo real em CPU pelo mesmo RoadNeuralSession, rejeição de pesos alterados, letterbox/RGB/NCHW, padding, valores não finitos/fora da faixa, tolerância numérica, quadro uniforme, suporte de estrada, marcações tracejadas com lacunas e conversão RGBA com strides/crop. Os testes geométricos sintéticos não medem precisão em estrada.

A reprodução real do vídeo usa SSD TensorFlow Lite e RoadNeuralSession/AdasEngine compartilhados com o app. Resultados: 82 quadros e execuções neurais; 66 quadros com candidatos a curvas; 7 pares confirmados pelo critério interno; três candidatos a veículos. Sem GPS/calibração, nenhum risco crítico. A versão anterior registrou zero pares. Não há ground truth, portanto não é possível calcular precisão/recall ou afirmar prontidão comercial. Estatísticas de tempo medem o host, não a K2000N.

Build/lint, assinaturas, empacotamento de ABI/JNI e checksums são conferidos na entrega. O runtime para computador está somente no escopo de testes; libs JNI do APK são comprimidas para manter o APK genérico abaixo do limite de arquivo do GitHub. Logs e resultados atuais estão em relatorios/3_3; os relatórios anteriores são históricos.

A inspeção ELF identificou alinhamento de 4096 bytes na ponte libonnxruntime4j_jni.so ARM64 do ONNX Runtime 1.20.0. Assim, o APK genérico não suporta dispositivos com páginas de memória de 16 KB, embora passe na verificação de alinhamento ZIP. O perfil K2000N contém somente ARM32 e se destina a Android 7/8. Detalhes por biblioteca e APK estão em relatorios/3_3/apks-verificacao.json.

## Pendências externas e limites

Nenhum Android conectado via ADB. GPS, áudio/FM, renderização, consumo de RAM/temperatura, taxa de inferência em ARM32, imagem do driver e gravação/tela apagada na K2000N não foram executados. O helper de hardware agora exige avanço das duas redes em segundo plano e examina o vídeo real; --native-usb exige fonte USB previamente autorizada e --require-realtime verifica o orçamento de 500 ms.

YOLOP preserva a licença MIT oficial e a proveniência do grafo; seus dados de treinamento não tiveram auditoria independente de todos os termos comerciais. SSD preserva Apache-2.0. A autorização comercial da biblioteca do APK fornecido segue não verificada. Pedestres/ciclistas/placas/semáforos com alertas completos não foram integrados nesta revisão. FCW continua uma estimativa temporal de expansão de caixa, não uma distância métrica certificada.
