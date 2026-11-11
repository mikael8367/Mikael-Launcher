# Horizon Rust — bootstrap Android ARM64

## O que entrou nesta etapa

- Crate Rust independente em `horizon-android/`, com saída `cdylib`.
- Ponte JNI Kotlin em `HorizonNativeBridge.kt`.
- Build script `scripts/build-horizon-android.sh` para produzir `libhorizon_android.so` em `app/src/main/jniLibs/arm64-v8a/`.
- Testes unitários Rust para dimensões inválidas, criação/remoção de sessão e handles inválidos.
- Entrada Android `android_main` via Winit `NativeActivity`, declarada em `HorizonNativeActivity`.
- Protótipo WGPU que cria uma janela, superfície, solicita um adaptador e dispositivo gráfico e apresenta um frame de cor sólida.
- UI na aba Horizon com ação para abrir o teste técnico do renderizador.

## Limite explícito

Esta etapa avançou além do JNI: o NativeActivity inicializa o event loop Winit e tenta criar uma superfície WGPU, escolher um adaptador compatível e apresentar um frame de teste. O backend gráfico é selecionado pelo WGPU entre os recursos disponíveis no dispositivo.

**Limite explícito:** este teste ainda não inicia `horizon-client`, não carrega o Minecraft 26.3, não prepara assets, não cria mundos e não implementa controles de toque do jogo. Ele valida somente a janela/superfície e o caminho básico de apresentação gráfica. A compatibilidade real precisa ser testada em um dispositivo Android ARM64; a compilação cruzada sozinha não prova compatibilidade com a GPU PowerVR.

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

1. Testar a NativeActivity e a apresentação WGPU no dispositivo Android ARM64.
2. Capturar o adaptador/backend escolhido e registrar erros Vulkan/GLES sem derrubar o launcher.
3. Integrar o cliente upstream `horizon-client` e adaptar o event loop para receber o `AndroidApp` da NativeActivity.
4. Implementar entrada por toque, áudio, armazenamento e pausa/retomada.
5. Preparar recursos do jogo e implementar instalação/importação compatível.
6. Abrir menu, criar/carregar/salvar mundo e só então habilitar o botão Jogar.
