"""Download the pinned development dependencies; never bundle upstream mods."""
import hashlib
import json
from pathlib import Path
from urllib.request import Request, urlopen

root = Path(__file__).resolve().parents[1]
libs = root / 'libs'
libs.mkdir(exist_ok=True)
for dependency in json.loads((root / 'docs/forge-dependencies.json').read_text()):
    target = libs / dependency['file']
    if target.exists() and hashlib.sha512(target.read_bytes()).hexdigest() == dependency['sha512']:
        continue
    with urlopen(Request(dependency['url'], headers={'User-Agent': 'Iron-and-Nouveau-development'}), timeout=120) as response:
        data = response.read()
    if hashlib.sha512(data).hexdigest() != dependency['sha512']:
        raise ValueError('Hash mismatch: ' + dependency['file'])
    target.write_bytes(data)
    print(dependency['project'], dependency['version'])
