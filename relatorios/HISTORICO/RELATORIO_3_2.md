# DashCam ADAS PRO — revisão 3.2 experimental

Data: 12/10/2026. versionCode 32, targetSdk 35, minSdk 24. K2000N informada como Android 7.1. Os dados adicionais recebidos sobre CPU/RAM são descrições de variantes, não diagnóstico físico desta central.

## Faixas dinâmicas

A revisão corrige a busca limitada a um pico por linha e a rejeição de algumas curvas por direção. O detector agora avalia picos em quatro larguras, coerência local direcional e textura do entorno; ajusta curvas por consenso e refina com mínimos quadrados ponderados. Pode acompanhar ambas as bordas curvando para o mesmo lado. As bordas são classificadas no trecho inferior, e pares que cruzam ou têm perspectiva incompatível são rejeitados.

A primeira observação tem confiança limitada, sem ativar LDW. É necessária concordância com a observação seguinte. Pequenas variações recebem suavização limitada ao suporte atual; perda de evidência apaga a borda imediatamente. O desenho usa apenas o intervalo vertical medido e confiança mínima 0,65. A ausência de uma borda nunca sintetiza outra. LDW também exige que a referência inferior esteja dentro do intervalo observado, evitando extrapolação para disparar avisos. FCW usa curvas apenas quando o alvo está no intervalo observado. Câmera orientada verticalmente suspende o processamento ADAS; a prévia e gravação permanecem disponíveis para corrigir a orientação.

Testes incluem faixa escura por contraste, curva para a direita com ambas as bordas no mesmo sentido, marcação larga/tracejada, pontos claros isolados, textura aleatória, perda de evidência, reconfirmação e bloqueio de extrapolação. Erros de coordenadas são medidos nas cenas sintéticas. Esses testes não medem precisão em estrada.

No vídeo fornecido, o processamento RGB compartilha a classe AdasEngine usada no APK: 82 quadros reais, 40 com candidatos a marcações e zero pares confirmados com ambas as confianças ≥0,65. Houve três candidatos a veículos. Sem GPS/calibração, nenhum risco crítico foi produzido. Não há ground truth, recall, precisão ou validação em trânsito. A cobertura de pares continua insuficiente para afirmar LDW operacional. Resultados em relatorios/3_2/offline; latência de SSD mede apenas o host da nuvem.

## Opções de áudio

Seis padrões PCM: suave, duplo, triplo, ascendente, descendente e alternado. Frequência base 400–1800 Hz, cada tom 80–240 ms, intervalo entre tons 60 ms, volume separado de voz e rampa para evitar clique. A reprodução usa fila serial com rejeição de sobreposição dentro da instância, sem solicitar AudioFocus. O botão de prévia testa a configuração selecionada antes de salvar. Testes verificam formas distintas, ganho/silêncio, clipping, intervalo silencioso e frequência por cruzamentos de zero.

Não houve reprodução em alto-falante, Bluetooth, Spotify/Waze ou rádio FM físico nesta sessão. MIX sem foco de áudio não garante mistura com o caminho FM proprietário da central.

## Referência nativa do APK

APK com.ankai.cardvr, versão 20250515.3, targetSdk 21; contém somente lib/armeabi-v7a/libmycamera.so, ELF32 ARM. Foram reproduzidos 25 descritores JNI e 23 métodos de callbacks com os nomes de classe originais necessários ao RegisterNatives. Nenhum classes.dex do outro app foi incorporado. Hashes, ABI e proveniência em referencias_nativas/.

O APK genérico não carrega nem inclui a biblioteca legada. A compilação -Pk2000Native=true inclui o binário copiado sem alteração e restringe todas as bibliotecas a armeabi-v7a. Não tenta carregar ARM32 em processo ARM64. O checker build-tools/verificar_referencia_nativa.py verifica os descritores no DEX final, callbacks, ABI e hashes de modelo/driver; não executa o driver ARM.

A tela de teste USB utiliza UsbManager com autorização explícita, abre o descritor do dispositivo, carrega a biblioteca, cria sessão UFD como no APK original, configura isTest=false e conecta a prévia à superfície. Aguarda inicialização real antes de solicitar START_PREVIEW, com timeout limitado. Fecha sessão/descritor ao sair ou desconectar e oferece girar/espelhar. Está habilitada para Android 7/8, evitando assumir compatibilidade do driver com restrições de APIs privadas das versões posteriores. Um buffer atualizado na superfície é indicado como atualização, não como comprovação de câmera funcionando.

A prévia USB é um teste de integração, **não integra ainda gravação nem LDW/FCW USB**. A versão atual mantém esses recursos no serviço Camera2. Não houve teste físico de carregar o driver, decodificar UFD/UVC, combinar áudio/FM, recuperar USB ou gravar na K2000N. A licença geral de redistribuição da biblioteca do APK não foi identificada; não afirmar autorização comercial.

## Interface

Botões nas páginas passam a usar altura mínima em dp e conteúdo expansível, corrigindo a restrição de 70 pixels que podia cortar rótulos em telas densas. Insets reservam as barras e recortes mesmo quando escondidos, e o texto de estado ADAS foi movido para abaixo dos controles superiores. A prévia e os overlays usam o mesmo mapeamento de espelhamento/FIT/FILL; orientação é corrigida por câmera e invalida a calibração anterior.

## Validação e pendências

54 testes JVM aprovados, sem falhas/erros/skips. Build e lint dos dois perfis foram executados; resultados atuais em relatorios/3_2. As assinaturas, alinhamento ZIP e empacotamento JNI são verificados na entrega. Nenhum teste físico aprovado: a lista ADB não apresentou aparelhos. Scripts Windows preparados, execução Windows não realizada.

A gravação Camera2 continua no serviço foreground independente da LIVE, com segmentos e proteção; o relatório 3.1 preservado em relatorios/HISTORICO documenta a implementação anterior. Continuidade real da gravação, GPS, câmera, interface, áudio e compatibilidade nativa exigem teste no equipamento. Placas/semáforos/pedestres/ciclistas com alertas completos permanecem fora desta revisão; páginas demonstrativas são identificadas como testes.
