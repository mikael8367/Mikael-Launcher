# Mikael Java Engine Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish a compiling, testable modular Java-engine foundation in Mikael Launcher while preserving the current Compose UI and keeping Horizon Rust isolated.

**Architecture:** Keep the current Android application as the host and introduce a domain contract between Compose screens and game engines. Audit the pinned FCL source before adding any Gradle modules or native binaries; connect only APIs and artifacts verified to exist upstream. This first milestone stops at a tested adapter boundary and a documented integration map, not a claim that Minecraft already launches.

**Tech Stack:** Kotlin 2.0.21, Android Gradle Plugin 8.7.3, Gradle 8.9, Android minSdk 26, Jetpack Compose, JUnit 4, GitHub Actions, pinned FCL GPL-3.0 submodule.

**Spec:** `docs/superpowers/specs/2026-11-10-java-engine-modular-integration-design.md`

## Global Constraints

- Preserve the existing Kotlin/Compose application, landscape navigation and visual identity.
- Keep Minecraft Java and Horizon Rust as separate engine implementations and storage domains.
- Keep FCL pinned to upstream commit `f5bb1526eb371d81cb086d74cdb8535043b51ee4` until an explicit audited update.
- Do not upgrade AGP, Kotlin, Gradle, compileSdk or minSdk without a compatibility finding and a separate justified change.
- Do not advertise runtime, renderer, loader, installation or launch capability until the real backend path is implemented and verified.
- Do not embed proprietary Minecraft game files.
- Preserve FCL/dependency license notices and GPL-3.0 source obligations.
- Do not generate, sign or publish an APK.
- Never store credentials in source, logs or test fixtures.

## Review Focus

- Missing Compose extension imports or dependency drift causing Kotlin compilation failures — baseline CI compile task.
- Missing or inaccessible FCL submodule content during checkout — submodule checkout and source-inventory task.
- Engine contracts that accidentally couple Java and Horizon Rust — contract unit tests and package-boundary review.
- Android lifecycle or activity assumptions in FCL that cannot be safely called from Compose — audit findings must identify required host/lifecycle integration before adapter code.
- ARM64/native artifacts absent or mismatched — native ABI inventory test/report; no renderer/runtime may be marked available without evidence.

---

### Task 1: Restore and lock the Android baseline

**Files:**
- Modify: `app/src/main/java/com/mikael/launcher/ui/screens/AdditionalLauncherScreens.kt`
- Inspect: `.github/workflows/android-ui-checks.yml`
- Test: existing `:app:compileDebugKotlin` and `:app:testDebugUnitTest` CI tasks.

**Interfaces:**
- Consumes: current Compose dependencies and app module configuration.
- Produces: a green baseline on this branch before introducing engine abstractions.

- [ ] **Step 1: Reproduce the existing compile failure** by running the Android UI workflow on the current branch and preserve the exact compiler output.
- [ ] **Step 2: Confirm the import failure** by inspecting the imports and the `Modifier.size(42.dp)` call in `AdditionalLauncherScreens.kt`.
- [ ] **Step 3: Add the precise missing import** `androidx.compose.foundation.layout.size`; do not alter unrelated UI code.
- [ ] **Step 4: Run baseline verification** with `gradle :app:compileDebugKotlin :app:testDebugUnitTest` in CI.
- [ ] **Step 5: Commit** as `fix(android): restore Compose Kotlin compilation baseline`.

### Task 2: Audit the pinned FCL source and build boundary

**Files:**
- Inspect: `third_party/FoldCraftLauncher/`
- Create: `docs/superpowers/audits/2026-11-10-fcl-upstream-audit.md`
- Inspect: `settings.gradle.kts`, root `build.gradle.kts`, `app/build.gradle.kts`, `.gitmodules`

**Interfaces:**
- Consumes: pinned FCL submodule and the green baseline from Task 1.
- Produces: an evidence-based integration map; no app dependency is added in this task.

- [ ] **Step 1: Inventory Gradle modules and plugin versions** from FCL settings and build files; record AGP, Kotlin, Gradle wrapper, repositories and included builds.
- [ ] **Step 2: Inventory Android entry points and lifecycle requirements** (Application, Activity, Services, JNI initialization, permissions, storage and callbacks).
- [ ] **Step 3: Inventory native artifacts** by ABI, packaging format, size and declared license; explicitly identify ARM64 availability.
- [ ] **Step 4: Map candidate APIs** for version metadata, downloads, runtime selection, renderer selection, launch and logs; cite exact upstream file paths and symbols.
- [ ] **Step 5: Record compatibility blockers** and a minimal integration sequence. Do not copy FCL classes or add Gradle includes until each dependency boundary is understood.
- [ ] **Step 6: Review license notices** for FCL and the native dependencies selected for the first integration slice.

### Task 3: Define engine-neutral domain contracts

**Files:**
- Create: `app/src/main/java/com/mikael/launcher/engine/GameEngine.kt`
- Create: `app/src/main/java/com/mikael/launcher/engine/EngineModels.kt`
- Test: `app/src/test/java/com/mikael/launcher/engine/EngineContractTest.kt`

**Interfaces:**
- Consumes: FCL audit report from Task 2; contracts must not invent upstream API calls.
- Produces: engine-neutral result and capability types usable by the UI and later adapters.

- [ ] **Step 1: Write unit tests** for engine identity, supported capabilities, operation states and typed failure representation.
- [ ] **Step 2: Run the focused test** and confirm it fails because the contract types do not yet exist.
- [ ] **Step 3: Define `GameEngine`** with a stable engine identifier, capability snapshot and lifecycle-safe operation boundary; keep method signatures small and independent of Android UI classes.
- [ ] **Step 4: Define sealed operation/result models** for idle, progress, success, cancellation and typed failure; include a user-safe message and diagnostic code without credentials.
- [ ] **Step 5: Run focused and full unit tests** and confirm all contract tests pass.
- [ ] **Step 6: Review package boundaries** to ensure no FCL implementation types leak into Compose screens and no Java-specific type is required by Horizon Rust.
- [ ] **Step 7: Commit** as `feat(engine): add engine-neutral domain contracts`.

### Task 4: Add a truthful Java-engine availability boundary

**Files:**
- Create: `app/src/main/java/com/mikael/launcher/engine/java/JavaEngineAvailability.kt`
- Test: `app/src/test/java/com/mikael/launcher/engine/java/JavaEngineAvailabilityTest.kt`
- Modify only if required: the existing Play action entry point in `HomeScreen.kt`.

**Interfaces:**
- Consumes: contracts from Task 3 and verified findings from Task 2.
- Produces: a Java-engine state that can distinguish unavailable, initializing and ready without claiming launch support prematurely.

- [ ] **Step 1: Write tests** proving the Java engine reports unavailable until a verified FCL adapter is installed and initialized, and that missing native/runtime prerequisites produce a specific reason.
- [ ] **Step 2: Run tests and confirm the expected failure** before implementation.
- [ ] **Step 3: Implement the availability model** using only audit-confirmed prerequisites; do not add fake launch methods or simulated success.
- [ ] **Step 4: Connect the existing UI only to truthful availability state**; retain a clear disabled/coming-soon state if the backend is not yet callable.
- [ ] **Step 5: Run compile and unit tests** and inspect the UI state for repeated taps and lifecycle recreation.
- [ ] **Step 6: Commit** as `feat(java-engine): expose verified backend availability`.

### Task 5: Harden CI for the integration boundary

**Files:**
- Modify: `.github/workflows/android-ui-checks.yml`
- Modify only if justified by Task 2: Gradle configuration files.
- Test: GitHub Actions workflow.

**Interfaces:**
- Consumes: tasks 1–4 and the audited FCL build requirements.
- Produces: repeatable CI checks for the host app and the engine contract.

- [ ] **Step 1: Add a CI check** that confirms the FCL submodule is checked out at the pinned commit.
- [ ] **Step 2: Keep host compile and unit tests as required gates**; add FCL-specific build validation only if the audit proves it can run safely in the current CI environment.
- [ ] **Step 3: Add static checks** for accidental APK publishing, unpinned submodule changes and credentials in tracked source/configuration.
- [ ] **Step 4: Run the complete workflow** and inspect all failed job logs, not just the summary.
- [ ] **Step 5: Perform a final bug review** covering Gradle configuration, lifecycle assumptions, engine isolation, native ABI claims, failure states and credential/log exposure.
- [ ] **Step 6: Commit** as `ci(android): verify modular Java engine foundation`.

## Milestone exit criteria

- The current Android app compiles and its unit tests pass in CI.
- The FCL source audit documents exact modules, build compatibility, Android lifecycle, candidate APIs, ABIs and licenses.
- Engine contracts are covered by unit tests and do not couple Horizon Rust to Minecraft Java.
- The Java engine reports unavailable until a real, verified adapter exists.
- No APK is generated or published.
- The final report distinguishes completed work, CI-verified work, and device-only validation still required.

## Scope deliberately deferred to follow-up plans

The following are separate integration milestones and must receive their own implementation plans after this foundation is reviewed: FCL Android bootstrap and lifecycle bridge; Java runtime install/import and ARM64 validation; Vanilla metadata/dependency installer; real Vanilla process launch and log capture; renderer integration; Fabric/Forge/OptiFine; mods/resource packs/shaders/worlds/modpacks; and device compatibility/performance testing.
