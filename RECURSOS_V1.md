# DashCam ADAS PRO K200ND — Escopo e estado da V1

## Implementado no código-fonte (não compilado, não testado no equipamento)
- Descoberta de câmeras externas publicadas como Camera2 EXTERNAL; prévia TextureView.
- Gravação MP4 720p/30 com clipes de 3 minutos e proteção manual do clipe atual.
- Velocímetro GPS, cadastro de radar na posição GPS atual com limite/tipo, listagem e exclusão.
- Persistência dos pontos radar localmente; avisos de aproximação GPS com filtro por rumo e antirrepetição de 2 minutos.
- Alertas MIX ou apenas voz, bipe ou visual; volume individual 65% sons e 80% voz por padrão.
- Sem solicitar audio focus e sem alterar volume de outros aplicativos. **Não garante comportamento do rádio FM no firmware K200ND.**
- Botão de teste de áudio em configurações.

## Ainda não implementado (exige desenvolvimento e validação)
- Driver USB Host/UVC para câmeras não expostas pelo Camera2; compatibilidade da câmera específica.
- IA ativa: detecção de veículos/pedestres, colisão frontal, saída de faixa, veículo à frente andando, reconhecimento de placas e semáforos.
- Gatilhos de voz de eventos ADAS: o motor de alertas só está conectado aos radares GPS cadastrados.
- Gestão de armazenamento em loop com exclusão automática, gravação em segundo plano, boot e PiP.
- Captura de impacto, modo noturno avançado, calibração de IA, mapas e radar por velocidade média.
- Testes reais de áudio com Spotify, Waze, rádio FM e hardware K200ND.

## Build
Este ambiente não tem Android SDK, Gradle nem conectividade para instalá-los; **não foi gerado APK**. Abra o projeto no Android Studio com JDK 17 / Android SDK 35 e Gradle 8.9. A build ainda pode revelar ajustes adicionais.

## Segurança
Protótipo de bancada. Não use como detector de colisões/sinal confiável em via pública. Radar cadastrado é informação comunitária/manual, pode estar desatualizado. Cadastre pontos somente com veículo parado.
