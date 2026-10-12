# DASHCAM ADAS PRO
## DOCUMENTO TÉCNICO MESTRE — TRANSFERÊNCIA DE DESENVOLVIMENTO PARA CLAUDE

**Data de consolidação:** 11/10/2026  
**Projeto:** DashCam ADAS PRO  
**Plataforma:** Android  
**Linguagem da base:** Kotlin  
**Finalidade:** Produto comercial multi-dispositivo de gravação veicular e assistência avançada ao motorista (ADAS)  
**Base disponível mais recente:** V4.8 — Faixas Beta

---

# 1. OBJETIVO DESTE DOCUMENTO

Este documento reúne o histórico técnico, os requisitos aprovados pelo proprietário, os recursos existentes, os problemas conhecidos e o trabalho necessário para finalizar o DashCam ADAS PRO.

O desenvolvimento deverá continuar a partir do **código-fonte real da V4.8**, aproveitando as implementações válidas das versões anteriores.

**ORDEM EXPRESSA:**

O responsável pela implementação deverá:

1. Inspecionar integralmente o projeto Android fornecido.
2. Corrigir problemas existentes, preservando funcionalidades aprovadas.
3. Implementar os módulos ADAS reais, incluindo LDW e FCW.
4. Integrar todos os módulos à LIVE, ao sistema de alertas e à execução em segundo plano.
5. Aplicar as regras visuais e funcionais descritas neste documento.
6. Executar os testes técnicos possíveis no ambiente de desenvolvimento.
7. **GERAR A BUILD DEBUG DO APLICATIVO.**
8. Entregar o APK Debug, o código-fonte integral e o ZIP com `GERAR APK.bat`.
9. Informar de maneira verificável quais testes foram concluídos e quais ainda exigem hardware ou pista real.

A autorização para gerar a build de teste já foi concedida pelo proprietário.

A antiga orientação de aguardar nova autorização para compilar **não deve ser utilizada como impedimento nesta tarefa**.

Nenhuma decisão de produto que contrarie ou altere os requisitos aprovados poderá ser adotada sem autorização do proprietário.

---

# 2. HARDWARE E COMPATIBILIDADE COMERCIAL

O DashCam ADAS PRO não é exclusivo de um equipamento.

Deve atender, conforme suporte técnico efetivamente desenvolvido, a:

- Centrais multimídia Android.
- K2001N/NWD.
- Tablets Android.
- Smartphones Android.
- Câmeras integradas.
- Câmeras USB-A/USB-C compatíveis.
- Câmeras UVC e dispositivos proprietários por adaptadores específicos.

## 2.1 Dispositivo de referência

**K2001N/NWD**

- Android 7.1.
- API 25.
- Aproximadamente 2 GB RAM.
- Tela de referência 1024 × 600.
- Câmera USB ADAS originalmente utilizada no UCarDVR.
- Aplicativo original: `com.ankai.cardvr`.
- Biblioteca proprietária identificada: `libmycamera.so`.
- Arquitetura original relevante: `armeabi-v7a`.

O funcionamento dessa câmera no UCarDVR **não comprova** que ela esteja acessível pelo Camera2.

A integração exige identificar o protocolo, a interface USB, o caminho de captura e as permissões necessárias.

Não presumir que adicionar uma biblioteca proprietária sem autorização ou conhecimento do protocolo resolverá a compatibilidade.

## 2.2 Requisitos de compatibilidade

- Preservar compatibilidade a partir de Android 7.0/API 24, conforme configuração conhecida do projeto, quando tecnicamente possível.
- Suportar arquiteturas ARM de 32 e 64 bits conforme os componentes nativos utilizados.
- Manter compatibilidade com diferentes resoluções e densidades.
- Detectar recursos reais antes de habilitar funções.
- Empregar processamento adaptativo conforme CPU, GPU, RAM e câmera.
- Não obrigar todos os aparelhos a utilizar configurações da K2001N.

---

# 3. INVENTÁRIO DO QUE JÁ FOI FEITO

Os estados abaixo foram apurados pelo histórico do projeto, pela documentação das versões e pelas capturas de tela enviadas.

**Importante:** código presente não significa funcionalidade integralmente testada.

| Componente | Estado conhecido |
|---|---|
| Projeto Android/Kotlin | Existente |
| Launcher diretamente na LIVE | Implementado nas versões anteriores |
| Menu Premium Luxury 3D | Arte estática com áreas de toque |
| LIVE com Camera2 | Implementada |
| Exibição real da câmera em tablet | Confirmada visualmente pelo proprietário |
| Correção do crash do TextureView | Aplicada na V4.6 |
| Seleção de câmeras Camera2 | Código existente |
| Diagnóstico de dispositivos e USB | Acrescentado na V4.4 |
| Rotação e controle FULL | Código acrescentado na V4.7, requer validação |
| Gravação MP4/H.264 | Código existente, necessita testes |
| Proteção manual de clipes | Código existente |
| Cadastro manual de radares GPS | Código existente |
| Monitoramento de radares cadastrados | Código existente, sem validação completa |
| Registro local de eventos | Estrutura existente |
| TTS em português | Código existente, qualidade premium pendente |
| Audio MIX sem solicitar AudioFocus | Implementação inicial, não validada no rádio FM |
| Popups personalizados | Implementação parcial |
| Processamento experimental de faixas | V4.8 Beta |
| LDW real com saída de faixa | Incompleto |
| FCW real com risco de colisão | Não implementado integralmente |
| Reconhecimento de pedestres | Pendente |
| Reconhecimento de ciclistas | Pendente |
| Reconhecimento automático de placas | Pendente |
| Reconhecimento de semáforos | Pendente |
| Eventos independentes da LIVE em segundo plano | Pendente |
| Gestão completa de gravação circular | Pendente |
| Google Drive automático | Pendente |
| Validação comercial multi-dispositivo | Pendente |

---

# 4. EVOLUÇÃO DAS VERSÕES

## V4.3 — LIVE Premium

Implementações:

- LIVE como página principal.
- Interface escura Premium Luxury.
- HUD superior.
- Região principal da câmera.
- Painel ADAS lateral.
- Botões inferiores.
- Integração inicial de alertas visuais.

Limitação: o visual ainda não reproduz integralmente a referência fotográfica aprovada.

## V4.4 — Base comercial

Implementações:

- Diagnóstico de fabricante e modelo.
- Identificação da versão Android.
- Consulta à memória RAM.
- Descoberta de câmeras Camera2.
- Enumeração de dispositivos USB, VID/PID e interfaces.
- Perfis de desempenho sugeridos.

Limitação: descobrir uma câmera USB não significa conseguir obter seus quadros.

## V4.5 — Inicialização defensiva

Implementações:

- Tratamento separado de erros de inicialização.
- Tela de diagnóstico quando a LIVE falha.
- Proteções contra falhas de câmera e serviços.

## V4.6 — Correção de crash

**Erro identificado:**

`UnsupportedOperationException: TextureView doesn't support displaying a background drawable`

Causa:

Aplicação de fundo diretamente em `TextureView`.

Correção:

Remover o fundo do `TextureView` e aplicar acabamento ao contêiner `FrameLayout`.

O proprietário apresentou posteriormente telas com vídeo real, demonstrando que a LIVE conseguiu funcionar em pelo menos um dispositivo.

## V4.7 — Câmera e controles

Código acrescentado para:

- Orientação da câmera.
- Rotação manual.
- Ajustes persistentes.
- Botão de tela cheia.
- Popups com ícones contextuais.

Essas alterações necessitam verificação em dispositivos reais.

## V4.8 — Faixas Beta

O documento técnico da V4.8 registra:

- Amostragem de imagem do `TextureView`.
- Processamento experimental em aproximadamente 160 × 90.
- Taxa de análise aproximada de 2,8 Hz.
- Extração de bordas claras.
- Ajuste linear das possíveis faixas.
- Rejeição por confiança.
- Overlay de linhas.
- Botão FAIXAS BETA.

**Limitação crítica:**

O algoritmo ainda não calcula saída de faixa com confiabilidade suficiente para ser considerado LDW operacional de segurança.

Também não possui FCW real integrado.

A V4.8 é a base de partida, não o produto final.

---

# 5. INTERFACE PREMIUM LUXURY — REQUISITOS OFICIAIS

## 5.1 Página de abertura

Ao iniciar o aplicativo, abrir diretamente a **LIVE**.

O menu 3D com cartões não deve substituir a tela inicial.

## 5.2 Aparência

Preservar a identidade aprovada:

- Fundo preto piano.
- Elementos em vidro escuro.
- Brilhos azuis.
- Acabamento 3D realista.
- Bordas luminosas discretas.
- Ícones visualmente refinados.
- Alertas translúcidos.
- Sem caixas grandes do Android para alertas de direção.
- Interface responsiva.

A câmera deve exibir imagens verdadeiras. Não substituir o feed por vídeo ou fotografia simulada.

## 5.3 Estrutura LIVE

A LIVE deverá conter:

- Câmera principal em destaque.
- Painel secundário quando houver um segundo feed real disponível.
- HUD com velocidade, GPS e estado verdadeiro da gravação.
- Alertas ADAS sobrepostos ao vídeo.
- Controles inferiores.
- Acesso rápido às configurações.
- Botão FULL visível.
- Ajuste de orientação.
- Silenciamento apenas da voz, conforme preferência persistida.

Estados como REC, GPS e ADAS ativo não devem aparecer como funcionais quando estiverem indisponíveis.

## 5.4 Controles auto-recolhíveis

**Requisito obrigatório aprovado:**

Os botões da LIVE devem recolher automaticamente após inatividade.

Quando recolhidos:

- A imagem da câmera ocupa mais espaço.
- Os alertas continuam visíveis.
- O estado essencial da gravação permanece acessível.
- Um toque mostra novamente os controles.
- O acesso a FULL e às configurações não pode ficar permanentemente perdido.

O tempo de recolhimento deverá ser configurável. Seu valor definitivo depende de aprovação do proprietário.

## 5.5 Menu 3D

Preservar o menu Premium Luxury com oito áreas:

1. CÂMERA LIVE.
2. PROTEÇÃO ADAS.
3. RADARES GPS.
4. PLACAS DE TRÂNSITO.
5. SEMÁFOROS.
6. EVENTOS.
7. LUXURY AUDIO.
8. CONFIGURAÇÕES.

A arte original aprovada deve ser preservada e integrada a funções reais, não apenas demonstrativas.

---

# 6. CORREÇÃO DEFINITIVA DA ORIENTAÇÃO DA CÂMERA

**Prioridade crítica.**

O proprietário demonstrou por capturas que a câmera exibia imagens giradas.

Implementar sistema de orientação que considere:

- Orientação física do sensor.
- Rotação atual do display.
- Facing frontal ou traseiro.
- Comportamento de câmeras USB.
- Firmware que já entrega quadros pré-rotacionados.
- Espelhamento horizontal quando necessário.

## 6.1 Ajustes obrigatórios

Disponibilizar:

- Automático.
- 0°.
- 90°.
- 180°.
- 270°.
- Espelhar horizontalmente, quando aplicável.
- Ajustar à tela (FIT).
- Preencher a tela (FILL).
- Tela cheia.

Salvar os ajustes **individualmente por câmera**.

## 6.2 Transformação de coordenadas

A mesma transformação aplicada ao vídeo deve ser considerada pelas projeções ADAS.

Não permitir:

- Caixa do veículo deslocada do objeto real.
- Linhas de faixa fora da pista.
- Detector analisando região diferente daquela exibida.
- Corte ou distorção causados por transformação inadequada.

Deve existir um mapeamento explícito entre:

1. Coordenadas do quadro capturado.
2. Coordenadas processadas pelo ADAS.
3. Coordenadas exibidas na LIVE.

---

# 7. MOTOR ADAS REAL — ARQUITETURA OBRIGATÓRIA

O projeto deverá possuir um motor de visão computacional independente da Activity da LIVE.

Arquitetura recomendada:

**CameraSource → FrameProcessor → VisionEngine → RiskEngine → EventManager → AlertManager → LIVE/Background**

## 7.1 CameraSource

Implementar abstração de origem da câmera, com adaptadores específicos:

- Camera2.
- USB UVC compatível.
- Câmeras proprietárias quando tecnicamente integradas.
- Fonte de vídeo gravado para testes offline.

A interface de câmera deve oferecer:

- Quadros.
- Timestamp.
- Resolução.
- Orientação.
- Identificador da câmera.
- Estado de conexão.
- Erros recuperáveis.

Evitar usar exclusivamente `TextureView.getBitmap()` como mecanismo definitivo de inferência. Preferir uma saída de quadros apropriada, como `ImageReader` com formato suportado, quando o hardware permitir.

## 7.2 VisionEngine

Responsável pela análise de:

- Marcação de faixa.
- Veículos.
- Pedestres.
- Ciclistas.
- Placas.
- Semáforos.

Preferir execução local com modelos adequados e licenciados para uso comercial.

É permitido utilizar bibliotecas de visão computacional e modelos quantizados, desde que:

- Tenham licenciamento compatível.
- Suportem as versões Android e arquiteturas exigidas.
- Possuam fallback por CPU quando necessário.
- Sejam testados em dispositivos de baixa memória.
- Não bloqueiem a renderização da LIVE.

## 7.3 RiskEngine

Responsável por:

- Avaliação de saída de faixa.
- Avaliação de aproximação frontal.
- Confiança temporal.
- Rejeição de detecções espúrias.
- Transição entre estados de risco.
- Recuperação automática.

A lógica do risco não deve ser baseada apenas na cor exibida ou em um botão de demonstração.

---

# 8. LDW — LANE DEPARTURE WARNING

## REGRA SUPREMA APROVADA

**Quando o veículo invadir uma faixa, a linha correspondente deverá ficar VERMELHA PISCANTE, com aviso visual e sonoro.**

## 8.1 Comportamento normal

- Reconhecer as marcações reais da pista.
- Projetar linhas acompanhando a geometria detectada.
- Acompanhar curvas.
- Suavizar oscilações.
- Mostrar o estado de confiança.
- Não desenhar linhas falsas.

## 8.2 Aproximação da marcação

Quando houver aproximação relevante de uma marcação:

- Sinalização âmbar, se suportada pela estimativa.
- Sem alerta vermelho antecipado sem risco confirmado.

## 8.3 Saída de faixa confirmada

Quando houver cruzamento confirmado de marcação:

- Linha invadida vermelha piscante.
- Ícone contextual de saída de faixa.
- Alerta sonoro.
- Aviso de voz em português conforme configuração.
- Registro do evento quando a gravação e os dados estiverem disponíveis.

## 8.4 Recuperação

Quando o veículo retornar à posição segura ou a condição de saída deixar de existir:

- Encerrar o piscar vermelho automaticamente.
- Atualizar o estado visual.
- Evitar repetição excessiva de alertas.

## 8.5 Baixa confiança

Quando a marcação desaparecer ou a detecção não for confiável:

- Suspender o alerta crítico.
- Ocultar ou reduzir a projeção.
- Informar indisponibilidade quando necessário.
- Nunca manter linha vermelha como se o risco ainda fosse confirmado.

## 8.6 Implementação técnica

Exigir, conforme a abordagem validada:

- Pré-processamento de iluminação.
- Detecção de marcações reais.
- Identificação da faixa de circulação do veículo.
- Acompanhamento temporal.
- Estimativa de posição lateral.
- Compensação de perspectiva.
- Calibração por câmera.
- Identificação consistente de saída de faixa.
- Tratamento de marcações interrompidas e entradas laterais.

A versão experimental baseada somente em segmentos lineares deve evoluir para uma implementação capaz de acompanhar curvas e situações reais.

---

# 9. FCW — FORWARD COLLISION WARNING

## REGRA SUPREMA APROVADA

**Quando houver risco confirmado de colisão com o veículo da frente, o veículo deverá ser destacado em VERMELHO PISCANTE na LIVE.**

## 9.1 Detecção

Implementar:

- Reconhecimento do veículo à frente.
- Rastreamento entre quadros.
- Identificação do veículo relevante à trajetória.
- Avaliação de aproximação.
- Estimativa de risco considerando dados disponíveis.

## 9.2 Alerta de risco

Quando o risco for suficientemente confirmado:

- Destacar o veículo real com contorno vermelho piscante.
- Exibir alerta visual de risco de colisão.
- Emitir som de alerta.
- Emitir aviso de voz configurável.
- Registrar o evento.

## 9.3 Recuperação

Encerrar automaticamente o alerta quando:

- A aproximação perigosa cessar.
- O risco deixar de existir.
- O veículo rastreado sair da trajetória relevante.

## 9.4 Baixa confiança

Não declarar colisão iminente sem evidência suficiente.

A simples presença de um veículo próximo não comprova risco de colisão.

O cálculo pode considerar movimento relativo, expansão aparente do objeto, trajetória, velocidade e outros dados tecnicamente disponíveis.

A estimativa monocular de distância ou tempo até colisão possui limitações e exige calibração e testes.

## 9.5 Critério de aprovação

O FCW não estará concluído se apenas:

- Desenhar uma caixa fixa.
- Marcar qualquer carro de vermelho.
- Disparar alerta por botão de teste.
- Apresentar uma animação sem análise da câmera.

---

# 10. REGRA GLOBAL DE RECUPERAÇÃO E CONFIANÇA

Todos os detectores devem implementar estados explícitos, como:

- Sem detecção.
- Detecção candidata.
- Detecção confirmada.
- Risco confirmado.
- Recuperação.
- Indisponível ou confiança insuficiente.

**Regras obrigatórias:**

- Confirmação temporal antes dos alertas críticos.
- Rejeição de resultados inconsistentes.
- Encerramento automático quando o risco desaparecer.
- Proteção contra alertas repetitivos.
- Tratamento da perda de quadros da câmera.
- Ausência de alarmes vermelhos sem detecção confiável.

Limiares, frequências de piscar e duração dos alertas devem ser ajustáveis e submetidos à aprovação do proprietário antes de serem considerados definitivos.

---

# 11. RECONHECIMENTO DE SEMÁFOROS

## 11.1 Regra visual oficial

O proprietário forneceu duas imagens:

- **Luz verde real:** semáforo aberto.
- **Luz vermelha real:** semáforo fechado.

A LIVE deverá apresentar **apenas a luz correspondente**, brevemente no centro da tela.

Não mostrar:

- Semáforo inteiro.
- Popup retangular grande.
- Texto ocupando o centro.
- Botão OK.
- Mensagem falsa sem detecção real.

## 11.2 Funcionamento

O módulo deve:

1. Detectar semáforos na imagem.
2. Identificar qual semáforo é relevante ao sentido de circulação.
3. Reconhecer o estado.
4. Acompanhar o mesmo semáforo ao longo dos quadros.
5. Confirmar a mudança de estado.
6. Exibir brevemente a luz correspondente.

Transições aprovadas:

- Fechado → aberto: exibir luz verde.
- Aberto → fechado: exibir luz vermelha.

O proprietário aprovou explicitamente as referências vermelha e verde. Não adicionar novas representações visuais como padrão sem seu aval.

O alerta deve aparecer somente quando a detecção for confiável.

## 11.3 Em segundo plano

Se houver acesso real à câmera e suporte do Android:

- Manter a detecção.
- Enviar aviso compatível com as preferências de áudio/notificações.
- Não abrir a LIVE automaticamente.

Se a câmera estiver indisponível, não anunciar mudança de semáforo.

---

# 12. RECONHECIMENTO DE PLACAS

Desenvolver reconhecimento real de sinalização brasileira, priorizando:

- Limite de velocidade.
- PARE.
- Entrada proibida.
- Outras placas relevantes conforme evolução aprovada.

O módulo deverá:

- Localizar placas na imagem.
- Classificar o tipo.
- Reconhecer limite numérico quando aplicável.
- Confirmar resultados entre quadros.
- Evitar leitura de sinalização de vias não relacionadas.
- Associar eventos ao contexto da direção.
- Não confundir teste de popup com reconhecimento automático.

As gravações reais fornecidas pelo proprietário incluem exemplos de placas de 60 km/h.

Essas imagens são material de teste, não prova de que o detector já funciona.

---

# 13. PEDESTRES, CICLISTAS E OBSTÁCULOS

Implementar detecção real de:

- Pedestres.
- Ciclistas.
- Veículos.
- Objetos relevantes à área trafegável.

O sistema deve distinguir:

- Objeto visível.
- Objeto próximo.
- Objeto que representa risco.

Não disparar alerta crítico apenas porque existe uma pessoa na calçada ou um veículo estacionado.

Exigir associação à trajetória, estabilidade temporal e confiança suficiente.

---

# 14. EVENTOS FUNCIONANDO EM SEGUNDO PLANO

## REGRA OFICIAL

**Os eventos do aplicativo devem continuar funcionando mesmo quando a LIVE não estiver na tela, dentro das capacidades reais de cada versão do Android e hardware.**

Implementar arquitetura de serviço independente da interface.

## 14.1 Foreground Service

Implementar serviço persistente adequado para:

- Gravação.
- Monitoramento GPS.
- Processamento ADAS quando permitido.
- Gestão de eventos.
- Notificações.
- Alertas sonoros e voz.

Iniciar o serviço com as permissões e tipos exigidos pelo Android correspondente.

## 14.2 Continuidade

Quando o usuário abrir rádio, música, GPS ou outro aplicativo:

- Não encerrar indevidamente o monitoramento.
- Preservar gravações ativas quando o hardware permitir.
- Continuar eventos GPS.
- Continuar ADAS somente enquanto houver quadros reais.
- Preservar o estado da sessão.

## 14.3 Limitações obrigatórias

Implementar tratamento para:

- Restrição de câmera em segundo plano.
- Permissões.
- Suspensão por economia de bateria.
- Perda da câmera USB.
- Conflito com outro aplicativo que utiliza a câmera.
- Encerramento do serviço pelo sistema.
- Android moderno com restrições adicionais de inicialização.

Quando o acesso à câmera for perdido, suspender detectores dependentes do vídeo.

Não gerar eventos de colisão, faixas ou semáforo sem quadros reais.

---

# 15. ÁUDIO MIX E VOZ PREMIUM

O aplicativo é destinado também a centrais multimídia automotivas.

## 15.1 Requisitos

- Alertas em português brasileiro.
- Voz feminina natural conforme identidade aprovada.
- Sons premium.
- Volumes ajustáveis.
- Silenciamento independente da voz.
- Preferências persistentes.
- Não interromper permanentemente a música ou rádio.

## 15.2 Compatibilidade

O código já possui uma implementação inicial de MIX sem solicitar AudioFocus.

Contudo, o comportamento em rádios FM de fabricantes diferentes precisa de validação.

Prever modo visual sem voz quando a integração de áudio do dispositivo não permitir reprodução adequada.

Não utilizar efeitos sonoros sintéticos básicos como se fossem os masters premium definitivos.

---

# 16. GRAVAÇÃO DASHCAM

## 16.1 Recursos previstos

- Prévia LIVE real.
- Gravação MP4/H.264 conforme capacidade do hardware.
- Gravação contínua em segmentos.
- Configuração de duração dos segmentos.
- Exclusão circular dos clipes comuns mais antigos.
- Proteção dos eventos importantes.
- Gravação de emergência.
- Identificação do estado REC.
- Tratamento de falhas de armazenamento.

## 16.2 Informações nos vídeos

Conforme recursos disponíveis e permissões:

- Data.
- Hora.
- Velocidade GPS.
- Coordenadas.
- Identificação do dispositivo/câmera.
- Eventos associados.

Quando aprovado para o vídeo final, renderizar as informações no próprio arquivo e armazenar metadados adicionais.

Adicionar integridade por hash SHA-256 de arquivos protegidos, com registro verificável.

## 16.3 Eventos de impacto

Quando sensores compatíveis estiverem disponíveis:

- Detectar impacto conforme calibração.
- Proteger automaticamente o clipe relevante.
- Salvar informações do evento.
- Preservar gravação anterior/posterior se houver buffer implementado.

Não prometer gravação em estacionamento com equipamento desligado ou sem alimentação permanente.

---

# 17. RADARES GPS

A base possui cadastro manual de radares e monitoramento aproximado.

A versão comercial deverá assegurar:

- Banco local persistente.
- Cadastro, edição e exclusão.
- Identificação do limite de velocidade.
- Distância e direção aproximadas.
- Antirrepetição de alertas.
- Operação em segundo plano.
- Preferências de voz/som.
- Continuidade offline dos dados existentes.

Distinguir radar cadastrado de radar realmente identificado por visão computacional.

Não declarar uma base nacional completa quando ela não estiver disponível ou validada.

---

# 18. BACKUP GOOGLE DRIVE

Funcionalidade prevista para a versão comercial:

- Upload opcional de vídeos protegidos.
- Autenticação autorizada pelo usuário.
- Gravação local primeiro.
- Fila de uploads.
- Recuperação após falha de conexão.
- Suporte a Wi-Fi e dados móveis, conforme preferência aprovada.
- Indicação de estado do backup.
- Proteção de dados e consentimento explícito.

A implementação deve respeitar os requisitos atuais da API Google Drive e o controle de acesso do usuário.

---

# 19. TEMAS E PERSONALIZAÇÃO

Preservar a identidade Premium Luxury e os temas planejados:

- Luxury Blue — padrão.
- Carbon Sport.
- Executive Gold.
- Ice Silver.
- Stealth.
- Emerald.

As variações não podem prejudicar as cores funcionais dos alertas de segurança.

As cores dos riscos devem permanecer semanticamente reconhecíveis.

---

# 20. PRIVACIDADE, LICENCIAMENTO E USO COMERCIAL

Considerar:

- LGPD.
- Permissões de câmera e localização.
- Consentimento para backup.
- Armazenamento seguro.
- Retenção de vídeos.
- Transparência sobre processamento local.
- Licenciamento comercial de bibliotecas e modelos de IA.
- Requisitos da loja de distribuição, quando aplicáveis.
- Declaração clara das limitações de ADAS.
- Tratamento de logs sem exposição de informações sensíveis.

O ADAS é um sistema de assistência e não substitui a atenção do condutor.

Não afirmar capacidade de detecção infalível, cálculo preciso de distância sem calibração ou certificação de segurança inexistente.

---

# 21. ARQUITETURA DE CÓDIGO RECOMENDADA

Respeitar a organização existente sempre que possível, mas separar responsabilidades.

Estrutura lógica sugerida:

**camera/**
- CameraSource
- Camera2Source
- UvcSource
- ProprietaryCameraAdapter
- CameraCalibration
- CameraTransform

**vision/**
- VisionEngine
- LaneDetector
- ObjectDetector
- TrafficSignDetector
- TrafficLightDetector
- ObjectTracker

**risk/**
- LaneDepartureEvaluator
- ForwardCollisionEvaluator
- RiskStateMachine
- ConfidenceFilter

**alerts/**
- AlertManager
- AlertAudio
- AlertOverlay
- EventPriorityManager

**recording/**
- RecordingManager
- RecordingSegmentStore
- EmergencyClipProtector
- EvidenceMetadata

**background/**
- DashCamForegroundService
- CameraSessionManager
- SensorMonitor
- ServiceRecovery

**radar/**
- RadarStore
- RadarMonitor

**diagnostics/**
- DeviceCapabilities
- CameraDiagnostics
- CrashReporter

**ui/**
- MainActivity
- PremiumMenuActivity
- LiveControls
- FullscreenController

A organização final deverá preservar os módulos existentes e evitar duplicação de serviços ou captura concorrente.

---

# 22. PROBLEMAS REAIS JÁ IDENTIFICADOS

## Problema A — LIVE não abria

Erro comprovado de fundo aplicado a `TextureView`.

**Tratamento:** preservar a correção da V4.6.

## Problema B — Câmera girada

Vídeo apresentado em orientação incorreta em capturas reais.

**Tratamento:** revisar transformações e testar diferentes câmeras.

## Problema C — Tela cheia insuficiente

Necessidade de botão visível e retorno adequado.

**Tratamento:** validar controles FULL e SAIR FULL.

## Problema D — Controles ocupando espaço

Requisito de recolhimento automático.

**Tratamento:** implementar e validar animação e acessibilidade.

## Problema E — Popups inadequados

Algumas interfaces mostram avisos grandes e genéricos.

**Tratamento:** utilizar overlays Premium Luxury e os ícones aprovados.

## Problema F — ADAS incompleto

Faixas experimentais sem saída de faixa confiável; FCW ausente.

**Tratamento:** implementar motor de inferência, avaliação de riscos e alertas reais.

## Problema G — Integração USB não garantida

A câmera original da K2001N pode depender do protocolo UCarDVR.

**Tratamento:** investigar e implementar o backend compatível, respeitando permissões e licenças.

## Problema H — Segundo plano

Monitoramento e gravação não estão integralmente desacoplados da Activity.

**Tratamento:** implementar serviço robusto e gestão de sessão.

---

# 23. MATERIAL REAL DE TESTES DISPONÍVEL

O proprietário enviou vídeos e imagens de ruas reais, incluindo:

- `20261011_154413.mp4`
- `Video Project 1.mp4`
- Capturas VLC de vias urbanas.

As capturas mostram:

- Estradas retas.
- Curvas.
- Marcações descontínuas.
- Entradas laterais.
- Veículos estacionados.
- Pessoas próximas à pista.
- Placas de 60 km/h.
- Sombreamento por árvores.

Esses materiais deverão compor a primeira suíte de testes offline.

**Não declarar métricas de acerto com base apenas na inspeção visual das imagens.**

Realizar inferência real, registrar resultados e comparar as detecções com marcações de referência.

---

# 24. PLANO DE IMPLEMENTAÇÃO OBRIGATÓRIO

## Etapa 1 — Integridade da base

- Extrair o ZIP V4.8.
- Inspecionar Gradle e AndroidManifest.
- Conferir dependências.
- Verificar recursos visuais.
- Corrigir erros de código.
- Confirmar inicialização da LIVE.

## Etapa 2 — Captura e orientação

- Corrigir orientação.
- Calibrar transformações.
- Preservar proporção.
- Corrigir fullscreen.
- Conectar frames ao motor ADAS.

## Etapa 3 — Motor de faixas

- Substituir a dependência exclusiva do detector experimental.
- Implementar detecção e rastreamento.
- Calcular desvio lateral.
- Integrar LDW.
- Implementar vermelho piscante.
- Implementar áudio e recuperação.

## Etapa 4 — FCW

- Integrar detector de veículos.
- Implementar rastreamento.
- Avaliar risco frontal.
- Desenhar destaque no veículo real.
- Integrar alerta vermelho piscante.
- Implementar áudio e recuperação.

## Etapa 5 — Semáforos e placas

- Implementar detectores.
- Reconhecer estados.
- Integrar referências visuais.
- Confirmar transições.
- Bloquear falsos alertas.

## Etapa 6 — Outros objetos

- Pedestres.
- Ciclistas.
- Veículos laterais.
- Eventos relevantes.

## Etapa 7 — Segundo plano

- Serviço persistente.
- Gravação.
- GPS.
- Eventos ADAS.
- Notificações.
- Recuperação de falhas.

## Etapa 8 — Interface final

- Controles auto-recolhíveis.
- Menu 3D.
- Indicadores reais.
- Alertas refinados.
- Tema Premium Luxury.

## Etapa 9 — Validação

- Testes unitários.
- Testes com vídeos fornecidos.
- Testes de ciclo de vida Android.
- Testes de câmera.
- Testes de gravação.
- Medição de desempenho.
- Verificação de permissões.
- Revisão de segurança.

## Etapa 10 — BUILD DEBUG

**Compilar o aplicativo real e entregar o APK de teste.**

---

# 25. GERAR APK.BAT — REQUISITO OBRIGATÓRIO

O ZIP final deverá conter um arquivo executável:

`GERAR APK.bat`

O script deve:

1. Abrir na pasta do projeto.
2. Verificar Java/JDK.
3. Verificar Android SDK.
4. Utilizar o Gradle Wrapper incluído no projeto.
5. Executar a compilação Debug.
6. Salvar log de compilação.
7. Detectar erros de compilação.
8. Informar claramente o motivo de falha.
9. Copiar o APK gerado para uma pasta de fácil acesso.
10. Mostrar o caminho final do arquivo.

Comando principal esperado:

`gradlew.bat --no-daemon clean :app:assembleDebug`

Caminho padrão do APK:

`app/build/outputs/apk/debug/app-debug.apk`

Destino de entrega recomendado:

`APK_GERADO/DashCam_ADAS_PRO_DEBUG.apk`

Verificar a versão efetiva de JDK, Gradle, Android Gradle Plugin e SDK exigida pelo projeto. A documentação existente cita JDK 17, SDK Platform 35 e Gradle 8.9 ou compatível.

**Incluir no ZIP:**

- `gradlew.bat`
- `gradlew`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradle/wrapper/gradle-wrapper.jar`
- `GERAR APK.bat`
- Projeto Kotlin completo.
- Recursos visuais.
- Bibliotecas e arquivos de configuração necessários.
- Modelos de IA compatíveis com licenciamento, quando utilizados.
- Documentação.
- Relatório de testes.

---

# 26. CRITÉRIOS DE APROVAÇÃO TÉCNICA

O projeto não estará concluído apenas porque a interface foi exibida ou a compilação passou.

## LDW

- Identifica faixas reais.
- Acompanha curvas.
- Reconhece saída confirmada.
- Pisca a linha correspondente em vermelho.
- Emite alerta configurado.
- Para automaticamente.
- Não alarma com baixa confiança.

## FCW

- Detecta e acompanha o veículo real.
- Identifica situação de risco relevante.
- Destaca o veículo em vermelho piscante.
- Emite alerta configurado.
- Para automaticamente.
- Não dispara somente por proximidade aparente.

## Semáforos

- Reconhece estados reais.
- Identifica o semáforo relevante.
- Confirma alterações.
- Exibe somente luz verde/vermelha realista no centro da LIVE.
- Não inventa mudanças.

## Câmera

- Abre sem crash.
- Mantém orientação.
- Mantém proporção.
- Opera em FULL.
- Preserva alinhamento do ADAS.

## Segundo plano

- Serviço não depende da Activity.
- Continua funções permitidas pelo sistema.
- Trata perda de câmera.
- Não simula eventos.
- Mantém alertas adequados.

## Build

- Compilação Debug bem-sucedida.
- APK instalável.
- Sem erros fatais de inicialização nos dispositivos testados.
- Registro de build.
- Código-fonte ZIP íntegro.

---

# 27. MATRIZ DE TESTES COMERCIAIS

Testar, conforme equipamentos disponíveis:

| Cenário | Resultado esperado |
|---|---|
| Android 7.1 / K2001N | Abertura, diagnóstico e funcionalidades suportadas |
| Tablet Android com câmera interna | LIVE e orientação correta |
| Smartphone Android | LIVE responsiva |
| Câmera USB UVC compatível | Captura quando backend funcionar |
| Alternar para rádio/música | Serviço mantém funções permitidas |
| Voltar à LIVE | Sessão recuperada sem crash |
| Faixa reta | Projeção alinhada |
| Curva | Projeção acompanha a geometria |
| Linha apagada | Sem alerta vermelho falso |
| Veículo à frente | Rastreamento coerente |
| Aproximação perigosa confirmada | FCW |
| Risco encerrado | Alerta desaparece |
| Semáforo aberto | Luz verde breve |
| Semáforo fechado | Luz vermelha breve |
| Perda da câmera | Detectores suspendem eventos |
| Pouca RAM | Degradação controlada e sem travamento |

Os limites quantitativos de desempenho e acurácia deverão ser medidos, documentados e submetidos à aprovação antes da liberação comercial.

---

# 28. ENTREGÁVEIS EXIGIDOS DO CLAUDE

Ao concluir a implementação, fornecer:

**1. APK DEBUG**

`DashCam_ADAS_PRO_DEBUG.apk`

**2. CÓDIGO-FONTE INTEGRAL**

`DashCam_ADAS_PRO_FONTE_COMPLETA.zip`

**3. ARQUIVO PARA WINDOWS**

`GERAR APK.bat`

**4. RELATÓRIO TÉCNICO**

Informar:

- Funcionalidades implementadas.
- Arquivos modificados.
- Dependências adicionadas.
- Build executada.
- Resultado da compilação.
- Testes realizados.
- Problemas encontrados.
- Limitações de hardware.
- Pendências que exigem validação em campo.

**5. MATRIZ DE REQUISITOS**

Cada requisito deve indicar:

- Implementado.
- Testado.
- Parcial.
- Pendente.

Não omitir funções pendentes nem classificá-las como completas apenas por existir uma interface.

---

# 29. REGRAS SUPREMAS DO PROPRIETÁRIO

Estas regras prevalecem sobre propostas não aprovadas.

**REGRA 1 — NENHUMA FUNCIONALIDADE APROVADA PODE SER ESQUECIDA.**

Todos os requisitos deste documento devem integrar o planejamento e os resultados da implementação.

**REGRA 2 — NÃO TOMAR DECISÕES DE PRODUTO SEM APROVAÇÃO.**

Alterações de identidade visual, comportamento, tempos, regras de alerta ou escopo precisam de aval do proprietário quando não estiverem expressamente aprovadas.

Decisões técnicas internas necessárias à implementação devem ser documentadas e não podem contrariar os requisitos.

**REGRA 3 — ADAS REAL, NÃO DEMONSTRATIVO.**

Nenhum botão ou animação substitui a detecção verdadeira.

**REGRA 4 — LDW E FCW SÃO OBRIGATÓRIOS.**

Ambos devem possuir detecção, alerta e recuperação, além de comportamento seguro sob baixa confiança.

**REGRA 5 — COMPATIBILIDADE COMERCIAL MULTI-DISPOSITIVO.**

A K2001N é um perfil importante, não uma limitação do produto.

**REGRA 6 — ORIENTAÇÃO CORRETA DA CÂMERA.**

A imagem e os overlays devem permanecer alinhados.

**REGRA 7 — EVENTOS EM SEGUNDO PLANO.**

Implementar continuidade real até os limites técnicos permitidos pelo sistema.

**REGRA 8 — SEMÁFOROS COM AS LUZES REAIS APROVADAS.**

Somente a luz correspondente aparece brevemente no centro da LIVE.

**REGRA 9 — BUILD AUTORIZADA.**

A tarefa inclui expressamente gerar a build Debug de teste. Não suspender a compilação alegando que o usuário não autorizou build.

**REGRA 10 — TRANSPARÊNCIA TÉCNICA.**

Não anunciar funções como prontas sem implementação e testes verificáveis.

---

# 30. INSTRUÇÃO FINAL PARA O CLAUDE

Você atuará como engenheiro Android sênior, arquiteto de software, especialista em Camera2/UVC, visão computacional e desenvolvimento automotivo.

Receberá o código-fonte V4.8 do DashCam ADAS PRO e os materiais visuais aprovados.

**Sua tarefa não é elaborar outro planejamento genérico. É executar a implementação.**

Proceda da seguinte forma:

1. Inspecione todos os arquivos do projeto.
2. Identifique erros de compilação, arquitetura e funcionamento.
3. Preserve as implementações válidas.
4. Corrija definitivamente orientação, espelhamento, fullscreen e layout responsivo.
5. Integre o pipeline de frames reais.
6. Implemente LDW real com linha vermelha piscante, som, confiança e recuperação.
7. Implemente FCW real com rastreamento do veículo, risco frontal, contorno vermelho piscante, som e recuperação.
8. Implemente reconhecimento real de placas, semáforos, pedestres e ciclistas, conforme os requisitos.
9. Integre os serviços de background.
10. Preserve o áudio MIX e implemente os tratamentos necessários.
11. Finalize gravação, eventos, radares e demais funções descritas.
12. Execute testes de código e validação com vídeos reais.
13. Corrija os erros identificados.
14. **EXECUTE A COMPILAÇÃO DEBUG DO PROJETO ANDROID.**
15. Gere o APK Debug.
16. Gere o ZIP do código-fonte com `GERAR APK.bat`.
17. Entregue relatório técnico com evidências e limitações.

Não utilize argumentos relacionados a uma antiga proibição de build, pois a compilação de teste foi expressamente autorizada.

Não entregue código fictício, implementações vazias, detectores simulados ou resultados de teste inventados.

Não realize alterações de produto não autorizadas.

Quando uma limitação de hardware impedir determinada funcionalidade, implemente tratamento seguro e documente exatamente a restrição, sem ocultar o problema.

**OBJETIVO FINAL: DASHCAM ADAS PRO FUNCIONAL, ESTÁVEL, COMERCIALMENTE PREPARADO E COM APK DEBUG COMPILADO PARA TESTES.**

---

**FIM DO DOCUMENTO TÉCNICO MESTRE**
