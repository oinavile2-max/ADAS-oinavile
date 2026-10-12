# Relatório técnico — entrega parcial, 11/10/2026

## Resultado verificável

Ambiente Android preparado: JDK Temurin 17.0.16, Gradle 8.9, Android Gradle Plugin 8.7.3, Kotlin 2.0.21, SDK Platform 35 e Build Tools 35.0.0.
O APK Debug da base DashCam foi compilado e a assinatura APK v2 foi verificada por apksigner. A inspeção por aapt confirmou br.com.dashcamadas, minSdk 24, targetSdk 28, Activity MAIN/LAUNCHER MainActivity e application-debuggable.
Isso verifica a estrutura e assinatura do APK; **não comprova instalação ou inicialização em Android**. Não houve teste em dispositivo, emulador, câmera ou pista.

Nove testes JUnit executados: seis de coordenadas e três de armazenamento; zero falhas, erros ou testes ignorados. Os testes cobrem rotação, centro, espelhamento, FIT/FILL, transformação inversa, SHA-256 conhecido, preservação de clipe ativo/protegido e exclusão dos clipes antigos.
Não são testes de inferência ADAS. Nenhuma métrica de acurácia foi produzida.

O lint falhou por ExpiredTargetSdkVersion: targetSdk 28, já presente no ZIP. Não foi desabilitado nem criada baseline. Migração para o alvo de publicação vigente exige ajustes de permissões, câmera, serviços e armazenamento e validação Android; não foi feita nesta entrega. Avisos adicionais constam no relatório de lint.

## Mudanças realizadas na base enviada

- Gradle Wrapper completo incluído; distribuição 8.9 e checksum SHA-256 obrigatório.
- GERAR APK.bat usa Wrapper, verifica presença de Java/javac, SDK Platform 35 e Build Tools 35.0.0, registra build-debug.log, preserva o código de saída e copia o APK para APK_GERADO. Seu fluxo Windows **não foi executado**, pois não há Windows neste ambiente.
- ImageReader YUV_420_888 como entrada real de análise, com acquireLatestImage, fechamento dos quadros e processamento fora da UI. Seleciona resoluções pequenas de proporção compatível. Há tentativa de prévia/gravação sem análise quando a combinação de streams não é aceita. Essas rotas precisam de testes Camera2 no hardware.
- Detector experimental original foi preservado; não foi convertido em LDW de segurança. Frames são orientados antes da análise e suas coordenadas voltam ao quadro original para projeção. Resultados de sessões anteriores são descartados e a projeção expira quando deixam de chegar quadros.
- CameraTransform compartilhado na projeção do overlay, com rotação, FIT/FILL, espelhamento e inversa. Preferências FIT/FILL, rotação e espelhamento são salvas por câmera; FULL preserva FIT/FILL e Voltar retorna da tela cheia.
- Recolhimento de controles configurável: desativado/5/10/15/30 s. O padrão permanece desativado; o tempo definitivo precisa de aprovação. FULL e configurações permanecem acessíveis. Não houve teste visual de layout em aparelhos.
- HUD distingue REC OFF e gravação iniciada; callbacks de sessões obsoletas são rejeitados, fechamento da câmera evita recriar a prévia ao sair, e listener GPS é removido ao destruir a Activity.
- Gravação escolhe tamanho anunciado pelo MediaRecorder e aplica orientação; falha com armazenamento insuficiente. Isso ainda requer teste real de MP4, orientação e segmentos.
- Limite configurável para exclusão de clipes comuns antigos, desativado por padrão, com preservação do clipe ativo e da pasta de protegidos.
- Hash SHA-256 lateral para clipes protegidos. O algoritmo foi testado; o fluxo Android de proteção precisa de teste de integração.

Arquivos originais alterados: GERAR APK.bat, app/build.gradle.kts, MainActivity.kt, LaneAssistView.kt e SettingsActivity.kt.
Arquivos adicionados: CameraTransform.kt, ClipStorage.kt, CameraTransformTest.kt, ClipStorageTest.kt, gradlew, gradlew.bat, gradle/wrapper/* e documentação de entrega.
Dependência adicionada: junit:junit:4.13.2, somente testes. Nenhum modelo de IA, biblioteca de câmera proprietária ou backend UVC foi incorporado.

## Pendências que impedem concluir o pedido integral

LDW operacional com curvas, calibração, confiança temporal, alertas e recuperação; FCW com detector e rastreador real; pedestres, ciclistas, placas e semáforos; serviço independente de câmera/gravação/GPS em segundo plano; captura UVC/proprietária; backup Drive; masters de áudio; validação comercial e migração do targetSdk continuam pendentes. O NoOpAdasEngine original continua presente e não representa inferência real. Os botões de teste existentes continuam identificados como TESTE.

Só foram recebidos o ZIP V4.8 e o documento mestre. Os vídeos 20261011_154413.mp4 e Video Project 1.mp4, as referências aprovadas das luzes vermelha/verde e um modelo com licença comercial não chegaram ao ambiente. A resposta “sim” autorizou a continuidade, mas não forneceu esses arquivos. Eles são necessários para a validação offline e a integração das referências aprovadas. Integração proprietária requer protocolo/documentação e equipamento; Drive precisa de configuração OAuth e consentimento no aplicativo.

A matriz de requisitos lista conservadoramente os requisitos do documento. “Parcial” indica que existe uma base ou mudança de código, mas o comportamento completo não foi validado. “Pendente” não deve ser interpretado como bloqueado exclusivamente por materiais: há implementação restante além das validações.

## Reprodução

Na nuvem: source /workspace/cloud-setup/env.sh; cd /workspace/DashCam_ADAS_PRO_V4_8_FAIXAS_EXPERIMENTAL_FONTE; ./gradlew --no-daemon --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process :app:assembleDebug :app:testDebugUnitTest
Lint: ./gradlew --no-daemon :app:lintDebug (falha conhecida de targetSdk, não ocultar).
Windows: extraia o ZIP, configure JDK 17 e ANDROID_HOME e execute GERAR APK.bat. Não distribua este APK como produto comercial concluído.

## Ambiente selecionado originalmente

/workspace/apk-manage-chilenoAPPS: assembleDebug e lintDebug passaram; testDebugUnitTest retornou NO-SOURCE (não há testes). Os arquivos versionados ficaram inalterados. Acesso Git de leitura foi confirmado pelo proxy existente.
install_script e start_skill foram salvos no rascunho. A instalação foi repetida com sucesso. Não houve publicação nem validação em nova tarefa. Revise e salve as configurações do ambiente e publique pela interface para ativar o snapshot.
ADB não foi validado: a versão instalada tenta criar /home/agent/.android, fora das raízes graváveis da sandbox. Compilação e SDK usam os diretórios graváveis em /workspace; conexão a dispositivo permanece sem verificação.
