#!/usr/bin/env python3
"""Generate the classifier centroids asset from the seed phrases.

Run offline, not on-device. Encoding the ~44 seed phrases at app start would
cost 44 encoder passes before the first message could be classified, so the
centroids are computed here and shipped as a small binary asset instead
(11 classes x 2 gates + category centroids, 384 floats each, a few tens of KB).

The seed phrases live in `tools/encoder_eval.py` and are deliberately hand-written
from the category definitions rather than sampled from the golden corpus — if the
corpus seeded the centroids, scoring on the same corpus would measure
memorisation instead of generalisation.

Usage:
    .zcode/enc-venv/bin/python tools/build_embedding_asset.py \\
        --model .zcode/enc/minilm-arm64/model_qint8_arm64.onnx \\
        --out app/src/main/assets/auraspend_embedding.bin
"""

from __future__ import annotations

import argparse
import json
import struct
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from encoder_eval import (  # noqa: E402
    CATEGORY_SEEDS, EXPENSE_SEEDS, INCOME_SEEDS, NEGATIVE_SEEDS, Encoder,
)

REPO = Path(__file__).resolve().parent.parent
MAGIC = b"AURAEMB1"


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--model", default=".zcode/enc/minilm-arm64/model_qint8_arm64.onnx")
    ap.add_argument("--out", default="app/src/main/assets/auraspend_embedding.bin")
    args = ap.parse_args()

    model = Path(args.model)
    if not model.exists():
        sys.exit(f"model not found: {model}")

    enc = Encoder(model)

    def centroid(seeds: list[str]) -> np.ndarray:
        v = enc.encode(seeds).mean(axis=0)
        return (v / np.linalg.norm(v)).astype(np.float32)

    # Gate 1: is this a transaction at all? Index 0 = not, 1 = yes.
    gate_txn = [
        centroid(NEGATIVE_SEEDS),
        centroid(EXPENSE_SEEDS + INCOME_SEEDS),
    ]
    # Gate 2: income vs expense, only consulted for messages gate 1 accepted.
    gate_type = [centroid(INCOME_SEEDS), centroid(EXPENSE_SEEDS)]
    # Gate 3: category, only for expenses. Income maps to cat_salary by type.
    cat_ids = list(CATEGORY_SEEDS)
    gate_cat = [centroid(CATEGORY_SEEDS[c]) for c in cat_ids]

    dim = gate_txn[0].shape[0]
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)

    with out.open("wb") as f:
        f.write(MAGIC)
        f.write(struct.pack("<I", dim))
        meta = {
            "transaction": ["not_a_transaction", "transaction"],
            "type": ["income", "expense"],
            "category": cat_ids,
            "model": model.name,
        }
        blob = json.dumps(meta).encode()
        f.write(struct.pack("<I", len(blob)))
        f.write(blob)
        for name, group in (
            ("transaction", gate_txn), ("type", gate_type), ("category", gate_cat)
        ):
            f.write(struct.pack("<B", len(group)))
            for vec in group:
                f.write(vec.tobytes())

    size = out.stat().st_size
    print(f"wrote {out}  ({size / 1024:.1f} KB, dim={dim})")
    print(f"  transaction centroids : {len(gate_txn)}")
    print(f"  type centroids        : {len(gate_type)}")
    print(f"  category centroids    : {len(gate_cat)}  {cat_ids}")

    # Verify the asset round-trips: reload it and confirm every vector matches.
    # This reads the format exactly as EmbeddingClassifier will, including the
    # per-group count byte — a check that skipped it would have compared a length
    # prefix against a float and "verified" nothing.
    with out.open("rb") as f:
        assert f.read(len(MAGIC)) == MAGIC, "magic mismatch"
        d = struct.unpack("<I", f.read(4))[0]
        assert d == dim, f"dim mismatch: header {d} != computed {dim}"
        n = struct.unpack("<I", f.read(4))[0]
        json.loads(f.read(n))
        for name, group in (
            ("transaction", gate_txn), ("type", gate_type), ("category", gate_cat)
        ):
            count = struct.unpack("<B", f.read(1))[0]
            assert count == len(group), f"{name}: {count} centroids, expected {len(group)}"
            for i, expected in enumerate(group):
                got = np.frombuffer(f.read(d * 4), dtype=np.float32)
                assert np.allclose(got, expected, atol=1e-6), f"{name}[{i}] mismatch"
        assert f.read() == b"", "trailing bytes after the last centroid"
    print("  round-trip verified (all 15 vectors, no trailing bytes)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
