# APKs 4.0 — 15/10/2026

Continuação da revisão de descoberta DVR baseada no RELATORIO_UCARDVR.md enviado pelo usuário. Os pacotes usam o código de produto da revisão `d2c887ca9ec710493b6ecca29425a8715e718fc7`, sem nova alteração da lógica de captura ou dos modelos neste empacotamento. A entrega anterior em `camera-discovery-4_0` foi somente em código; esta etapa gera os APKs para o teste físico.

## Arquivos

| Pacote em APKS | Uso |
|---|---|
| DashCam_ADAS_PRO_4_0_NWD_K2001N_ARM32_NEON_DEBUG.apk | NWD K2001N, Android 7.1.1, ARM32; driver legado restrito a Android 7/8 |
| DashCam_ADAS_PRO_4_0_NEURAL_GENERIC_DEBUG.apk | Celular/tablet com câmera exposta pela Camera2; ARM32, ARM64, x86 e x86_64; Android 7.1 ou superior |

Ambos têm applicationId br.com.dashcamadas, versionCode 40, minSdk 25 e targetSdk 37. Instale somente o perfil adequado: os dois substituem o mesmo aplicativo. São APKs debug experimentais, com o certificado das entregas anteriores deste repositório. Uma instalação existente assinada por outro computador pode exigir investigação do erro de assinatura; não desinstale sem antes preservar gravações e configurações.

## Verificações

Comando por perfil, usando JDK 17 e o wrapper Gradle do projeto:

```sh
./gradlew --no-daemon --max-workers=2 :app:assembleDebug :app:lintDebug
./gradlew --no-daemon --max-workers=2 -Pk2000Native=true :app:assembleDebug :app:lintDebug
```

Os perfis são executados em sequência, preservando os resultados antes do próximo. Cada perfil passa por 134 testes JVM, incluindo inferência YOLOP real no host com uma e duas threads. Zero falhas, erros ou skips. Lint: zero erros, 117 avisos por perfil; os avisos não foram suprimidos para publicar.

Os APKs foram abertos e conferidos: ZIP íntegro, metadados/ABIs, ausência da permissão INTERNET, permissões legadas READ/WRITE_EXTERNAL_STORAGE limitadas a API27, modelos incorporados com SHA-256 esperado e driver NWD íntegro. O pacote genérico contém seis bibliotecas ELF64 com segmentos LOAD alinhados para páginas de 16 KB; o NWD contém somente ARM32. JNI antigos não utilizados de libaums continuam excluídos.

Assinatura verificada em API25 e API37 (V2); API23 forçada exclusivamente para conferir o esquema V1, sem declarar compatibilidade do aplicativo com Android anterior a API25. Certificado SHA-256: `6b7459556adedfac6a298c4305a55c3f8709770c38bdf9dc21ba7d38c8162dd8`. Metadados, tamanhos, hashes e logs de cada perfil estão nesta pasta. `SHA256SUMS.txt` na raiz também cobre os downloads.

## Teste na K2001N

1. Instale o APK **4.0 NWD**. Feche o outro aplicativo DVR para liberar o dispositivo.
2. Entre em **CÂMERA → Buscar DVR USB/volumes**. Escolha **Volumes primeiro** e conceda o acesso em **Permitir busca em volumes Android 7/8**.
3. Atualize e selecione **VOLUME DVR** se houver marcador encontrado. Se não houver, tente **USB NATIVO 3322:6655** e autorize o Android. O prazo de autorização é 20 segundos; após expirar, inicie outro pedido na tela.
4. Confirme chegada de imagem, orientação com **GIRAR** e continuidade dos dois painéis em LIVE sem gravar. Depois use **REC/PARAR** e reproduza um segmento salvo.
5. Se falhar, use **Copiar relatório** ou **Compartilhar relatório** na busca. O relatório identifica permissões, interfaces, volumes e arquivos; a mensagem da LIVE ajuda a distinguir abertura do SDK de recepção real de quadros.

Não há Android físico conectado nesta nuvem. Instalação, primeira imagem FILE/UFD, exposição, GPS, fluidez, consumo de RAM e gravação na central não estão validados. Encontrar USB ou marcador não comprova vídeo. A compensação noturna Camera2 não se aplica ao driver DVR, cuja exposição permanece controlada pela própria câmera.

## Referência da Play Store

O acesso público a com.pdt.cameraadas foi tentado novamente nesta etapa e permaneceu bloqueado pelo proxy (CONNECT 403). A comparação com esse aplicativo continua pendente; nenhuma funcionalidade dele é apresentada como examinada ou integrada. Os domínios necessários já estão salvos no rascunho das configurações do ambiente.
