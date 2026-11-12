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

**Limite do catálogo:** isso cobre as versões publicadas no manifesto oficial atual. Builds Classic/Indev/Infdev que não estejam nesse manifesto não são inventados nem apresentados como instaláveis por esse fluxo. O manifesto não lista a primeira Combat Test ("1.14.3 - Combat Test"), que foi distribuída por download direto; ela não aparece no catálogo e não é fabricada.

## Instalação oficial implementada

`MinecraftInstaller` grava no diretório de jogo resolvido por `FclGameDirectoryResolver`: por padrão `filesDir/minecraft`; quando o usuário escolhe uma pasta local compatível, usa esse caminho para Vanilla, Fabric e Quilt. Forge/NeoForge usam o repositório FCL e criam o perfil completo no mesmo diretório.

1. Baixa e valida o JSON da versão pelo SHA-1 do manifesto.
2. Baixa o client JAR e verifica SHA-1/tamanho quando fornecidos. Metadados antigos que expõem o client em `mainJar.downloads.artifact` também são aceitos.
3. Resolve coordenadas Maven para caminhos de bibliotecas; usa `downloads.artifact` quando presente e monta a URL a partir do repositório Maven declarado (ou `libraries.minecraft.net`) quando ausente.
4. Valida URLs HTTPS, SHA-1 e tamanho dos artefatos quando esses campos são fornecidos.
5. Não baixa classificadores nativos desktop (`natives-linux`, `natives-windows`, `natives-osx`) como se fossem bibliotecas Android ARM64; a camada nativa Android é responsabilidade do FCL.
6. Valida cada entrada do índice de assets; índice sem `objects`, hash inválido, tamanho negativo ou caminho inseguro interrompe a instalação em vez de deixar assets faltando silenciosamente. Se os metadados não informam a URL do índice de assets, a instalação falha com mensagem clara em vez de estourar um erro genérico de rede.
7. Baixa objetos por SHA-1 e confere hash e tamanho, inclusive tamanho zero quando declarado.
8. Para índices antigos com `virtual: true`, materializa cópias em `assets/virtual/<assetIndexId>/` usando o ID real do índice (por exemplo `pre-1.6`, não um "legacy" fixo); para `map_to_resources: true`, materializa em `resources/` na raiz do diretório de jogo. Esses são exatamente os caminhos que o `DefaultGameRepository.reconstructAssets` do FCL consulta; gravar em `assets/resources/` deixava sons e texturas de versões pré-1.6 em pastas que o jogo nunca lê.
9. Baixa a configuração de logging quando declarada.
10. Evita downloads duplicados, valida arquivos existentes e usa arquivos temporários antes de substituir os destinos.
11. Executa até quatro downloads simultâneos e publica progresso para a interface.
12. Só cadastra a instalação na lista do launcher depois que todas as tarefas terminam com sucesso.

### Downloads: retomada, corrupção e cancelamento

- **Retomada (HTTP Range):** um download interrompido deixa um arquivo `.part`. A próxima tentativa envia `Range: bytes=<tamanho>-`; uma resposta `206` continua de onde parou, uma resposta `200` (servidor ignorou o Range) recomeça do zero e um `416` (`.part` obsoleto ou maior que o arquivo remoto) descarta o `.part` e recomeça. A validação final de SHA-1/tamanho garante que o arquivo retomado é idêntico ao original.
- **Corrupção:** conteúdo que não passa na verificação de SHA-1/tamanho após o download é classificado como `CorruptDownloadException`, o `.part` é apagado e a tentativa é refeita do zero (até 3 tentativas com backoff). Antes disso, um arquivo corrompido abortava a instalação inteira.
- **Falhas transientes de I/O** preservam o `.part` para que a próxima tentativa retome; cancelamento, corrupção e erros de validação apagam o `.part` para nunca acrescentar dados a um arquivo inválido.
- **Cancelamento seguro:** interromper a instalação (navegar para outra tela, fechar o app) cancela as corrotinas de download e limpa os `.part` pendentes; a instalação pode ser refeita depois sem corrupção.
- **Status HTTP:** 408/429/500/502/503/504 são reintentados com backoff; 400/403/404/410 falham imediatamente com a URL e o status na mensagem. Redirecionamento para HTTP é rejeitado. Os caminhos de destino são normalizados e impedidos de escapar da pasta do jogo.
- **Concorrência:** instalações simultâneas no mesmo diretório são serializadas por um mutex por diretório, impedindo que duas instalações apaguem os `.part` uma da outra.

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

- **Fabric:** consulta a API oficial Fabric Meta, lista versões compatíveis com o Minecraft selecionado, baixa o perfil JSON e as bibliotecas, valida HTTPS/SHA-1/tamanho quando declarados e gera um perfil que herda os metadados Mojang. Após gravar o perfil mesclado, o instalador copia o client JAR base já validado para `versions/<perfil>/<perfil>.jar` (conferindo o tamanho contra `downloads.client`), para que o FCL encontre o JAR executável do perfil sem baixar de novo.
- **Quilt:** usa o mesmo fluxo de perfil, consultando a API oficial Quilt Meta e preservando o perfil base do Minecraft. Bibliotecas sem repositório explícito caem no repositório oficial do Quilt (`maven.quiltmc.org/repository/release/`); apontar para `maven.fabricmc.net` produzia 404 para artefatos exclusivos do Quilt.
- **Forge:** consulta a lista de componentes Forge do FCL e usa `GameBuilder` + `ForgeInstallTask` do FCL. O FCL baixa o installer, identifica o formato antigo/novo, executa os passos necessários e salva o perfil no repositório.
- **NeoForge:** consulta a lista oficial de versões NeoForge do FCL e usa `NeoForgeInstallTask`, incluindo a cadeia de processors e o runner isolado `:jvm` do FCL.
- **OptiFine:** consulta o catálogo de componentes OptiFine do FCL e usa o `GameBuilder` com `OptiFineRemoteVersion`/`OptiFineInstallTask`. O patcher do FCL valida a versão-base, aplica o patch e usa o `InstallerProcessRunner` com tentativas de Java 8, 17, 11 e 21 quando necessário. O perfil só é cadastrado após o builder terminar e o repositório encontrar o resultado.
- **Legacy Fabric:** conectado via pipeline FCL (`LegacyFabricInstallTask`), com catálogo consultado pelo `FclModLoaderInstaller`.
- **LiteLoader:** conectado via pipeline FCL (`LiteLoaderInstallTask`), com catálogo consultado pelo `FclModLoaderInstaller`.
- **Forge + OptiFine:** disponível como perfil combinado. A tela consulta os catálogos dos dois componentes, exige que ambos tenham uma versão publicada para a versão-base selecionada e permite escolher as versões separadamente. A instalação é sequencial: cria o perfil-base, aplica Forge, aplica OptiFine sobre a versão já modificada pelo Forge e só então salva/seleciona o perfil final. Isso respeita a exigência do `OptiFineInstallTask` de encontrar um wrapper Forge compatível; não usa a coleção não ordenada de componentes do `GameBuilder` para definir a ordem.

Forge, NeoForge, OptiFine, Legacy Fabric e LiteLoader constroem o perfil pelo pipeline do FCL; não são tratados como simples download de JAR. Fabric/Quilt preservam as bibliotecas do Minecraft, adicionam bibliotecas do loader, combinam argumentos de inicialização e rejeitam identificadores/caminhos inseguros.

### Instalação sem o catálogo da Mojang carregado

Os loaders com backend FCL (Forge, NeoForge, OptiFine, Legacy Fabric, LiteLoader e Forge + OptiFine) baixam o JSON oficial da versão-base por conta própria dentro do pipeline FCL. Por isso a tela não exige mais que o catálogo da Mojang tenha sido carregado para instalá-los: se a consulta ao manifesto falhou ou ainda está em andamento, a instalação usa o ID de versão selecionado diretamente. Vanilla, Fabric e Quilt continuam exigindo a entrada oficial do catálogo, porque o instalador base precisa da URL e do SHA-1 do manifesto.

### Ainda pendentes — não apresentados como instaladores funcionais

- **Rift:** projeto descontinuado (existia apenas para versões de teste 1.13) e sem instalador integrado; a UI o marca explicitamente como indisponível.
- A combinação Forge + OptiFine usa a verificação interna do patcher FCL; versões específicas ainda precisam ser validadas em dispositivo Android ARM64 e podem ser recusadas pelo instalador se o wrapper Forge não for compatível.
- O suporte a componentes não pode ser considerado concluído apenas por listar versões ou baixar um JAR.

## "OptiFine patcher failed": causa raiz e comportamento atual

O erro `OptiFine patcher failed` vinha do `OptiFineInstallTask` do FCL: quando o instalador OptiFine contém a classe `optifine.Patcher` (instaladores modernos), o FCL executa o patch em um processo Java separado (`:jvm`, via `InstallerProcessRunner`) e tenta as JREs 8 → 17 → 11 → 21. Se todas falham (runtime Java ausente no aparelho, ou o patcher recusa a versão-base), a tarefa lança `OptiFine patcher failed` e o perfil não é criado.

Pontos verificados nesta auditoria:

- **Não é um bug do launcher:** o patcher OptiFine é um processo Java real; sem uma runtime Java instalada no dispositivo (fluxo de runtimes do FCL), ele não pode executar. Instaladores antigos, como os de 1.12.2, não contêm `Patcher.class`: o FCL apenas copia o JAR e gera o perfil sem precisar de JVM.
- **Pré-checagem de runtime antes de instalar seria errada:** bloquearia justamente os casos que funcionam sem JVM (como OptiFine 1.12.2). A mensagem de erro do FCL já orienta a instalar uma runtime Java.
- **Não há instalador falso:** o launcher nunca cria um perfil chamado "OptiFine" sem passar pelo pipeline; o perfil só aparece se o `GameBuilder` + `OptiFineInstallTask` terminarem e o repositório do FCL encontrar a versão gerada.
- **Forge + OptiFine 1.12.2:** o `OptiFineInstallTask` aceita o wrapper Forge (launchwrapper) como versão-base. A combinação precisa ser validada em dispositivo real; o instalador recusa combinações incompatíveis em vez de produzir um perfil quebrado.

## Limites que ainda impedem o botão Jogar de funcionar

- Os classificadores `natives-linux`, `natives-windows` e `natives-osx` são ignorados no download Android; o runtime FCL precisa fornecer os natives Android ARM64.
- A camada de runtime Java ARM64 do FCL ainda não foi conectada.
- A adaptação FCL para construir classpath, extrair natives corretos, preparar argumentos JVM, abrir a superfície SDL/renderer e iniciar o processo ainda não foi integrada.
- Metadados históricos sem `assetIndex` ainda precisam de tratamento específico por geração; o instalador não inventa um índice ausente. Os layouts `virtual` e `map_to_resources` são materializados nos caminhos corretos quando as flags aparecem no índice.

Portanto, esta etapa instala o conjunto de arquivos oficiais descrito pelo manifesto para versões suportadas, mas **não declara o jogo pronto para execução**. A disponibilidade do motor permanece bloqueada até concluir e testar o adaptador FCL em Android ARM64.

## Verificações

Os testes unitários cobrem a resolução de coordenadas Maven comuns, classifiers, extensões, fallback para repositório Maven (incluindo o repositório próprio do Quilt), preservação de artefatos explícitos, rejeição de repositórios inseguros, validação de índices de assets históricos/inválidos, política de instalação sem catálogo carregado, preservação dos metadados de download do client no perfil mesclado e o tratamento de falhas de download (status transientes, corrupção e backoff). O workflow Android também compila o app e executa a suíte unitária. Testes unitários não substituem a validação em aparelho Android ARM64.
