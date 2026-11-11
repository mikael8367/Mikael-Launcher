#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET="aarch64-linux-android"
LIB_NAME="libhorizon_android.so"
DEST_DIR="$ROOT_DIR/app/src/main/jniLibs/arm64-v8a"

if ! command -v cargo >/dev/null 2>&1; then
  echo "ERRO: Rust/Cargo não está instalado." >&2
  exit 1
fi
if ! command -v cargo-ndk >/dev/null 2>&1; then
  echo "ERRO: instale cargo-ndk (cargo install cargo-ndk)." >&2
  exit 1
fi
if [[ -z "${ANDROID_NDK_HOME:-}${ANDROID_NDK_ROOT:-}" ]]; then
  echo "ERRO: defina ANDROID_NDK_HOME ou ANDROID_NDK_ROOT." >&2
  exit 1
fi

cd "$ROOT_DIR/horizon-android"
cargo ndk -t arm64-v8a -o "$DEST_DIR" build --release
test -s "$DEST_DIR/$LIB_NAME"
file "$DEST_DIR/$LIB_NAME"
echo "Biblioteca ARM64 gerada em: $DEST_DIR/$LIB_NAME"
