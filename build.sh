#!/bin/bash
set -e

JAVA6="/opt/java6/bin"
ROOT="$(dirname "$0")"
SRC="$ROOT/src"
OUT="$ROOT/out"
JAR="$OUT/Kokotmetr.jar"

mkdir -p "$OUT"

echo "Compiling..."
$JAVA6/javac -source 1.4 -target 1.4 -d "$OUT" "$SRC/Kokotmetr.java"

echo "Packaging..."
$JAVA6/jar cfm "$JAR" <(echo -e "Manifest-Version: 1.0\nMain-Class: Kokotmetr\n") \
    -C "$OUT" Kokotmetr.class \
    -C "$OUT" 'Kokotmetr$1.class' \
    -C "$OUT" 'Kokotmetr$2.class' \
    -C "$OUT" 'Kokotmetr$3.class' \
    -C "$OUT" 'Kokotmetr$4.class' \
    -C "$OUT" 'Kokotmetr$ProgressBar.class' \
    -C "$ROOT/assets" kokotmetr_linka.jpg \
    -C "$ROOT/assets" kokotmetr_radius.jpg

echo "Done: $JAR"
