# Auditoria de integração — Horizon Rust

Data da auditoria: 2026-11-10
Repositório analisado: https://github.com/horizon-rust/horizon
Ref analisada: main

## Resultado

O repositório é o projeto correto: uma reimplementação nativa em Rust do Minecraft Java 26.3. O README declara licença MIT para o trabalho original (as dependências conservam licenças próprias), jogo single-player, multiplayer, geração de mundo, renderização, saves, launcher e mods próprios.

**Não existe, na documentação pública analisada, um pacote Android ARM64 pronto para instalar.** A tabela oficial de downloads lista Windows x64/ARM64, macOS ARM64/x64 e Linux x86-64. Não lista Android nem APK.

## Evidências técnicas

- Workspace Rust usa edition 2024 e exige Rust 1.95+.
- Preparação de recursos do jogo exige JDK 25 e uma cópia legítima do Minecraft Java 26.3, incluindo o client e server jar próprios do usuário.
- O cliente usa winit e wgpu com backends gráficos multiplataforma. O upstream atual contém módulos Android de estado de entrada/gamepad/teclado, mas isso não prova que o launcher e o cliente completo estejam integrados ou testados como app Android.
- O launcher upstream usa GPUI e dependências de desktop; não é uma biblioteca Android que possa ser adicionada diretamente ao app Compose.
- O upstream atual possui módulos de entrada Android para gamepad e keypad, mas a integração com a Activity/Surface do nosso app, toque, áudio, permissões, armazenamento e pausa/retomada ainda precisa ser conectada e testada.
- O projeto não executa mods Java de Forge/Fabric/NeoForge. Tem formatos e API próprios para mods Horizon.
- A preparação dos recursos envolve JDK 25; não se deve assumir que a rotina desktop de preparação possa ser executada diretamente no Android.

## Compatibilidade do dispositivo

O alvo inicial do Mikael Launcher é Android ARM64. A compatibilidade com o Galaxy A12/PowerVR GE8320 não pode ser inferida apenas por o projeto usar wgpu/Vulkan/GLES. É necessário detectar as APIs gráficas reais no dispositivo e validar a criação de superfície/dispositivo. Não forçar Vulkan como requisito até o teste confirmar suporte funcional.

## Plano seguro de integração

1. Manter Horizon como motor separado do motor Java/FCL.
2. Fixar uma revisão upstream e auditar crates, licenças, recursos, scripts de build e dependências nativas.
3. Integrar o cliente upstream ao alvo Android ARM64 experimental, sem incluir o launcher GPUI desktop; validar se o event loop e o backend winit/wgpu podem operar no ciclo de vida Android.
4. Compilar primeiro um protótipo mínimo Rust + JNI/NativeActivity e validar carregamento ARM64 e superfície gráfica.
5. Validar winit/wgpu no Android e testar Vulkan/GLES por detecção de capacidade; tratar incompatibilidade como erro recuperável.
6. Adaptar entrada por toque, armazenamento privado/SAF, áudio, rede, ciclo de vida e logs.
7. Preparar/importar os recursos de jogo em fluxo compatível, verificando integridade e direitos de uso. Nunca incluir assets proprietários no repositório.
8. Integrar instalador somente quando houver artefato Android oficial/reproduzível, checksums assinados ou verificáveis e política de atualização.
9. Só habilitar o botão Instalar/Jogar após teste real em Android ARM64. Não distribuir APK sem solicitação explícita do usuário.

## Bloqueios atuais

- Sem release Android oficial.
- Sem release Android ARM64 oficial.
- Sem build reproduzível do cliente completo neste app.
- Sem inicialização Android do jogo comprovada.
- Sem validação de Vulkan/GLES no dispositivo-alvo.
- Sem integração de toque/lifecycle/armazenamento/áudio.
- Sem artefato Android e checksum para o instalador.

## Critério para declarar concluído

Build reproduzível ARM64; dependências e licenças auditadas; teste de carregamento JNI; superfície gráfica funcional; menu/tela inicial; entrada por toque; importação/preparação de recursos; criação/carregamento/salvamento de mundo; logs de falha; testes de instalação interrompida, arquivo corrompido, pouco espaço e atualização. A conclusão exige teste real em dispositivo Android, não apenas compilação cruzada.
