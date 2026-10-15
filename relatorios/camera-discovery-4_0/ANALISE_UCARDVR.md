# Descoberta DVR na K2001N — revisão de código 4.0

Entrada: RELATORIO_UCARDVR.md enviado pelo usuário, análise estática do APK com.ankai.cardvr 20250515.3. O MD é tratado como relatório técnico; suas afirmações sobre um módulo camerasearch não significam que esses arquivos tenham sido anexados ou integrados anteriormente. Não houve teste físico descrito no documento. A implementação abaixo foi escrita para br.com.dashcamadas e conferida com as referências nativas já presentes no projeto.

## Lacuna encontrada

A revisão 3.9 já reconhecia 3322:6655 e tinha o transporte UFD por SCSI/FAT32/callbacks. Não descobria marcadores nos volumes montados, não tinha modo de busca FILE, não atualizava a lista com hotplug e não encerrava pedidos de permissão por prazo. O receiver de autorização era removido em onStop, podendo perder o resultado durante o diálogo do Android.

## Integração aplicada

| Evidência do relatório | Aplicação no projeto |
|---|---|
| E3/E5: seis filtros VID/PID/classe/subclasse | NativeUsbPolicy preservada; os mesmos seis filtros entram no manifesto para abertura da tela ao conectar o DVR. Sem curinga para armazenamento comum. |
| E6: cinco marcadores e protocolos 0/0/1/1/2 | DvrMarkerSearch procura arquivos não vazios na raiz, nas pastas de dados do app original/atual e em subpastas imediatas. Verifica até 64 entradas por diretório; rejeita links para fora do volume. |
| Caminho FILE em volumes removíveis | MountedDvrDiscovery usa StorageManager público, diretórios de /storage e /proc/mounts para variantes legadas. Traduz /mnt/media_rw para /storage e admite mounts USB de fornecedor acessíveis. Ignora armazenamento primário. Não cria pastas do aplicativo para enumerar volumes. |
| Seleção de transporte | Quatro modos: USB primeiro, volumes primeiro, somente USB e somente volumes. Nenhum modo abre hardware sem seleção do usuário. |
| E7: hotplug | Busca assíncrona com debounce de 450 ms para USB/volumes; descarte de resultados de buscas anteriores; captura FILE encerra ao remover o volume. |
| E8: autorização USB | Prazo de 20 s, conferência do dispositivo e da permissão real do UsbManager; resultados tardios/antigos são rejeitados, inclusive após retomar a Activity. Receiver permanece registrado até onDestroy. Não bloqueia UI com wait/notify. |
| E13: transporte nativo por arquivo | A fonte dvrfile passa JSON name/dvr e transporte −1 a nativeConnect; UFD mantém transporte 0 e callbacks UMsd. A assinatura J0 do original confirma a diferença entre FILE e UFD. Ambos alimentam ImageReader RGBA, serviço de gravação e as redes existentes. |
| E15: diagnóstico | Tela Buscar DVR USB/volumes, Atualizar, Copiar e Compartilhar. Lista todos os USBs, inclusive não candidatos, com VID/PID, classe/subclasse/protocolo, endpoints, permissão e candidatos de volume. |

A seleção USB memoriza VID/PID para reencontrar uma única unidade após mudança do caminho /dev/bus/usb. A permissão continua obrigatória; unidades indistinguíveis não são escolhidas automaticamente. O caminho de marcador é validado novamente antes de abrir o SDK. A busca é somente leitura; a comunicação com o DVR envia comandos pelo SDK/endpoint, conforme o protocolo original.

Não foi reproduzida a reflexão grantDevicePermission, FLAG_SYSTEM, persistent=true ou instalação como app de sistema. As novas permissões READ/WRITE_EXTERNAL_STORAGE são limitadas a API27 e solicitadas somente para a busca de volumes no perfil legado. Android moderno mantém Camera2 e não recebe permissões globais de armazenamento. O driver continua ARM32/API24–27; o aplicativo tem minSdk25, portanto o perfil efetivo começa no Android7.1.

## Melhorias independentes já em andamento

Perfis de pouca luz usam medição da imagem com histerese, controles Camera2 declarados pelo dispositivo e fallback para a requisição padrão. Nenhum comando de exposição foi inventado para o driver DVR. Brilho automático e gravação automática são opcionais; início automático exige imagem real e respeita parada manual. A notificação passa a oferecer gravar/parar/proteger. Aparência neon e limiares de risco existentes foram preservados.

## Limites e entrega

Compilação, testes JVM (incluindo os pesos YOLOP reais), lint e alinhamento de bibliotecas são executados em ambos os perfis. Contagens e hashes finais: verificacao.json. Nenhum assemble/package de APK é executado nesta revisão; os APKs 3.9 publicados continuam antigos. Não há aparelho conectado, portanto primeira imagem FILE/UFD, autorização no firmware, gravação, precisão ADAS e latência ARM32 não foram confirmadas.

O relatório fornecido é análise estática. O protocolo interno do binário não foi inteiramente auditado, e a presença de marcador/USB não é prova de vídeo. Se o firmware não expuser volume acessível, UFD é a alternativa; se nenhum USB for enumerado, o aplicativo não pode corrigir porta, cabo ou alimentação por software.

A referência pública com.pdt.cameraadas permanece bloqueada pelo proxy. Nenhum recurso desse outro app foi apresentado como verificado. Domínios salvos no rascunho do ambiente: play.google.com e play-lh.googleusercontent.com; ativação/publicação ainda pendente.
