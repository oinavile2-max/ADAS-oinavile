# DashCam ADAS PRO V4.2 — Correções técnicas (fonte; SEM BUILD)

## Alterações concretas
1. `AndroidManifest.xml`: o LAUNCHER passa a ser `MainActivity` (LIVE). `PremiumMenuActivity` deixa de iniciar automaticamente e fica acessível pela opção CONFIG. O menu 3D é mantido como painel de ajustes/seções.
2. `MainActivity.kt`: evita chamadas duplicadas de `openCamera()`; aguarda `TextureView` estar disponível antes da prévia; consulta os tamanhos de prévia realmente anunciados pelo Camera2 em vez de fixar cegamente 1280×720; só oculta a mensagem de câmera indisponível após a sessão de prévia ser configurada e a solicitação repetida ser aceita; trata falhas de abertura/configuração com status visível; tenta reabrir câmera no `onResume()`.
3. `PremiumSectionActivity.kt`: os avisos de teste de placas/ADAS/semáforos não usam mais `AlertDialog` modal com botão OK; mostram banner temporário próprio, explicitamente identificado como TESTE.
4. Preservados os recursos Camera2, gravação MP4, menu 3D, proporção FIT/FILL para tablet, radar GPS e `GERAR APK.bat`.

## Limitações reais
- O motivo exato do fechamento no LDPlayer **não pode ser comprovado sem Logcat/stack trace**. As alterações acima corrigem causas plausíveis de falha no fluxo de câmera, mas não são prova de ausência de crash.
- O código LIVE ainda é uma reconstrução nativa do layout, **não uma reprodução pixel-perfect** do preview fotográfico aprovado. A implementação 3D fiel continua pendente.
- A câmera USB proprietária UCarDVR, IA ADAS, alertas automáticos reais, áudio FM-SAFE e upload Drive não estão implementados/validados.
- O arquivo `GERAR APK.bat` está incluído, mas não foi executado. **Nenhum APK foi compilado.**

## Diagnóstico se ainda fechar
Execute `adb logcat -c`, abra a LIVE, e depois `adb logcat -d -v time > dashcam_crash.txt` no PC com ADB e LDPlayer conectado. Envie o trecho `FATAL EXCEPTION`/`AndroidRuntime` para identificar a causa real.
