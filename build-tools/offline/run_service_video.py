#!/usr/bin/env python3
"""Real SSD inference + shared Kotlin lanes/risk, not a simulated camera or annotated accuracy test."""
from pathlib import Path
import os, subprocess, struct, hashlib, csv, json, time
import numpy as np
from PIL import Image
from ai_edge_litert.interpreter import Interpreter

import sys
repo=Path(__file__).resolve().parents[2]; video=Path(sys.argv[1]);out=Path(sys.argv[2]);out.mkdir(parents=True,exist_ok=True)
model=repo/'app/src/main/assets/vehicles_ssd_mobilenet_v2.tflite'
assert hashlib.sha256(model.read_bytes()).hexdigest()=='42fb3d70ffb7bb37dd518f730f7be784b831c2078f30c497d0019cc2e987fa26'
interpreter=Interpreter(model_path=str(model),num_threads=2);interpreter.allocate_tensors()
input_info=interpreter.get_input_details()[0];outputs=interpreter.get_output_details()
assert input_info['shape'].tolist()==[1,300,300,3] and input_info['dtype']==np.uint8
stdlib=next(Path(os.environ['GRADLE_USER_HOME']).glob('caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.0.21/*/*.jar'))
classpath=str(repo/'app/build/tmp/kotlin-classes/debug')+os.pathsep+str(stdlib)
subprocess.run(['javac','--release','17','-cp',classpath,'-d',str(out/'classes'),str(repo/'build-tools/offline/ServiceVisionProbe.java')],check=True)
latencies=[];all_detections=[];frames=0
with (out/'risk-trace.csv').open('w') as trace,(out/'execution.log').open('w') as errors:
    processor=subprocess.Popen(['java','-cp',str(out/'classes')+os.pathsep+classpath,'ServiceVisionProbe'],stdin=subprocess.PIPE,stdout=trace,stderr=errors)
    decoder=subprocess.Popen(['ffmpeg','-nostdin','-v','error','-i',str(video),'-vf','fps=3,scale=320:180','-pix_fmt','rgb24','-f','rawvideo','pipe:1'],stdout=subprocess.PIPE,stderr=errors)
    try:
        while True:
            frame_bytes=decoder.stdout.read(320*180*3)
            if not frame_bytes: break
            if len(frame_bytes)!=320*180*3: raise RuntimeError('Truncated decoder frame')
            rgb=np.frombuffer(frame_bytes,np.uint8).reshape((180,320,3))
            resized=np.asarray(Image.fromarray(rgb).resize((300,300),Image.Resampling.BILINEAR),np.uint8)
            interpreter.set_tensor(input_info['index'],resized[None])
            started=time.perf_counter();interpreter.invoke();latencies.append((time.perf_counter()-started)*1000)
            boxes=interpreter.get_tensor(outputs[0]['index'])[0];classes=interpreter.get_tensor(outputs[1]['index'])[0]
            scores=interpreter.get_tensor(outputs[2]['index'])[0];count=int(interpreter.get_tensor(outputs[3]['index'])[0])
            candidates=[]
            for i in range(min(count,20)):
                category=int(classes[i]);score=float(scores[i])
                if category not in [2,5,7] or score<.5: continue
                top,left,bottom,right=[float(v) for v in np.clip(boxes[i],0,1)]
                if right<=left or bottom<=top: continue
                candidates.append((left,top,right,bottom,score,category))
                all_detections.append([frames,frames/3,category,score,left,top,right,bottom])
            gray=((rgb[:,:,0].astype(np.uint16)*30+rgb[:,:,1].astype(np.uint16)*59+rgb[:,:,2].astype(np.uint16)*11)//100).astype(np.uint8)
            processor.stdin.write(struct.pack('>iq',frames,round(frames*1000/3))+gray.tobytes()+struct.pack('>i',len(candidates)))
            for candidate in candidates: processor.stdin.write(struct.pack('>fffffi',*candidate))
            frames+=1
        processor.stdin.close()
        processor_exit=processor.wait();decoder_exit=decoder.wait()
        if processor_exit or decoder_exit: raise RuntimeError(f'Processor={processor_exit}, decoder={decoder_exit}')
    except BaseException:
        processor.kill();decoder.kill();raise
if frames==0:raise RuntimeError('No real frames processed')
with (out/'vehicle-candidates.csv').open('w') as f:
    writer=csv.writer(f);writer.writerow(['frame','time_s','coco_class','confidence','left','top','right','bottom']);writer.writerows(all_detections)
summary={'frames':frames,'vehicle_candidates':len(all_detections),'frames_with_vehicle_candidates':len(set(r[0] for r in all_detections)),
 'cpu_inference_ms_median':float(np.median(latencies)),'cpu_inference_ms_p95':float(np.percentile(latencies,95)),
 'runtime':'ai-edge-litert 1.4.0 CPU 2 threads; host, not Android device','reference_annotations':False,'measured_gps_speed':False,'calibration_available':False}
(out/'summary.json').write_text(json.dumps(summary,indent=2))
print(json.dumps(summary,indent=2));print((out/'execution.log').read_text())
