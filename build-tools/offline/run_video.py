#!/usr/bin/env python3
"""Execute the Android candidate detector on a real horizontal 16:9 video.
Usage: source /workspace/cloud-setup/env.sh; ./gradlew :app:compileDebugKotlin
       python3 build-tools/offline/run_video.py VIDEO.mp4 OUTPUT_DIRECTORY
Output is candidate geometry; it does not measure accuracy or trigger LDW.
"""
from pathlib import Path
import hashlib, json, os, subprocess, sys

repo=Path(__file__).resolve().parents[2]
video=Path(sys.argv[1]).resolve()
out=Path(sys.argv[2]).resolve()
if not video.is_file(): raise SystemExit('Input video missing')
out.mkdir(parents=True,exist_ok=True)
metadata=subprocess.check_output(['ffprobe','-v','error','-show_entries','format=duration:stream=codec_name,codec_type,width,height,r_frame_rate','-of','json',str(video)],text=True)
stream=next(s for s in json.loads(metadata)['streams'] if s['codec_type']=='video')
if abs(stream['width']/stream['height']-16/9)>.02: raise SystemExit('This probe requires a horizontal 16:9 video; define/test orientation before analyzing other inputs')
(out/'video-metadata.json').write_text(metadata)
stdlibs=list(Path(os.environ['GRADLE_USER_HOME']).glob('caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.0.21/*/*.jar'))
if len(stdlibs)!=1: raise SystemExit('Build Android dependencies first; expected Kotlin stdlib 2.0.21')
classes=repo/'app/build/tmp/kotlin-classes/debug'
if not (classes/'br/com/dashcamadas/LaneGeometry.class').is_file(): raise SystemExit('Run :app:compileDebugKotlin first')
classpath=str(classes)+os.pathsep+str(stdlibs[0])
subprocess.run(['javac','--release','17','-cp',classpath,'-d',str(out/'classes'),str(repo/'build-tools/offline/VideoLaneProbe.java')],check=True)
with (out/'lane-candidates.csv').open('w') as data,(out/'detector.log').open('w') as err:
    decoder=subprocess.Popen(['ffmpeg','-nostdin','-v','error','-i',str(video),'-vf','fps=3,scale=320:180','-pix_fmt','gray','-f','rawvideo','pipe:1'],stdout=subprocess.PIPE,stderr=err)
    probe=subprocess.Popen(['java','-cp',str(out/'classes')+os.pathsep+classpath,'VideoLaneProbe','320','180'],stdin=decoder.stdout,stdout=data,stderr=err)
    decoder.stdout.close()
    probe_exit=probe.wait();decode_exit=decoder.wait()
    if probe_exit or decode_exit: raise SystemExit(f'Decoder failed ({decode_exit}) or detector failed ({probe_exit}); see detector.log')
digest=hashlib.sha256()
with video.open('rb') as source:
    for chunk in iter(lambda:source.read(1024*1024),b''): digest.update(chunk)
(out/'input-sha256.txt').write_text(digest.hexdigest()+'  '+video.name+'\n')
print((out/'detector.log').read_text())
