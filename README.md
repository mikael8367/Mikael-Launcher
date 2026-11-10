# Mikael Launcher

Launcher Android de Minecraft com interface horizontal, navegação lateral e identidade visual própria.

## Estado do projeto

**Versão:** 0.1.0-alpha  
**Etapa atual:** interface base

Nesta etapa estão sendo construídos o shell visual, a navegação e a tela inicial. As áreas de Java, Horizon, contas, versões, mods e downloads ainda não executam operações reais.

## Interface
- Android nativo com Kotlin e Jetpack Compose.
- Orientação principal em paisagem.
- Menu lateral recolhível com Início, Jogar, Versões, Mods, Downloads e Configurações.
- Tema escuro com verde principal e detalhes azul, roxo e amarelo.
- Transições entre seções e estados visuais próprios.

## Requisitos de desenvolvimento
- JDK 17.
- Android SDK com plataforma 35.
- Gradle 8.9 ou compatível com Android Gradle Plugin 8.7.3.

## Compilar
Com Gradle instalado e Android SDK configurado:

```bash
gradle assembleDebug
```

O APK de depuração será produzido em `app/build/outputs/apk/debug/`. Nenhum APK é gerado ou publicado automaticamente por este repositório.

## Próximas etapas
1. Finalizar e validar a interface em diferentes tamanhos de tela.
2. Implementar gerenciamento de perfis e versões do motor Java.
3. Integrar o motor Horizon de forma independente, após verificar compatibilidade do dispositivo.
4. Implementar contas, downloads e gerenciamento de mods.
