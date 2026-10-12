# Modelo neural de faixas / área dirigível

Origem oficial: https://github.com/hustvl/YOLOP, commit `8d8f68df318c71f01d6f813c024df646c7d1978f`.
Arquivo original: `weights/yolop-320-320.onnx`, SHA256 `86d6e8b6dfdef195c061e9bcad82d9487bb5ee1ac4a1cf9a3dc4736657a07369`.

Derivação: `onnx.utils.extract_model` (onnx 1.17.0), entrada `images`, saídas `drive_area_seg` e `lane_line_seg`; removeu a cabeça de detecção não utilizada, sem treinar, substituir ou fabricar pesos. Comparação com o grafo oficial teve erro máximo zero para as duas saídas no ensaio registrado em relatorios/3_3/modelo-neural-verificacao.json.

Modelo empacotado: `road_lanes_yolop_320.onnx`, 22.144.259 bytes, SHA256 `f93ced8254ccbb532ff9f9593e7b7d0022f28210831b615f5039066c9f5222fe`.
Entrada float32 NCHW [1,3,320,320], RGB, letterbox com RGB114, normalização ImageNet: médias 0.485/0.456/0.406 e desvios 0.229/0.224/0.225. Saídas float32 [1,2,320,320], sigmoides independentes para fundo/primeiro plano, remapeadas ao quadro sem padding. Margem e limiares são experimentais.

Licença MIT do repositório oficial preservada integralmente em MODEL_LICENSE_YOLOP_MIT.txt. O trabalho original utiliza BDD100K; não houve auditoria independente de todos os termos dos dados de treinamento nem homologação comercial/veicular. Nenhum treinamento online ou envio de quadros ocorre no app.
