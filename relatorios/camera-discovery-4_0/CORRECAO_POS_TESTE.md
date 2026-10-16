# Investigação após falha física informada — 16/10/2026

O usuário informou que o APK 4.0 não encontrou a câmera. O relatório atual da central foi solicitado; ainda não está disponível nesta revisão. A foto anterior mostrava USB 3322:6655, reconhecido pela política da 4.0, mas não comprova que o dispositivo continua enumerado, que o perfil NWD está instalado ou que há autorização/acesso ao transporte.

## Falhas de cobertura corrigidas no código

1. A busca montada examinava somente as primeiras 64 entradas de cada diretório antes de verificar nomes de marcadores. Arquivos de gravação podiam esconder um marcador válido. A filtragem agora considera os nomes conhecidos em todo o diretório; o limite de 64 aplica-se às subpastas exploradas, depois de excluir arquivos comuns. A busca continua rasa, sem criação de arquivos e sem seguir links para fora do volume.
2. Subpastas imediatas eram examinadas somente pela raiz, sem verificar suas pastas Android/data do aplicativo original e do atual. O DEX fornecido, método git.my.camera.core.g0.c, inclui essa consulta (dexdump.txt, instruções 019b–01b2). Essas duas pastas passam a ser verificadas também para cada subvolume explorado. Canonicalização e restrições ao volume são preservadas.

Dois testes de regressão cobrem diretórios cheios e marcadores em Android/data dentro de subvolumes. A suíte existente também verifica arquivos vazios, aliases de mounts, exclusão de links externos, modos de busca e autorização com prazo.

## Escopo da entrega

Esta branch contém uma correção de código e testes. Os APKs 4.0 e o ZIP publicados na main permanecem os artefatos da entrega anterior; não incluem automaticamente esta alteração. Nenhum novo APK foi empacotado nesta investigação. Evidências desta revisão estão em verificacao-pos-teste.json e validacao-pos-teste.log.

Não há comprovação de que as falhas acima expliquem a central do usuário. O relatório de Buscar DVR USB/volumes e a mensagem da LIVE são necessários para distinguir detecção, autorização, transporte e primeira imagem. Nenhum teste de instalação/captura física foi realizado nesta nuvem.
