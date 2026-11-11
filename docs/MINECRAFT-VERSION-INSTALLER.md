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
3. Resolve coordenadas Maven para caminhos de bibliotecas e baixa os artefatos aplicáveis às regras Linux/ARM64 do ambiente.
4. Baixa os classificadores nativos Linux descritos no JSON, mantendo os arquivos compactados na cache de bibliotecas.
5. Baixa o índice de assets e todos os objetos endereçados por hash.
6. Baixa a configuração de logging quando declarada.
7. Evita downloads duplicados, valida arquivos existentes e usa arquivos temporários antes de substituir os destinos.
8. Executa até quatro downloads simultâneos e publica progresso para a interface.
9. Só cadastra a instalação na lista do launcher depois que todas as tarefas terminam com sucesso.

Downloads usam HTTPS; redirecionamento para HTTP é rejeitado. Os caminhos de destino são normalizados e impedidos de escapar da pasta do jogo.

## Limites que ainda impedem o botão Jogar de funcionar

- Os classificadores `natives-linux` oficiais são armazenados, mas não são extraídos nem considerados compatíveis com Android ARM64 automaticamente.
- A camada de runtime Java ARM64 do FCL ainda não foi conectada.
- A adaptação FCL para construir classpath, extrair natives corretos, preparar argumentos JVM, abrir a superfície SDL/renderer e iniciar o processo ainda não foi integrada.
- Forge, Fabric e OptiFine exigem instaladores/patchers próprios e validação de dependências; selecionar um nome no perfil não equivale a instalar esses loaders.
- O cache de assets ainda precisa ser ligado às rotinas de reconstrução/virtualização do `GameRepository` do FCL para cobrir integralmente os layouts históricos.

Portanto, esta etapa instala o conjunto de arquivos oficiais descritos pelo manifesto para versões suportadas, mas **não declara o jogo pronto para execução**. A disponibilidade do motor permanece bloqueada até concluir e testar o adaptador FCL em Android ARM64.

## Verificações

Os testes unitários cobrem a resolução de coordenadas Maven comuns, classifiers, extensões e rejeição de coordenadas inválidas. O workflow Android também compila o app e executa a suíte unitária.
