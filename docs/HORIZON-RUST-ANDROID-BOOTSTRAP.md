# Horizon Rust — bootstrap Android ARM64

## O que entrou nesta etapa

- Crate Rust independente em `horizon-android/`, com saída `cdylib`.
- Ponte JNI Kotlin em `HorizonNativeBridge.kt`.
- Build script `scripts/build-horizon-android.sh` para produzir `libhorizon_android.so` em `app/src/main/jniLibs/arm64-v8a/`.
- Testes unitários Rust para dimensões inválidas, criação/remoção de sessão e handles inválidos.

## Limite explícito

Isto é somente um bootstrap JNI. A sessão guarda dimensões e um handle; ainda não cria uma janela/superfície, não inicializa wgpu, não consulta Vulkan/GLES, não processa toque e não executa o Horizon. Os campos de capacidade retornam false intencionalmente para não anunciar recursos ainda não integrados.

A biblioteca nativa é carregada apenas quando `HorizonNativeBridge.load()` é chamada. Se o .so não estiver empacotado, a ponte retorna indisponibilidade em vez de derrubar a tela do launcher.

## Build em ambiente com Android NDK

Requisitos: Rust toolchain, target `aarch64-linux-android`, Android NDK e `cargo-ndk`.

```bash
rustup target add aarch64-linux-android
cargo install cargo-ndk
export ANDROID_NDK_HOME="$ANDROID_SDK_ROOT/ndk/<versao-instalada>"
bash scripts/build-horizon-android.sh
```

Depois do build, o Gradle empacota automaticamente `app/src/main/jniLibs/arm64-v8a/libhorizon_android.so` no APK quando uma compilação APK for solicitada. Nenhum binário é incluído no Git e nenhum APK é gerado nesta etapa.

## Próximas etapas reais

1. Testar carregamento JNI em dispositivo ARM64.
2. Integrar uma superfície Android e lifecycle.
3. Fazer spike de winit/wgpu Android e testar Vulkan/GLES por capacidade real.
4. Implementar entrada por toque, áudio e armazenamento.
5. Integrar os módulos do upstream Horizon sem o launcher GPUI desktop.
6. Preparar recursos do jogo e só então construir fluxo de inicialização.
