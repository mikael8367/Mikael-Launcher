# FCL upstream integration audit

**Audited revision:** FCL-Team/FoldCraftLauncher@f5bb1526eb371d81cb086d74cdb8535043b51ee4
**Date:** 2026-11-10
**Scope:** source/build boundary reconnaissance; not a complete line-by-line security or license audit.

## Executive result

FCL is an Android application, not a reusable Android library. Its main :FCL module applies com.android.application, owns application ID com.tungsten.fcl, an extensive Android manifest, native CMake/JNI code, app resources and runtime/game assets. It cannot safely be added as a normal dependency to Mikael Launcher's existing :app module.

The safe direction remains staged extraction/adaptation behind Mikael-owned interfaces. Do not include the FCL application module directly and do not replace the Mikael manifest or Application class.

## Build compatibility

| Setting | Mikael Launcher | Pinned FCL |
|---|---:|---:|
| Android Gradle Plugin | 8.7.3 | 8.13.2 |
| Kotlin | 2.0.21 | 2.4.10 |
| Gradle | 8.9 | 8.14.4 |
| compileSdk | 35 | 35 |
| minSdk | 26 | 26 |
| Java/JVM target | 17 | 17 |
| targetSdk | 35 | 34 |

FCL declares plugin aliases in gradle/libs.versions.toml, includes :FCL, :Terracotta, :ZipFileSystem, :LWJGL, :LWJGL:lwjgl-3.3.3 and :LWJGL:lwjgl-3.4.1. FCL uses KSP, Kotlin serialization, core-library desugaring, Room, CMake/NDK 27.0.12077973, and a substantial dependency set. Its :FCL module is an application module, not a library module.

**Decision:** do not upgrade the Mikael root build just to match FCL, and do not include FCL's root settings/build in the host build. First isolate the smallest compatible pure-Java/Kotlin core pieces and identify which Android/native pieces require a dedicated adapter/library module. Gradle compatibility must be proven in a separate CI branch before any build-system migration.

## Android lifecycle and application coupling

- com.tungsten.fcl.FCLApp extends Application, stores a static singleton and a weak reference to the current Activity, and registers Activity lifecycle callbacks.
- FCL manifest sets android:name=.FCLApp, declares numerous Activities, Services, providers, permissions and app-specific resources.
- MainActivity extends FCL's FCLActivity, initializes ConfigHolder and RendererManager, creates FCL UI bindings, and owns profile/account/UI state.
- JVMActivity is the game surface host. It consumes a static FCLBridge and renderer/game menu state, starts GameService, and manages a TextureView/SDL display lifecycle.
- FCLGameLauncher extends DefaultLauncher; its constructor requires Android Context, GameRepository, Version, AuthInfo and LaunchOptions.

These are not safe to call directly from Compose without a lifecycle bridge. The global Activity singleton and static bridge are important constraints for lifecycle recreation and stale Activity references.

**Decision:** do not merge FCL's manifest or use its FCLApp as Mikael's Application. Any extraction must define explicit initialization/teardown and Activity/surface handoff contracts.

## Candidate backend seams found

- Version/game model and repository: com.tungsten.fclcore.game.GameRepository, DefaultGameRepository, Version, GameVersion and LaunchOptions.
- Launch orchestration: com.tungsten.fclcore.launch.DefaultLauncher and com.tungsten.fcl.game.FCLGameLauncher.
- Runtime utilities: com.tungsten.fcl.util.RuntimeUtils, including Java runtime installation from packaged universal + architecture-specific tar.xz assets.
- Renderer registry: com.mio.manager.RendererManager and com.mio.data.Renderer; renderer metadata includes IDs and Minecraft-version bounds.
- Game rendering host: com.tungsten.fcl.activity.JVMActivity, coupled to FCL bridge, SDL, native and service components.

These are candidate seams only. Before coding against them, inspect complete public/protected method signatures, transitive types, threading contract, task/event model and initialization order.

## Native ABI and renderer inventory

Pinned FCL has jniLibs directories for arm64-v8a, armeabi-v7a, x86 and x86_64. ARM64 includes ANGLE libraries, GL4ES, VGPU, Freedreno Vulkan, unpack200 and other native libraries. The other ABIs have different sets; they must not be treated as feature-equivalent.

Additional renderer/native artifacts are packaged as AARs under FCL/libs, including MobileGlues, NG-GL4ES, SDL, kopper-zink, LWJGL natives and SPIR-V support. Runtime/JRE assets are packaged separately. The build script contains ABI-aware asset filtering for JRE and LWJGL/JNA native directories.

**Decision:** ARM64 is the first validation target. Do not expose a renderer/runtime as available solely because its name exists in the UI; validate its binary, ABI, dependency closure, game-version compatibility and device execution path. A complete size/hash/license inventory is still required before adding native artifacts to Mikael.

## Manifest and permission implications

FCL's manifest declares broad legacy/external storage permissions (including MANAGE_EXTERNAL_STORAGE), network/state, wake lock, notifications, multiple foreground-service types, install/delete package permissions and microphone permission. It also declares multiple Activities, Services, providers, network security config and app-specific theme/resources.

**Decision:** do not copy the FCL manifest wholesale. Each permission/component must be justified against an implemented Mikael feature and current Android behavior. Prefer SAF/private app storage where possible; request sensitive permissions only when strictly required and explain why.

## Signing material and supply-chain caution

The public upstream repository root contains files named key-store.jks, debug-key.jks and private_key.pepk, and the FCL build script references the key store for signing. Their presence upstream is not permission to use them. The integration must not copy or use upstream signing material; Mikael's signing identity must remain independent and must never be committed to the repository.

## License

The pinned FCL repository declares GPL-3.0. The selected code and dependency license obligations must be reviewed before redistribution. Preserve notices and provide corresponding source as required by the applicable license. Each native/AAR/JAR dependency needs its own license and provenance inventory; the top-level FCL license alone is not a complete dependency audit.

## Baseline CI findings

- Workflow run 42927445001 on commit 38ccea3005edc37ee405dccabbbcaf3632ec3677: :app:compileDebugKotlin succeeded after adding the missing androidx.compose.foundation.layout.size import.
- The same run failed in two existing LauncherSectionTest assertions: tests expected six navigation sections, while LauncherSection.entries currently contains twelve. The implementation already contains Accounts, Resource Packs, Shaders, Worlds, Performance and Logs.
- Updated test expectations to the twelve current sections in commit fdb90d256884544fa32d9bd953bf214331e3c4f3. A fresh workflow result for that commit is still required; the previous run does not verify the updated test file.

## Open audit items before a real adapter

1. Inspect exact DefaultLauncher method lifecycle and launch callback signatures, and all event/task interfaces.
2. Trace GameRepository construction, directories, download managers and version install flow.
3. Trace runtime asset extraction, architecture naming and executable validation, including custom runtime import support.
4. Map all transitive native libraries and their licenses, ABI coverage, size and initialization order.
5. Identify a supported way to host the game surface without FCL's full MainActivity and global Activity singleton.
6. Prove Gradle integration in CI before changing the host build.
7. Verify the license and provenance of every binary dependency selected for the first slice.

## Audit conclusion

The modular approach is feasible as a multi-stage engineering effort, but there is no verified drop-in FCL library. The next implementation slice should finish the host baseline, then create engine-neutral contracts; do not yet wire the FCL application module, import its manifest, copy signing material, or claim Minecraft launch support.