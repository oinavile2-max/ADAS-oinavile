#!/usr/bin/env python3
"""Real SSD + Java/Kotlin ONNX session, same NN preprocessing and masks as app; no invented GPS."""
from pathlib import Path
import argparse, csv, hashlib, json, os, struct, subprocess, time
import numpy as np
from PIL import Image
from ai_edge_litert.interpreter import Interpreter

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('video',type=Path);parser.add_argument('output',type=Path)
args=parser.parse_args();repo=Path(__file__).resolve().parents[2];out=args.output;out.mkdir(parents=True,exist_ok=True)
cache=Path(os.environ['GRADLE_USER_HOME'])/'caches/modules-2/files-2.1'
stdlib=next((cache/'org.jetbrains.kotlin/kotlin-stdlib/2.0.21').glob('*/*.jar'))
ort=next((cache/'com.microsoft.onnxruntime/onnxruntime/1.20.0').glob('*/*.jar'))
cp=os.pathsep.join(map(str,[repo/'app/build/tmp/kotlin-classes/debug',stdlib,ort]))
subprocess.run(['javac','--release','17','-cp',cp,'-d',str(out/'classes'),str(repo/'build-tools/offline/NeuralVisionProbe.java')],check=True)
ssd=repo/'app/src/main/assets/vehicles_ssd_mobilenet_v2.tflite';lane=repo/'app/src/main/assets/road_lanes_yolop_320.onnx'
assert hashlib.sha256(ssd.read_bytes()).hexdigest()=='42fb3d70ffb7bb37dd518f730f7be784b831c2078f30c497d0019cc2e987fa26'
assert hashlib.sha256(lane.read_bytes()).hexdigest()=='f93ced8254ccbb532ff9f9593e7b7d0022f28210831b615f5039066c9f5222fe'
interpreter=Interpreter(model_path=str(ssd),num_threads=2);interpreter.allocate_tensors()
input_info=interpreter.get_input_details()[0];output_info=interpreter.get_output_details()
frames=0;detections=[];ssd_ms=[]
with (out/'trace.csv').open('w') as trace,(out/'execution.log').open('w') as errors:
    processor=subprocess.Popen(['java','-cp',str(out/'classes')+os.pathsep+cp,'NeuralVisionProbe',str(lane)],stdin=subprocess.PIPE,stdout=trace,stderr=errors)
    decoder=subprocess.Popen(['ffmpeg','-nostdin','-v','error','-i',str(args.video),'-vf','fps=3,scale=320:180','-pix_fmt','rgb24','-f','rawvideo','pipe:1'],stdout=subprocess.PIPE,stderr=errors)
    try:
        while True:
            data=decoder.stdout.read(320*180*3)
            if not data:break
            if len(data)!=320*180*3:raise RuntimeError('Incomplete decoded video frame')
            rgb=np.frombuffer(data,np.uint8).reshape((180,320,3))
            resized=np.asarray(Image.fromarray(rgb).resize((300,300),Image.Resampling.BILINEAR),np.uint8)
            interpreter.set_tensor(input_info['index'],resized[None]);start=time.perf_counter();interpreter.invoke();ssd_ms.append((time.perf_counter()-start)*1000)
            boxes=interpreter.get_tensor(output_info[0]['index'])[0];classes=interpreter.get_tensor(output_info[1]['index'])[0]
            scores=interpreter.get_tensor(output_info[2]['index'])[0];count=int(interpreter.get_tensor(output_info[3]['index'])[0]);selected=[]
            for i in range(min(count,20)):
                category=int(classes[i]);score=float(scores[i])
                if category not in [2,5,7] or score<.5:continue
                top,left,bottom,right=[float(v) for v in np.clip(boxes[i],0,1)]
                if right<=left or bottom<=top:continue
                selected.append((left,top,right,bottom,score,category));detections.append([frames,category,score,left,top,right,bottom])
            processor.stdin.write(struct.pack('>iq',frames,round(frames*1000/3))+data+struct.pack('>i',len(selected)))
            for detection in selected:processor.stdin.write(struct.pack('>fffffi',*detection))
            frames+=1
        processor.stdin.close()
        a=processor.wait();b=decoder.wait()
        if a or b:raise RuntimeError(f'Neural processor={a}, decoder={b}')
    except BaseException:processor.kill();decoder.kill();raise
if not frames:raise RuntimeError('No video frames')
rows=list(csv.DictReader((out/'trace.csv').open()))
assert len(rows)==frames and all(int(r['critical_risks'])==0 for r in rows)
nn_ms=np.array([float(r['nn_pipeline_ms']) for r in rows])
summary={'frames':frames,'actual_lane_neural_executions':len(rows),'vehicle_candidates':len(detections),
 'frames_with_lane_pixels':sum(int(r['lane_pixels'])>0 for r in rows),'frames_with_road_pixels':sum(int(r['road_pixels'])>0 for r in rows),
 'lane_candidate_frames':sum(bool(r['left_confidence'] or r['right_confidence']) for r in rows),
 'confirmed_pair_frames':sum(bool(r['left_confidence'] and r['right_confidence'] and float(r['left_confidence'])>=.65 and float(r['right_confidence'])>=.65) for r in rows),
 'neural_lane_plus_geometry_ms_median':float(np.median(nn_ms)),'neural_lane_plus_geometry_ms_p95':float(np.percentile(nn_ms,95)),
 'ssd_ms_median':float(np.median(ssd_ms)),'critical_risks':0,'reference_annotations':False,'measured_gps_speed':False,'calibration_available':False,
 'runtime':'Java ONNX Runtime 1.20.0 CPU + shared Kotlin RoadNeuralSession/AdasEngine; host, not Android',
 'video_sha256':hashlib.sha256(args.video.read_bytes()).hexdigest()}
(out/'summary.json').write_text(json.dumps(summary,indent=2))
with (out/'vehicle-candidates.csv').open('w') as f:
    w=csv.writer(f);w.writerow(['frame','class','confidence','left','top','right','bottom']);w.writerows(detections)
print(json.dumps(summary,indent=2))
