# Referência nativa fornecida pelo usuário

APK: `apkfiles.com_618718_ucardvr.apk`, pacote `com.ankai.cardvr`, versão `20250515.3`.

SHA256 APK: `5669b6b81f591b872c227e831a0a3a4fa693cc557ae2d7365b0ac6024bdb5499`.

SHA256 libmycamera.so: `fa9d9a7936f2ae12ca8f6540f214bb581c0b26ac5222c1b76384dcf4f2795bd9`.

Biblioteca copiada sem alteração, ELF32 ARM, ABI armeabi-v7a; depende somente das bibliotecas de sistema listadas no ELF (android, log, EGL, GLESv2, OpenSLES, stdc++, m, c, dl). Somente a compilação `-Pk2000Native=true` inclui o binário e restringe o APK a ARM32. O APK genérico preserva as demais ABIs e não inclui esta biblioteca.

`MyCamera.java` reproduz os 25 descritores JNI e callbacks necessários ao RegisterNatives. O adaptador é escrito neste projeto; nenhum classes.dex do outro aplicativo foi incorporado. Os comandos de prévia 6/7 e configuração 18 foram confirmados na enumeração original; API completa em jni-api.json.

Prévia USB experimental disponível na tela de teste; não integra ainda gravação/LDW/FCW ao fluxo USB deste driver. A gravação/ADAS existentes usam Camera2. Não há teste físico da K2000N nem comprovação de compatibilidade com toda câmera UVC. O APK fornecido não contém licença geral de redistribuição da biblioteca; os direitos comerciais não foram verificados.

## Estado 3.3

A nota de prévia isolada acima descreve a revisão 3.2. Na 3.3, a biblioteca continua byte a byte igual, mas seu render de prévia type=0/ID=hash da Surface alimenta ImageReader RGBA do serviço. As duas redes analisam esses quadros e um EGL recordable próprio alimenta MediaRecorder. Não se utiliza um tipo de encoder presumido do SDK. O código de integração foi compilado; carregamento/render/gravação com a câmera física continuam não executados.

## Identificação corrigida do aparelho

Em 12/10/2026, o usuário corrigiu a central para **NWD K2001N com Android 7.1.1 (API 25)**. O nome k2000Native é mantido somente como identificador de compilação histórico. A versão está dentro da faixa API 24–27 prevista; CPU/ABI e câmera continuam sem confirmação física.


## DVR identificado na central — revisão 3.9

Foto enviada pelo usuário: Allwinner / QUAD-CORE T3 k2001-nwd, Android 7.1.1/API25, armeabi-v7a/armeabi, processo 32 bits, RAM 2048 MB. Camera2 retorna SecurityException apesar da permissão CAMERA concedida. USB: 058F:6387 e 3322:6655, interfaces8/6. A foto confirma descritores/erro, não confirma transmissão de imagens.

O recurso `com.ankai.cardvr:xml/ufd_device_filter` (0x7f0d0002, arquivo `res/4p.xml`) contém seis filtros únicos: 3322:6655, 2298:6698, 0098:1198, 0198:1298 e 1B3F:8301 com classe/subclasse 0/0; 0EFF:0001 com classe/subclasse 8/0. A duplicação de 3322:6655 é eliminada no adaptador. O método original f0.b compara VID, PID, classe e subclasse do dispositivo; não exige interface14. O novo filtro conserva essa correspondência exata e continua excluindo armazenamento comum, inclusive 058F:6387 observado na foto.

MyCamera.P0 e v0.umsd* abrem o armazenamento do DVR, localizam um arquivo de comunicação existente e encaminham operações seek/read/write. `assets/filenames.json` associa akfile/elene ao protocolo 0, kkfile/eefile ao 1 e jrfile ao 2. O método g0.j forma JSON {name: caminho, dvr: protocolo}. Na conexão por callbacks, MyCamera.J0 passa esse JSON e transporte 0 a nativeConnect. O descritor USB bruto não substitui esse caminho UFD. O adaptador 3.9 implementa esses callbacks; não incorpora classes.dex do APK original.

A busca abre exclusivamente o DVR selecionado e autorizado pelo Android. Procura os nomes de comunicação na raiz FAT32 ou em Android/data/com.ankai.cardvr e Android/data/br.com.dashcamadas. Não cria, exclui, redimensiona ou formata arquivos; a escrita de comandos fica limitada ao endpoint existente e ao tamanho já alocado. O cursor avança na leitura e permanece na escrita, conforme o callback original. Cancelamento, limites de buffer e falhas impedem repetição sem progresso.

O transporte SCSI/FAT32 usa `me.jahnen.libaums:core:0.10.0`, Apache-2.0, com adaptador próprio da API USB Host. Os dois JNI legados dessa dependência (usb-lib/errno-lib) são excluídos, sem alterar os checks de alinhamento de 16 KB; o adaptador não chama suas classes de comunicação/errno. O fechamento libera a interface mesmo após falha ao descobrir partições. Não exige permissões globais de armazenamento, root ou privilégios de aplicativo do sistema. Interfaces UVC classe 14 usam nativeCreateUVC; o DVR referenciado usa nativeCreateUFD.

O binário libmycamera.so mantém seu SHA256 original. Inicialização, primeira imagem, gravação e precisão/latência da IA nesse DVR ainda não foram testadas fisicamente. Se o dispositivo não apresentar FAT32/endpoint compatível, o app informa a falha; não considera o USB conectado como prova de imagem nem ativa alertas sem quadros reais.
