# Interface Base Android Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Entregar a primeira interface funcional do Mikael Launcher em Kotlin/Jetpack Compose, otimizada para orientação horizontal.

**Architecture:** Uma Activity Compose hospeda um shell de navegação com sidebar persistente, estado de seção e conteúdo independente por tela. Tema, componentes compartilhados e telas ficam separados para facilitar a integração futura dos motores Java e Horizon.

**Tech Stack:** Kotlin, Android Gradle Plugin, Jetpack Compose, Material 3, Kotlin DSL.

**Spec:** `docs/superpowers/specs/2026-11-10-launcher-interface-design.md`

## Global Constraints
- Aplicativo Android nativo em Kotlin.
- Orientação principal landscape.
- Navegação lateral esquerda, recolhível e animada.
- Seções: Início, Jogar, Versões, Mods, Downloads e Configurações.
- Tema escuro, verde principal, detalhes azul/roxo/amarelo.
- Não gerar APK nesta etapa.
- Não implementar motores, autenticação ou downloads reais nesta etapa.

## Review Focus
- Larguras pequenas em landscape: sidebar e conteúdo não podem se sobrepor.
- Sidebar recolhida: ícones precisam continuar acessíveis e identificáveis.
- Troca rápida entre abas durante animação: estado selecionado e conteúdo devem permanecer sincronizados.
- Rotação e recriação da Activity: seção selecionada deve ter comportamento previsível.
- Telas vazias: não podem sugerir que funcionalidades ainda não implementadas já funcionam.

---

### Task 1: Scaffold Android e tema

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/mikael/launcher/MainActivity.kt`
- Create: `app/src/main/java/com/mikael/launcher/ui/theme/Color.kt`
- Create: `app/src/main/java/com/mikael/launcher/ui/theme/Theme.kt`

- [ ] Configurar projeto Android Kotlin com Compose e Material 3.
- [ ] Declarar Activity principal e orientação landscape.
- [ ] Criar paleta escura com verde principal e cores auxiliares.
- [ ] Verificar consistência dos plugins, versões e namespaces.

### Task 2: Shell de navegação

**Files:**
- Create: `app/src/main/java/com/mikael/launcher/ui/navigation/LauncherSection.kt`
- Create: `app/src/main/java/com/mikael/launcher/ui/navigation/LauncherScaffold.kt`
- Create: `app/src/main/java/com/mikael/launcher/ui/components/LauncherSidebar.kt`

- [ ] Definir as seis seções e seus ícones/nomes.
- [ ] Implementar sidebar expandida e recolhida com animação.
- [ ] Ligar seleção da sidebar ao conteúdo exibido.
- [ ] Garantir alvos de toque adequados e conteúdo sem sobreposição.

### Task 3: Tela inicial e estados das demais seções

**Files:**
- Create: `app/src/main/java/com/mikael/launcher/ui/screens/HomeScreen.kt`
- Create: `app/src/main/java/com/mikael/launcher/ui/screens/PlaceholderScreen.kt`
- Create: `app/src/main/java/com/mikael/launcher/ui/components/LauncherCard.kt`

- [ ] Criar cabeçalho de boas-vindas e identidade visual do launcher.
- [ ] Criar cards de status e atalhos visuais sem alegar integração real.
- [ ] Criar estados vazios informativos para Jogar, Versões, Mods, Downloads e Configurações.
- [ ] Aplicar espaçamento e tamanhos responsivos para paisagem.

### Task 4: Documentação e verificação

**Files:**
- Create: `README.md`
- Create: `docs/INTERFACE.md`

- [ ] Documentar escopo, estrutura, requisitos e estado atual.
- [ ] Revisar imports, namespaces, recursos, manifest e dependências.
- [ ] Executar build e testes disponíveis; se o ambiente não permitir, registrar exatamente a limitação.
- [ ] Fazer revisão profunda de navegação, estados, dimensões, acessibilidade e possíveis crashes.
- [ ] Corrigir problemas encontrados antes de declarar a etapa concluída.
