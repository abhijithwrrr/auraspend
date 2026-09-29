#!/usr/bin/env python3
"""
Decode an R8 Configuration Analyzer report and print the three Play-relevant scores.

Google requires, from February 2027, a minimum of 25% optimization, 25%
obfuscation and 25% shrinking on any app bundle containing more than 10 MB of
uncompressed DEX. This prints the measured values so that can be checked rather
than assumed.

The score is the percentage of the *live* program R8 was free to act on:

    score = 100 - (pinned / total_live) * 100

where `pinned` counts classes/fields/methods that some keep rule holds back from
that operation, and `total_live` is BuildInfo's live class + field + method
count. This mirrors the report's own JavaScript exactly; the report is a
JavaScript app over an embedded protobuf, so the numbers are otherwise only
visible by rendering it in a browser.

Usage: r8_scores.py <configanalyzer.pb|configanalyzer.html> [--threshold 25]
"""

import base64
import re
import struct
import sys

DONT_OBFUSCATE, DONT_OPTIMIZE, DONT_SHRINK = 0, 1, 2
FIELD_BUILD_INFO = 15
FIELD_KEPT_TABLES = (9, 10, 11)  # class, field, method
FIELD_KEEP_CONSTRAINTS = 12
FIELD_KEEP_RULE_RADIUS = 13
FIELD_GLOBAL_RULE = 14


def varint(buf, i):
    result = shift = 0
    while True:
        byte = buf[i]
        i += 1
        result |= (byte & 0x7F) << shift
        if not byte & 0x80:
            return result, i
        shift += 7


def fields(buf):
    """Yield (field_number, wire_type, value) for one protobuf message."""
    i = 0
    while i < len(buf):
        key, i = varint(buf, i)
        number, wire = key >> 3, key & 7
        if wire == 0:
            value, i = varint(buf, i)
        elif wire == 2:
            length, i = varint(buf, i)
            value = buf[i:i + length]
            i += length
        elif wire == 5:
            value, i = buf[i:i + 4], i + 4
        elif wire == 1:
            value, i = buf[i:i + 8], i + 8
        else:
            raise ValueError(f"unsupported wire type {wire}")
        yield number, wire, value


def packed_varints(buf):
    out, i = [], 0
    while i < len(buf):
        value, i = varint(buf, i)
        out.append(value)
    return out


def load_payload(path):
    """Accept either the raw .pb or the .html that embeds it as base64."""
    with open(path, "rb") as handle:
        head = handle.read(8)
        handle.seek(0)
        if head[:1] == b"<":
            html = handle.read()
            match = re.search(
                r"<script[^>]*>\s*([A-Za-z0-9+/=]{500,})\s*</script>", html, re.S
            )
            if not match:
                raise SystemExit(f"{path}: no embedded payload found")
            return base64.b64decode(match.group(1))
        return handle.read()


def parse(raw):
    constraints = {}
    rules = {}
    globals_ = set()
    kept_by = {n: [] for n in FIELD_KEPT_TABLES}
    build = {}

    for number, wire, value in fields(raw):
        if number == FIELD_BUILD_INFO:
            for sub, _, leaf in fields(value):
                build[sub] = leaf
        elif number in FIELD_KEPT_TABLES:
            keep = []
            for sub, sub_wire, leaf in fields(value):
                if sub == 4:  # kept_by
                    keep += packed_varints(leaf) if sub_wire == 2 else [leaf]
            kept_by[number].append(keep)
        elif number == FIELD_KEEP_CONSTRAINTS:
            cid, values = None, []
            for sub, sub_wire, leaf in fields(value):
                if sub == 1:
                    cid = leaf
                elif sub == 2:
                    values += packed_varints(leaf) if sub_wire == 2 else [leaf]
            constraints[cid] = values
        elif number == FIELD_KEEP_RULE_RADIUS:
            rid = cid = source = None
            for sub, _, leaf in fields(value):
                if sub == 1:
                    rid = leaf
                elif sub == 2:
                    source = leaf.decode(errors="replace")
                elif sub == 3:
                    cid = leaf
            rules[rid] = (cid, source)
        elif number == FIELD_GLOBAL_RULE:
            for sub, _, leaf in fields(value):
                if sub == 2:
                    globals_.add(leaf.decode(errors="replace"))

    return constraints, rules, globals_, kept_by, build


def score(constraints, rules, globals_, kept_by, build, constraint, global_flag):
    total = build.get(4, 0) + build.get(5, 0) + build.get(6, 0)
    if total == 0:
        return 0.0, 0, total
    if global_flag in globals_:
        return 0.0, total, total
    pinned = 0
    for table in FIELD_KEPT_TABLES:
        for keep in kept_by[table]:
            for rid in keep:
                cid, _ = rules.get(rid, (None, None))
                if cid in constraints and constraint in constraints[cid]:
                    pinned += 1
                    break
    return 100 - (pinned / total) * 100, pinned, total


def main():
    argv = sys.argv[1:]
    threshold = 25.0
    if "--threshold" in argv:
        index = argv.index("--threshold")
        threshold = float(argv[index + 1])
        del argv[index:index + 2]
    if not argv:
        raise SystemExit(__doc__)
    path = argv[0]

    constraints, rules, globals_, kept_by, build = parse(load_payload(path))
    total = build.get(4, 0) + build.get(5, 0) + build.get(6, 0)
    print(f"  live program: {total} members "
          f"({build.get(4)} classes / {build.get(5)} fields / {build.get(6)} methods)")
    print(f"  keep rules:   {len(rules)}   global: {sorted(globals_) or 'none'}")
    if globals_ & {"-dontobfuscate", "-dontoptimize", "-dontshrink"}:
        print("  WARNING: a global dont* rule is in force; scores are pinned to 0.")

    failed = False
    for label, constraint, flag in (
        ("Obfuscation", DONT_OBFUSCATE, "-dontobfuscate"),
        ("Optimization", DONT_OPTIMIZE, "-dontoptimize"),
        ("Shrinking", DONT_SHRINK, "-dontshrink"),
    ):
        pct, pinned, denom = score(
            constraints, rules, globals_, kept_by, build, constraint, flag
        )
        ok = pct >= threshold
        failed |= not ok
        print(f"  {label:13s} {pct:7.3f}%  ({pinned}/{denom} pinned)  "
              f"{'PASS' if ok else 'FAIL'} vs {threshold:g}%")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
