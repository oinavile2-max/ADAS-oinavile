# V4.6 — correção de crash LIVE

**Erro comprovado na tela:** `UnsupportedOperationException: TextureView doesn't support displaying a background drawable`.

**Causa localizada:** `MainActivity.kt`, `buildUi()`, `TextureView(this).apply { setBackgroundColor(...) }`. Android `TextureView` não aceita `background drawable`.

**Correção aplicada:** remoção de `setBackgroundColor` do `TextureView`. O `FrameLayout` pai já possui `background = panel(...)` e mantém o acabamento escuro Premium Luxury.

**Validação:** busca estática confirma ausência de fundo no `TextureView`, estrutura básica do projeto validada pelo script. **Não foi compilado nem testado em aparelho**; outros problemas de câmera, permissão ou áudio podem aparecer depois.

**Para testar:** execute `GERAR APK.bat` e instale o novo APK (não reutilize o APK V4.5). Se surgir outra falha, envie a nova tela/logcat.
