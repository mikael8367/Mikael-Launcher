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

`MinecraftInstaller` grava no diretório privado do aplicativo (`filesDir/minecraft`) e:

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

## Mod loaders exibidos na seleção de versão

A tela de versões agora permite escolher e memorizar Vanilla, Forge, NeoForge, Fabric, Legacy Fabric, Quilt, OptiFine, LiteLoader e Rift. Essa seleção é apenas a escolha do tipo de instalação: não cria um perfil loader nem baixa seus arquivos ainda. Cada loader precisa de um backend próprio para consultar versões compatíveis, validar metadados, resolver bibliotecas e aplicar seu formato de instalação.

- Fabric e Quilt usam perfis JSON que precisam ser mesclados ao metadata Mojang.
- Forge e NeoForge exigem tratamento de instalador/processors e suas bibliotecas específicas.
- OptiFine e os loaders históricos exigem fluxos e compatibilidades por versão.
- O botão de instalação permanece limitado ao Vanilla até esses fluxos serem implementados e testados; selecionar outro loader mostra explicitamente que o instalador está pendente.

## Limites que ainda impedem o botão Jogar de funcionar

- Os classificadores `natives-linux`, `natives-windows` e `natives-osx` são ignorados no download Android; o runtime FCL precisa fornecer os natives Android ARM64.
- A camada de runtime Java ARM64 do FCL ainda não foi conectada.
- A adaptação FCL para construir classpath, extrair natives corretos, preparar argumentos JVM, abrir a superfície SDL/renderer e iniciar o processo ainda não foi integrada.
- Forge, NeoForge, Fabric, Legacy Fabric, Quilt, OptiFine, LiteLoader e Rift exigem backends/patchers próprios e validação de dependências; os nomes já podem ser selecionados na interface, mas ainda não são instaláveis.
- Metadados históricos sem `assetIndex` ainda precisam de tratamento específico por geração; o instalador não inventa um índice ausente. Os layouts `virtual` e `map_to_resources` são materializados quando as flags aparecem no índice.

Portanto, esta etapa instala o conjunto de arquivos oficiais descritos pelo manifesto para versões suportadas, mas **não declara o jogo pronto para execução**. A disponibilidade do motor permanece bloqueada até concluir e testar o adaptador FCL em Android ARM64.

## Verificações

Os testes unitários cobrem a resolução de coordenadas Maven comuns, classifiers, extensões, fallback para repositório Maven, preservação de artefatos explícitos, rejeição de repositórios inseguros e validação de índices de assets históricos/inválidos. O workflow Android também compila o app e executa a suíte unitária.
