# Mikael Launcher — Interface Design

## Objetivo
Construir a primeira etapa visual de um launcher Android de Minecraft, com foco em uma experiência horizontal, organizada e agradável em telas de celular.

## Requisitos confirmados
- Orientação principal em paisagem (landscape).
- Navegação fixa na lateral esquerda, aberta por padrão e recolhível.
- Itens: Início, Jogar, Versões, Mods, Downloads e Configurações.
- Ícones acompanhados de rótulos quando expandida; somente ícones quando recolhida.
- Animações suaves para recolher/expandir e trocar de seção.
- Tema escuro com verde principal e detalhes em azul, roxo e amarelo.
- Cards com cantos arredondados, hierarquia visual clara e espaçamento consistente.
- Referência visual: Fold Craft Launcher, sem copiar sua identidade visual.
- A interface deve ser responsiva para diferentes dimensões de telas Android.

## Escopo desta etapa
- Criar a base Android nativa em Kotlin com Jetpack Compose.
- Implementar navegação visual entre as seis seções.
- Criar a tela Início como primeira tela completa, com cabeçalho, identificação do launcher, painel de status visual e atalhos para as seções.
- Criar estados vazios bem apresentados para as outras seções, sem simular funcionalidades ainda inexistentes.
- Implementar animações e estados de navegação.
- Documentar como executar e validar o projeto.

## Fora do escopo desta etapa
- Inicialização real do Minecraft.
- Instalação e gerenciamento real dos motores Java e Horizon.
- Login Microsoft, Offline ou Ely.by.
- Download de versões, mods ou modpacks.
- Geração de APK ou publicação de release.

## Decisões técnicas
- Kotlin nativo para Android.
- Jetpack Compose para interface declarativa e animações.
- Material 3 como base de componentes, com tema e componentes próprios.
- Navegação e estado da interface desacoplados da futura integração dos motores.
- Orientação landscape configurada para a atividade principal nesta primeira etapa.

## Critérios de aceitação
- O projeto contém configuração Gradle Android coerente e arquivos Kotlin compiláveis.
- A atividade principal abre diretamente na interface, sem tela branca.
- A barra lateral aparece à esquerda e alterna corretamente entre expandida e recolhida.
- Os seis itens de navegação abrem suas respectivas telas/estados.
- A tela Início mantém boa hierarquia visual em paisagem e não corta os controles em larguras comuns.
- Animações não bloqueiam a navegação nem deixam o estado visual dessincronizado.
- Não há dependências de motores Java/Horizon nesta etapa.
- A documentação informa claramente o que está implementado e o que ainda é apenas visual.
