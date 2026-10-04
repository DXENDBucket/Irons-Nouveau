"""Migrate a pack's Iron glyph config display to tier one, preserving a backup."""
from pathlib import Path
import re
import shutil

pack = Path.cwd()
folder = pack / "config" / "irons_nouveau"
backup = pack / "config" / "irons_nouveau-backup-0.9.0"
changed = 0
for path in folder.glob("glyph_*.toml"):
    original = path.read_text(encoding="utf-8")
    updated = re.sub(r"(?m)^(\s*glyph_tier\s*=\s*)\d+", r"\g<1>1", original)
    updated = re.sub(r"(#\s*The tier of the glyph\r?\n\s*# Default: )\d+", r"\g<1>1", updated)
    if original == updated:
        continue
    backup.mkdir(exist_ok=True)
    saved = backup / path.name
    if not saved.exists():
        shutil.copy2(path, saved)
    path.write_text(updated, encoding="utf-8", newline="")
    changed += 1
print(f"Updated {changed} glyph configs; backups: {backup}")
