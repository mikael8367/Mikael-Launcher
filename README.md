# Mikael Launcher

Launcher Android de Minecraft com interface horizontal, navegação lateral e identidade visual própria.

## Estado do projeto

**Versão:** 0.1.0-alpha  
**Etapa atual:** interface interativa

A interface já tem navegação entre as seis áreas, seleção visual de motor/perfil, seleção de versão, seletor de arquivos para mods, atualização da tela de downloads e preferências locais.

## Interface
- Android nativo com Kotlin e Jetpack Compose.
- Orientação principal em paisagem.
- Menu lateral recolhível com Início, Jogar, Versões, Mods, Downloads e Configurações.
- Tema escuro com verde principal e detalhes azul, roxo e amarelo.
- Transições entre seções.
- Controles com estados e respostas locais.

## Limites importantes desta etapa
- Selecionar Java/Horizon e um perfil não inicia o jogo ainda.
- Selecionar uma versão não baixa nem instala o Minecraft.
- O seletor de arquivos abre o seletor do Android, mas a importação/cópia e validação de mods ainda não foram implementadas.
- Atualizar na área Downloads atualiza o estado da tela; não consulta uma fila de downloads real.
- Preferências ainda são locais à tela e não são aplicadas aos motores.
- Login Microsoft/Offline/Ely.by, motores Java e Horizon, instalação, downloads reais e gerenciamento persistente de mods serão implementados em etapas próprias.

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
1. Validar a interface em dispositivos e dimensões diferentes.
2. Implementar preferências persistentes e perfis.
3. Implementar gerenciamento de versões do motor Java.
4. Integrar o motor Horizon de forma independente, após verificar compatibilidade do dispositivo.
5. Implementar autenticação, downloads reais e gerenciamento de mods.
