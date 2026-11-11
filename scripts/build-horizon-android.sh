#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LIB_NAME="libhorizon_android.so"
JNI_LIBS_DIR="$ROOT_DIR/app/src/main/jniLibs"
ABI_DIR="$JNI_LIBS_DIR/arm64-v8a"
LIB_PATH="$ABI_DIR/$LIB_NAME"

if ! command -v cargo >/dev/null 2>&1; then
  echo "ERRO: Rust/Cargo não está instalado." >&2
  exit 1
fi
if ! command -v cargo-ndk >/dev/null 2>&1; then
  echo "ERRO: instale cargo-ndk (cargo install cargo-ndk)." >&2
  exit 1
fi

NDK_DIR="${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-}}"
if [[ -z "$NDK_DIR" || ! -d "$NDK_DIR" ]]; then
  echo "ERRO: defina ANDROID_NDK_HOME ou ANDROID_NDK_ROOT para uma pasta NDK válida." >&2
  exit 1
fi

# Evita que duas variáveis apontem para NDKs diferentes e que o cargo-ndk
# escolha silenciosamente uma instalação diferente da validada no CI.
export ANDROID_NDK_HOME="$NDK_DIR"
export ANDROID_NDK_ROOT="$NDK_DIR"

cd "$ROOT_DIR/horizon-android"
cargo ndk -t arm64-v8a --platform 26 -o "$JNI_LIBS_DIR" build --release

if [[ ! -s "$LIB_PATH" ]]; then
  echo "ERRO: cargo-ndk não gerou a biblioteca esperada: $LIB_PATH" >&2
  echo "Arquivos .so encontrados no diretório jniLibs:" >&2
  find "$JNI_LIBS_DIR" -type f -name '*.so' -print >&2 || true
  exit 1
fi

file "$LIB_PATH"
echo "Biblioteca ARM64 gerada em: $LIB_PATH"
