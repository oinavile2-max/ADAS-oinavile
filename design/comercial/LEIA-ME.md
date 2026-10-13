# Conceito comercial aprovado e aplicado

Direção aprovada: refinar o neon atual, brilho, ícones, tipografia e acabamento. Os popups permanecem vermelhos.

A maquete index.html e suas imagens registram o conceito aprovado. Não são capturas Android. No aplicativo, CommercialPage/SettingsActivity implementam quatro abas nativas com cards responsivos e controles reais. Na tela menor, a navegação vira uma faixa horizontal e os cards empilham quando necessário. O conteúdo rola, sem esconder o salvamento.

Aplicado: sensibilidade/espessura, volumes, seis padrões de bip, frequência/duração/prévia sonora, segmentos/quota dos clipes, orientação/USB, modo noturno, animação decorativa, timeout de controles e informações do aparelho. Rascunhos persistem ao trocar abas/recriar a Activity. Limites ADAS são validados antes de gravar. Mídia, eventos e autorização USB seguem os mesmos cards; dados vêm do armazenamento real e dos serviços.

Novo ícone: icone.html e icone-profissional.png mostram exemplos de recorte/tamanho. A arte principal está em app/src/main/res/drawable-xxxhdpi/app_icon_art.png; recursos launcher clássico, adaptativo e monocromático estão em res. A margem adaptativa evita corte do detalhe vermelho. As imagens originais geradas foram preservadas fora do checkout.

Compilação do código, testes e lint foram executados, sem empacotamento de APK. Evidências em ../../relatorios/comercial-android-atual. Android físico, identidade pixel a pixel e launcher OEM ainda não validados. SDK 37.2/target 37, mínimo Android 7.1/API25; duas dependências neurais atualizadas para suporte a páginas de 16 KB sem perder a base antiga. Consulte ../../RELATORIO_TECNICO.md.

## Temas neon aplicados

Temas-neon.html e temas-neon.png mostram seis cores, matiz e transparências. São maquetes, não screenshots Android. O app implementa os controles e prévia em CONFIG → Aparência; salva neon_color/neon_tone/neon_button_transparency/neon_popup_transparency/neon_popup_follows_theme em ui_appearance. Cores de popups comuns são opcionais; o padrão mantém vermelho. Banners de eventos permanecem vermelhos com piso de alfa para o fundo. Ver README e relatorios/temas-neon. Sem novo APK nesta etapa.
