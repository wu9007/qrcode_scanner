#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
LIB="$ROOT/lib"
OUT="$ROOT/out"
mkdir -p "$LIB" "$OUT"
CORE="$LIB/core-3.5.3.jar"
JAVASE="$LIB/javase-3.5.3.jar"
if [[ ! -f "$CORE" ]]; then
  curl -fsSL -o "$CORE" https://repo1.maven.org/maven2/com/google/zxing/core/3.5.3/core-3.5.3.jar
fi
if [[ ! -f "$JAVASE" ]]; then
  curl -fsSL -o "$JAVASE" https://repo1.maven.org/maven2/com/google/zxing/javase/3.5.3/javase-3.5.3.jar
fi
javac -encoding UTF-8 -cp "$CORE:$JAVASE" -d "$OUT" "$ROOT/DecoderHarness.java"
java -cp "$OUT:$CORE:$JAVASE" DecoderHarness "$OUT"
