# DashCam ADAS PRO — 3.6, temas neon e aparência comercial

Conceito aprovado aplicado ao código Android: cards em vidro escuro, tipografia legível, controles em cores neon e banners de evento com neon vermelho. Configurações em quatro abas; mídia, eventos e câmera USB seguem a mesma identidade. Novo ícone profissional com versões clássica, adaptativa e monocromática.

**Esta entrega é de código-fonte, sem novo APK.** Os APKs publicados são entregas anteriores. Compilação do código, testes e lint desta revisão têm evidências em `relatorios/temas-neon`. Aparência no aparelho e execução em todas as versões ainda precisam de teste físico.

## Downloads

- [Código-fonte atualizado 3.6](https://github.com/oinavile2-max/ADAS-oinavile/raw/refs/heads/main/DashCam_Fonte.zip)
- [APK 3.4 NWD K2001N ARM32 — entrega anterior](https://github.com/oinavile2-max/ADAS-oinavile/raw/refs/heads/main/APKS/DashCam_ADAS_PRO_3_4_NWD_K2001N_ARM32_NEON_DEBUG.apk)
- [APK genérico 3.3 — histórico](https://github.com/oinavile2-max/ADAS-oinavile/raw/refs/heads/main/APKS/DashCam_ADAS_PRO_3_3_NEURAL_GENERIC_DEBUG.apk)

Os APKs antigos não contêm a tela comercial e temas 3.6 nem a atualização de bibliotecas desta revisão. O genérico 3.3 histórico não suporta páginas de memória de 16 KB.

## Temas neon

Em **CONFIG → Aparência**, escolha ciano, violeta, rosa, verde, laranja ou vermelho. Ajuste o tom entre −30° e +30° e a transparência do fundo dos botões e popups entre 0% e 80%. A prévia muda antes de salvar; textos, ícones e bordas não são apagados pela transparência. Salvar aplica o tema às telas e à LIVE, com preferências persistidas.

**Popups comuns acompanham o tema** é opcional e vem desativado para manter o vermelho aprovado. Habilitar aplica a cor escolhida a HUD, diálogos, listas e avisos comuns. Banners de evento continuam vermelhos; fundos dos avisos mantêm uma opacidade mínima nominal de 75%. O botão Restaurar cores e transparência prepara o padrão; pressione Salvar para persistir.

[Prévia dos temas](design/comercial/temas-neon.png) · [Maquete interativa](design/comercial/temas-neon.html). Renderizadas no navegador, não são capturas Android. **89 testes aprovados por perfil**, com lint sem erros nesta revisão; logs em `relatorios/temas-neon`. Nenhum novo APK.

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
- **Câmera e gravação:** segmentos de 1/3/5 minutos, quota de armazenamento, orientação/seleção na LIVE e USB no perfil compatível.
- **Aparência:** modo noturno (brilho da janela do app), animações decorativas do HUD, recolhimento dos controles e informações do aparelho.

O salvamento é específico da aba; alternar abas preserva rascunhos. Valores avançados inválidos rejeitam a gravação inteira dos limites ADAS. Ajustes de risco continuam bloqueados em movimento. Câmera, redes, alertas e gravação mantêm os serviços existentes.

Os dados do HUD e caixas são reais. O quadro direito usa o mesmo bitmap analisado. Sensibilidade não dispensa GPS/calibração/confiança; espessura altera somente o desenho. O aviso de pedestre permanece experimental. Semáforos, placas e ciclistas com alertas completos continuam pendentes.

[Conceito e implementação](design/comercial/LEIA-ME.md) · [Prévia do ícone](design/comercial/icone-profissional.png) · [Relatório técnico](RELATORIO_TECNICO.md).

## Verificar sem gerar APK

```sh
./gradlew --no-daemon --max-workers=2 :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug :app:verifyNativePageAlignment
```

Para o perfil da central, acrescente `-Pk2000Native=true`. O teste de faixas executa os pesos reais em CPU. `verifyNeuralModelAssets` confere os dois modelos. Nenhum APK é empacotado por esse comando.

Um futuro build autorizado usa `:app:assembleDebug`, sujeito aos testes neurais, hashes e verificação das bibliotecas. Windows: `GERAR APK.bat` e `GERAR APK K2000N.bat` exigem SDK Platform 37.2 e Build Tools 36.0.0; execução Windows não validada nesta nuvem.

## Validação restante

Sem aparelho conectado: instalação, renderização, GPS, desempenho/RAM/temperatura, driver USB, áudio/FM e gravação com tela apagada continuam pendentes. Não há comprovação pixel a pixel das capturas Android. As prévias são HTML/SVG, e os ensaios neurais no host não medem precisão em estrada ou latência ARM32. `build-tools/test_android_device.py` prepara a verificação física quando houver aparelho autorizado.

O erro de instalação anterior não teve causa identificada. Os APKs 3.4/3.3/3.2 usam o mesmo certificado debug; builds em outro computador podem usar um certificado diferente. `adb install -r arquivo.apk` fornece o diagnóstico detalhado.

YOLOP preserva licença MIT; SSD, Apache-2.0. Proveniências dos modelos e do driver estão no repositório. A licença comercial do driver fornecido continua sem confirmação.
