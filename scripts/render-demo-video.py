"""Export the recorded local walkthrough as a captioned 1080p MP4.

Requires imageio-ffmpeg. Run after frontend/scripts/record-demo.mjs.
"""
import json
import shutil
import subprocess
from collections import Counter
from pathlib import Path
import imageio_ffmpeg

ROOT = Path(__file__).resolve().parents[1]
latest = json.loads((ROOT / 'artifacts/demo-video/latest.json').read_text())
output = Path(latest['output'])
verification = json.loads((output / 'verification.json').read_text())
ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()


def ass_time(seconds):
    centiseconds = round(seconds * 100)
    hours, centiseconds = divmod(centiseconds, 360000)
    minutes, centiseconds = divmod(centiseconds, 6000)
    secs, centiseconds = divmod(centiseconds, 100)
    return f'{hours}:{minutes:02}:{secs:02}.{centiseconds:02}'


def escape(text):
    return text.replace('\\', '\\\\').replace('{', '\\{').replace('}', '\\}')


header = '''[Script Info]
ScriptType: v4.00+
PlayResX: 1600
PlayResY: 900
WrapStyle: 2
ScaledBorderAndShadow: yes

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Title,Segoe UI,20,&H00E2B4A5,&H00FFFFFF,&H00000000,&H00000000,-1,0,0,0,100,100,1,0,1,0,0,7,48,48,816,1
Style: Caption,Segoe UI,25,&H00FFFFFF,&H00FFFFFF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,0,0,7,48,48,846,1

[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
'''
scenes = verification['scenes']
lines = [header]
for index, scene in enumerate(scenes):
    end = scenes[index + 1]['start'] if index + 1 < len(scenes) else verification['duration'] + 1
    for style, key in [('Title', 'title'), ('Caption', 'caption')]:
        lines.append(f'Dialogue: 0,{ass_time(scene["start"])},{ass_time(end)},{style},,0,0,0,,{escape(scene[key])}\n')
(output / 'captions.ass').write_text(''.join(lines), encoding='utf-8-sig')

def srt_time(seconds):
    milliseconds = round(seconds * 1000)
    hours, milliseconds = divmod(milliseconds, 3600000)
    minutes, milliseconds = divmod(milliseconds, 60000)
    secs, milliseconds = divmod(milliseconds, 1000)
    return f'{hours:02}:{minutes:02}:{secs:02},{milliseconds:03}'

srt = []
for index, scene in enumerate(scenes):
    end = scenes[index + 1]['start'] if index + 1 < len(scenes) else verification['duration']
    srt.append(f'{index + 1}\n{srt_time(scene["start"])} --> {srt_time(end)}\n{scene["title"]}\n{scene["caption"]}\n')
(output / 'captions.srt').write_text('\n'.join(srt), encoding='utf-8')

# Keep the frontend unobscured: captions occupy a new band beneath the screen.
destination = output / 'Freedance-Live-Demo.mp4'
command = [ffmpeg, '-hide_banner', '-y', '-i', latest['video'],
           '-vf', 'pad=1600:900:0:0:color=0x101828,ass=captions.ass,scale=1920:1080:flags=lanczos',
           '-an', '-c:v', 'libx264', '-threads', '4', '-preset', 'medium', '-crf', '19',
           '-pix_fmt', 'yuv420p', '-r', '25', '-movflags', '+faststart', str(destination)]
result = subprocess.run(command, cwd=output, capture_output=True, text=True)
(output / 'export.log').write_text(result.stderr, encoding='utf-8')
if result.returncode:
    raise RuntimeError(result.stderr[-4000:])

# Standalone viewing notes and evidence contain no passwords or authentication tokens.
notes = [
    '# Freedance — Live Microservices Demo',
    '',
    'A captioned, silent recording of real UI operations against the local Docker backend.',
    'Payments are simulated by the backend. No real money moves.',
    '',
    '## Chapters',
]
for scene in scenes:
    seconds = round(scene['start'])
    notes.append(f'- {seconds // 60:02}:{seconds % 60:02} — {scene["title"]}: {scene["caption"]}')
notes += ['', '## Persisted records', '', '```json', json.dumps(verification['ids'], indent=2), '```',
          '', f'Successful browser API requests: {len(verification["requests"])}.',
          f'API/browser errors during recording: {len(verification["failures"])}.',
          '', 'The recording covers user, job, proposal, contract, and wallet services through the API gateway.',
          'It demonstrates the main workflow, not every endpoint or failure path.']
services = {'auth': 'User service', 'users': 'User service', 'jobs': 'Job service',
            'proposals': 'Proposal service', 'contracts': 'Contract service', 'payouts': 'Wallet service'}
counts = Counter(services.get(request['path'].split('/')[2], 'Other') for request in verification['requests'])
notes += ['', '## Requests through the API gateway', '', '| Service | Successful requests |', '|---|---:|']
notes += [f'| {service} | {count} |' for service, count in counts.items()]
(output / 'README.md').write_text('\n'.join(notes) + '\n', encoding='utf-8')
latest['mp4'] = str(destination)
(ROOT / 'artifacts/demo-video/latest.json').write_text(json.dumps(latest, indent=2))
print(destination)
print(f'{destination.stat().st_size / 1024 / 1024:.1f} MB')
