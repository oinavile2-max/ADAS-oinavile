# ADAS V4.8 — estado técnico real

Implementado: pipeline experimental de faixas a partir dos pixels reais de `TextureView` com amostragem de 160×90 a ~2,8 Hz, extração de bordas claras, ajuste linear, rejeição por confiança e overlay que desaparece quando não há detecção. Botão FAIXAS BETA liga/desliga.

**Não é um assistente de faixa de segurança pronto para uso.** Limitações: linhas curvas, iluminação, sombras, chuva, rotação/crop da câmera, coordenadas e calibração; não calcula saída de faixa confiável, não gera alertas de segurança. Não usar para condução. A análise é protótipo e precisa de validação em vídeo real, datasets e hardware.

**Não implementado:** detecção real de pedestres, ciclistas, placas, veículos e semáforos. Exigem modelos treinados e licenciados, integração e validação. Captura proprietária UCarDVR ainda pendente. Sem build/testes em dispositivos.
