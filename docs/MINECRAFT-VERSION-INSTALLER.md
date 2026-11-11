# Catálogo e instalação de versões Minecraft Java

## Catálogo

O launcher consulta o manifesto oficial `version_manifest_v2.json` da Mojang e apresenta todas as entradas publicadas nesse manifesto. A interface separa:

- Releases
- Snapshots
- Beta históricos
- Alpha históricos
- Edições de primeiro de abril identificadas pelos IDs conhecidos
- Todas as entradas do manifesto

A pesquisa usa o ID da versão. O manifesto é a fonte da URL e do SHA-1 do JSON de cada versão.

**Limite do catálogo:** isso cobre as versões publicadas no manifesto oficial atual. Builds Classic/Indev/Infdev que não estejam nesse manifesto não são inventados nem apresentados como instaláveis por esse fluxo.

## Instalação oficial implementada

`MinecraftInstaller` grava no diretório de jogo resolvido por `FclGameDirectoryResolver`: por padrão `filesDir/minecraft`; quando o usuário escolhe uma pasta local compatível, usa esse caminho tanto para Vanilla quanto para Fabric/Quilt. O perfil ativo do FCL é apontado para o mesmo diretório.

1. Baixa e valida o JSON da versão pelo SHA-1 do manifesto.
2. Baixa o client JAR e verifica SHA-1/tamanho quando fornecidos.
3. Resolve coordenadas Maven para caminhos de bibliotecas; usa `downloads.artifact` quando presente e monta a URL a partir do repositório Maven declarado (ou `libraries.minecraft.net`) quando ausente.
4. Valida URLs HTTPS, SHA-1 e tamanho dos artefatos quando esses campos são fornecidos.
5. Não baixa classificadores nativos desktop (`natives-linux`, `natives-windows`, `natives-osx`) como se fossem bibliotecas Android ARM64; a camada nativa Android é responsabilidade do FCL.
6. Valida cada entrada do índice de assets; índice sem `objects`, hash inválido, tamanho negativo ou caminho inseguro interrompe a instalação em vez de deixar assets faltando silenciosamente.
7. Baixa objetos por SHA-1 e confere hash e tamanho, inclusive tamanho zero quando declarado.
8. Para índices antigos com `virtual: true`, materializa cópias em `assets/virtual/legacy/`; para `map_to_resources: true`, materializa `assets/resources/`.
9. Baixa a configuração de logging quando declarada.
10. Evita downloads duplicados, valida arquivos existentes e usa arquivos temporários antes de substituir os destinos.
11. Executa até quatro downloads simultâneos e publica progresso para a interface.
12. Só cadastra a instalação na lista do launcher depois que todas as tarefas terminam com sucesso.

Downloads usam HTTPS; redirecionamento para HTTP é rejeitado. Os caminhos de destino são normalizados e impedidos de escapar da pasta do jogo.


## Diretório de jogo personalizado e permissões Android

- A tela Configurações permite escolher uma pasta com o seletor SAF do Android e persiste a autorização de leitura/gravação.
- Para que os instaladores baseados em `File` e o FCL usem o caminho diretamente, o diretório precisa estar em armazenamento local compartilhado. Provedores de nuvem e pastas protegidas do Android não são aceitos como diretório de jogo.
- Android 11+: a aplicação solicita que o usuário habilite **Acesso a todos os arquivos** na tela especial do sistema. Android 8–10: solicita leitura e gravação de armazenamento em tempo de execução.
- A pasta escolhida é usada por `MinecraftInstaller`, `ModLoaderProfileInstaller` e pelo `Profiles.getSelectedProfile().gameDir` do FCL. Alterar a seleção após a inicialização atualiza o perfil ativo.
- O botão **Migrar versões e arquivos existentes** copia arquivos ausentes do antigo `filesDir/minecraft` e do antigo diretório privado do FCL para a pasta atual. Arquivos que já existem no destino são preservados e não são sobrescritos.
- Os runtimes Java, bibliotecas nativas do launcher e arquivos internos do FCL continuam no armazenamento privado do aplicativo; somente o diretório de jogo (versões, libraries, assets, mods, resourcepacks, shaderpacks e saves) é redirecionado.
- O usuário pode voltar ao diretório padrão. O aplicativo não apaga automaticamente a pasta anterior ao trocar de local.

## Mod loaders e estado real dos instaladores

A tela de versões oferece Vanilla, Forge, NeoForge, Fabric, Legacy Fabric, Quilt, OptiFine, LiteLoader e Rift. A seleção visual não significa que todos estejam instalados.

### Instaladores conectados nesta etapa

- **Fabric:** consulta a API oficial Fabric Meta, lista versões compatíveis com o Minecraft selecionado, baixa o perfil JSON e as bibliotecas, valida HTTPS/SHA-1/tamanho quando declarados e gera um perfil que herda os metadados Mojang.
- **Quilt:** usa o mesmo fluxo de perfil, consultando a API oficial Quilt Meta e preservando o perfil base do Minecraft.
- Ambos instalam primeiro a versão Vanilla correspondente. O perfil derivado só é cadastrado depois que os downloads e a gravação do JSON terminam com sucesso.

O merge preserva as bibliotecas do Minecraft, adiciona as bibliotecas do loader, combina os argumentos de inicialização e rejeita identificadores/caminhos inseguros. Fontes: documentação da API Fabric Meta e fluxo de instalação Fabric/Quilt.

### Ainda pendentes — não apresentados como instaladores funcionais

- **Forge e NeoForge:** precisam de integração do formato de instalador/processors e do bootstrap ForgeWrapper, com geração e validação das bibliotecas específicas.
- **Legacy Fabric:** precisa de adapter próprio para a API e as versões históricas.
- **OptiFine:** precisa do patcher compatível com cada geração e de validação das combinações Forge/Vanilla.
- **LiteLoader e Rift:** precisam de instaladores legados e suporte às versões realmente publicadas.
- O suporte a loaders não pode ser considerado concluído apenas por listar versões ou baixar um JAR.

## Limites que ainda impedem o botão Jogar de funcionar

- Os classificadores `natives-linux`, `natives-windows` e `natives-osx` são ignorados no download Android; o runtime FCL precisa fornecer os natives Android ARM64.
- A camada de runtime Java ARM64 do FCL ainda não foi conectada.
- A adaptação FCL para construir classpath, extrair natives corretos, preparar argumentos JVM, abrir a superfície SDL/renderer e iniciar o processo ainda não foi integrada.
- Forge, NeoForge, Fabric, Legacy Fabric, Quilt, OptiFine, LiteLoader e Rift exigem backends/patchers próprios e validação de dependências; os nomes já podem ser selecionados na interface, mas ainda não são instaláveis.
- Metadados históricos sem `assetIndex` ainda precisam de tratamento específico por geração; o instalador não inventa um índice ausente. Os layouts `virtual` e `map_to_resources` são materializados quando as flags aparecem no índice.

Portanto, esta etapa instala o conjunto de arquivos oficiais descritos pelo manifesto para versões suportadas, mas **não declara o jogo pronto para execução**. A disponibilidade do motor permanece bloqueada até concluir e testar o adaptador FCL em Android ARM64.

## Verificações

Os testes unitários cobrem a resolução de coordenadas Maven comuns, classifiers, extensões, fallback para repositório Maven, preservação de artefatos explícitos, rejeição de repositórios inseguros e validação de índices de assets históricos/inválidos. O workflow Android também compila o app e executa a suíte unitária.
