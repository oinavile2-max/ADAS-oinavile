# Prévia da interface — sem build Android

Referências: imagens fornecidas pelo usuário em 12/10/2026. Central: NWD K2001N, Android 7.1.1.

`popups.html` é uma maquete interativa: abra no navegador e altere velocidade, GPS e evento. `popups.png` mostra os dois componentes. Os ícones vetoriais vêm do mesmo premium_ui_icons.json utilizado no código Android. Esta captura foi renderizada em HTML/SVG; não é uma captura do APK.

O popup 1 contém arco de velocidade, estrada, veículo, GPS e estado do sensor. No Android os valores vêm dos serviços e de eventos reais do acelerômetro; ausência de dados produz `--`. Resolução exibida corresponde à configuração de vídeo comunicada pelo serviço; 4K/HDR não é afirmado sem suporte real. O exemplo 68 km/h, GPS OK e 4K HDR pertence exclusivamente à maquete.

O popup 2 usa canvas proporcional de 752×194, retângulo interno de 720×162, raio de 19, borda luminosa e ícone vetorial próprio do evento. O radar fixo exibe RADAR FIXO À FRENTE e o limite cadastrado. O título/ícone do radar é preservado pelo evento estruturado, sem tentar inferir o tipo por uma mensagem de voz. Eventos ADAS críticos usam vermelho e têm prioridade sobre aviso de radar. Não foram adicionadas detecções de pedestres/semáforos à rede nesta revisão.

A LIVE segue as posições da referência 1679×937: quadro principal, quadro ADAS lateral, popup 1 flutuante e seis botões inferiores. O quadro lateral recebe o mesmo quadro real analisado; não representa uma segunda câmera. Faixas/caixas reais recebem contorno luminoso. Gravação permanece acessível no botão superior esquerdo; toque longo protege o clipe. No ícone de câmera lateral estão seleção, rotação, espelhamento, faixas, gravação e tela cheia.

Validação nesta etapa: renderização da maquete e alteração dos estados GPS/colisão/faixa no navegador, sem erros JavaScript; revisão de código/XML/JSON e verificação de whitespace. Um parser Kotlin externo aceitou os novos componentes e arquivos principais; tem limitações nos dois serviços, também presentes na revisão anteriormente compilada. Nenhuma compilação, lint Gradle, teste JVM/Android ou instalação foi executada nesta revisão por instrução expressa do usuário. Os resultados de 73 testes e os APKs da versão 3.3 são anteriores a esta alteração visual. Identidade pixel a pixel no Android não foi comprovada.
