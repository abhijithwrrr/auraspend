#!/usr/bin/env bash
# Offline privacy audit of a prebuilt native inference runtime.
#
# The app's whole promise is that financial data stays on the device. Adding a
# third-party native library is the single highest-risk change for that promise,
# so this checks the artifact rather than trusting the vendor's documentation.
#
# Needle's README states telemetry is on by default and is disabled with
# NEEDLE_TELEMETRY=0 / DO_NOT_TRACK=1 — both environment variables, which Android
# does not reliably hand to a statically-linked library. Before wiring it in, the
# question "can this code phone home?" deserves a direct answer.
#
# Usage:  tools/audit_native_runtime.sh <path-to-lib.a> [expected-symbol-regex]
# Re-run whenever the vendor publishes a new build; a changed digest means the
# answer below no longer applies and this must be repeated.
set -uo pipefail

LIB="${1:-}"
# Symbol that proves the scan is actually reading the artifact. Defaults to the
# Needle C API; override to audit a different library.
EXPECT="${2:-needle_(init|complete|load)}"
if [[ -z "$LIB" || ! -f "$LIB" ]]; then
  echo "usage: $0 <path to .a or .so> [expected-symbol-regex]" >&2
  exit 2
fi

echo "artifact : $LIB"
echo "size     : $(wc -c < "$LIB") bytes"
echo "sha256   : $(shasum -a 256 "$LIB" | awk '{print $1}')"
echo

fail=0

# A scan that finds nothing is only meaningful if it can find something. Prove
# the method works before trusting a clean result.
if nm "$LIB" 2>/dev/null | grep -qE "$EXPECT"; then
  echo "[ok]   scan validated: found expected API symbols ($EXPECT)"
else
  echo "[FAIL] scan is broken - found no needle_* symbols, so a clean result below"
  echo "       would be meaningless. Do not trust this run."
  exit 1
fi
echo

# The decisive check: a network capability has to come from an import, because
# the library cannot reach the network without linking something that does it.
# NOTE: do not use \b here. POSIX ERE has no word boundary, so grep -E '\b'
# matches a literal backspace and this check would silently match nothing —
# i.e. it would pass every binary, including ones that phone home.
NETSYMS="(socket|connect|getaddrinfo|gethostbyname|gethostbyaddr|SSL_|curl_|bind|listen|sendto|recvfrom|recvmsg|sendmsg)"
NET=$(nm -u "$LIB" 2>/dev/null | grep -cE "$NETSYMS")
if [[ "$NET" -eq 0 ]]; then
  echo "[ok]   no network symbols imported (socket/connect/SSL/curl/getaddrinfo)"
else
  echo "[FAIL] $NET network symbol(s) imported - this library can reach the network:"
  nm -u "$LIB" 2>/dev/null | grep -E "$NETSYMS" | head
  fail=1
fi

# A self-contained HTTP stack would still leave hostnames and URLs behind.
URLS=$(strings -a "$LIB" 2>/dev/null | grep -cE 'https?://|\.com/|\.io/|\.dev/')
if [[ "$URLS" -eq 0 ]]; then
  echo "[ok]   no URLs or hostnames embedded in the binary"
else
  echo "[FAIL] $URLS URL/hostname string(s) embedded:"
  strings -a "$LIB" 2>/dev/null | grep -oE 'https?://[a-zA-Z0-9._/-]+' | sort -u | head
  fail=1
fi

# Runtime code loading is how a static scan gets side-stepped.
DYN=$(nm -u "$LIB" 2>/dev/null | grep -cE '^(.* )U _(dlopen|dlsym|system|popen|fork|execve)$')
if [[ "$DYN" -eq 0 ]]; then
  echo "[ok]   no dlopen/dlsym/system/exec - no runtime code loading"
else
  echo "[FAIL] runtime code loading symbols present:"
  nm -u "$LIB" 2>/dev/null | grep -E 'U _(dlopen|dlsym|system|popen|fork|execve)$' | head
  fail=1
fi

# Explicit telemetry surface, independent of any capability to use it.
TEL=$(strings -a "$LIB" 2>/dev/null | grep -icE 'telemetry|posthog|sentry|amplitude|mixpanel|segment\.io|analytics_key')
if [[ "$TEL" -eq 0 ]]; then
  echo "[ok]   no telemetry/analytics strings in the binary"
else
  echo "[FAIL] $TEL telemetry string(s) present:"
  strings -a "$LIB" 2>/dev/null | grep -iE 'telemetry|posthog|sentry|amplitude|mixpanel' | sort -u | head
  fail=1
fi

echo
if [[ "$fail" -eq 0 ]]; then
  echo "RESULT: no reachable network or telemetry surface in this artifact."
  echo "        Still re-run on every version bump, and confirm on-device."
else
  echo "RESULT: blocked. Do not ship this artifact until understood."
fi
exit "$fail"
