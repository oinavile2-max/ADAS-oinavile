# Correção de layout K2001N — 1024 × 600

- O menu Premium Luxury mantém a imagem original `premium_menu.jpg` (1672 × 940) sem deformação.
- A arte é centralizada e dimensionada proporcionalmente para caber na área útil da tela.
- Os toques são convertidos para as coordenadas reais da imagem, evitando áreas de toque deslocadas.
- O fundo das pequenas margens usa azul-escuro, sem recortar o menu.
- A tela LIVE utiliza controles compactos e rolagem horizontal quando necessário; o status fica em uma linha independente.
- Orientação paisagem mantida.

## Limitações

A adaptação foi feita no código-fonte; não foi possível validar visualmente no hardware K2001N ou compilar APK neste ambiente. A câmera proprietária e o rádio FM exigem testes independentes.

## Compilar

No Windows, extraia o ZIP e execute `GERAR APK.bat`.
