#!/usr/bin/env python3
"""Real-device camera/background/MP4 check. Not executed successfully without a connected Android.
Requires adb, ffprobe and the Debug APK. Usage:
 python3 build-tools/test_android_device.py --serial SERIAL --apk APK --install --grant-test-permissions --output RESULTS
Permissions/install are changed only when their explicit flags are supplied.
This test starts a short recording, presses HOME, verifies capture progress, stops and examines the real MP4.
"""
from pathlib import Path
import argparse,json,subprocess,shutil,sys,time,shlex

parser=argparse.ArgumentParser()
parser.add_argument('--serial',required=True);parser.add_argument('--apk',type=Path)
parser.add_argument('--install',action='store_true');parser.add_argument('--grant-test-permissions',action='store_true')
parser.add_argument('--require-gps',action='store_true');parser.add_argument('--output',type=Path,required=True)
parser.add_argument('--native-usb',action='store_true',help='Require native source previously selected/authorized in the app')
parser.add_argument('--require-realtime',action='store_true',help='Require neural processing within the 500 ms alert budget')
args=parser.parse_args();package='br.com.dashcamadas'
adb=shutil.which('adb') or '/workspace/android-sdk/platform-tools/adb'
def run(*cmd): return subprocess.check_output([adb,'-s',args.serial,*cmd],text=True,stderr=subprocess.STDOUT).strip()
devices=subprocess.check_output([adb,'devices'],text=True)
if args.serial+'\tdevice' not in devices: raise SystemExit('No connected/authorized Android with this serial; no physical tests executed')
if run('shell','getprop','ro.kernel.qemu')=='1':raise SystemExit('An emulator is not a physical Android; these physical tests are not executed')
if not shutil.which('ffprobe'): raise SystemExit('ffprobe required to verify actual encoded video')
args.output.mkdir(parents=True,exist_ok=True)
if args.install:
    if not args.apk or not args.apk.is_file():raise SystemExit('Provide --apk for installation')
    run('install','-r',str(args.apk.resolve()))
if args.grant_test_permissions:
    sdk=int(run('shell','getprop','ro.build.version.sdk'))
    perms=['android.permission.CAMERA','android.permission.ACCESS_COARSE_LOCATION','android.permission.ACCESS_FINE_LOCATION']
    if sdk>=33:perms+=['android.permission.POST_NOTIFICATIONS']
    for permission in perms:run('shell','pm','grant',package,permission)
run('shell','am','start','-W','-n',package+'/.MainActivity')
def diagnostic():
    text=run('shell','run-as',package,'cat','files/capture-diagnostics.json')
    data=json.loads(text)
    uptime=float(run('shell','cat','/proc/uptime').split()[0])*1000
    if uptime-data['observed_elapsed_ms']>3000:raise RuntimeError('Stale diagnostics, not proof of an active service')
    return data
def wait_for(predicate):
    last=None
    for _ in range(20):
        try:
            last=diagnostic()
            if predicate(last):return last
        except (subprocess.CalledProcessError,ValueError,RuntimeError):pass
        time.sleep(1)
    raise RuntimeError('Required camera/recording state unavailable; last diagnostics: '+str(last))
def command(value):
    snippet='printf %s '+shlex.quote(value)+' > files/capture-test-command'
    run('shell','run-as',package,'sh','-c',shlex.quote(snippet))
initial=wait_for(lambda d:d['connected'] and d['processed_frames']>=2 and d['model_loaded'] and d.get('lane_neural_inferences',0)>=2 and d.get('vehicle_neural_inferences',0)>=2 and (not args.native_usb or d.get('source_kind')=='USB_NATIVE_RGBA'))
if initial['recording']:raise SystemExit('An existing recording is active; test will not interrupt it')
try:
    command('START_RECORD')
    started=wait_for(lambda d:d['recording'] and d['recording_capture_frames']>0)
    run('shell','input','keyevent','KEYCODE_HOME')
    time.sleep(5)
    background=diagnostic()
    assert background['recording'] and background['processed_frames']>started['processed_frames'] and background['recording_capture_frames']>started['recording_capture_frames'], 'Capture did not continue in background'
    assert background['lane_neural_inferences']>started['lane_neural_inferences'] and background['vehicle_neural_inferences']>started['vehicle_neural_inferences'], 'Real neural execution did not continue in background'
    command('STOP_RECORD')
    closed=wait_for(lambda d:not d['recording'] and d['completed_segments']>initial['completed_segments'])
    clip=closed['last_completed_clip']
    local=args.output/'real-device-clip.mp4'
    with local.open('wb') as f:subprocess.run([adb,'-s',args.serial,'exec-out','run-as',package,'cat',clip],stdout=f,check=True)
    media=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_entries','format=duration:stream=codec_name,width,height','-of','json',str(local)],text=True))
    assert float(media['format']['duration'])>=3 and any(s['codec_name']=='h264' for s in media['streams']), 'MP4/H264 output failed verification'
    if args.require_gps:assert background['gps_speed_available'], 'No recent GPS speed observation; GPS test did not pass'
    if args.require_realtime:assert background['last_inference_ms']<=500,'Neural pipeline exceeded 500 ms; alert latency requirement did not pass'
    run('shell','am','start','-W','-n',package+'/.MainActivity')
    wait_for(lambda d:d['processed_frames']>background['processed_frames'])
    result={'camera_and_background_recording':'passed','neural_execution_in_background':'passed','source_kind':background['source_kind'],'neural_latency_ms':background['last_inference_ms'],'alert_latency_budget_met':background['last_inference_ms']<=500,'media':media,'gps_observation':background['gps_speed_available'],'initial':initial,'background':background,'closed':closed,'visual_interface':'requires manual review','road_adas_accuracy':'not validated'}
    (args.output/'device-result.json').write_text(json.dumps(result,indent=2))
    print(json.dumps(result,indent=2))
finally:
    command('STOP_RECORD')
