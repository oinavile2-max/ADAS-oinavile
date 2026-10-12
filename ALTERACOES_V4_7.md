# V4.7 — correções de experiência LIVE

- Corrige transformação do TextureView com rotação do sensor e rotação do display, incluindo fallback manual por câmera salvo em SharedPreferences.
- Botão FULL visível no vídeo; SAIR FULL para restaurar os painéis. Duplo toque continua disponível.
- Botão GIRAR para ciclos automática → 0° → 90° → 180° → 270° → automática.
- Popup em overlay com ícone contextual ao lado da mensagem. Ícones não são prova de detecção real.
- Mantém a captura real Camera2; não adiciona detecção automática ADAS.

## Validação necessária
Não foi executado build nem testes em dispositivo. A matriz de rotação pode exigir ajuste em firmwares que aplicam rotação prévia à textura. Verificar em portrait/landscape, câmera frontal/traseira e modo FULL.
