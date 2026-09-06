# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

QKSMS is an open-source Android SMS/MMS replacement app (`com.moez.QKSMS`, GPLv3). It is a multi-module Gradle project written in Kotlin, using RxJava2 as its reactive backbone and Room for persistence.

## Build & test commands

The app has a `withAnalytics` / `noAnalytics` flavor dimension crossed with `debug` / `release` build types, giving four variants (e.g. `noAnalyticsDebug`, `withAnalyticsRelease`). For local development use the `noAnalytics` flavor — `withAnalytics` requires a `google-services.json` and secrets that live in the encrypted `secrets.tar.enc` (decrypted only in CI), so `withAnalytics` builds fail on a fresh checkout.

```bash
# Assemble the debug APK (use noAnalytics locally)
./gradlew :presentation:assembleNoAnalyticsDebug

# Full build the CI release path (needs secrets — will fail without google-services.json)
./gradlew :presentation:assembleWithAnalyticsRelease :presentation:bundleWithAnalyticsRelease

# Android Lint
./gradlew :presentation:lintNoAnalyticsDebug

# Clean
./gradlew clean
```

Tests are **instrumented** tests (`src/androidTest`, require a device/emulator) — there are no `src/test` unit tests despite JUnit/Mockito being on the classpath. Frameworks: JUnit4, Mockito (`mockito-android`), Espresso.

```bash
# Run all instrumented tests (device/emulator required)
./gradlew connectedNoAnalyticsDebugAndroidTest

# Run a single instrumented test class
./gradlew :data:connectedNoAnalyticsDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.moez.QKSMS.repository.MessageRepositoryTest
```

Build environment: JDK 21, Kotlin 2.3.21, AGP 9.3.1, KSP 2.3.11, Gradle 9.5.0, compileSdk 37, minSdk 23, targetSdk 37. Maven repos are routed through Aliyun mirrors (see root `build.gradle`). AGP built-in Kotlin is enabled (the standalone `kotlin-android` plugin is removed); `com.android.legacy-kapt` is kept only for moshi's `kaptRelease moshi-kotlin-codegen`.

> **JDK 21 required.** AGP 9's built-in Kotlin toolchain and lint analyzer require JDK 21 APIs. Building on JDK 17 fails with `NoSuchMethodError`. Set the Gradle JDK to 21 (Android Studio: Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK).

## Module structure & dependency direction

Five modules (`settings.gradle`), dependencies flow one way:

```
presentation → data, domain, common, android-smsmms
data         → domain, common, android-smsmms
domain       → common
common       → android-smsmms
```

- **`presentation`** (`com.android.application`) — the app module. All UI, Dagger wiring, and screen-level logic. Packages: `common` (base classes/widgets/util), `feature` (one package per screen), `injection` (Dagger), `interactor`.
- **`domain`** (`com.android.library`) — pure business layer. Holds **repository interfaces** (`repository/`), **model classes** (`model/`), and **use-case interactors** (`interactor/`, ~29 files). No Android UI.
- **`data`** — **repository implementations** (`repository/Room*RepositoryImpl.kt`), the Room database (`db/`), platform receivers/services, sync/blocking logic. Implements the `domain` contracts.
- **`common`** — shared compat/util code and vendored Glide GIF encoder.
- **`android-smsmms`** (`com.klinker.android.send_message`) — vendored third-party SMS/MMS transport library. Rarely touched.

When adding a feature that touches data: define the interface in `domain/repository/`, implement it in `data/repository/`, and wire it through Dagger in `presentation/injection/`.

## Architecture pattern (MVI-flavored, RxJava2)

Base classes live in `presentation/.../common/base/`. Two parallel patterns coexist — pick the one matching the host:

1. **Activity + ViewModel** (Jetpack `ViewModel`): used for top-level Activities. Example: `feature/compose/` has `ComposeActivity` + `ComposeViewModel` + `ComposeView` (interface) + `ComposeState` (data class).
2. **Conductor Controller + Presenter** (`QkController` extends Conductor `LifecycleController`, `QkPresenter`): used for screens hosted inside a container Activity via [Conductor](https://github.com/bluelinelabs/Conductor). Example: `SettingsActivity` attaches a router hosting `SettingsController`. Also `BlockingController`, `ConversationInfoController`, `BackupController`.

The unidirectional flow is the same for both:

- **View** is an interface extending `QkView<State>` / `QkViewContract<State>` with a single `render(state)` method. It exposes user actions as RxJava "intent" streams (`Observable`/`Subject` properties like `messageClickIntent`).
- **State** is an immutable data class.
- **ViewModel/Presenter** (`QkViewModel` / `QkPresenter`) holds `state: BehaviorSubject<State>` plus a `stateReducer` subject of `State.() -> State` lambdas. Call `newState { copy(...) }` to push reducers. `bindView(view)` / `bindIntents(view)` subscribes the view's intents and wires state → `view::render` via **AutoDispose** (`autoDispose(view.scope())`) so subscriptions are lifecycle-scoped.
- **Activity/Controller** implements the View interface, forwards widget events into the intent Subjects (via `rxbinding2`), and calls `bindView`/`bindIntents`.

RecyclerViews use `QkAdapter`/`QkViewHolder`; `QkListAdapter` subclasses drive updates via `DiffUtil` against Room's `Flowable`/`LiveData` query results.

Cross-Activity navigation goes through `common/Navigator.kt` using plain `Intent`s — Conductor is only for in-Activity routing, not global navigation.

## Dependency injection (Dagger 2, dagger-android)

- `injection/AppComponent.kt` — the `@Singleton` app graph; installs `AppModule` + the `ActivityBuilderModule` / `BroadcastReceiverBuilderModule` / `ServiceBuilderModule` (each uses `@ContributesAndroidInjector`).
- `injection/AppModule.kt` — app-scoped singletons (Context, managers, Room database/DAOs).
- Per-feature Dagger `@Module` classes live under `feature/<name>/` (e.g. `ComposeActivityModule.kt`). Two features have their own subcomponents: `ConversationInfoComponent`, `ThemePickerComponent`.
- `domain` and `data` classes use constructor injection (`@Inject constructor`) only — they declare no `@Component`/`@Module`; they get wired into the app graph from `presentation`.

## Persistence (Room)

- Model classes are in **`domain/model/`** (`Conversation`, `Message`, `MmsPart`, `Contact`, `Recipient`, `ScheduledMessage`, etc.) — plain Kotlin classes, mapped to Room entities in `data/db/entity/`. `Attachment`, `BackupFile`, `SearchResult` in the same package are transient models with no Room entity.
- The Room database is `data/db/QkDatabase.kt`. Schema changes require bumping `@Database(version = ...)` and adding a `Migration` step.
- `QkDatabase` does **not** enable `allowMainThreadQueries()` — Room's main-thread check is active. Repository methods that return a plain value (e.g. `getConversation`, `getRecipient`, `getContacts`) are synchronous and will throw if called on the main thread. Wrap them in `Single.fromCallable {}.subscribeOn(Schedulers.io())` / `Completable.fromAction {}`, or put an `observeOn(Schedulers.io())` upstream in the Rx chain, and hop back with `observeOn(AndroidSchedulers.mainThread())` before touching views. Calls made inside an `Interactor.buildObservable` are already backgrounded by `Interactor.execute()`.
- Room's `Flowable`/`Observable` queries emit on Room's own IO executor (thread names `arch_disk_io_N`), not on the subscriber's thread. Any subscription that writes to a view — including `notifyDataSetChanged()` from an adapter — needs an explicit `observeOn(AndroidSchedulers.mainThread())`.

## Conventions & gotchas

- **Translations**: managed via [Crowdin](https://crowdin.com/project/qksms). Do not edit translated `values-<locale>/strings.xml` files directly — only edit the default `presentation/src/main/res/values/strings.xml`.
- **Analytics/Crashlytics/Billing** (Firebase Crashlytics, Amplitude, Play Billing) are gated behind the `withAnalytics` flavor. The `google-services` and `firebase-crashlytics` plugins are applied only when the Gradle task name contains `WithAnalytics`. Keep analytics-only code in `withAnalyticsImplementation` dependencies / flavor-specific source sets.
- **Signing**: local builds sign with the checked-in `debug.keystore` (alias/password `android`). CI (`System.getenv("CI") == "true"`) uses `../keystore` and env vars `keystore_password` / `key_alias` / `key_password`.
- **View binding** is enabled (`buildFeatures { viewBinding true }`); `buildConfig` is on.
- **Git commits**: do not include an AI signature in commit messages (no `Co-Authored-By: Claude` trailer or similar).
