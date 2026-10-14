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

Resultado final: ambos os perfis compilaram, 84 testes passaram por perfil, lint sem erros e 91 avisos por perfil. Seis bibliotecas ELF64 com alinhamento16KB verificadas; modelo YOLOP real executado pelo teste compartilhado no host. APK3.4 anterior preservado.

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
