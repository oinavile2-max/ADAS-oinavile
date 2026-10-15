# DashCam ADAS PRO — código 3.5 comercial, Android 7.1–atual

12/10/2026. Conceito aprovado pelo usuário aplicado em widgets Android nativos. Sem novo APK; versionCode 35 reservado no código. Downloads de APK permanecem das versões anteriores.

## Interface

CommercialPage compartilha marca, novo ícone, superfícies em vidro, cabeçalhos, botões e cards. Configurações têm quatro abas, sidebar em largura >=900 dp e faixa horizontal rolável em telas menores. Pares de cards ficam lado a lado somente com >=580 dp disponíveis; nos demais casos empilham. Área do conteúdo rola independentemente do cabeçalho e do botão Salvar. Componentes respeitam densidade, áreas de toque e insets. Tudo é nativo, sem WebView.

Os controles reais preservam os mesmos nomes de preferências: adas_tuning, audio e camera. Cada aba salva seu grupo; rascunhos sobrevivem à troca de aba e à recriação da Activity. Limites técnicos ADAS ficam recolhidos, com validação conjunta e bloqueio de gravação em movimento. Exemplo das linhas é rotulado como visual e não se apresenta como inferência.

Modo noturno usa brilho da janela sem alterar configurações globais. Preferência de animação altera apenas transições/arco decorativo do HUD; não muda risco, confirmação, avisos, captura ou gravação. Mídia lista somente clipes finalizados sem arquivo de lock; eventos usam registros reais e mantêm origem de teste. USB continua autorização no serviço existente.

## Ícone

Arte original gerada para o aplicativo: lente ciano, estrada e detalhe vermelho. PNG transparente preservado, com qualificador xxxhdpi para escala pelo Android. Android 7.1 usa layer-list clássico; Android 8+ usa foreground/background adaptativos com margem adicional, evitando corte do indicador pela máscara circular; Android 13+ recebe recurso monocromático. Prévias de launcher são renderizações em navegador, não capturas Android.

## Compatibilidade

minSdk 25, compileSdk 37.2, targetSdk 37, sem maxSdk. Plataforma 37.2 verificada no canal estável oficial. AGP 9.4.1 e Gradle 9.6.0, checksum oficial da distribuição conferido. Navegação fullscreen migrou de onBackPressed para ComponentActivity/OnBackPressedDispatcher (AndroidX Activity 1.13.0, minSdk 23).

ONNX Runtime 1.31.0 tem minSdk 24 e mantém as quatro ABIs. LiteRT 1.4.2 tem minSdk 21, o mesmo pacote Interpreter e as quatro ABIs. A ponte ONNX antiga ARM64 e o TensorFlow Lite antigo x86_64 não satisfaziam o alinhamento de 16 KB. A tarefa verifyNativePageAlignment confere segmentos LOAD ELF de todas as dependências 64 bits mescladas antes dos filtros de empacotamento, e é obrigatória para futuros builds. As seis bibliotecas passam. O driver específico original ARM32 só é carregado em API 24–27; o app inicia em API 25. Isso não oferece UVC universal para Android atual.

A atualização ONNX acrescenta inicializador de telemetria e permissão de internet no AAR. Ambos são removidos do manifesto do app, e RoadNeuralSession desativa telemetria. O manifesto mesclado foi inspecionado. Pesos dos modelos e limiares de risco permanecem preservados.

## Evidências

Comando por perfil: compileDebugKotlin, testDebugUnitTest, lintDebug e verifyNativePageAlignment. Nenhuma tarefa de assemble/package foi solicitada nesta etapa. Relatórios atuais: relatorios/comercial-android-atual. Testes incluem inferência real YOLOP no runtime atualizado e três novos casos de validação de preferências. Não há Android conectado; nenhum teste físico de launcher/interface/GPS/USB/gravação ou latência ARM32 foi realizado.

Os ensaios de vídeo da 3.3 e do SSD no host continuam históricos. O pacote Python LiteRT não possui versão 1.4.2 publicada; não foi alegado um teste de inferência Android SSD com esse runtime. A API do Interpreter compilou, os modelos/hash foram verificados, e a execução Android ainda depende do aparelho. Relatório histórico do APK 3.4: relatorios/HISTORICO/RELATORIO_3_4.md.

Resultado final: ambos os perfis compilaram, 84 testes passaram por perfil, lint sem erros e 91 avisos por perfil. Seis bibliotecas ELF64 com alinhamento de 16 KB verificadas; modelo YOLOP real executado pelo teste compartilhado no host. APK3.4 anterior preservado.

## Temas neon — código 3.6, 13/10/2026

NeonThemeSettings centraliza seis presets, deslocamento de matiz ±30°, transparências independentes 0–80% e opção de cor para popups comuns. NeonTheme mantém uma paleta imutável em cache e listener em preferências de applicationContext; não lê disco a cada frame. Somente preenchimentos recebem alfa: textos/ícones/bordas permanecem legíveis. Banners de eventos têm cor vermelha fixa; preenchimento crítico tem alfa mínimo 191/255 (75% nominal).

Aplicado aos botões internos, seis botões LIVE, botões com ícone, bordas da câmera, marca no cabeçalho, sliders/switches, HUD, diálogos, menus suspensos e avisos. O registro de estilo não substitui os ícones dos botões da câmera/gravação nem os controles Switch. Salvar atualiza a tela e a LIVE ao retornar; prévia e restauração operam em rascunhos antes de salvar. Alterações de IA, fontes/encoder e Android SDK não fazem parte deste ajuste visual.

89 testes passaram em cada perfil, sem falhas/skips; compilação Kotlin, lint e alinhamento de bibliotecas conferidos sem gerar APK. Cinco testes adicionais cobrem padrão histórico, adesão explícita de popup ao tema, independência de alfa, proteção vermelha/opacidade dos eventos e normalização de limites. Prévias do navegador verificadas; Android físico ainda não testado. Evidências: relatorios/temas-neon.


## Fluidez da câmera integrada — código 3.7, 14/10/2026

Relato do usuário: celular/tablet, câmera integrada, APK 3.3, travamento na LIVE mesmo sem gravar. Identificados no código o pacing de 200 ms (máximo cinco quadros/s) e a supressão de onPreview quando havia um resultado neural recente. A prévia não depende mais da inferência, nem é substituída por bitmaps analisados mais antigos.

CameraPreviewView passou a conter TextureView opcional somente no painel principal. O serviço configura YUV de análise e Surface de prévia direta, acrescentando a superfície do MediaRecorder quando solicitado. Seleção de prévia até 1280×720 com proporção próxima à análise. Rejeição da saída direta ativa uma tentativa sem essa superfície; o limite alternativo vem de camera/preview_fps, padrão 15, opções 10/15/24/30. SurfaceTexture pertence à Activity; Surface wrapper pertence ao serviço. A Activity desconecta a superfície antes de desvincular, e onUnbind repete a proteção. A configuração de sessão invalida callbacks de tentativas anteriores.

A prévia direta não executa YUV→ARGB para cada quadro de vídeo. Instantâneos auxiliares continuam limitados a 4/s, e a análise tem cadência independente. Buffers IntArray de conversão e leitura neural são reutilizados; bitmaps entregues à UI não são alterados nem reciclados. Mailbox substitui a atualização pendente pelo quadro mais novo. Thread de câmera tem prioridade maior que a análise; modelos usam 1–2 threads, com uma em aparelhos até quatro núcleos ou até 2,5 GiB de RAM. Após análises de até 500 ms, intervalo = duração+150 ms, limitado a 250–700 ms; acima de 500 ms, intervalo = duas vezes duração, limitado a 1–20 s. Isso reduz duty cycle sob sobrecarga, sem alterar limiares de risco ou autorizar alertas atrasados.

Sobreposições permanecem associadas ao bitmap/timestamp analisado no painel auxiliar. O vídeo direto não recebe caixas de outro instante. O caminho alternativo só anota o principal se timestamps coincidirem. Essa escolha evita recuar o vídeo e também evita simular acompanhamento de objetos em quadros que não foram analisados; não equivale a rastreamento visual a 30 fps.

capture-diagnostics.json acrescenta camera_received_frames, max_source_gap_ms, last_conversion_ms, preview_rendered_frames, preview_render_fps, preview_max_draw_gap_ms, preview_last_draw_gap_ms, preview_direct_gpu, preview_fps_limit, neural_interval_ms e neural_threads_per_model. FPS conta desenhos distintos do bitmap ou atualizações da TextureView na UI, não callbacks neurais nem taxa de vídeo codificado. test_android_device.py aceita --live-only e --min-preview-fps e mede incremento do contador ao longo de oito amostras sem iniciar gravação; seu modo anterior de teste de gravação permanece disponível.

Testes novos verificam pacing em fonte de 30 fps, independência de inferência lenta, troca de limite/reinício, descarte de backlog, monotonicidade do vídeo, associação exata do overlay, expiração da taxa após parada e reutilização de buffers YUV/RGBA sem pixels antigos. O teste compartilhado executa o YOLOP real com uma e duas threads. Compilação e lint não substituem validação da superfície Camera2, da inferência LiteRT em Android, da temperatura ou da fluidez no aparelho. Evidências finais em relatorios/fluidez-camera. Nenhum APK novo foi solicitado/gerado nesta etapa; minSdk 25 e alvo moderno preservados.

Resultado final 3.7: perfis genérico e NWD compilaram; 97 testes por perfil, zero falhas/erros/skips; lint com 95 avisos e zero erros por perfil. Modelos e seis bibliotecas ELF64 verificados. Nenhum aparelho ADB conectado e nenhum APK gerado.


## Build 3.7 autorizado — 14/10/2026

Após a entrega somente em código, o usuário solicitou BUILD. Gerados assembleDebug genérico (ARM32, ARM64, x86 e x86_64) e NWD K2001N (somente ARM32, com driver específico), ambos com código de produto da revisão 24c2731, versionCode 37, minSdk 25 e targetSdk 37. Comando por perfil: ./gradlew --no-daemon --max-workers=2 [ -Pk2000Native=true ] :app:assembleDebug :app:lintDebug. Os gates existentes executaram verificação dos pesos, teste neural real e alinhamento de bibliotecas antes de empacotar. 97 testes por perfil sem falhas/erros/skips; lint sem erros e 95 avisos por perfil.

APKs debug assinados por V1/V2; verificadores em API25 e API37 passaram. API23 foi forçada somente para exercitar o esquema V1, sem implicar suporte do aplicativo abaixo de API25. Certificado SHA-256 6b7459556adedfac6a298c4305a55c3f8709770c38bdf9dc21ba7d38c8162dd8, igual ao das versões 3.2–3.4. O conteúdo real de cada APK foi aberto e verificado: modelos com hashes esperados, ABI correta, ausência de INTERNET e ZIP íntegro/alinhado. Seis bibliotecas ELF64 efetivamente empacotadas no genérico têm segmentos LOAD compatíveis com páginas de 16 KB. APK ARM32 não contém ELF64.

Evidências e SHA-256 dos pacotes em relatorios/build-3_7; downloads em APKS e README. Fonte ZIP atualizada com documentação e relatórios. Os APKs são debug experimentais; build e integridade não validam desempenho/instalação física, GPS, câmera, precisão ADAS ou gravação em transições. Nenhum aparelho ADB conectado nesta nuvem; regressão de fluidez relatada no 3.3 ainda precisa ser reproduzida e medida no aparelho usando 3.7.


## Correções 3.8 — câmera, gravação e menus — 14/10/2026

Após teste físico do 3.7, o usuário informou divergência de orientação, ausência de botão principal REC, travamento do painel auxiliar e bugs de menu sem especificar tela/ação. Código versionCode38; SDK, dependências, pesos e certificado permanecem preservados.

TextureView desenha o transform de BufferQueue, enquanto ImageReader fornece pixels não orientados. O 3.7 aplicava a matriz de apresentação supondo que a imagem da TextureView estivesse nesse mesmo espaço. TextureBufferOrientation extrai as oito combinações H/V/R90 da matriz da SurfaceTexture (removendo o flip GL e normalizando as escalas de crop). A matriz da View usa os cantos como já apresentados pelo produtor, com o destino definido por CameraTransform. Essa compensação trata a rotação uma só vez, também para câmera frontal e espelhamento manual. A transformação é atualizada somente quando a orientação do produtor muda. Após resize, o buffer volta ao tamanho de captura; SCALER_ROTATE_AND_CROP_NONE é solicitado em API31+ apenas quando anunciado. Mudança de orientação incrementa um epoch, invalidando prévias, resultados e avisos da orientação anterior.

REC/PARAR foi acrescentado como botão nativo 112×48dp sobre a câmera principal, com borda vermelha, habilitação pelo estado conectado e alteração pelo estado real de gravação. GIRAR ocupa outro canto, aplicando +90° a ambos os painéis; o menu restaura o automático. Esses controles pertencem ao painel da câmera e permanecem ao entrar em FULL ou ocultar o rodapé. O ícone anterior do cabeçalho permanece como atalho. O toque longo ainda protege o clipe atual conforme a função existente, sem alegar buffer pré-evento.

O painel auxiliar não espera a rede no modo LIVE, padrão. As cópias seguem o limite configurado de 10/15/24/30fps independentemente da inferência. AnalysisResolution prefere 320×180, mantendo a proporção de captura quando há saída compatível; evita dimensões menores quando outra opção útil existe, e mantém fallback para dispositivos restritos. A GPU principal mantém sua superfície/resolução separada. ANÁLISE é uma seleção explícita para o bitmap realmente analisado, com suas próprias sobreposições; resultados antigos nunca substituem o vídeo no modo LIVE. Essa distinção preserva associação quadro/caixa, sem simular rastreamento de objetos em quadros novos. Limite de 500ms para alarmes permanece; a latência ~949ms da captura do usuário ainda não foi medida novamente em hardware.

Cortes e navegação: status da LIVE passou a ter altura/fontes coerentes com o layout de referência e exibição da mensagem completa ao tocar; ADAS abre o painel de assistência, CONFIG legado abre Settings. CommercialPage ajusta título/subtítulo/cabeçalho em altura curta, mantém abas e conteúdo rolável, e títulos de cards recebem largura com peso. SafeInsets acomoda IME nas páginas internas. NeonDialogs ajusta largura ao display (máximo720dp, margem32dp), acrescenta padding12dp à superfície e usa aceleração normal; bordas neon seguem as preferências anteriores. Timeout recolhe somente os controles do rodapé, mantendo o vídeo auxiliar. Não há comprovação de que todo bug de menu não descrito foi resolvido.

Testes adicionais: seis testes da transformação do produtor, com oito flags, quatro rotações e FIT/FILL/espelhamento (128 combinações); três testes de continuidade LIVE e associação de sobreposição; três de seleção de resolução. Modelos reais seguem verificados e YOLOP executa em uma/duas threads no host. Diagnóstico acrescenta contadores/FPS do auxiliar, modo de painel, tamanho de análise e rotate-and-crop. Helper físico aceita --min-auxiliary-fps para medir o modo LIVE sem gravar. Evidências finais em relatorios/build-3_8. Sem Android ADB conectado nesta nuvem; validação visual, instalação, câmera e gravação permanecem físicas.

Resultado final 3.8: dois perfis assembleDebug concluídos, 109 testes cada com zero falhas/erros/skips; lint com 98 avisos e zero erros em ambos. Assinaturas V1/V2 e certificado anterior verificados; modelos reais embarcados e seis bibliotecas ELF64 no APK genérico conferidos. Testes físicos continuam pendentes.


## DVR USB da NWD — build 3.9, 14/10/2026

A foto do diagnóstico identificou Allwinner T3/NWD, Android 7.1.1/API25, processo ARM32, RAM 2048 MB e USB 3322:6655 com interface 8/6. O filtro anterior oferecia somente interfaces 14/255; ignorava o DVR referenciado no recurso ufd_device_filter do APK fornecido. O novo filtro confere VID/PID/classe/subclasse nos seis registros exatos. 058F:6387, também presente na central, continua excluído como armazenamento não referenciado. A seleção exige ação do usuário e autorização USB separada da permissão CAMERA.

O caminho UFD estava incompleto: um descritor USB bruto não substitui o endpoint JSON/transporte 0 do SDK, e os callbacks umsd* não faziam I/O. DvrUsbStorage agora abre somente o DVR selecionado, descobre seu endpoint de comunicação FAT32 e associa o protocolo original; DvrByteChannel implementa seek/read/write, leitura em chunks, pacing, cancelamento e limites de buffers/tamanho. Comandos não avançam o cursor nem aumentam o arquivo. Nada é criado, excluído, formatado ou redimensionado. Erros são encaminhados ao serviço na thread de câmera, sem escapar dos callbacks JNI. A inicialização do SDK não é confundida com chegada de imagem real.

Transporte por me.jahnen.libaums:core:0.10.0 (Apache-2.0) com DvrUsbCommunication, API USB Host própria e fechamento idempotente. Os JNI antigos usb-lib/errno-lib da dependência são excluídos; não são necessários nesse transporte. O primeiro ensaio de empacotamento encontrou desalinhamento de liberrno-lib e foi rejeitado pelo check obrigatório; a correção removeu esse JNI não utilizado, preservando o check, em vez de alterar sua exigência. A conexão USB é liberada mesmo após falha em descobrir partições. Bibliotecas neurais e driver libmycamera.so permanecem íntegros. Candidatos UVC classe 14 usam nativeCreateUVC; referências UFD usam nativeCreateUFD.

O diagnóstico mostra autorização por USB, identifica referências DVR e explica que uma lista Camera2 inacessível não comprova ausência física de câmera. Inclui mensagem da SecurityException quando disponível. A nova rota não contorna a segurança do sistema nem exige root/permissão de armazenamento global. Reconhecimento por descritores e disponibilidade do arquivo de protocolo não comprovam renderização do driver.

Dois APKs debug 3.9, versionCode 39, minSdk 25 / targetSdk 37: 118 testes por perfil (9 novos; zero falhas/erros/skips), lint com 101 avisos e zero erros. Tests verificam o DVR observado, exclusão do outro USB, os descritores exatos, cursor/chunks/pacing, escrita restrita, limites, erro de I/O, cancelamento e aliases de protocolo. Inferência YOLOP real com 1/2 threads no host mantida. Assinaturas V1/V2/API25/API37 e certificado anterior conferidos; modelos reais/hash e seis bibliotecas ELF64/alinhamento de 16 KB no pacote genérico verificados. Metadados, SHA256 e relatórios em relatorios/build-3_9.

Nenhum Android por ADB está conectado. Não houve ensaio físico de câmera USB, gravação, latência ARM32 ou precisão ADAS nessa central. Teste indicado: APK 3.9 NWD, fechar outro DVR, selecionar CÂMERA/USB NATIVO 3322:6655, autorizar o Android e confirmar primeira imagem. Se falhar, a mensagem diferencia transporte/endpoint; transmitir a foto dessa mensagem/diagnóstico é necessário para a próxima investigação física. Camera2 pode continuar inacessível no firmware.


## Código 4.0 — descoberta DVR e pouca luz, 15/10/2026

O relatório UCarDVR recebido revelou a ausência do caminho FILE da câmera montada como armazenamento. A implementação adiciona busca assíncrona em USB/volumes, quatro prioridades de transporte, reconhecimento dos cinco marcadores, suporte a mounts legados de fornecedor, hotplug e relatório completo copiável. A captura FILE usa o mesmo serviço/encoder/redes, com JSON e transporte −1 no SDK; UFD preserva callbacks/transporte 0. A lista deixa de depender da Camera2. Autorização USB tem prazo de 20 s e validação após retorno; a Activity mantém o receiver durante o diálogo de permissão. Scanner e serviço revalidam limites de volume e disponibilidade do endpoint, sem root/hidden APIs.

As alterações de pouca luz/gravação iniciadas antes do relatório foram concluídas: perfis Auto/Dia/Noite com histerese e parâmetros Camera2 suportados, fallback de controles rejeitados, brilho automático opcional, início automático opcional após imagem real e ações na notificação. Exposição DVR permanece controlada pela câmera. Modelos, limiares críticos e identidade neon preservados. Nenhum APK novo empacotado; a revisão está entregue em código-fonte. Evidências e limitações: relatorios/camera-discovery-4_0/ANALISE_UCARDVR.md e verificacao.json.

Resultado final 4.0: 134 testes aprovados por perfil, zero falhas/erros/skips, lint sem erros e 117 avisos em ambos. Hashes dos modelos/driver e alinhamento de seis bibliotecas ELF64 verificados; compilação dos perfis genérico e NWD concluída. Nenhum APK novo produzido e nenhum Android físico conectado.

## Empacotamento 4.0 — 15/10/2026

Continuação após a entrega do código: APKs debug genérico e NWD K2001N ARM32 gerados a partir do código de produto d2c887ca9ec710493b6ecca29425a8715e718fc7. Incluem a descoberta USB/FILE, os ajustes de pouca luz Camera2, a gravação automática opcional e as ações de gravação na notificação. Não houve nova mudança da lógica de produto neste empacotamento. Os APKs históricos permanecem preservados.

134 testes por perfil, incluindo inferência YOLOP real com uma e duas threads, sem falhas/erros/skips; lint sem erros e 117 avisos. APKs conferidos quanto a assinaturas V1/V2, certificado anterior, API25/API37, conteúdo dos modelos, driver NWD, permissões, ABI, integridade ZIP e alinhamento de 16 KB das seis bibliotecas ELF64 no genérico. Valores e evidências em relatorios/build-4_0. Permissões de volumes mantêm maxSdk27; não há permissão INTERNET. Nenhum teste de instalação ou captura física foi realizado. Comparação com o app com.pdt.cameraadas ainda bloqueada pelo proxy (CONNECT 403).
