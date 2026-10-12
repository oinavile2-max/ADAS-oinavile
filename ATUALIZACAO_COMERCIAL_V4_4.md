# DashCam ADAS PRO — V4.4: Base comercial multi-dispositivo

## Implementado nesta atualização

- Preservada LIVE como launcher e menu 3D como tela de configurações.
- Introduzido `DeviceCapabilities.kt`: diagnóstico passivo de fabricante, modelo, API Android, RAM, perfil de desempenho, permissão de câmera, recurso USB host, câmeras Camera2, formatos de preview enumeráveis e dispositivos USB VID/PID/classes/interfaces.
- Seletor de câmera agora tem botão **Diagnóstico** e, se Camera2 não retornar dispositivos, mostra o diagnóstico em vez de presumir que não há USB.
- Compartilhamento explícito do relatório por ação do usuário; nenhuma telemetria ou envio automático.
- Perfil econômico/equilibrado/avançado **sugerido** por RAM, sem prometer aceleração de IA ou alterar automaticamente parâmetros de gravação.
- Rótulo da câmera corrigido: ID Camera2 detectado não é necessariamente câmera externa.

## Limites comprováveis

- Enumerar USB **não** equivale a suporte UVC, abertura do dispositivo ou frames disponíveis. Um backend UVC real e adaptadores proprietários precisam ser implementados e testados.
- Faixas dinâmicas, pedestres, ciclistas, placas, semáforos e risco de colisão **não estão implementados**; não exibir detecção simulada.
- Android 7.1+ é o objetivo de compatibilidade de API (`minSdk=24`), não garantia de suporte universal a hardware.
- Os perfis de memória são heurísticos, não benchmarks.
- **Sem compilação, sem APK, sem teste em dispositivos reais.**
- A LIVE recebeu ajustes de UI na V4.3, mas ainda não foi validada como reprodução fotográfica 3D integral da referência.

## Próximas etapas obrigatórias antes da comercialização

1. Validar compilação, lifecycle e crashes via logcat (somente após autorização de build).
2. Criar interface `CameraSource` e implementações Camera2, UVC e adaptadores proprietários, com testes reais de abertura, resolução e FPS.
3. Integrar inferência local quantizada de faixas, objetos e placas; benchmark por chipset e medir falsos positivos.
4. Implementar diagnóstico de calibração da câmera, confiança e falha segura (ocultar overlay quando não há confiança).
5. Realizar matriz de testes Android 7.1+, 1024×600, tablets, smartphones, head units, USB-C, USB-A, áudio FM e ciclo de gravação.
6. Validar privacidade/LGPD, licenças comerciais de modelos e bibliotecas, políticas de distribuição e requisitos de segurança.
7. Produzir preview visual fiel à referência, colher aprovação e só então build mediante autorização explícita.

## Build

`GERAR APK.bat` preservado no pacote; **não executado**.
