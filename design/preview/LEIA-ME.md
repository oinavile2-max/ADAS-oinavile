# Prévia da interface — histórico visual e build 3.4

**Atualização:** o usuário autorizou o build posteriormente. A versão 3.4 compilou, com 81 testes aprovados e lint sem erros. O registro abaixo descreve a etapa anterior sem build; capturas continuam sendo maquetes, não screenshots Android. Evidências atuais: ../../relatorios/3_4.

Referências: imagens fornecidas pelo usuário em 12/10/2026. Central: NWD K2001N, Android 7.1.1.

`popups.html` é uma maquete interativa: abra no navegador e altere velocidade, GPS e evento. `popups.png` mostra os dois componentes. Os ícones vetoriais vêm do mesmo premium_ui_icons.json utilizado no código Android. Esta captura foi renderizada em HTML/SVG; não é uma captura do APK.

O popup 1 contém arco de velocidade, estrada, veículo, GPS e estado do sensor. No Android os valores vêm dos serviços e de eventos reais do acelerômetro; ausência de dados produz `--`. Resolução exibida corresponde à configuração de vídeo comunicada pelo serviço; 4K/HDR não é afirmado sem suporte real. O exemplo 68 km/h, GPS OK e 4K HDR pertence exclusivamente à maquete.

O popup 2 usa canvas proporcional de 752×194, retângulo interno de 720×162, raio de 19, borda luminosa e ícone vetorial próprio do evento. O radar fixo exibe RADAR FIXO À FRENTE e o limite cadastrado. O título/ícone do radar é preservado pelo evento estruturado, sem tentar inferir o tipo por uma mensagem de voz. Eventos ADAS críticos usam vermelho e têm prioridade sobre aviso de radar. Na revisão vermelha, a classe pessoa do SSD COCO foi ligada ao fluxo real; o aviso experimental de pedestre ainda não teve build/teste Android. Semáforos e ciclistas com alertas completos continuam fora do fluxo neural.

A LIVE segue as posições da referência 1679×937: quadro principal, quadro ADAS lateral, popup 1 flutuante e seis botões inferiores. O quadro lateral recebe o mesmo quadro real analisado; não representa uma segunda câmera. Faixas/caixas reais recebem contorno luminoso. Gravação permanece acessível no botão superior esquerdo; toque longo protege o clipe. No ícone de câmera lateral estão seleção, rotação, espelhamento, faixas, gravação e tela cheia.

Validação nesta etapa: renderização da maquete e alteração dos estados GPS/colisão/faixa no navegador, sem erros JavaScript; revisão de código/XML/JSON e verificação de whitespace. Um parser Kotlin externo aceitou os novos componentes e arquivos principais; tem limitações nos dois serviços, também presentes na revisão anteriormente compilada. Nenhuma compilação, lint Gradle, teste JVM/Android ou instalação foi executada nesta revisão por instrução expressa do usuário. Os resultados de 73 testes e os APKs da versão 3.3 são anteriores a esta alteração visual. Identidade pixel a pixel no Android não foi comprovada.

## Revisão de neon vermelho e assistente

A instrução posterior do usuário substitui a borda âmbar por **neon vermelho em todos os popups do aplicativo**: HUD, banner de evento, banners de teste, diálogos de seleção/confirmação/calibração, listas suspensas e avisos de preferências. As permissões USB/câmera continuam sendo janelas do próprio Android.

A tela direita mantém a identidade da LIVE e o mesmo quadro real. Durante risco confirmado, acompanha o alvo com recorte limitado à imagem e contorno vermelho com brilho; o mapeamento é compartilhado entre bitmap e caixa. Ao perder o alvo/dados recentes, o destaque é removido. Faixas ciano, seta em perspectiva e marcador sob o veículo seguem as referências posteriores. Seta/corredor exigem duas curvas realmente observadas; não são desenhados a partir de um modelo de estrada fictício.

Sensibilidade LDW: 0–100%, padrão 50%, margem de proximidade da borda ±0,06 da largura da faixa. Espessura: 1–12 dp, padrão 4 dp; altera somente o desenho das linhas/seta, sem reduzir confiança. Os controles estão em CÂMERA → Sensibilidade e espessura das faixas, com salvamento específico de limites ADAS. Ajustes são bloqueados quando o GPS informa movimento. GPS, calibração, confiança e confirmação temporal continuam necessários para alertas.

Pessoa: o mesmo SSD faz uma única inferência e separa veículos (2/5/7) de pessoas (0). Aviso experimental exige pessoa com confiança >=0,6, suporte da máscara de área dirigível, região à frente, GPS >=3 m/s, calibração e confirmação temporal. Troca de pessoa reinicia confirmação; não assume distância em metros. O vídeo recebido foi analisado no host com 27 execuções reais do SSD e produziu um candidato pessoa de confiança 0,5, abaixo do limiar de aviso. Não há referência anotada para medir precisão.

Regra para futuros builds: verifyNeuralModelAssets confere hashes dos dois modelos; empacotamento/assemble debug e release dependem de testDebugUnitTest, que contém teste de inferência real YOLOP. Novos testes de separação de classes, pedestre/GPS/máscara, sensibilidade e projeção com foco foram preparados, **não executados**. A ligação das tarefas Gradle também não foi executada nesta etapa. Sem build, não há confirmação semântica da compilação, funcionamento Android nem identidade pixel a pixel.

assistente.html demonstra o quadro direito e os controles em um cenário vetorial ilustrativo. Os alvos e valores da maquete não são detecções reais. Capturas de rua e frames originais do vídeo não foram publicados nesta branch.
