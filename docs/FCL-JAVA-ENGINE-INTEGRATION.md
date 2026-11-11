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
- [ ] Auditar os módulos Android, bibliotecas nativas e dependências do FCL.
- [ ] Adaptar o backend FCL à Activity e ao ciclo de vida do Mikael Launcher.
- [x] Implementar a primeira camada independente de instalação Vanilla pelo manifesto oficial (client, bibliotecas Maven aplicáveis, cache de classifiers nativos, assets e logging; sem inicialização do jogo).
- [ ] Adaptar o GameRepository/DependencyManager do FCL, extração e substituição de natives Android ARM64 e layout histórico de assets.
- [ ] Integrar runtimes Java e renderizadores reais.
- [ ] Ligar o botão Iniciar ao processo de jogo e encaminhar stdout/stderr para Logs.
- [ ] Validar Vanilla, Forge, Fabric e OptiFine em dispositivos ARM64 reais.

**Importante:** o submódulo é a fonte upstream, não uma integração executável ainda. O motor não deve ser apresentado como funcional até que o backend, os binários nativos e o fluxo de instalação/execução estejam ligados e testados.

## Por que a integração será gradual

O FCL não é uma dependência Android pequena: contém módulos Gradle próprios, componentes JNI e bibliotecas nativas de renderização/áudio/entrada. O projeto atual usa Android Gradle Plugin 8.7.3, enquanto o upstream usa uma configuração Gradle diferente. Copiar apenas uma classe ou declarar uma dependência fictícia não faria o Minecraft iniciar e poderia quebrar a compilação do launcher.

A próxima etapa técnica é mapear os módulos mínimos e suas dependências, sem substituir a interface atual nem misturar o Horizon Rust.

## Licença e atribuição

O código upstream do Fold Craft Launcher é distribuído sob GNU GPL v3.0. Ao incorporar ou distribuir uma versão modificada/derivada, o projeto deverá cumprir as condições aplicáveis da GPL-3.0, incluindo fornecer o código-fonte correspondente e preservar avisos de copyright/licença. As dependências nativas e bibliotecas de terceiros também precisam de uma auditoria de licença individual.

Até a conclusão da integração, o submódulo permanece separado do módulo `:app`; não se deve remover ou alterar avisos de licença do upstream.
