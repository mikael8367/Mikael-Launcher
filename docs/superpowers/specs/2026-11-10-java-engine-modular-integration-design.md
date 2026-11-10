# Mikael Launcher — integração modular do motor Minecraft Java

**Data:** 2026-11-10  
**Status:** Proposta para aprovação  
**Branch alvo:** `feature/java-engine-fcl-foundation`

## 1. Intenção

Transformar o Mikael Launcher em um launcher Android capaz de instalar, configurar e iniciar Minecraft Java Edition de verdade, mantendo a interface Kotlin/Jetpack Compose existente. O backend do Fold Craft Launcher (FCL) será aproveitado por integração gradual e isolada, sem substituir a navegação e a identidade visual do Mikael Launcher.

O Horizon Rust permanece como um segundo motor independente. Nenhuma classe, formato de pacote ou fluxo de instalação do Minecraft Java será presumido compatível com Horizon Rust.

## 2. Estado atual e evidências

- O app atual usa Kotlin, Jetpack Compose, Material 3, Android Gradle Plugin 8.7.3, Kotlin 2.0.21, Gradle 8.9, minSdk 26 e Java 17 para compilar.
- A navegação lateral e as telas de versões, contas, mods, runtimes, renderizadores, downloads e logs existem, mas várias ações ainda são demonstrativas ou persistem apenas preferências locais.
- O submódulo FCL está fixado em `third_party/FoldCraftLauncher`, no commit upstream `f5bb1526eb371d81cb086d74cdb8535043b51ee4`.
- O FCL é um projeto Android completo com módulos Gradle, JNI e bibliotecas nativas; não é uma dependência simples que possa ser adicionada ao app com uma única linha.
- O FCL upstream declara GPL-3.0. A integração e qualquer distribuição derivada devem preservar avisos e cumprir as obrigações aplicáveis da licença, incluindo disponibilização do código-fonte correspondente.
- O workflow atual falha antes dos testes: `AdditionalLauncherScreens.kt:219` usa `Modifier.size` sem importar a extensão `androidx.compose.foundation.layout.size`. Esse é um bloqueio de compilação preexistente e deverá ser corrigido e verificado antes de medir regressões da integração.

## 3. Objetivos

1. Manter a interface, navegação, tema e identidade do Mikael Launcher.
2. Integrar o backend Java do FCL atrás de interfaces próprias do Mikael Launcher.
3. Instalar versões oficiais do Minecraft Java com cliente, libraries, assets e natives.
4. Suportar, conforme compatibilidade comprovada do backend, Vanilla, Forge, Fabric e OptiFine.
5. Gerenciar runtimes Java instaláveis/importáveis e validar arquitetura/versão antes de iniciar.
6. Conectar renderizadores realmente disponíveis no backend, sem exibir opções como funcionais se não estiverem instaladas.
7. Montar argumentos de JVM e do jogo, respeitando RAM disponível, limites Android e configurações por perfil.
8. Iniciar e acompanhar o processo do jogo, exibindo logs, progresso, erros e relatórios de falha.
9. Integrar mods, resource packs, shaders, mundos e modpacks ao diretório correto de cada instalação.
10. Preservar isolamento entre Minecraft Java e Horizon Rust.
11. Manter o projeto utilizável em fluxo mobile; desenvolvimento, configuração e testes devem poder ser acompanhados pelo GitHub Actions, sem exigir que o usuário tenha PC.

## 4. Fora de escopo nesta integração

- Não implementar nem alterar o motor Horizon Rust.
- Não embutir arquivos proprietários do Minecraft; os arquivos de jogo devem ser obtidos por fluxos legítimos e fontes oficiais/permitidas.
- Não prometer compatibilidade universal com todos os mods, loaders, renderizadores ou dispositivos.
- Não ativar renderizador, runtime ou loader apenas porque existe uma opção na interface.
- Não gerar, assinar ou publicar APK sem pedido explícito do usuário.
- Não remover avisos, licenças ou atribuições do FCL e de suas dependências.

## 5. Abordagens consideradas

### A. Substituir a base atual pelo aplicativo FCL

Vantagem: maior proximidade com a estrutura upstream e menos adaptação inicial do backend.  
Desvantagem: risco alto de perder ou reescrever a interface, navegação e identidade do Mikael Launcher; grande migração de Gradle e módulos.

### B. Integrar o FCL por módulos atrás de um adaptador (escolhida)

Vantagem: preserva o app Compose atual e permite conectar uma capacidade por vez, com fronteira clara entre interface e motor.  
Desvantagem: exige mapear APIs internas do FCL, adaptar ciclo de vida Android, dependências e integração Gradle; a primeira execução real pode demorar mais.

### C. Reimplementar o motor Java do zero

Vantagem: controle total sobre arquitetura e dependências.  
Desvantagem: duplicaria anos de trabalho em instalação, loaders, bibliotecas, runtimes, JNI e renderização, com risco técnico e manutenção muito maiores.

**Decisão:** abordagem B. O FCL permanece como upstream versionado em submódulo; o app Mikael consome capacidades selecionadas por adaptadores internos, sem acoplar as telas diretamente às classes internas do FCL.

## 6. Arquitetura proposta

### Camada de interface

As telas Compose existentes continuam sendo a camada de apresentação. Elas chamam casos de uso/serviços do Mikael, nunca classes internas do FCL diretamente.

### Contrato do motor

Criar uma fronteira de domínio, por exemplo:

- `GameEngine`: contrato de ciclo de vida e capacidades do motor.
- `JavaEngine`: implementação para Minecraft Java.
- `HorizonEngine`: contrato separado, sem implementação Java compartilhada.
- `EngineCapabilities`: lista explícita de funções suportadas pelo backend ativo.
- `EngineResult` / estados de operação: sucesso, progresso, cancelamento e erro tipado.

Os nomes finais e pacotes serão definidos no plano após mapear as APIs reais do FCL; não criar wrappers fictícios em torno de APIs ainda não verificadas.

### Adaptador FCL

O adaptador encapsula o backend upstream e isola:
- inicialização e ciclo de vida Android;
- resolução de versão e metadados;
- instalação de arquivos e verificação de integridade;
- runtime Java e variáveis de ambiente;
- seleção de renderizador e bibliotecas nativas;
- criação de processo e encaminhamento de logs;
- cancelamento e limpeza de operações.

### Persistência e arquivos

- Armazenar preferências e perfis em repositório próprio, não espalhar SharedPreferences pelas telas.
- Separar diretórios por motor e instalação.
- Usar APIs de armazenamento Android (SAF/MediaStore conforme o caso), evitando caminhos absolutos presumidos.
- Baixar para arquivos temporários, validar tamanho/hash quando disponível e só então mover para o destino final.
- Downloads devem ser retomáveis quando o backend suportar; falhas não podem deixar arquivos parciais apresentados como instalados.

## 7. Fluxos funcionais

### Instalação e versões

1. Consultar manifesto oficial de versões.
2. Usuário escolhe release/snapshot e loader compatível.
3. Resolver metadados e dependências.
4. Baixar cliente, libraries, assets e natives.
5. Validar arquivos e registrar instalação concluída.
6. Exibir progresso, velocidade quando disponível, pausa/cancelamento e retry.

Não misturar metadados/arquivos de versões diferentes nem marcar instalação como concluída antes da validação.

### Loaders e modding

- Vanilla primeiro como caminho-base.
- Fabric e Forge em fatias separadas, respeitando versões suportadas.
- OptiFine tratado como instalação/patch compatível com versão e loader, sem assumir que todas as combinações funcionam.
- Modrinth e CurseForge devem filtrar por versão e loader, validar dependências e conflitos e instalar apenas artefatos compatíveis.
- Modpacks devem ser importados com validação de manifesto, paths seguros e limites de armazenamento.
- Mods Java não devem ser aplicados ao Horizon Rust.

### Runtime

- Descobrir ABI/arquitetura do dispositivo e validar cada runtime antes de selecioná-lo.
- Instalar/importar runtimes em diretório privado e verificar executável, versão, arquitetura e integridade.
- Não presumir que Java 8/17/21/25 funciona em todas as versões de Minecraft ou em todos os dispositivos.
- O runtime selecionado no perfil deve ser o runtime realmente usado na inicialização.

### Renderização

- Inventariar os renderizadores e bibliotecas realmente incluídos no backend FCL.
- Cada opção deve indicar disponibilidade e compatibilidade; renderizadores experimentais ou ausentes ficam desativados com explicação.
- Não declarar Vulkan, Zink, VirGL, ANGLE, GL4ES ou Software como operacionais sem teste no dispositivo e caminho de inicialização real.
- Falha de inicialização gráfica deve produzir erro e log identificáveis, sem loop infinito.

### Execução e diagnóstico

- Botão Jogar valida conta, instalação, runtime, renderizador e espaço disponível antes de iniciar.
- O processo precisa ter ciclo de vida controlado, estado de inicialização/execução/encerramento e cancelamento seguro.
- Capturar stdout/stderr, logs do launcher e crash reports sem bloquear a UI.
- Falhas devem mostrar causa acionável e permitir copiar/compartilhar relatório, respeitando privacidade.
- Evitar iniciar duas instâncias da mesma instalação por toque repetido.

## 8. Segurança, privacidade e confiabilidade

- Não armazenar senhas em texto puro. OAuth deve usar fluxo oficial com navegador/Custom Tabs e PKCE quando aplicável.
- Chaves CurseForge permanecem em armazenamento privado e nunca entram em logs, commits ou relatórios.
- Validar URLs, redirects, tamanhos, hashes e caminhos de arquivos baixados/importados.
- Rejeitar entradas ZIP com traversal, caminhos absolutos, links perigosos ou expansão desproporcional.
- Downloads devem ter timeouts, retry limitado e mensagens de erro claras.
- Nunca executar binário ou script importado sem validação de origem, arquitetura e integridade.
- Logs não devem expor tokens, credenciais ou dados pessoais desnecessários.

## 9. Compatibilidade e limites de dispositivo

- Plataforma alvo: Android, minSdk atual 26.
- Arquitetura inicial de validação: ARM64-v8a; outras ABIs só serão anunciadas quando os binários correspondentes existirem.
- O dispositivo de referência conhecido para testes do usuário é um Galaxy A12 ARM64 com recursos limitados; testes de memória e renderização precisam considerar dispositivos modestos.
- A aplicação não deve assumir que há PC disponível para o usuário. CI e instruções mobile-first são necessários.
- Limites de RAM devem considerar memória do sistema, runtime e renderer, e não apenas a RAM física total.

## 10. Plano de entrega por fatias

1. **Recuperar baseline:** corrigir o erro de compilação existente, executar compile e unit tests, registrar baseline verde.
2. **Auditoria FCL:** mapear módulos, APIs públicas/internas, ciclo de vida, bibliotecas nativas, ABIs, versões de AGP/Gradle/Kotlin e licenças.
3. **Contrato do motor:** criar interfaces e estados testáveis, mantendo o JavaEngine ainda indisponível até o adaptador existir.
4. **Inicialização FCL:** adaptar Activity/ciclo de vida e provar que o backend pode inicializar sem iniciar Minecraft.
5. **Runtime real:** instalar/importar e validar runtime Java ARM64 e associá-lo a um perfil.
6. **Instalador Vanilla:** baixar metadados, cliente, libraries, assets e natives com validação e cancelamento.
7. **Primeira execução Vanilla:** montar argumentos, iniciar processo e capturar logs em dispositivo ARM64.
8. **Renderizadores:** conectar e validar individualmente os renderizadores suportados.
9. **Loaders:** integrar Fabric e Forge; depois avaliar OptiFine e combinações suportadas.
10. **Biblioteca de conteúdo:** mods, resource packs, shaders, mundos e modpacks, cada um com validação e persistência.
11. **Robustez:** recuperação de falhas, prevenção de duplicidade, limpeza de temporários, relatórios e testes de regressão.
12. **Compatibilidade final:** matriz por Android/ABI/runtime/renderer/loader, sem prometer combinações não testadas.

Cada fatia deve ser revisada e testada antes da seguinte. A implementação não deve tentar ligar todos os recursos de uma vez.

## 11. Critérios de aceitação

- A interface Compose e a navegação lateral permanecem funcionais.
- O Horizon Rust continua separado e não recebe arquivos Java.
- O baseline de compilação e testes está verde antes da integração.
- Uma versão Vanilla pode ser instalada com arquivos validados e estado persistente.
- O botão Jogar inicia uma instalação Vanilla real em dispositivo ARM64 compatível.
- Logs e erros são visíveis e copiáveis, sem travar a interface.
- Runtime e renderizador exibidos como selecionados correspondem aos binários realmente utilizados.
- Loader só aparece como compatível quando o backend confirmar a combinação.
- Download interrompido ou inválido nunca aparece como instalação concluída.
- Não há APK gerado/publicado sem solicitação explícita.

## 12. Riscos e decisões técnicas pendentes para o plano

- O FCL upstream usa uma estrutura Gradle e módulos nativos que podem exigir migração controlada do build. Não atualizar AGP/Kotlin/Gradle por tentativa; primeiro mapear versões e incompatibilidades.
- É necessário identificar a menor superfície de API do FCL que permite inicialização, instalação e execução sem copiar partes desconexas.
- Dependências nativas precisam de inventário de ABIs, tamanho, licença e impacto de memória.
- As telas atuais ainda têm ações placeholder; o plano deve ligar cada ação apenas quando seu serviço real estiver pronto.
- O workflow de CI deve ser corrigido e ampliado gradualmente; o erro atual de `Modifier.size` é anterior à integração e não deve ser confundido com erro do FCL.

## 13. Chamadas do responsável pelo projeto

- Abordagem escolhida: integração modular FCL atrás de adaptador.
- Preservar UI e arquitetura atual do Mikael Launcher.
- Manter Horizon Rust independente.
- Não gerar APK sem solicitação explícita.
- A ordem de implementação e os detalhes internos de integração serão refinados no plano técnico após aprovação deste documento.
