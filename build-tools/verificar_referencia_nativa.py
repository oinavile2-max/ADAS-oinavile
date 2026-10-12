#!/usr/bin/env python3
"""Verify external JNI contract and ABI packaging; does not execute the ARM driver."""
from pathlib import Path
import argparse, hashlib, json, os, re, subprocess, tempfile, zipfile

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('apk', type=Path)
parser.add_argument('--native', action='store_true', help='Expect the K2000N ARM32 library')
parser.add_argument('--sdk', default=os.environ.get('ANDROID_HOME'), help='Android SDK with build-tools 35.0.0')
args=parser.parse_args()
if not args.sdk: parser.error('Configure ANDROID_HOME or --sdk')
repo=Path(__file__).resolve().parents[1]
dexdump=Path(args.sdk)/'build-tools/35.0.0'/('dexdump.exe' if os.name=='nt' else 'dexdump')
expected=json.loads((repo/'referencias_nativas/jni-api.json').read_text())
callbacks=json.loads((repo/'referencias_nativas/jni-callbacks.json').read_text())
with zipfile.ZipFile(args.apk) as apk:
    if apk.testzip() is not None: raise RuntimeError('Invalid APK ZIP')
    abis={n.split('/')[1] for n in apk.namelist() if n.startswith('lib/') and n.endswith('.so')}
    driver='lib/armeabi-v7a/libmycamera.so'
    if args.native:
        assert abis=={'armeabi-v7a'}, abis
        data=apk.read(driver)
        assert hashlib.sha256(data).hexdigest()=='fa9d9a7936f2ae12ca8f6540f214bb581c0b26ac5222c1b76384dcf4f2795bd9'
        assert data[:5]==b'\x7fELF\x01' and int.from_bytes(data[18:20],'little')==40
    else:
        assert not any(n.endswith('/libmycamera.so') for n in apk.namelist())
        assert {'armeabi-v7a','arm64-v8a'}<=abis, abis
    assert hashlib.sha256(apk.read('assets/vehicles_ssd_mobilenet_v2.tflite')).hexdigest()=='42fb3d70ffb7bb37dd518f730f7be784b831c2078f30c497d0019cc2e987fa26'
    with tempfile.TemporaryDirectory(prefix='dashcam-jni-') as directory:
        listings=[]
        for n in apk.namelist():
            if re.fullmatch(r'classes\d*\.dex',n):
                path=Path(directory)/n;path.write_bytes(apk.read(n))
                # DEX strings use modified UTF-8; escape unrelated surrogate bytes.
                listings.append(subprocess.check_output([str(dexdump),str(path)]).decode('utf-8',errors='backslashreplace'))
        listing='\n'.join(listings)
def methods(class_name):
    block=re.search(r"Class descriptor\s+: 'Lgit/my/camera/core/"+re.escape(class_name)+r";'(.*?)(?=Class #|\Z)",listing,re.S)
    assert block is not None, class_name
    return {name:(signature,access) for name,signature,access in re.findall(r"name\s+: '(.*?)'\s+type\s+: '(\(.*?\).*?)'\s+access\s+:.*?\((.*?)\)",block.group(1))}
actual=methods('MyCamera')
for method in expected:
    signature,access=actual[method['name']]
    assert signature==method['signature'] and 'NATIVE' in access and ('STATIC' in access)==method['static'],method
for name,required in callbacks.items():
    actual=methods('MyCamera$'+name)
    for method,signature in required.items(): assert actual[method][0]==signature,(name,method)
print(json.dumps({'apk':args.apk.name,'native_profile':args.native,'abis':sorted(abis),'jni_methods_verified':len(expected),'callback_methods_verified':sum(len(m) for m in callbacks.values()),'model_sha256_verified':True,'native_runtime_tested':False},indent=2))
