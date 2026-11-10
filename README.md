# Mikael Launcher

Launcher Android de Minecraft com interface horizontal, navegação lateral e identidade visual própria.

## Estado do projeto

**Versão:** 0.1.0-alpha  
**Etapa atual:** interface interativa

A interface inclui navegação entre as áreas principais, seleção visual de motor/perfil, gerenciamento visual de instalações, abas e ações para contas, mods, pacotes de recursos, shaders, mundos, downloads, desempenho e diagnóstico. O seletor de arquivos Android está disponível para a futura importação.

## Interface
- Android nativo com Kotlin e Jetpack Compose.
- Orientação principal em paisagem.
- Menu lateral recolhível com Início, Jogar, Versões, Contas, Mods, Pacotes de recursos, Shaders, Mundos, Downloads, Desempenho, Logs e Configurações.
- Tema escuro com verde principal e detalhes azul, roxo e amarelo.
- Transições entre seções.
- Controles com estados e respostas locais.

## Limites importantes desta etapa
- Selecionar Java/Horizon e um perfil não inicia o jogo ainda.
- Selecionar uma versão não baixa nem instala o Minecraft.
- O seletor de arquivos abre o seletor do Android, mas a importação/cópia e validação de mods ainda não foram implementadas.
- Atualizar na área Downloads atualiza o estado da tela; não consulta uma fila de downloads real.
- Preferências ainda são locais à tela e não são aplicadas aos motores.
- Os botões de adicionar/editar/duplicar/excluir instalações, loader, runtime, RAM, argumentos JVM, contas, mundos, shaders, pacotes, fila de downloads, desempenho e logs já estão representados na interface; as operações reais serão ligadas em etapas próprias.
- Login Microsoft/Offline/Ely.by, motores Java e Horizon, instalação, downloads reais e gerenciamento persistente de mods ainda não executam operações reais nesta fase.

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


## Etapa 2 — interface de contas e perfil

- Menu de perfil no canto superior direito, com nome e avatar em estilo pixel-art provisório.
- Dropdown para alternar entre perfis locais já adicionados e abrir a área de contas.
- Área de contas com opções visuais Microsoft, Offline e Ely.by.
- Cadastro local Offline com validação de nome (1–16 caracteres, letras, números e sublinhado).
- Seleção e remoção de perfis locais; o perfil escolhido é refletido no menu superior.
- Minecraft Java e Horizon Rust continuam apresentados como motores separados.

**Limites desta etapa:** o avatar ainda é ilustrativo e não baixa a skin real; Microsoft e Ely.by ainda não autenticam; perfis locais são estado de interface, não tokens de sessão oficiais. A integração segura de OAuth, consulta de skin e persistência definitiva será feita na etapa de lógica.

## Catálogo de mods
- Catálogo integrado com a API pública do Modrinth.
- Pesquisa opcional na API CurseForge usando uma chave informada pelo usuário nas Configurações.
- Filtros por versão do Minecraft e loader (Forge, Fabric, NeoForge, Quilt ou todos).
- Fonte selecionável: Modrinth, CurseForge ou ambos.
- Primeira página carregada automaticamente e paginação incremental ao rolar a lista.
- A chave CurseForge é guardada no armazenamento privado do aplicativo; não é incluída no código-fonte.
- A listagem é de descoberta; baixar, validar dependências e instalar mods ainda exigem a próxima etapa de integração.

## Configurações avançadas e runtimes
- Assistente de primeira inicialização para escolher uma família de runtime Java e o renderizador gráfico preferido.
- Preferências de runtime, renderizador, limite de RAM e argumentos JVM salvas localmente.
- Opções visuais, isolamento de perfis, verificações de mods, backups, desempenho, rede, downloads, armazenamento, privacidade, acessibilidade e diagnóstico.
- Renderizadores apresentados na interface: GL4ES, Zink, VirGL, ANGLE, Vulkan experimental e Software.
- As escolhas de runtime e renderizador são preferências nesta etapa; os binários, downloads, validação de compatibilidade e ativação real ainda precisam ser conectados ao motor Android.
- O assistente inicial aparece uma vez após a primeira abertura e a escolha fica salva no armazenamento privado do aplicativo.

## Catálogo de versões do Minecraft Java
- Consulta o manifesto oficial de versões da Mojang pela internet.
- Categorias: Recentes (releases), Snapshots, Beta, Alpha, Primeiro de abril e Todas.
- Pesquisa pelo identificador da versão e exibição da data de lançamento quando disponível.
- Seleção da versão antes de criar um perfil local.
- Inclui versões históricas e snapshots especiais quando publicados no manifesto oficial.
- A lista depende de conexão com a internet; há estado de carregamento, mensagem de erro e opção de tentar novamente.
- **Limite atual:** criar um perfil salva a seleção no aparelho, mas ainda não baixa o cliente, bibliotecas, assets ou arquivos nativos do Minecraft. O instalador real será implementado em outra etapa.
