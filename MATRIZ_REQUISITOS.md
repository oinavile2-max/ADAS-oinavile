# Matriz de requisitos — atualização targetSdk 35

Nenhum item desta matriz afirma validação de ADAS em câmera, vídeo ou pista. O documento mestre integral é preservado em REQUISITOS_MESTRE.md. Os nove testes executados validam apenas componentes de geometria e armazenamento.

| ID | Seção / requisito | Estado |
|---|---|---|
| S1 | OBJETIVO DESTE DOCUMENTO | Pendente |
| R001 | S1: Inspecionar integralmente o projeto Android fornecido. | Pendente |
| R002 | S1: Corrigir problemas existentes, preservando funcionalidades aprovadas. | Pendente |
| R003 | S1: Implementar os módulos ADAS reais, incluindo LDW e FCW. | Pendente |
| R004 | S1: Integrar todos os módulos à LIVE, ao sistema de alertas e à execução em segundo plano. | Pendente |
| R005 | S1: Aplicar as regras visuais e funcionais descritas neste documento. | Pendente |
| R006 | S1: Executar os testes técnicos possíveis no ambiente de desenvolvimento. | Pendente |
| R007 | S1: **GERAR A BUILD DEBUG DO APLICATIVO.** | Pendente |
| R008 | S1: Entregar o APK Debug, o código-fonte integral e o ZIP com `GERAR APK.bat`. | Parcial |
| R009 | S1: Informar de maneira verificável quais testes foram concluídos e quais ainda exigem hardware ou pista real. | Pendente |
| S2 | HARDWARE E COMPATIBILIDADE COMERCIAL | Pendente |
| R010 | S2: Centrais multimídia Android. | Pendente |
| R011 | S2: K2001N/NWD. | Pendente |
| R012 | S2: Tablets Android. | Pendente |
| R013 | S2: Smartphones Android. | Pendente |
| R014 | S2: Câmeras integradas. | Pendente |
| R015 | S2: Câmeras USB-A/USB-C compatíveis. | Pendente |
| R016 | S2: Câmeras UVC e dispositivos proprietários por adaptadores específicos. | Pendente |
| R017 | S2: Android 7.1. | Pendente |
| R018 | S2: API 25. | Pendente |
| R019 | S2: Aproximadamente 2 GB RAM. | Pendente |
| R020 | S2: Tela de referência 1024 × 600. | Pendente |
| R021 | S2: Câmera USB ADAS originalmente utilizada no UCarDVR. | Pendente |
| R022 | S2: Aplicativo original: `com.ankai.cardvr`. | Pendente |
| R023 | S2: Biblioteca proprietária identificada: `libmycamera.so`. | Pendente |
| R024 | S2: Arquitetura original relevante: `armeabi-v7a`. | Pendente |
| R025 | S2: Preservar compatibilidade a partir de Android 7.0/API 24, conforme configuração conhecida do projeto, quando tecnicamente possível. | Pendente |
| R026 | S2: Suportar arquiteturas ARM de 32 e 64 bits conforme os componentes nativos utilizados. | Pendente |
| R027 | S2: Manter compatibilidade com diferentes resoluções e densidades. | Pendente |
| R028 | S2: Detectar recursos reais antes de habilitar funções. | Pendente |
| R029 | S2: Empregar processamento adaptativo conforme CPU, GPU, RAM e câmera. | Pendente |
| R030 | S2: Não obrigar todos os aparelhos a utilizar configurações da K2001N. | Pendente |
| S3 | INVENTÁRIO DO QUE JÁ FOI FEITO | Parcial |
| S4 | EVOLUÇÃO DAS VERSÕES | Parcial |
| R031 | S4: LIVE como página principal. | Pendente |
| R032 | S4: Interface escura Premium Luxury. | Pendente |
| R033 | S4: HUD superior. | Pendente |
| R034 | S4: Região principal da câmera. | Pendente |
| R035 | S4: Painel ADAS lateral. | Pendente |
| R036 | S4: Botões inferiores. | Pendente |
| R037 | S4: Integração inicial de alertas visuais. | Pendente |
| R038 | S4: Diagnóstico de fabricante e modelo. | Pendente |
| R039 | S4: Identificação da versão Android. | Pendente |
| R040 | S4: Consulta à memória RAM. | Pendente |
| R041 | S4: Descoberta de câmeras Camera2. | Pendente |
| R042 | S4: Enumeração de dispositivos USB, VID/PID e interfaces. | Pendente |
| R043 | S4: Perfis de desempenho sugeridos. | Pendente |
| R044 | S4: Tratamento separado de erros de inicialização. | Pendente |
| R045 | S4: Tela de diagnóstico quando a LIVE falha. | Pendente |
| R046 | S4: Proteções contra falhas de câmera e serviços. | Pendente |
| R047 | S4: Orientação da câmera. | Pendente |
| R048 | S4: Rotação manual. | Parcial |
| R049 | S4: Ajustes persistentes. | Pendente |
| R050 | S4: Botão de tela cheia. | Parcial |
| R051 | S4: Popups com ícones contextuais. | Pendente |
| R052 | S4: Amostragem de imagem do `TextureView`. | Pendente |
| R053 | S4: Processamento experimental em aproximadamente 160 × 90. | Pendente |
| R054 | S4: Taxa de análise aproximada de 2,8 Hz. | Pendente |
| R055 | S4: Extração de bordas claras. | Pendente |
| R056 | S4: Ajuste linear das possíveis faixas. | Pendente |
| R057 | S4: Rejeição por confiança. | Pendente |
| R058 | S4: Overlay de linhas. | Pendente |
| R059 | S4: Botão FAIXAS BETA. | Pendente |
| S5 | INTERFACE PREMIUM LUXURY — REQUISITOS OFICIAIS | Parcial |
| R060 | S5: Fundo preto piano. | Pendente |
| R061 | S5: Elementos em vidro escuro. | Pendente |
| R062 | S5: Brilhos azuis. | Pendente |
| R063 | S5: Acabamento 3D realista. | Pendente |
| R064 | S5: Bordas luminosas discretas. | Pendente |
| R065 | S5: Ícones visualmente refinados. | Pendente |
| R066 | S5: Alertas translúcidos. | Pendente |
| R067 | S5: Sem caixas grandes do Android para alertas de direção. | Pendente |
| R068 | S5: Interface responsiva. | Pendente |
| R069 | S5: Câmera principal em destaque. | Pendente |
| R070 | S5: Painel secundário quando houver um segundo feed real disponível. | Pendente |
| R071 | S5: HUD com velocidade, GPS e estado verdadeiro da gravação. | Pendente |
| R072 | S5: Alertas ADAS sobrepostos ao vídeo. | Pendente |
| R073 | S5: Controles inferiores. | Pendente |
| R074 | S5: Acesso rápido às configurações. | Pendente |
| R075 | S5: Botão FULL visível. | Pendente |
| R076 | S5: Ajuste de orientação. | Pendente |
| R077 | S5: Silenciamento apenas da voz, conforme preferência persistida. | Pendente |
| R078 | S5: A imagem da câmera ocupa mais espaço. | Pendente |
| R079 | S5: Os alertas continuam visíveis. | Pendente |
| R080 | S5: O estado essencial da gravação permanece acessível. | Pendente |
| R081 | S5: Um toque mostra novamente os controles. | Pendente |
| R082 | S5: O acesso a FULL e às configurações não pode ficar permanentemente perdido. | Pendente |
| R083 | S5: CÂMERA LIVE. | Pendente |
| R084 | S5: PROTEÇÃO ADAS. | Pendente |
| R085 | S5: RADARES GPS. | Pendente |
| R086 | S5: PLACAS DE TRÂNSITO. | Pendente |
| R087 | S5: SEMÁFOROS. | Pendente |
| R088 | S5: EVENTOS. | Pendente |
| R089 | S5: LUXURY AUDIO. | Pendente |
| R090 | S5: CONFIGURAÇÕES. | Pendente |
| S6 | CORREÇÃO DEFINITIVA DA ORIENTAÇÃO DA CÂMERA | Parcial |
| R091 | S6: Orientação física do sensor. | Pendente |
| R092 | S6: Rotação atual do display. | Parcial |
| R093 | S6: Facing frontal ou traseiro. | Pendente |
| R094 | S6: Comportamento de câmeras USB. | Pendente |
| R095 | S6: Firmware que já entrega quadros pré-rotacionados. | Pendente |
| R096 | S6: Espelhamento horizontal quando necessário. | Parcial |
| R097 | S6: Automático. | Pendente |
| R098 | S6: 0°. | Pendente |
| R099 | S6: 90°. | Pendente |
| R100 | S6: 180°. | Pendente |
| R101 | S6: 270°. | Pendente |
| R102 | S6: Espelhar horizontalmente, quando aplicável. | Pendente |
| R103 | S6: Ajustar à tela (FIT). | Parcial |
| R104 | S6: Preencher a tela (FILL). | Parcial |
| R105 | S6: Tela cheia. | Parcial |
| R106 | S6: Caixa do veículo deslocada do objeto real. | Pendente |
| R107 | S6: Linhas de faixa fora da pista. | Pendente |
| R108 | S6: Detector analisando região diferente daquela exibida. | Pendente |
| R109 | S6: Corte ou distorção causados por transformação inadequada. | Pendente |
| R110 | S6: Coordenadas do quadro capturado. | Pendente |
| R111 | S6: Coordenadas processadas pelo ADAS. | Pendente |
| R112 | S6: Coordenadas exibidas na LIVE. | Pendente |
| S7 | MOTOR ADAS REAL — ARQUITETURA OBRIGATÓRIA | Parcial |
| R113 | S7: Camera2. | Pendente |
| R114 | S7: USB UVC compatível. | Pendente |
| R115 | S7: Câmeras proprietárias quando tecnicamente integradas. | Pendente |
| R116 | S7: Fonte de vídeo gravado para testes offline. | Pendente |
| R117 | S7: Quadros. | Pendente |
| R118 | S7: Timestamp. | Pendente |
| R119 | S7: Resolução. | Pendente |
| R120 | S7: Orientação. | Pendente |
| R121 | S7: Identificador da câmera. | Pendente |
| R122 | S7: Estado de conexão. | Pendente |
| R123 | S7: Erros recuperáveis. | Pendente |
| R124 | S7: Marcação de faixa. | Pendente |
| R125 | S7: Veículos. | Pendente |
| R126 | S7: Pedestres. | Pendente |
| R127 | S7: Ciclistas. | Pendente |
| R128 | S7: Placas. | Pendente |
| R129 | S7: Semáforos. | Pendente |
| R130 | S7: Tenham licenciamento compatível. | Pendente |
| R131 | S7: Suportem as versões Android e arquiteturas exigidas. | Pendente |
| R132 | S7: Possuam fallback por CPU quando necessário. | Pendente |
| R133 | S7: Sejam testados em dispositivos de baixa memória. | Pendente |
| R134 | S7: Não bloqueiem a renderização da LIVE. | Pendente |
| R135 | S7: Avaliação de saída de faixa. | Pendente |
| R136 | S7: Avaliação de aproximação frontal. | Pendente |
| R137 | S7: Confiança temporal. | Pendente |
| R138 | S7: Rejeição de detecções espúrias. | Pendente |
| R139 | S7: Transição entre estados de risco. | Pendente |
| R140 | S7: Recuperação automática. | Pendente |
| S8 | LDW — LANE DEPARTURE WARNING | Pendente |
| R141 | S8: Reconhecer as marcações reais da pista. | Pendente |
| R142 | S8: Projetar linhas acompanhando a geometria detectada. | Pendente |
| R143 | S8: Acompanhar curvas. | Pendente |
| R144 | S8: Suavizar oscilações. | Pendente |
| R145 | S8: Mostrar o estado de confiança. | Pendente |
| R146 | S8: Não desenhar linhas falsas. | Pendente |
| R147 | S8: Sinalização âmbar, se suportada pela estimativa. | Pendente |
| R148 | S8: Sem alerta vermelho antecipado sem risco confirmado. | Pendente |
| R149 | S8: Linha invadida vermelha piscante. | Pendente |
| R150 | S8: Ícone contextual de saída de faixa. | Pendente |
| R151 | S8: Alerta sonoro. | Pendente |
| R152 | S8: Aviso de voz em português conforme configuração. | Pendente |
| R153 | S8: Registro do evento quando a gravação e os dados estiverem disponíveis. | Pendente |
| R154 | S8: Encerrar o piscar vermelho automaticamente. | Pendente |
| R155 | S8: Atualizar o estado visual. | Pendente |
| R156 | S8: Evitar repetição excessiva de alertas. | Pendente |
| R157 | S8: Suspender o alerta crítico. | Pendente |
| R158 | S8: Ocultar ou reduzir a projeção. | Pendente |
| R159 | S8: Informar indisponibilidade quando necessário. | Pendente |
| R160 | S8: Nunca manter linha vermelha como se o risco ainda fosse confirmado. | Pendente |
| R161 | S8: Pré-processamento de iluminação. | Pendente |
| R162 | S8: Detecção de marcações reais. | Pendente |
| R163 | S8: Identificação da faixa de circulação do veículo. | Pendente |
| R164 | S8: Acompanhamento temporal. | Pendente |
| R165 | S8: Estimativa de posição lateral. | Pendente |
| R166 | S8: Compensação de perspectiva. | Pendente |
| R167 | S8: Calibração por câmera. | Pendente |
| R168 | S8: Identificação consistente de saída de faixa. | Pendente |
| R169 | S8: Tratamento de marcações interrompidas e entradas laterais. | Pendente |
| S9 | FCW — FORWARD COLLISION WARNING | Pendente |
| R170 | S9: Reconhecimento do veículo à frente. | Pendente |
| R171 | S9: Rastreamento entre quadros. | Pendente |
| R172 | S9: Identificação do veículo relevante à trajetória. | Pendente |
| R173 | S9: Avaliação de aproximação. | Pendente |
| R174 | S9: Estimativa de risco considerando dados disponíveis. | Pendente |
| R175 | S9: Destacar o veículo real com contorno vermelho piscante. | Pendente |
| R176 | S9: Exibir alerta visual de risco de colisão. | Pendente |
| R177 | S9: Emitir som de alerta. | Pendente |
| R178 | S9: Emitir aviso de voz configurável. | Pendente |
| R179 | S9: Registrar o evento. | Pendente |
| R180 | S9: A aproximação perigosa cessar. | Pendente |
| R181 | S9: O risco deixar de existir. | Pendente |
| R182 | S9: O veículo rastreado sair da trajetória relevante. | Pendente |
| R183 | S9: Desenhar uma caixa fixa. | Pendente |
| R184 | S9: Marcar qualquer carro de vermelho. | Pendente |
| R185 | S9: Disparar alerta por botão de teste. | Pendente |
| R186 | S9: Apresentar uma animação sem análise da câmera. | Pendente |
| S10 | REGRA GLOBAL DE RECUPERAÇÃO E CONFIANÇA | Pendente |
| R187 | S10: Sem detecção. | Pendente |
| R188 | S10: Detecção candidata. | Pendente |
| R189 | S10: Detecção confirmada. | Pendente |
| R190 | S10: Risco confirmado. | Pendente |
| R191 | S10: Recuperação. | Pendente |
| R192 | S10: Indisponível ou confiança insuficiente. | Pendente |
| R193 | S10: Confirmação temporal antes dos alertas críticos. | Pendente |
| R194 | S10: Rejeição de resultados inconsistentes. | Pendente |
| R195 | S10: Encerramento automático quando o risco desaparecer. | Pendente |
| R196 | S10: Proteção contra alertas repetitivos. | Pendente |
| R197 | S10: Tratamento da perda de quadros da câmera. | Pendente |
| R198 | S10: Ausência de alarmes vermelhos sem detecção confiável. | Pendente |
| S11 | RECONHECIMENTO DE SEMÁFOROS | Pendente |
| R199 | S11: **Luz verde real:** semáforo aberto. | Pendente |
| R200 | S11: **Luz vermelha real:** semáforo fechado. | Pendente |
| R201 | S11: Semáforo inteiro. | Pendente |
| R202 | S11: Popup retangular grande. | Pendente |
| R203 | S11: Texto ocupando o centro. | Pendente |
| R204 | S11: Botão OK. | Pendente |
| R205 | S11: Mensagem falsa sem detecção real. | Pendente |
| R206 | S11: Detectar semáforos na imagem. | Pendente |
| R207 | S11: Identificar qual semáforo é relevante ao sentido de circulação. | Pendente |
| R208 | S11: Reconhecer o estado. | Pendente |
| R209 | S11: Acompanhar o mesmo semáforo ao longo dos quadros. | Pendente |
| R210 | S11: Confirmar a mudança de estado. | Pendente |
| R211 | S11: Exibir brevemente a luz correspondente. | Pendente |
| R212 | S11: Fechado → aberto: exibir luz verde. | Pendente |
| R213 | S11: Aberto → fechado: exibir luz vermelha. | Pendente |
| R214 | S11: Manter a detecção. | Pendente |
| R215 | S11: Enviar aviso compatível com as preferências de áudio/notificações. | Pendente |
| R216 | S11: Não abrir a LIVE automaticamente. | Pendente |
| S12 | RECONHECIMENTO DE PLACAS | Pendente |
| R217 | S12: Limite de velocidade. | Pendente |
| R218 | S12: PARE. | Pendente |
| R219 | S12: Entrada proibida. | Pendente |
| R220 | S12: Outras placas relevantes conforme evolução aprovada. | Pendente |
| R221 | S12: Localizar placas na imagem. | Pendente |
| R222 | S12: Classificar o tipo. | Pendente |
| R223 | S12: Reconhecer limite numérico quando aplicável. | Pendente |
| R224 | S12: Confirmar resultados entre quadros. | Pendente |
| R225 | S12: Evitar leitura de sinalização de vias não relacionadas. | Pendente |
| R226 | S12: Associar eventos ao contexto da direção. | Pendente |
| R227 | S12: Não confundir teste de popup com reconhecimento automático. | Pendente |
| S13 | PEDESTRES, CICLISTAS E OBSTÁCULOS | Pendente |
| R228 | S13: Pedestres. | Pendente |
| R229 | S13: Ciclistas. | Pendente |
| R230 | S13: Veículos. | Pendente |
| R231 | S13: Objetos relevantes à área trafegável. | Pendente |
| R232 | S13: Objeto visível. | Pendente |
| R233 | S13: Objeto próximo. | Pendente |
| R234 | S13: Objeto que representa risco. | Pendente |
| S14 | EVENTOS FUNCIONANDO EM SEGUNDO PLANO | Pendente |
| R235 | S14: Gravação. | Pendente |
| R236 | S14: Monitoramento GPS. | Pendente |
| R237 | S14: Processamento ADAS quando permitido. | Pendente |
| R238 | S14: Gestão de eventos. | Pendente |
| R239 | S14: Notificações. | Pendente |
| R240 | S14: Alertas sonoros e voz. | Pendente |
| R241 | S14: Não encerrar indevidamente o monitoramento. | Pendente |
| R242 | S14: Preservar gravações ativas quando o hardware permitir. | Pendente |
| R243 | S14: Continuar eventos GPS. | Pendente |
| R244 | S14: Continuar ADAS somente enquanto houver quadros reais. | Pendente |
| R245 | S14: Preservar o estado da sessão. | Pendente |
| R246 | S14: Restrição de câmera em segundo plano. | Pendente |
| R247 | S14: Permissões. | Pendente |
| R248 | S14: Suspensão por economia de bateria. | Pendente |
| R249 | S14: Perda da câmera USB. | Pendente |
| R250 | S14: Conflito com outro aplicativo que utiliza a câmera. | Pendente |
| R251 | S14: Encerramento do serviço pelo sistema. | Pendente |
| R252 | S14: Android moderno com restrições adicionais de inicialização. | Pendente |
| S15 | ÁUDIO MIX E VOZ PREMIUM | Parcial |
| R253 | S15: Alertas em português brasileiro. | Pendente |
| R254 | S15: Voz feminina natural conforme identidade aprovada. | Pendente |
| R255 | S15: Sons premium. | Pendente |
| R256 | S15: Volumes ajustáveis. | Pendente |
| R257 | S15: Silenciamento independente da voz. | Pendente |
| R258 | S15: Preferências persistentes. | Pendente |
| R259 | S15: Não interromper permanentemente a música ou rádio. | Pendente |
| S16 | GRAVAÇÃO DASHCAM | Parcial |
| R260 | S16: Prévia LIVE real. | Pendente |
| R261 | S16: Gravação MP4/H.264 conforme capacidade do hardware. | Pendente |
| R262 | S16: Gravação contínua em segmentos. | Pendente |
| R263 | S16: Configuração de duração dos segmentos. | Pendente |
| R264 | S16: Exclusão circular dos clipes comuns mais antigos. | Parcial |
| R265 | S16: Proteção dos eventos importantes. | Pendente |
| R266 | S16: Gravação de emergência. | Pendente |
| R267 | S16: Identificação do estado REC. | Pendente |
| R268 | S16: Tratamento de falhas de armazenamento. | Pendente |
| R269 | S16: Data. | Pendente |
| R270 | S16: Hora. | Pendente |
| R271 | S16: Velocidade GPS. | Pendente |
| R272 | S16: Coordenadas. | Pendente |
| R273 | S16: Identificação do dispositivo/câmera. | Pendente |
| R274 | S16: Eventos associados. | Pendente |
| R275 | S16: Detectar impacto conforme calibração. | Pendente |
| R276 | S16: Proteger automaticamente o clipe relevante. | Pendente |
| R277 | S16: Salvar informações do evento. | Pendente |
| R278 | S16: Preservar gravação anterior/posterior se houver buffer implementado. | Pendente |
| S17 | RADARES GPS | Parcial |
| R279 | S17: Banco local persistente. | Pendente |
| R280 | S17: Cadastro, edição e exclusão. | Pendente |
| R281 | S17: Identificação do limite de velocidade. | Pendente |
| R282 | S17: Distância e direção aproximadas. | Pendente |
| R283 | S17: Antirrepetição de alertas. | Pendente |
| R284 | S17: Operação em segundo plano. | Pendente |
| R285 | S17: Preferências de voz/som. | Pendente |
| R286 | S17: Continuidade offline dos dados existentes. | Pendente |
| S18 | BACKUP GOOGLE DRIVE | Pendente |
| R287 | S18: Upload opcional de vídeos protegidos. | Pendente |
| R288 | S18: Autenticação autorizada pelo usuário. | Pendente |
| R289 | S18: Gravação local primeiro. | Pendente |
| R290 | S18: Fila de uploads. | Pendente |
| R291 | S18: Recuperação após falha de conexão. | Pendente |
| R292 | S18: Suporte a Wi-Fi e dados móveis, conforme preferência aprovada. | Pendente |
| R293 | S18: Indicação de estado do backup. | Pendente |
| R294 | S18: Proteção de dados e consentimento explícito. | Pendente |
| S19 | TEMAS E PERSONALIZAÇÃO | Pendente |
| R295 | S19: Luxury Blue — padrão. | Pendente |
| R296 | S19: Carbon Sport. | Pendente |
| R297 | S19: Executive Gold. | Pendente |
| R298 | S19: Ice Silver. | Pendente |
| R299 | S19: Stealth. | Pendente |
| R300 | S19: Emerald. | Pendente |
| S20 | PRIVACIDADE, LICENCIAMENTO E USO COMERCIAL | Pendente |
| R301 | S20: LGPD. | Pendente |
| R302 | S20: Permissões de câmera e localização. | Pendente |
| R303 | S20: Consentimento para backup. | Pendente |
| R304 | S20: Armazenamento seguro. | Pendente |
| R305 | S20: Retenção de vídeos. | Pendente |
| R306 | S20: Transparência sobre processamento local. | Pendente |
| R307 | S20: Licenciamento comercial de bibliotecas e modelos de IA. | Pendente |
| R308 | S20: Requisitos da loja de distribuição, quando aplicáveis. | Pendente |
| R309 | S20: Declaração clara das limitações de ADAS. | Pendente |
| R310 | S20: Tratamento de logs sem exposição de informações sensíveis. | Pendente |
| S21 | ARQUITETURA DE CÓDIGO RECOMENDADA | Parcial |
| R311 | S21: CameraSource | Pendente |
| R312 | S21: Camera2Source | Pendente |
| R313 | S21: UvcSource | Pendente |
| R314 | S21: ProprietaryCameraAdapter | Pendente |
| R315 | S21: CameraCalibration | Pendente |
| R316 | S21: CameraTransform | Pendente |
| R317 | S21: VisionEngine | Pendente |
| R318 | S21: LaneDetector | Pendente |
| R319 | S21: ObjectDetector | Pendente |
| R320 | S21: TrafficSignDetector | Pendente |
| R321 | S21: TrafficLightDetector | Pendente |
| R322 | S21: ObjectTracker | Pendente |
| R323 | S21: LaneDepartureEvaluator | Pendente |
| R324 | S21: ForwardCollisionEvaluator | Pendente |
| R325 | S21: RiskStateMachine | Pendente |
| R326 | S21: ConfidenceFilter | Pendente |
| R327 | S21: AlertManager | Pendente |
| R328 | S21: AlertAudio | Pendente |
| R329 | S21: AlertOverlay | Pendente |
| R330 | S21: EventPriorityManager | Pendente |
| R331 | S21: RecordingManager | Pendente |
| R332 | S21: RecordingSegmentStore | Pendente |
| R333 | S21: EmergencyClipProtector | Pendente |
| R334 | S21: EvidenceMetadata | Pendente |
| R335 | S21: DashCamForegroundService | Pendente |
| R336 | S21: CameraSessionManager | Pendente |
| R337 | S21: SensorMonitor | Pendente |
| R338 | S21: ServiceRecovery | Pendente |
| R339 | S21: RadarStore | Pendente |
| R340 | S21: RadarMonitor | Pendente |
| R341 | S21: DeviceCapabilities | Pendente |
| R342 | S21: CameraDiagnostics | Pendente |
| R343 | S21: CrashReporter | Pendente |
| R344 | S21: MainActivity | Pendente |
| R345 | S21: PremiumMenuActivity | Pendente |
| R346 | S21: LiveControls | Pendente |
| R347 | S21: FullscreenController | Pendente |
| S22 | PROBLEMAS REAIS JÁ IDENTIFICADOS | Parcial |
| S23 | MATERIAL REAL DE TESTES DISPONÍVEL | Pendente |
| R348 | S23: `20261011_154413.mp4` | Pendente |
| R349 | S23: `Video Project 1.mp4` | Pendente |
| R350 | S23: Capturas VLC de vias urbanas. | Pendente |
| R351 | S23: Estradas retas. | Pendente |
| R352 | S23: Curvas. | Pendente |
| R353 | S23: Marcações descontínuas. | Pendente |
| R354 | S23: Entradas laterais. | Pendente |
| R355 | S23: Veículos estacionados. | Pendente |
| R356 | S23: Pessoas próximas à pista. | Pendente |
| R357 | S23: Placas de 60 km/h. | Pendente |
| R358 | S23: Sombreamento por árvores. | Pendente |
| S24 | PLANO DE IMPLEMENTAÇÃO OBRIGATÓRIO | Parcial |
| R359 | S24: Extrair o ZIP V4.8. | Pendente |
| R360 | S24: Inspecionar Gradle e AndroidManifest. | Pendente |
| R361 | S24: Conferir dependências. | Pendente |
| R362 | S24: Verificar recursos visuais. | Pendente |
| R363 | S24: Corrigir erros de código. | Pendente |
| R364 | S24: Confirmar inicialização da LIVE. | Pendente |
| R365 | S24: Corrigir orientação. | Pendente |
| R366 | S24: Calibrar transformações. | Pendente |
| R367 | S24: Preservar proporção. | Pendente |
| R368 | S24: Corrigir fullscreen. | Pendente |
| R369 | S24: Conectar frames ao motor ADAS. | Pendente |
| R370 | S24: Substituir a dependência exclusiva do detector experimental. | Pendente |
| R371 | S24: Implementar detecção e rastreamento. | Pendente |
| R372 | S24: Calcular desvio lateral. | Pendente |
| R373 | S24: Integrar LDW. | Pendente |
| R374 | S24: Implementar vermelho piscante. | Pendente |
| R375 | S24: Implementar áudio e recuperação. | Pendente |
| R376 | S24: Integrar detector de veículos. | Pendente |
| R377 | S24: Implementar rastreamento. | Pendente |
| R378 | S24: Avaliar risco frontal. | Pendente |
| R379 | S24: Desenhar destaque no veículo real. | Pendente |
| R380 | S24: Integrar alerta vermelho piscante. | Pendente |
| R381 | S24: Implementar áudio e recuperação. | Pendente |
| R382 | S24: Implementar detectores. | Pendente |
| R383 | S24: Reconhecer estados. | Pendente |
| R384 | S24: Integrar referências visuais. | Pendente |
| R385 | S24: Confirmar transições. | Pendente |
| R386 | S24: Bloquear falsos alertas. | Pendente |
| R387 | S24: Pedestres. | Pendente |
| R388 | S24: Ciclistas. | Pendente |
| R389 | S24: Veículos laterais. | Pendente |
| R390 | S24: Eventos relevantes. | Pendente |
| R391 | S24: Serviço persistente. | Pendente |
| R392 | S24: Gravação. | Pendente |
| R393 | S24: GPS. | Pendente |
| R394 | S24: Eventos ADAS. | Pendente |
| R395 | S24: Notificações. | Pendente |
| R396 | S24: Recuperação de falhas. | Pendente |
| R397 | S24: Controles auto-recolhíveis. | Pendente |
| R398 | S24: Menu 3D. | Pendente |
| R399 | S24: Indicadores reais. | Pendente |
| R400 | S24: Alertas refinados. | Pendente |
| R401 | S24: Tema Premium Luxury. | Pendente |
| R402 | S24: Testes unitários. | Pendente |
| R403 | S24: Testes com vídeos fornecidos. | Pendente |
| R404 | S24: Testes de ciclo de vida Android. | Pendente |
| R405 | S24: Testes de câmera. | Pendente |
| R406 | S24: Testes de gravação. | Pendente |
| R407 | S24: Medição de desempenho. | Pendente |
| R408 | S24: Verificação de permissões. | Pendente |
| R409 | S24: Revisão de segurança. | Pendente |
| S25 | GERAR APK.BAT — REQUISITO OBRIGATÓRIO | Parcial |
| R410 | S25: Abrir na pasta do projeto. | Pendente |
| R411 | S25: Verificar Java/JDK. | Pendente |
| R412 | S25: Verificar Android SDK. | Pendente |
| R413 | S25: Utilizar o Gradle Wrapper incluído no projeto. | Parcial |
| R414 | S25: Executar a compilação Debug. | Pendente |
| R415 | S25: Salvar log de compilação. | Pendente |
| R416 | S25: Detectar erros de compilação. | Pendente |
| R417 | S25: Informar claramente o motivo de falha. | Pendente |
| R418 | S25: Copiar o APK gerado para uma pasta de fácil acesso. | Pendente |
| R419 | S25: Mostrar o caminho final do arquivo. | Pendente |
| R420 | S25: `gradlew.bat` | Parcial |
| R421 | S25: `gradlew` | Parcial |
| R422 | S25: `gradle/wrapper/gradle-wrapper.properties` | Parcial |
| R423 | S25: `gradle/wrapper/gradle-wrapper.jar` | Parcial |
| R424 | S25: `GERAR APK.bat` | Parcial |
| R425 | S25: Projeto Kotlin completo. | Pendente |
| R426 | S25: Recursos visuais. | Pendente |
| R427 | S25: Bibliotecas e arquivos de configuração necessários. | Pendente |
| R428 | S25: Modelos de IA compatíveis com licenciamento, quando utilizados. | Pendente |
| R429 | S25: Documentação. | Pendente |
| R430 | S25: Relatório de testes. | Pendente |
| S26 | CRITÉRIOS DE APROVAÇÃO TÉCNICA | Parcial |
| R431 | S26: Identifica faixas reais. | Pendente |
| R432 | S26: Acompanha curvas. | Pendente |
| R433 | S26: Reconhece saída confirmada. | Pendente |
| R434 | S26: Pisca a linha correspondente em vermelho. | Pendente |
| R435 | S26: Emite alerta configurado. | Pendente |
| R436 | S26: Para automaticamente. | Pendente |
| R437 | S26: Não alarma com baixa confiança. | Pendente |
| R438 | S26: Detecta e acompanha o veículo real. | Pendente |
| R439 | S26: Identifica situação de risco relevante. | Pendente |
| R440 | S26: Destaca o veículo em vermelho piscante. | Pendente |
| R441 | S26: Emite alerta configurado. | Pendente |
| R442 | S26: Para automaticamente. | Pendente |
| R443 | S26: Não dispara somente por proximidade aparente. | Pendente |
| R444 | S26: Reconhece estados reais. | Pendente |
| R445 | S26: Identifica o semáforo relevante. | Pendente |
| R446 | S26: Confirma alterações. | Pendente |
| R447 | S26: Exibe somente luz verde/vermelha realista no centro da LIVE. | Pendente |
| R448 | S26: Não inventa mudanças. | Pendente |
| R449 | S26: Abre sem crash. | Pendente |
| R450 | S26: Mantém orientação. | Pendente |
| R451 | S26: Mantém proporção. | Pendente |
| R452 | S26: Opera em FULL. | Pendente |
| R453 | S26: Preserva alinhamento do ADAS. | Pendente |
| R454 | S26: Serviço não depende da Activity. | Pendente |
| R455 | S26: Continua funções permitidas pelo sistema. | Pendente |
| R456 | S26: Trata perda de câmera. | Pendente |
| R457 | S26: Não simula eventos. | Pendente |
| R458 | S26: Mantém alertas adequados. | Pendente |
| R459 | S26: Compilação Debug bem-sucedida. | Pendente |
| R460 | S26: APK instalável. | Pendente |
| R461 | S26: Sem erros fatais de inicialização nos dispositivos testados. | Pendente |
| R462 | S26: Registro de build. | Pendente |
| R463 | S26: Código-fonte ZIP íntegro. | Pendente |
| S27 | MATRIZ DE TESTES COMERCIAIS | Pendente |
| S28 | ENTREGÁVEIS EXIGIDOS DO CLAUDE | Parcial |
| R464 | S28: Funcionalidades implementadas. | Pendente |
| R465 | S28: Arquivos modificados. | Pendente |
| R466 | S28: Dependências adicionadas. | Pendente |
| R467 | S28: Build executada. | Pendente |
| R468 | S28: Resultado da compilação. | Pendente |
| R469 | S28: Testes realizados. | Pendente |
| R470 | S28: Problemas encontrados. | Pendente |
| R471 | S28: Limitações de hardware. | Pendente |
| R472 | S28: Pendências que exigem validação em campo. | Pendente |
| R473 | S28: Implementado. | Pendente |
| R474 | S28: Testado. | Pendente |
| R475 | S28: Parcial. | Pendente |
| R476 | S28: Pendente. | Pendente |
| S29 | REGRAS SUPREMAS DO PROPRIETÁRIO | Pendente |
| S30 | INSTRUÇÃO FINAL PARA O CLAUDE | Pendente |
| R477 | S30: Inspecione todos os arquivos do projeto. | Pendente |
| R478 | S30: Identifique erros de compilação, arquitetura e funcionamento. | Pendente |
| R479 | S30: Preserve as implementações válidas. | Pendente |
| R480 | S30: Corrija definitivamente orientação, espelhamento, fullscreen e layout responsivo. | Parcial |
| R481 | S30: Integre o pipeline de frames reais. | Pendente |
| R482 | S30: Implemente LDW real com linha vermelha piscante, som, confiança e recuperação. | Pendente |
| R483 | S30: Implemente FCW real com rastreamento do veículo, risco frontal, contorno vermelho piscante, som e recuperação. | Pendente |
| R484 | S30: Implemente reconhecimento real de placas, semáforos, pedestres e ciclistas, conforme os requisitos. | Pendente |
| R485 | S30: Integre os serviços de background. | Pendente |
| R486 | S30: Preserve o áudio MIX e implemente os tratamentos necessários. | Pendente |
| R487 | S30: Finalize gravação, eventos, radares e demais funções descritas. | Pendente |
| R488 | S30: Execute testes de código e validação com vídeos reais. | Pendente |
| R489 | S30: Corrija os erros identificados. | Pendente |
| R490 | S30: **EXECUTE A COMPILAÇÃO DEBUG DO PROJETO ANDROID.** | Pendente |
| R491 | S30: Gere o APK Debug. | Pendente |
| R492 | S30: Gere o ZIP do código-fonte com `GERAR APK.bat`. | Parcial |
| R493 | S30: Entregue relatório técnico com evidências e limitações. | Pendente |

## Atualização desta etapa

| Requisito | Estado | Evidência / limite |
|---|---|---|
| targetSdk 35 / minSdk 24 | Implementado | Verificar aapt, build e lint da nova entrega; runtime em dispositivo pendente |
| GPS/radares independentes da LIVE | Parcial | Serviço real implementado; runtime Android ainda não testado |
| Gravação/câmera em segundo plano | Pendente | Captura permanece na Activity |
| Compartilhamento de MP4 | Parcial | FileProvider e content URI implementados; reprodutor real pendente |
| Layout Android 15 | Parcial | Insets implementados; validação visual pendente |
| Teste com Video Project 1.mp4 | Executado | 82 quadros; sem anotações de referência, não mede acurácia |
| LDW/FCW operacional | Pendente | Detector experimental não oferece risco calibrado nem detecção/rastreamento de veículos |

## Atualização — serviço de câmera e ADAS experimental

| Requisito | Código aplicado | Validação atual | Aceitação integral |
|---|---|---|---|
| Câmera/gravação sem Activity | DashCamForegroundService; outputs sem superfície da LIVE | Build/lint; teste físico preparado, não executado | Parcial |
| LDW, curvas, confiança e recuperação | Ajuste quadrático + RiskEngine + calibração/GPS | Casos JVM sintéticos; poucos candidatos no vídeo real | Parcial |
| FCW com detecção/rastreamento/TTC | SSD real + IoU + regressão/estados/cooldown | Inferência real no vídeo e casos JVM; alertas em trânsito não validados | Parcial |
| Vermelho piscante, voz e eventos | RiskOverlayView/AlertAudio/EventStore | Código/compilação; UI/áudio físicos não testados | Parcial |
| Perda de câmera/confiança | Suspensão, descarte por geração, tentativas de recuperação | Casos lógicos JVM; Camera2 em hardware não testado | Parcial |
| Gravação/proteção/circular/hash | MediaRecorder em serviço, quota, lock, hash | Componentes de arquivo testados; fluxo Android físico não testado | Parcial |
| GPS/interface em Android real | Helper ADB específico preparado | Zero dispositivos conectados; não executado | Pendente |
| Compatibilidade K2001N/UVC/proprietário | Camera2/ARM32 e ARM64 | Empacotamento verificado; backend proprietário não integrado | Parcial |

Os estados Pendente/Parcial do inventário completo acima continuam sendo estados de aceitação do requisito inteiro; implementação nesta etapa não implica validação comercial.


## Revisão 3.2 — faixas, áudio e APK de referência

| Requisito | Aplicado | Aceitação restante |
|---|---|---|
| Faixas dinâmicas | Consenso multi largura, textura/direção, curvas para ambos os lados e confirmação temporal | Pares confirmados no vídeo: zero; precisão real/LDW operacional não demonstrados |
| Mais bips | Seis padrões, frequência/duração/volume e prévia | Áudio e mistura com FM em hardware |
| Referências nativas APK | Binário ARM32 copiado, contrato JNI reconstruído, perfil ARM32 e tela de prévia USB | Runtime/câmera física e licença comercial da biblioteca não verificados |
| Captura/ADAS USB | Prévia nativa de teste | Gravação e LDW/FCW USB não integrados; caminho atual é Camera2 |
| Interface nas capturas | Alturas em dp, texto expansível, insets reservados e estado ADAS reposicionado | Verificação visual em Android real |
