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

## Auditoria da integração upstream

Foi inspecionado o repositório oficial `horizon-rust/horizon` no commit `b66b2f8372bdc0dd7358a2ad3695f782c0cb6689`.

- O pacote `horizon-client-lib` reúne sistemas reutilizáveis do cliente, mas não inicia a sessão jogável por conta própria.
- O ponto de entrada de jogo está em `crates/horizon-client/src/entry.rs`: `entry::main()` chama `game::run_with()`.
- O executável upstream chama `horizon_client::entry::main()` a partir de `src/main.rs`.
- `crates/horizon-client/src/game/loading_screen.rs` concentra `run_event_loop(Game)`, que cria `EventLoop::new()?.run_app(&mut split)`. O tipo `App` já implementa `winit::application::ApplicationHandler`, encaminhando `resumed`, `window_event`, `device_event` e `about_to_wait` para o carregamento e para o jogo.
- `crates/horizon-client/src/game/event_loop.rs` já abstrai `exit` e `set_control_flow` por `EventLoopCtl`, inclusive para o modo headless.
- O protótipo Android atual cria o loop com `EventLoop::builder()` + `with_android_app(AndroidApp)`. Chamar `entry::main()` diretamente não injeta esse loop: o upstream tenta criar outro com `EventLoop::new()`, portanto não é uma integração válida.
- O caminho de menor risco é refatorar o upstream para expor uma função Android que construa o mesmo `App`/`Split` e execute `run_app` no `EventLoop` criado com `AndroidApp`. Isso preserva o `ApplicationHandler` já existente e evita duplicar o ciclo de vida. Também será necessário tornar acessível a preparação de `Game`/argumentos e validar as dependências e os recursos no target Android.

Por isso, não foi adicionada uma dependência Cargo fictícia nem um botão que alegue iniciar o jogo. A integração do cliente completo permanece pendente até existir esse ponto de extensão no upstream e até as dependências Android do workspace serem validadas. O commit upstream acima é uma referência de auditoria, não uma cópia vendorizada do motor.

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
3. Preparar no upstream um ponto de entrada Android que reutilize o `EventLoop` criado com `AndroidApp`, sem criar um segundo loop; depois conectar `game::run_with` e o renderer real.
4. Implementar entrada por toque, áudio, armazenamento e pausa/retomada.
5. Preparar recursos do jogo e implementar instalação/importação compatível.
6. Abrir menu, criar/carregar/salvar mundo e só então habilitar o botão Jogar.
