# Validação estrutural de runtimes Java Android

O launcher agora possui um verificador estrutural para runtimes no formato esperado pelo adaptador FCL.

## Estrutura exigida

```text
<runtime>/
├── release
├── bin/
│   └── java
└── lib/
    └── server/
        └── libjvm.so
```

O verificador lê a versão declarada no arquivo `release` e o cabeçalho ELF de `libjvm.so`. Ele rejeita arquitetura incompatível com a ABI Android selecionada (arm64-v8a, armeabi-v7a, x86 ou x86_64).

## Limites

- A verificação não executa `bin/java`.
- Não comprova que o linker Android consegue carregar todas as dependências da JVM.
- Não comprova compatibilidade de JNI, SDL, LWJGL, OpenGL/Vulkan ou bibliotecas do Minecraft.
- Não baixa nem instala um runtime.
- Não habilita o botão Iniciar; o backend FCL e seu ciclo de vida ainda precisam ser integrados.

O objetivo é impedir que um runtime Linux x86/x86_64 seja aceito por engano como runtime Android ARM64. A disponibilidade do motor deve continuar indisponível até existir teste real de inicialização no dispositivo.
