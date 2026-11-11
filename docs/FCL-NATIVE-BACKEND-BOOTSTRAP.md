# Backend nativo ARM64 do FCL

## Objetivo desta etapa

Preparar, no próprio build Android do Mikael Launcher, a camada nativa oficial do Fold Craft Launcher (FCL), mantendo o código da interface Compose no módulo `:app` e isolando o CMake/JNI upstream em `:fcl-native`.

O submódulo FCL permanece fixado em `f5bb1526eb371d81cb086d74cdb8535043b51ee4`. Não copiamos nem alteramos arquivos do upstream.

## O que foi conectado

- `:fcl-native` é um Android Library module, dependente do mesmo Android Gradle Plugin do launcher.
- O CMake aponta diretamente para `third_party/FoldCraftLauncher/FCL/src/main/jni/CMakeLists.txt`.
- A compilação é limitada a `arm64-v8a`, arquitetura escolhida para o primeiro alvo de teste.
- As bibliotecas JNI pré-compiladas são lidas de `FCL/src/main/jniLibs`; o módulo filtra o ABI para ARM64.
- O AAR ByteHook 1.0.10 fornece o pacote Prefab exigido pelo `find_package(bytehook REQUIRED CONFIG)` do CMake.
- O módulo `:app` depende de `:fcl-native`, para que as bibliotecas sejam incluídas no pacote Android quando o app for empacotado futuramente.
- O CI compila `:fcl-native:externalNativeBuildDebug` e verifica a geração de `libpojavexec.so` e `libfcl.so`. Esse job compila bibliotecas; não executa a geração de APK.

## Componentes nativos upstream identificados

O CMake FCL gera 14 bibliotecas compartilhadas: `androidnsbypass`, `fcl`, `linkerhook`, `awt_headless`, `awt_xawt`, `pojavexec_awt`, `pojavexec`, `vulkan_check`, `vkshim`, `glxshim`, `jsound`, `flite`, `fliteWrapper` e `flite_cmu_us_kal16`.

O diretório de JNI pré-compilado do FCL também fornece renderizadores e componentes de driver. Eles permanecem no submódulo e são importados como origem do Gradle, sem duplicar binários manualmente no repositório do Mikael Launcher.

## O que esta etapa NÃO faz

A compilação dessas bibliotecas é necessária, mas não torna o botão Iniciar funcional. Ainda não incorporamos as classes Android e Kotlin do FCL nem seus recursos, assets e manifesto.

Ainda faltam:
1. Converter/adaptar o módulo `:FCL` do upstream, que hoje usa o plugin `com.android.application`, para uma biblioteca integrável sem assumir o papel de aplicativo principal.
2. Integrar `FCLApp`, `FCLGameRepository`, `LauncherHelper`, autenticação, gerenciador de runtimes e dependências `:ZipFileSystem` / `:Terracotta`.
3. Integrar a `JVMActivity`, `FCLBridge`, SDL e os recursos Android necessários, sem reintroduzir a tela principal do FCL.
4. Instalar e validar os runtimes Java corretos e as bibliotecas LWJGL/JNA por versão.
5. Passar a superfície Android ao FCLBridge e testar a inicialização real do Minecraft Vanilla em dispositivo ARM64.

O FCL inicia a JVM dentro do processo Android via seu backend nativo. A sequência real inclui construir o `FCLBridge`, configurar `JVMActivity` e aguardar a superfície gráfica antes de chamar o bridge. Não basta executar `java` com `ProcessBuilder`.

## Compatibilidade de build a resolver

O projeto Mikael Launcher atualmente usa AGP 8.7.3, Kotlin 2.0.21 e Gradle 8.9. O FCL upstream usa AGP 8.13.2, Kotlin 2.4.10 e Gradle 8.14.4, além de version catalog próprio e configurações KSP/serialization. Por isso, esta etapa integra somente a camada CMake e não altera às cegas a toolchain Kotlin/Gradle do aplicativo.

## Licença

O FCL upstream declara GPL-3.0. A incorporação e distribuição das partes Java, Kotlin, JNI e nativas deve preservar os avisos de licença e cumprir as obrigações da GPL-3.0. Cada dependência de terceiros continua sujeita à sua própria licença.
