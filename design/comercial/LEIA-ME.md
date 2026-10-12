# Refinamento comercial do neon atual

Direção escolhida pelo usuário: refinar brilho, ícones, tipografia e acabamento, preservando as referências aprovadas e o neon vermelho dos popups.

## Prévia visual

Abra `index.html` no navegador. `configuracoes.png` mostra a proposta; `popup-vermelho.png` mostra o diálogo com neon vermelho. Os ícones usam os mesmos vetores de `premium_ui_icons.json` do aplicativo. As opções de bip usam os seis nomes reais do código.

A proposta agrupa Assistente de faixa, Áudio e alertas, Câmera e gravação e Aparência. Valores em destaque, superfícies escuras com brilho discreto, ação principal e descrições curtas ajudam a leitura. Ajustes de sensibilidade/espessura funcionam na maquete; não alteram preferências do Android. Modo noturno, redução de animações e organização em quatro abas são propostas visuais, ainda não implementadas no app.

## Aplicado ao código Android nesta etapa

- `CommercialUi.kt`: acabamento comum dos botões internos, contraste claro sobre fundo escuro, tipografia média, áreas mínimas de 52 dp, cantos proporcionais à densidade, ripple e destaque de foco.
- `SettingsActivity.kt`: aplicação do acabamento aos botões, títulos com hierarquia mais clara e ações descritas em português simples.
- `PremiumSectionActivity.kt`: botões com o mesmo acabamento, espaçamento entre linhas e nomes de ações mais curtos; retirada da indicação histórica “sem build” sobre o fluxo da versão 3.4 já compilada.
- `PremiumVisuals.kt`: pequeno reflexo no vidro dos seis botões da LIVE e contorno de foco mais visível. Composição e vetores existentes preservados.

A organização completa em cards/abas exibida na maquete não está aplicada à Activity: representa a proposta para a próxima revisão da tela. Os componentes dos popups de evento/HUD, limites da IA, gravação e formatos de áudio não foram modificados nesta etapa.

## Verificação

Maquete renderizada em Chromium; quatro abas, controles de faixa, seis nomes de bip, popup vermelho e ausência de overflow horizontal em 800 px verificados. Nenhum erro JavaScript. Em 800×480, a proposta empilha os controles e exige rolagem; não é uma captura da central.

Parser estático Kotlin aceitou os novos componentes e os arquivos de apresentação alterados, exceto SettingsActivity, cuja construção já apresenta limitações no parser usado. Esse parser não substitui o compilador Kotlin. `git diff --check` passou.

Nenhum novo build Gradle, APK ou teste físico Android nesta revisão comercial. As evidências de 81 testes e os APKs/ZIP 3.4 pertencem à entrega anterior. Esta prévia não comprova a aparência final na NWD K2001N.
