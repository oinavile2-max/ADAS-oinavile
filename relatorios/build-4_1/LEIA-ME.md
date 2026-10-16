# NWD 4.1 — correção da ordem da prévia, 16/10/2026

As novas fotos do usuário confirmaram perfil nativo ativo/API25, USB 3322:6655 autorizado e dois caminhos do marcador akfile (/storage/udisk3 e /mnt/media_rw/udisk3). O usuário confirmou que selecionar VOLUME DVR volta à LIVE sem vídeo. Portanto, a detecção funcionou nessa tentativa; as correções de cobertura dos marcadores, feitas antes das fotos, não explicam por si só essa falha.

## Defeito encontrado na abertura

A 4.0 chamava nativeAddPreview antes de nativeConnect. A inspeção estática ARM32 da biblioteca enviada confirma:

- AbstractCamera inicializa o ponteiro da prévia no offset 0xa4 como nulo (0x12712).
- MyCamera::addPreview retorna sem registrar quando esse ponteiro é nulo (0x163ea–0x163ee). A entrada JNI nativeAddPreview chama diretamente o método virtual, sem enfileirar esse registro para depois.
- MyCamera::connect cria MyPreview e atribui o ponteiro durante a conexão (0x2188c–0x218a0).

Isso explica como a sessão pode inicializar sem uma superfície registrada para receber vídeo. A 4.1 conecta primeiro e só registra a superfície após os dois indicadores nativos de conexão e inicialização. Depois envia START_PREVIEW (comando 6, conferido no enum original). NativePreviewStartup mantém o prazo de 5 s, cancela consultas após fechamento e impede início duplicado ou nova tentativa após erro. O watchdog continua exigindo quadros reais; inicialização não habilita ADAS nem gravação por si só.

Evidência de instruções e SHA-256 da biblioteca: native-preview-order.log. O binário fornecido não foi alterado ou executado nesta análise. O defeito na sequência foi confirmado no código; sua relação com a central precisa do teste do novo APK.

## Diagnóstico e regressões

Callbacks passam a preservar as mensagens do SDK, limitadas a 240 caracteres e sem controles de linha. O relatório copiável da busca inclui versão, última etapa nativa, fonte e data da última sessão, estado da captura e contagem de quadros. O watchdog inclui a etapa/detalhe nativo em sua mensagem ao receber zero quadros. O aplicativo continua sem INTERNET.

Esta entrega também incorpora as correções da branch fix/k2001n-marker-discovery: marcadores são filtrados por nome sem o corte das primeiras 64 entradas, e as pastas Android/data são examinadas dentro dos subvolumes. A busca permanece rasa, restrita a arquivos existentes e sem seguir links para fora do volume.

140 testes por perfil sem falhas/erros/skips, incluindo quatro regressões do início da prévia e inferência YOLOP real no host. Lint sem erros, 117 avisos. NWD empacotado com assembleDebug; genérico validado em código, sem empacotar APK 4.1. Hashes e logs em verificacao.json. Assinaturas API25/API37, V1/V2 e certificado anterior verificados, além de modelos/driver, ABI ARM32, permissões e integridade do pacote. A verificação V1 força API23 somente para o esquema de assinatura: o aplicativo requer API25 ou superior.

## Teste físico pendente

Instale APKS/DashCam_ADAS_PRO_4_1_NWD_K2001N_ARM32_NEON_DEBUG.apk na central, feche o outro DVR e escolha VOLUME DVR • /storage/udisk3/akfile na busca. Se continuar sem vídeo, aguarde a mensagem da LIVE e use Atualizar busca → Copiar relatório para incluir o diagnóstico da última abertura.

Nenhum Android físico está conectado à nuvem. Instalação, primeira imagem, latência, gravação e precisão ADAS não estão confirmadas. O usuário ainda não informou se o UCarDVR original mostra vídeo na mesma central. O genérico publicado continua 4.0; esta entrega de APK é direcionada à NWD.
