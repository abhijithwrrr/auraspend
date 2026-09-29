#!/usr/bin/env python3
"""One-shot migration: replace hardcoded `fontSize = N.sp` with MaterialTheme type-scale styles.

Kept in the repo because the mapping (size -> token) is a design decision worth
revisiting, and re-running it is how a future screen gets onboarded onto the scale.
"""
import re
import sys
from pathlib import Path

UI = Path("app/src/main/java/com/awbuilds/auraspend/ui")

# These files DEFINE the scale; they must keep literal sizes.
EXCLUDE = {UI / "designsystem" / "AuraTokens.kt", UI / "theme" / "Theme.kt"}

# size -> (typography token, weight baked into that token)
MAPPING = {
    11: ("labelSmall", "Medium"),
    12: ("labelMedium", "SemiBold"),
    13: ("bodySmall", "Normal"),
    14: ("bodyMedium", "Normal"),
    15: ("titleSmall", "Medium"),
    16: ("titleMedium", "SemiBold"),
    17: ("titleLarge", "SemiBold"),
    18: ("titleLarge", "SemiBold"),
    20: ("titleLarge", "SemiBold"),
    22: ("headlineSmall", "SemiBold"),
    28: ("headlineMedium", "SemiBold"),
    38: ("displaySmall", "SemiBold"),
}

WEIGHT_NAME = {"Medium": "FontWeight.Medium", "SemiBold": "FontWeight.SemiBold",
               "Normal": "FontWeight.Normal", "Bold": "FontWeight.Bold"}

changed_total = 0

for path in sorted(UI.rglob("*.kt")):
    if path in EXCLUDE:
        continue
    text = path.read_text(encoding="utf-8")
    lines = text.split("\n")
    out = []
    i = 0
    changed = 0
    while i < len(lines):
        line = lines[i]
        m = re.search(r"fontSize = (\d+)\.sp,?\s*$", line)
        if not m:
            out.append(line)
            i += 1
            continue

        size = int(m.group(1))
        if size not in MAPPING:
            out.append(line)
            i += 1
            continue

        token, token_weight = MAPPING[size]
        indent = line[: len(line) - len(line.lstrip())]

        # Look at what follows: a simple `fontWeight = X,` we may absorb.
        nxt = lines[i + 1] if i + 1 < len(lines) else ""
        wm = re.search(r"fontWeight = (FontWeight\.\w+),\s*$", nxt)
        absorb_weight = bool(wm) and wm.group(1) == WEIGHT_NAME[token_weight]

        # Rebuild the current line, keeping any prefix before `fontSize`.
        prefix = line[: m.start()].rstrip()
        if prefix.endswith(","):
            prefix = prefix[:-1]
        replacement = f"{prefix}, style = MaterialTheme.typography.{token}," if prefix else \
                     f"{indent}style = MaterialTheme.typography.{token},"

        # A `Text(` on the same line means the args are positional and this is the
        # first argument -> drop the leading comma.
        if re.search(r"Text\([^,]*$", prefix):
            replacement = f"{prefix}, style = MaterialTheme.typography.{token}," if prefix else \
                         f"{indent}style = MaterialTheme.typography.{token},"
        out.append(replacement)
        changed += 1
        i += 1
        if absorb_weight:
            i += 1  # drop the now-redundant fontWeight line
        continue

    if changed:
        path.write_text("\n".join(out), encoding="utf-8")
        print(f"{changed:3d}  {path.relative_to(Path('.'))}")
        changed_total += changed

print(f"\n{changed_total} sites migrated to the type scale")
sys.exit(0)
