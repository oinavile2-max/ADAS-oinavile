# DashCam ADAS PRO — 3.2 experimental

Android: minSdk 24 (Android 7.0+), targetSdk/compileSdk 35. Compilação: JDK 17, Gradle 8.9, SDK Platform 35 e Build Tools 35.0.0.

## Downloads

- [Código-fonte completo em ZIP](https://github.com/oinavile2-max/ADAS-oinavile/raw/refs/heads/main/DashCam_Fonte.zip)
- [APK K2000N ARM32 de teste](https://github.com/oinavile2-max/ADAS-oinavile/raw/refs/heads/main/APKS/DashCam_ADAS_PRO_3_2_K2000N_ARM32_DEBUG.apk): inclui a biblioteca do APK fornecido; prévia USB nativa habilitada no Android 7/8 (API 24–27), sem validação física.
- [APK genérico de teste](https://github.com/oinavile2-max/ADAS-oinavile/raw/refs/heads/main/APKS/DashCam_ADAS_PRO_3_2_GENERIC_DEBUG.apk): tablets/celulares, Camera2, ARM32/ARM64/x86; sem o driver legado.

Ambos são APKs debug assinados, não versões comerciais validadas. Instale somente o perfil adequado ao aparelho. A versão informada da K2000N foi Android 7.1; processador/câmera/VID/PID ainda não confirmados. Consulte o diagnóstico do aparelho no aplicativo.

## O que mudou

As faixas usam contraste em várias larguras, coerência direcional, rejeição de textura e consenso quadrático. Curvas para ambos os lados, inclusive com ambas as bordas curvando na mesma direção, são aceitas. O traçado só aparece após confirmação temporal e dentro dos pixels observados. A ausência de uma borda não cria uma faixa artificial. Câmera vertical suspende ADAS até corrigir a orientação.

Áudio: bip suave, duplo, triplo, ascendente, descendente e alternado, com frequência base, duração e volume ajustáveis. Abra **CONFIG → CONFIG → CONFIGURAR ÁUDIO E PADRÕES DE BIP**, escolha um padrão e pressione **OUVIR BIP SELECIONADO**; **SALVAR** mantém a escolha. MIX não solicita foco de áudio. Mistura com rádio FM depende do firmware e ainda precisa de teste.

Na K2000N ARM32, abra **TESTAR PRÉVIA USB NATIVA** na configuração, selecione a câmera e autorize o acesso USB. A sessão Camera2 será encerrada, finalizando eventual clipe, para iniciar esse teste. Esta tela usa a biblioteca copiada e o contrato JNI reconstruído; **não integra gravação/LDW/FCW ao USB do driver**. Esses recursos continuam no caminho Camera2. A prévia USB precisa de teste físico de compatibilidade.

Os botões das páginas de testes agora têm altura mínima em dp e crescem com o texto. Insets reservam espaço para barras/cortes da tela, inclusive quando a barra reaparece.

## Compilar

- Windows, genérico: `GERAR APK.bat`.
- Windows, K2000N ARM32: `GERAR APK K2000N.bat`.
- Linux/macOS, genérico: `./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
- Linux/macOS, ARM32: acrescente `-Pk2000Native=true` ao comando anterior.

A compilação Windows não foi executada nesta nuvem. Os comandos Gradle foram executados com JDK 17 e SDK 35. Não altere targetSdk para 28 para tentar carregar uma biblioteca de arquitetura incompatível.

## Evidências e limites

54 testes JVM aprovados. Nenhum Android físico conectado; GPS, câmera, áudio/FM, interface e driver nativo permanecem sem validação física. No vídeo real: 82 quadros, 40 com candidatos a faixa, **zero pares de faixas confirmados**, três candidatos a veículos. Não há anotações de referência, GPS ou calibração; não foi medida precisão e o vídeo ainda não valida LDW operacional.

Veja [RELATORIO_TECNICO.md](RELATORIO_TECNICO.md), [MATRIZ_REQUISITOS.md](MATRIZ_REQUISITOS.md), `relatorios/3_2` e [proveniência da biblioteca](referencias_nativas/PROVENIENCIA.md). Os relatórios anteriores são históricos.

Modelo de veículos: Google Coral SSD, Apache-2.0, checksum e licença em `app/src/main/assets`. O APK enviado contém um binário de câmera sem licença geral de redistribuição declarada; direitos comerciais dessa biblioteca não foram verificados.
