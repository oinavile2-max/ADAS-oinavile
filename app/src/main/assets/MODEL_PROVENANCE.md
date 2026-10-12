SSD MobileNet v2 COCO, quantized CPU model.
Official source: https://github.com/google-coral/test_data
Pinned commit: 104342d2d3480b3e66203073dac24f4e2dbb4c41
Original file: ssd_mobilenet_v2_coco_quant_postprocess.tflite
SHA-256: 42fb3d70ffb7bb37dd518f730f7be784b831c2078f30c497d0019cc2e987fa26
Repository license: Apache-2.0; copy included in MODEL_LICENSE_APACHE_2_0.txt.
COCO labels: coco_labels.txt from the same official commit.
Vehicle output categories (zero-based): car 2, bus 5, truck 7.
This model is object detection, not safety certification, metric distance estimation or FCW by itself.

Na versão 3.4 com neon vermelho, a classe COCO zero-based 0 (pessoa) também é consumida do mesmo SSD. Pessoas são separadas de veículos no decoder; não é um novo modelo nem treinamento online. O novo aviso experimental exige máscara de área dirigível, confiança, confirmação temporal, GPS e calibração. Ver design/preview/ssd-pessoa-inferencia-host.json para o ensaio adicional do modelo real no host; decoder e pré-condições de risco cobertos pelos testes JVM da 3.4; funcionamento no aparelho Android ainda não validado.
