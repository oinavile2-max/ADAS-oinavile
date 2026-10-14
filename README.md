# DashCam ADAS PRO — 3.9, DVR USB da NWD

Conceito aprovado aplicado ao código Android: cards em vidro escuro, tipografia legível, controles em cores neon e banners de evento com neon vermelho. Configurações em quatro abas; mídia, eventos e câmera USB seguem a mesma identidade. Novo ícone profissional com versões clássica, adaptativa e monocromática.

**Atualização 3.9 para o DVR da NWD.** O USB **3322:6655** mostrado no diagnóstico corresponde ao filtro UFD do APK original. O app agora oferece esse dispositivo e implementa o transporte pela interface de armazenamento, com leitura e comandos no endpoint da câmera. **058F:6387** continua excluído da seleção de câmera. Camera2 pode continuar retornando SecurityException; o caminho nativo é independente dessa API.

APKs NWD ARM32 e genérico com versionCode 39, minSdk 25 e targetSdk 37. Evidências em `relatorios/build-3_9`; os checks exigem testes reais dos modelos e alinhamento das bibliotecas de 64 bits. 118 testes passaram por perfil, com zero falhas/erros/skips; lint teve 101 avisos e zero erros por perfil. Assinaturas V1/V2, certificado anterior, modelos embarcados e seis bibliotecas ELF64 do genérico verificados. O driver original mantém seu hash. Primeira imagem, gravação e desempenho nessa central ainda precisam de teste físico; reconhecer o USB não comprova vídeo disponível.

## Downloads

- [APK 3.9 genérico — celular/tablet](https://github.com/oinavile2-max/ADAS-oinavile/raw/refs/heads/main/APKS/DashCam_ADAS_PRO_3_9_NEURAL_GENERIC_DEBUG.apk)
- [APK 3.9 NWD K2001N — ARM32](https://github.com/oinavile2-max/ADAS-oinavile/raw/refs/heads/main/APKS/DashCam_ADAS_PRO_3_9_NWD_K2001N_ARM32_NEON_DEBUG.apk)
- [Código-fonte atualizado 3.9](https://github.com/oinavile2-max/ADAS-oinavile/raw/refs/heads/main/DashCam_Fonte.zip)
- [APK 3.4 NWD K2001N ARM32 — entrega anterior](https://github.com/oinavile2-max/ADAS-oinavile/raw/refs/heads/main/APKS/DashCam_ADAS_PRO_3_4_NWD_K2001N_ARM32_NEON_DEBUG.apk)
- [APK genérico 3.3 — histórico](https://github.com/oinavile2-max/ADAS-oinavile/raw/refs/heads/main/APKS/DashCam_ADAS_PRO_3_3_NEURAL_GENERIC_DEBUG.apk)

Os APKs históricos 3.3/3.4 não contêm os temas e as correções das revisões 3.6–3.8 nem a atualização das bibliotecas. O genérico 3.3 histórico não suporta páginas de memória de 16 KB.

## Testar o DVR na NWD K2001N

1. Instale o **APK 3.9 NWD ARM32**, substituindo a versão anterior.
2. Feche o outro aplicativo DVR para liberar a interface USB.
3. Na LIVE, abra **CÂMERA → USB NATIVO • 3322:6655** e autorize o Android.
4. Aguarde a imagem real. REC permanece desabilitado enquanto a captura não estiver conectada.
5. Se surgir erro de endpoint/cartão/mode DVR ou de leitura, toque na mensagem de status para ver o texto completo e compartilhe o diagnóstico.

O diagnóstico mostra autorização USB por dispositivo e identifica o DVR do filtro original. Não tenta abrir armazenamento comum como câmera. O transporte usa SCSI/FAT32 e os nomes akfile/elene/kkfile/eefile/jrfile do SDK; procura arquivos existentes na raiz ou na pasta de dados do aplicativo original. Não cria arquivos nem formata o cartão. Implementação e limites em [proveniência nativa](referencias_nativas/PROVENIENCIA.md).

## Controles e painéis — correções 3.8

**REC / PARAR** agora tem botão textual na câmera principal, com indicação vermelha, disponível também em tela cheia e quando o rodapé é recolhido. Só é habilitado após conexão da fonte. Pressionar inicia/encerra a gravação do serviço; pressionar e segurar mantém a proteção de clipe existente. O pequeno ícone do cabeçalho continua como atalho.

**GIRAR** fica na câmera principal e aplica um quarto de volta aos dois painéis. O menu CÂMERA oferece Restaurar orientação automática. Na prévia direta, a orientação que o Android já aplica à TextureView é compensada antes de aplicar a orientação escolhida; o vídeo não deve receber duas rotações nem escalas diferentes em cada eixo. Transições de tamanho restauram a resolução da superfície. Em API31+, rotate-and-crop é desativado quando o dispositivo informa suporte ao modo NONE. Resultados da orientação anterior são descartados.

O botão **LIVE / ANÁLISE** no painel direito alterna entre vídeo contínuo (padrão) e o quadro realmente analisado, com suas caixas e faixas. A rede continua executando em ambos os modos. ANÁLISE acompanha a velocidade da inferência e pode atualizar mais devagar; não é promessa de IA a 30 fps. O modo LIVE não congela ao receber uma inferência demorada nem desenha caixas de outro instante.

Painéis internos adaptam o cabeçalho à altura disponível, preservam a rolagem e afastam o formulário do teclado. Títulos de cards quebram linha dentro da largura; a linha de status da LIVE não corta o texto verticalmente e permite consultar a mensagem completa com um toque. ADAS na LIVE abre seu painel, e CONFIG no menu antigo abre as configurações. Diálogos neon têm largura ajustada à tela e padding interno; recolher controles não esconde o painel auxiliar.

Instalação, orientação final, toques e fluidez precisam de confirmação física no aparelho do usuário. O relato de bugs nos menus não incluiu tela ou sequência específica até esta revisão; os ajustes tratam os cortes e rotas identificados no código e na captura fornecida.

## Fluidez da LIVE — câmera integrada

Correção motivada pelo teste em celular/tablet com câmera integrada e APK 3.3, que travava na LIVE mesmo sem gravar. O código limitava a prévia a cinco quadros por segundo e ignorava imagens novas enquanto havia um resultado recente da IA. Esses bloqueios foram removidos; desempenho físico ainda não medido.

Camera2 envia a imagem principal diretamente para TextureView pela GPU, sem converter cada quadro em bitmap para a LIVE. Uma saída menor continua alimentando as redes neurais. Se o aparelho rejeitar a combinação de saídas, o serviço tenta novamente com a prévia alternativa. Rotação, espelhamento e preencher/ajustar são preservados; sair da LIVE remove somente sua superfície de exibição.

A prévia alternativa recebe o quadro mais recente, com uma única atualização pendente. **CONFIG → Câmera e gravação → Fluidez da LIVE** oferece 10/15/24/30 quadros/s, padrão 15. Esse limite se aplica à alternativa em bitmap e ao USB; a prévia direta acompanha a câmera, sem promessa de 30 fps em qualquer aparelho. O painel auxiliar em LIVE acompanha essa cadência independentemente da IA. Camera2 prefere uma saída de análise próxima de 320×180 quando suportada, mantendo a proporção da gravação e evitando quadros menores se houver opção adequada. A prévia principal direta mantém sua resolução independente.

A IA continua executando os modelos locais. Seu ritmo se adapta à duração da análise, e aparelhos com até quatro núcleos ou até 2,5 GiB de RAM usam uma thread por modelo. Buffers de conversão e leitura de pixels são reutilizados, preservando bitmaps imutáveis para exibição. Resultados antigos nunca substituem o vídeo principal. No modo ANÁLISE do painel direito, as linhas e caixas aparecem sobre o próprio quadro analisado; não são sobrepostas a um quadro mais novo. No caminho alternativo, a sobreposição principal só ocorre quando os timestamps coincidem. Avisos atrasados continuam suspensos pelo limite de 500 ms.

Diagnóstico debug registra quadros recebidos, atualizações efetivamente desenhadas, FPS da prévia, pausas, tempo de conversão e inferência. O roteiro físico agora permite medir **LIVE sem gravação**:

```sh
python3 build-tools/test_android_device.py --serial SERIAL --live-only --min-preview-fps 12 --min-auxiliary-fps 12 --output resultados-live
```

Exige o APK 3.8 instalado e um aparelho físico autorizado por ADB. Nenhum aparelho está conectado nesta nuvem; taxa de quadros, estabilidade da SurfaceTexture, precisão em estrada e gravação durante transições ainda precisam de validação real. As versões 3.3/3.4 publicadas não recebem essas mudanças automaticamente.

## Temas neon

Em **CONFIG → Aparência**, escolha ciano, violeta, rosa, verde, laranja ou vermelho. Ajuste o tom entre −30° e +30° e a transparência do fundo dos botões e popups entre 0% e 80%. A prévia muda antes de salvar; textos, ícones e bordas não são apagados pela transparência. Salvar aplica o tema às telas e à LIVE, com preferências persistidas.

**Popups comuns acompanham o tema** é opcional e vem desativado para manter o vermelho aprovado. Habilitar aplica a cor escolhida a HUD, diálogos, listas e avisos comuns. Banners de evento continuam vermelhos; fundos dos avisos mantêm uma opacidade mínima nominal de 75%. O botão Restaurar cores e transparência prepara o padrão; pressione Salvar para persistir.

[Prévia dos temas](design/comercial/temas-neon.png) · [Maquete interativa](design/comercial/temas-neon.html). Renderizadas no navegador, não são capturas Android. **89 testes aprovados por perfil na revisão 3.6**, com lint sem erros; logs históricos em `relatorios/temas-neon`. Essa etapa visual 3.6 foi entregue somente em código.

## Android 7.1 até o Android atual

A configuração atual usa **minSdk 25 (Android 7.1)**, **compileSdk 37.2** e **targetSdk 37**, sem maxSdk. O SDK 37.2 foi conferido no canal estável oficial. Android Gradle Plugin 9.4.1, Gradle 9.6.0 e JDK 17. ONNX Runtime Android 1.31.0 e LiteRT 1.4.2 preservam suporte a API 25 e ao Interpreter existente.

| Perfil | Arquiteturas | Fonte de câmera |
|---|---|---|
| Geral, padrão | ARM32, ARM64, x86 e x86_64 | Câmeras disponíveis via Camera2 |
| NWD K2001N, `-Pk2000Native=true` | ARM32 | Camera2 ou driver USB específico, somente Android 7/8 |

O nome `k2000Native` é histórico; a central do usuário é NWD K2001N, Android 7.1.1. O driver original não fornece suporte universal a USB em Android moderno. Compatibilidade de uma câmera externa depende do sistema expô-la ou de um driver apropriado.

As bibliotecas de 64 bits são verificadas quanto a segmentos ELF de 16 KB pela tarefa `verifyNativePageAlignment`, obrigatória antes de futuros empacotamentos. A LIVE usa navegação de voltar compatível com gestos modernos. Inferência permanece local: permissão INTERNET e inicializador de telemetria do runtime são removidos do manifesto, e a sessão neural desativa telemetria.

## Interface e ajustes reais

CONFIG na LIVE abre diretamente as quatro abas:

- **Assistente de faixa:** sensibilidade 0–100%, espessura 1–12 dp, exemplo visual do traço, calibração e limites avançados recolhíveis.
- **Áudio e alertas:** modo de aviso, volume, seis padrões reais de bip, frequência, duração e reprodução do bip selecionado.
- **Câmera e gravação:** segmentos de 1/3/5 minutos, quota de armazenamento, limite da prévia alternativa, orientação/seleção na LIVE e USB no perfil compatível.
- **Aparência:** modo noturno (brilho da janela do app), animações decorativas do HUD, recolhimento dos controles e informações do aparelho.

O salvamento é específico da aba; alternar abas preserva rascunhos. Valores avançados inválidos rejeitam a gravação inteira dos limites ADAS. Ajustes de risco continuam bloqueados em movimento. Câmera, redes, alertas e gravação mantêm os serviços existentes.

Os dados do HUD e caixas são reais. O quadro direito usa o mesmo bitmap analisado. Sensibilidade não dispensa GPS/calibração/confiança; espessura altera somente o desenho. O aviso de pedestre permanece experimental. Semáforos, placas e ciclistas com alertas completos continuam pendentes.

[Conceito e implementação](design/comercial/LEIA-ME.md) · [Prévia do ícone](design/comercial/icone-profissional.png) · [Relatório técnico](RELATORIO_TECNICO.md).

## Verificar sem gerar APK

```sh
./gradlew --no-daemon --max-workers=2 :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug :app:verifyNativePageAlignment
```

Para o perfil da central, acrescente `-Pk2000Native=true`. O teste de faixas executa os pesos reais em CPU. `verifyNeuralModelAssets` confere os dois modelos. Nenhum APK é empacotado por esse comando.

O build usa `:app:assembleDebug`, sujeito aos testes neurais, hashes e verificação das bibliotecas. Windows: `GERAR APK.bat` e `GERAR APK K2000N.bat` exigem SDK Platform 37.2 e Build Tools 36.0.0; execução Windows não validada nesta nuvem.

## Validação restante

Sem aparelho conectado: instalação, renderização, GPS, desempenho/RAM/temperatura, driver USB, áudio/FM e gravação com tela apagada continuam pendentes. Não há comprovação pixel a pixel das capturas Android. As prévias são HTML/SVG, e os ensaios neurais no host não medem precisão em estrada ou latência ARM32. `build-tools/test_android_device.py` prepara a verificação física quando houver aparelho autorizado.

O erro de instalação anterior não teve causa identificada. Os APKs 3.8/3.7/3.4/3.3/3.2 usam o mesmo certificado debug; builds em outro computador podem usar um certificado diferente. `adb install -r arquivo.apk` fornece o diagnóstico detalhado.

YOLOP preserva licença MIT; SSD, Apache-2.0. Proveniências dos modelos e do driver estão no repositório. A licença comercial do driver fornecido continua sem confirmação.
