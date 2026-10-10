#!/usr/bin/env python3
"""Tolerance-based screenshot drift gate for the Aurora baselines.

Robolectric renders on the JVM, and identical app code still produces a few
hundred thousand differing *bytes* (a few percent of pixels) across CPU
architectures and operating systems — antialiasing noise. A byte-exact
`git status` comparison therefore fails on every machine that did not record
the baselines, which is what happened when the macOS-recorded baselines first
ran on the Linux CI runner.

This gate re-records the screenshots (the test task always runs with
`roborazzi.test.record=true`), then compares them against a pristine copy of
the committed baselines. It fails only when:
  * a file is missing or newly added,
  * the image dimensions changed, or
  * more than `--max-diff-ratio` of pixels differ by more than
    `--channel-threshold` in any channel (defaults: 0.5%, 8).

Measured cross-platform noise sits 5-10x below the default threshold, while a
genuine UI change (moved or resized elements) changes far more pixels by far
more than 8 per channel.

Usage:
  python3 tools/compare_screenshots.py --baseline DIR --actual DIR

Example (CI): snapshot `app/src/test/screenshots` before the test run, run
the tests, then compare the pristine snapshot against the re-recorded
directory.
"""
from __future__ import annotations

import argparse
import filecmp
import os
import sys

from PIL import Image, ImageChops


def strong_diff_stats(actual_path: str, baseline_path: str, channel_threshold: int):
    """Return (strong_pixel_count, total_pixels, max_delta, size_mismatch)."""
    a = Image.open(actual_path).convert("RGB")
    b = Image.open(baseline_path).convert("RGB")
    if a.size != b.size:
        return 0, 0, 0, (b.size, a.size)

    diff = ImageChops.difference(a, b)
    r, g, bl = diff.split()
    table = [0 if v <= channel_threshold else 255 for v in range(256)]
    strong = ImageChops.lighter(
        ImageChops.lighter(r.point(table), g.point(table)), bl.point(table)
    )
    strong_count = strong.histogram()[255]
    max_delta = max(r.getextrema()[1], g.getextrema()[1], bl.getextrema()[1])
    total = a.size[0] * a.size[1]
    return strong_count, total, max_delta, None


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--baseline", required=True, help="pristine committed baselines")
    ap.add_argument("--actual", required=True, help="freshly re-recorded screenshots")
    ap.add_argument(
        "--max-diff-ratio",
        type=float,
        default=0.005,
        help="max share of pixels allowed to differ strongly (default 0.5%%)",
    )
    ap.add_argument(
        "--channel-threshold",
        type=int,
        default=8,
        help="per-channel delta that counts as a real difference (default 8)",
    )
    args = ap.parse_args()

    baseline_files = sorted(f for f in os.listdir(args.baseline) if f.endswith(".png"))
    actual_files = sorted(f for f in os.listdir(args.actual) if f.endswith(".png"))

    failures: list[str] = []
    for f in sorted(set(baseline_files) - set(actual_files)):
        failures.append(f"{f}: committed baseline was not re-recorded")
    for f in sorted(set(actual_files) - set(baseline_files)):
        failures.append(f"{f}: new screenshot was recorded but is not committed")

    print(f"{'file':38s} {'strong-diff%':>12s} {'maxΔ':>5s}  verdict")
    for f in baseline_files:
        if f not in actual_files:
            continue
        actual_path = os.path.join(args.actual, f)
        baseline_path = os.path.join(args.baseline, f)

        if filecmp.cmp(baseline_path, actual_path, shallow=False):
            print(f"{f:38s} {0.0:11.3f}% {0:5d}  identical")
            continue

        strong, total, max_delta, mismatch = strong_diff_stats(
            actual_path, baseline_path, args.channel_threshold
        )
        if mismatch is not None:
            failures.append(f"{f}: size changed {mismatch[0]} -> {mismatch[1]}")
            print(f"{f:38s} {'-':>12s} {'-':>5s}  SIZE CHANGED")
            continue

        ratio = strong / total if total else 0.0
        verdict = "ok"
        if ratio > args.max_diff_ratio:
            verdict = "DRIFT"
            failures.append(
                f"{f}: {ratio:.3%} of pixels differ strongly "
                f"(> {args.max_diff_ratio:.3%} allowed)"
            )
        print(
            f"{f:38s} {ratio * 100:11.3f}% {max_delta:5d}  {verdict}"
        )

    print()
    if failures:
        print("Screenshot drift beyond the cross-platform noise floor:")
        for f in failures:
            print(f"  - {f}")
        print(
            "\nIf the change is intentional, re-record the baselines with "
            "./gradlew :app:testProdDebugUnitTest and commit the PNGs."
        )
        return 1

    print(
        f"All screenshots within tolerance "
        f"(max strong-diff {args.max_diff_ratio:.3%}, channel Δ ≤ {args.channel_threshold})."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
