# Integração do motor Minecraft Java (FCL)

## Decisão técnica

O Mikael Launcher manterá a interface Kotlin/Jetpack Compose. O motor Java será integrado como backend isolado, sem misturar o código do Horizon Rust.

O código upstream do Fold Craft Launcher foi fixado como submódulo Git em:

- Repositório: https://github.com/FCL-Team/FoldCraftLauncher
- Commit fixado: `f5bb1526eb371d81cb086d74cdb8535043b51ee4`
- Licença declarada pelo projeto: GNU GPL v3.0

Fixar um commit evita que uma atualização upstream inesperada altere o motor durante o desenvolvimento.

## Estado desta etapa

- [x] Registrar o upstream FCL como submódulo.
- [x] Fixar o commit upstream usado como base.
- [x] Preparar o checkout do submódulo no CI.
- [x] Auditar os módulos Android, bibliotecas nativas e dependências do FCL.
- [x] Criar o módulo isolado `:fcl-native` para compilar o CMake/JNI FCL em ARM64 e importar as bibliotecas nativas upstream.
- [ ] Adaptar o módulo Android/Kotlin do FCL à Activity e ao ciclo de vida do Mikael Launcher.
- [x] Implementar a primeira camada independente de instalação Vanilla pelo manifesto oficial (client, bibliotecas Maven aplicáveis, cache de classifiers nativos, assets e logging; sem inicialização do jogo).
- [ ] Adaptar o GameRepository/DependencyManager do FCL, extração e substituição de natives Android ARM64 e layout histórico de assets.
- [ ] Integrar runtimes Java e renderizadores reais ao `FCLGameLauncher` / `FCLBridge`.
- [ ] Ligar o botão Iniciar ao fluxo real `LauncherHelper → FCLGameLauncher → FCLBridge → JVMActivity` e encaminhar stdout/stderr para Logs.
- [ ] Validar Vanilla, Forge, Fabric e OptiFine em dispositivos ARM64 reais.

**Importante:** a camada CMake/JNI ARM64 agora compila como módulo separado, mas isso ainda não equivale a uma integração executável. O FCL Java/Kotlin, seus recursos/manifesto, os runtimes e o fluxo `FCLBridge` + `JVMActivity` ainda não foram incorporados. O motor não deve ser apresentado como funcional até que todas essas partes estejam ligadas e testadas em dispositivo ARM64.

## Por que a integração será gradual

O FCL não é uma dependência Android pequena: inclui o app Android `:FCL`, os módulos `:ZipFileSystem` e `:Terracotta`, dependências AAR/JAR, centenas de recursos/assets, CMake/JNI, SDL e a atividade de superfície de jogo. O app upstream usa AGP 8.13.2, Kotlin 2.4.10 e Gradle 8.14.4; o Mikael Launcher ainda usa AGP 8.7.3, Kotlin 2.0.21 e Gradle 8.9.

A primeira integração real isolou o CMake/JNI em `:fcl-native`, sem alterar a interface atual nem substituir o aplicativo principal do launcher. A próxima etapa é transformar/adaptar o módulo Android FCL para ser consumido pelo app principal, tratando o manifesto, `FCLApp`, `JVMActivity`, recursos e dependências Gradle. O fluxo nativo só será usado depois que a Activity puder fornecer a superfície gráfica correta ao `FCLBridge`.

## Licença e atribuição

O código upstream do Fold Craft Launcher é distribuído sob GNU GPL v3.0. Ao incorporar ou distribuir uma versão modificada/derivada, o projeto deverá cumprir as condições aplicáveis da GPL-3.0, incluindo fornecer o código-fonte correspondente e preservar avisos de copyright/licença. As dependências nativas e bibliotecas de terceiros também precisam de uma auditoria de licença individual.

Até a conclusão da integração, o submódulo permanece separado do módulo `:app`; não se deve remover ou alterar avisos de licença do upstream.
