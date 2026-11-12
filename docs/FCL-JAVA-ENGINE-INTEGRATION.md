# Integração do motor Minecraft Java (FCL)

## Decisão técnica

O Mikael Launcher mantém a interface Kotlin/Jetpack Compose. O motor Minecraft Java usa o Fold Craft Launcher (FCL) como backend Android/JVM, isolado do motor Horizon Rust.

O upstream FCL está fixado no submódulo:

- Repositório: https://github.com/FCL-Team/FoldCraftLauncher
- Commit: `f5bb1526eb371d81cb086d74cdb8535043b51ee4`
- Licença declarada: GNU GPL v3.0

A versão ARM64 é o primeiro alvo de execução. O código upstream é consumido pelo módulo Android `:fcl-android`; o backend CMake/JNI fica isolado em `:fcl-native`.

## O que já está conectado

- [x] Submódulo FCL fixado e checkout reproduzível no CI.
- [x] Módulos `:fcl-android`, `:fcl-native`, `:Terracotta` e `:ZipFileSystem` incluídos no build.
- [x] Classes Kotlin/Java, recursos, assets, manifesto e JNI do FCL consumidos pelo módulo Android.
- [x] `FCLApp`, inicialização de caminhos/logs, `RendererManager`, `JavaManager`, perfis e controles inicializados sem abrir a interface original do FCL.
- [x] Runtime ARM64 empacotado e instalador com staging, validação ELF e rollback.
- [x] Renderizadores GL4ES, Zink e VGPU disponíveis pelo registro FCL.
- [x] Instalador Vanilla com metadados, client, bibliotecas, natives, assets e logging.
- [x] Integração dos instaladores de Forge, NeoForge, Fabric, Quilt e OptiFine conforme os backends disponíveis no projeto.
- [x] Coordenador de inicialização que prepara versão, dependências, runtime, autenticação offline, JNA/LWJGL, `FCLBridge` e `JVMActivity`.
- [x] Adaptador `FclJavaGameEngine` ligado ao contrato `GameEngine`, com descoberta de perfis, consulta de disponibilidade e inicialização real.
- [x] Tela inicial consulta os perfis reais do repositório FCL e bloqueia o botão de iniciar quando o runtime ou renderizador não está pronto.
- [x] Aba **Jogar** abre a mesma tela de inicialização real, evitando o botão de demonstração antigo.
- [x] Botão **Iniciar Minecraft** ligado ao coordenador real; lista de perfis é consultada no repositório FCL.
- [x] Preferências de runtime, renderizador e memória aplicadas a uma configuração FCL específica por versão.
- [x] Serviço de sessão de jogo do FCL declarado no manifesto mesclado para manter a sessão em primeiro plano enquanto o jogo estiver aberto.
- [x] Instalação de versões executada por serviço Android em primeiro plano, com notificação persistente e progresso do download Vanilla.
- [x] Tela Jogar/Início conectada ao coordenador FCL, com seleção de perfil e conta offline.
- [x] Diagnóstico de inicialização com etapa atual, cadeia de exceções e caminho dos logs, com opção de copiar.
- [x] Testes unitários para validação de nome offline, disponibilidade do motor, runtime e instaladores.
- [x] CI compila as fontes Android/Kotlin, executa testes e compila/verifica o backend nativo ARM64.

## O que ainda exige validação real

A compilação do CI prova que as fontes e bibliotecas são construíveis; não prova que o Minecraft inicia corretamente no dispositivo. Antes de declarar o motor pronto:

- [ ] Instalar Java 8 ARM64 e iniciar Minecraft Vanilla 1.12.2 no Galaxy A12.
- [ ] Validar a superfície gráfica, toque, teclado, áudio, controles e retorno ao launcher.
- [ ] Testar versões modernas com Java 17 e o renderizador adequado.
- [ ] Validar Forge 1.12.2, OptiFine e Forge + OptiFine em instalação limpa; investigar o erro anterior do OptiFine patcher.
- [ ] Validar Fabric, Quilt e NeoForge individualmente, incluindo bibliotecas e metadados.
- [ ] Testar memória baixa, troca de aplicativo durante download e jogo, rotação, pausa/retomada e encerramento inesperado.
- [ ] Integrar autenticação Microsoft antes de oferecer esse método de conta.
- [ ] Integrar streaming de logs diretamente na tela Logs.
- [ ] Rever o encerramento do processo upstream: o FCL pode encerrar o processo do launcher ao sair do jogo; retorno automático à tela Compose ainda não foi garantido.

## Limitações conhecidas

- O botão de iniciar usa conta offline. Não deve ser anunciado como login Microsoft.
- GL4ES, Zink e VGPU são as opções expostas pelo launcher nesta integração. ANGLE e Software não são apresentados como opções FCL suportadas.
- A instalação em segundo plano usa um Foreground Service; o Android ainda pode encerrar processos em condições extremas de memória ou após uma parada forçada pelo usuário.
- Horizon Rust continua independente e não é usado como fallback do Minecraft Java.
- O launcher não deve afirmar que o jogo está funcionando apenas porque a versão aparece na lista ou porque o CI passou.
- Nenhum APK é gerado automaticamente; a geração do APK depende de pedido explícito.

## Licença e atribuição

O código upstream do Fold Craft Launcher é distribuído sob GNU GPL v3.0. A incorporação e distribuição de uma versão modificada/derivada deve cumprir as condições aplicáveis da GPL-3.0, incluindo fornecer o código-fonte correspondente e preservar avisos de copyright/licença. Cada dependência de terceiros continua sujeita à sua própria licença.
