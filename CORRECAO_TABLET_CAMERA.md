# Correção de proporção da câmera em tablets (V4.1)

- `TextureView.setTransform(Matrix)` preserva a proporção 16:9 do buffer 1280×720.
- Toque simples na câmera: alterna FIT (sem cortes) e FILL (preenche com corte central).
- Toque duplo na câmera: alterna modo tela cheia; toque duplo novamente retorna à interface.
- Não altera a proporção do vídeo gravado; corrige somente a prévia.
- Mantém a Home Premium Luxury, controles e `GERAR APK.bat`.
- Pendente: negociar resolução real suportada pela câmera e validar orientação em diferentes tablets.
- Não foi compilado ou testado em hardware.
