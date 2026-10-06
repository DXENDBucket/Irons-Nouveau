"""Optional Forge Hex integration-test dependencies; not included in release jars."""
import hashlib
import json
from pathlib import Path
from urllib.request import urlopen

root = Path(__file__).resolve().parent.parent
destination = root / "build" / "hex-dependencies"
destination.mkdir(parents=True, exist_ok=True)
for entry in json.loads((root / "hex-forge-dependencies.json").read_text(encoding="utf-8")):
    target = destination / (entry["name"] + "-hex1201.jar")
    if target.exists() and hashlib.sha512(target.read_bytes()).hexdigest() == entry["sha512"]:
        continue
    with urlopen(entry["url"], timeout=60) as response:
        data = response.read()
    if hashlib.sha512(data).hexdigest() != entry["sha512"]:
        raise RuntimeError("Checksum mismatch: " + entry["name"])
    target.write_bytes(data)
    print(target.name)
