from pathlib import Path
import xml.etree.ElementTree as ET
base=Path(__file__).resolve().parent.parent
manifest=base/'app/src/main/AndroidManifest.xml'
ET.parse(manifest)
files=['app/build.gradle.kts','settings.gradle.kts','app/src/main/java/br/com/dashcamadas/MainActivity.kt','app/src/main/java/br/com/dashcamadas/AlertAudio.kt','app/src/main/java/br/com/dashcamadas/RadarStore.kt']
for f in files:
    assert (base/f).is_file(),f
text=(base/files[3]).read_text()
assert 'requestAudioFocus(' not in text and 'abandonAudioFocus(' not in text
assert '65' in text and '80' in text
print('OK: Estrutura basica, manifest XML e regras estaticas de audio MIX.')
print('ATENCAO: Nao substitui compilacao nem testes fisicos de audio/camera.')
