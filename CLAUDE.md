# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

QKSMS is an open-source Android SMS/MMS replacement app (`com.moez.QKSMS`, GPLv3). It is a multi-module Gradle project written in Kotlin, using RxJava2 as its reactive backbone and Realm for persistence.

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

Build environment: JDK 21, Kotlin 2.3.21, AGP 9.3.0, KSP 2.3.11, Gradle 9.5.0, compileSdk 37, minSdk 23, targetSdk 33. Maven repos are routed through Aliyun mirrors (see root `build.gradle`). AGP built-in Kotlin is enabled (the standalone `kotlin-android` plugin is removed); Realm's annotation processor runs via `com.android.legacy-kapt`.

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
- **`domain`** (`com.android.library`) — pure business layer. Holds **repository interfaces** (`repository/`), **Realm model classes** (`model/`), and **use-case interactors** (`interactor/`, ~29 files). No Android UI.
- **`data`** — **repository implementations** (`repository/*Impl.kt`), Realm migrations (`migration/QkRealmMigration.kt`), platform receivers/services, sync/blocking logic. Implements the `domain` contracts.
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

RecyclerViews use `QkAdapter`/`QkViewHolder`; `QkRealmAdapter` bridges Realm `OrderedRealmCollection` change events into adapter updates.

Cross-Activity navigation goes through `common/Navigator.kt` using plain `Intent`s — Conductor is only for in-Activity routing, not global navigation.

## Dependency injection (Dagger 2, dagger-android)

- `injection/AppComponent.kt` — the `@Singleton` app graph; installs `AppModule` + the `ActivityBuilderModule` / `BroadcastReceiverBuilderModule` / `ServiceBuilderModule` (each uses `@ContributesAndroidInjector`).
- `injection/AppModule.kt` — app-scoped singletons (Context, managers, Realm config).
- Per-feature Dagger `@Module` classes live under `feature/<name>/` (e.g. `ComposeActivityModule.kt`). Two features have their own subcomponents: `ConversationInfoComponent`, `ThemePickerComponent`.
- `domain` and `data` classes use constructor injection (`@Inject constructor`) only — they declare no `@Component`/`@Module`; they get wired into the app graph from `presentation`.

## Persistence (Realm)

- Realm model classes are in **`domain/model/`** (`Conversation`, `Message`, `MmsPart`, `Contact`, `Recipient`, `ScheduledMessage`, etc. — those implementing `RealmObject`). Note `Attachment`, `BackupFile`, `SearchResult` in the same package are plain models, not Realm objects.
- Schema changes require bumping the version and adding a migration step in `data/migration/QkRealmMigration.kt`.
- Realm query helpers: `data/extensions/RealmExtensions.kt`.

## Conventions & gotchas

- **Translations**: managed via [Crowdin](https://crowdin.com/project/qksms). Do not edit translated `values-<locale>/strings.xml` files directly — only edit the default `presentation/src/main/res/values/strings.xml`.
- **Analytics/Crashlytics/Billing** (Firebase Crashlytics, Amplitude, Play Billing) are gated behind the `withAnalytics` flavor. The `google-services` and `firebase-crashlytics` plugins are applied only when the Gradle task name contains `WithAnalytics`. Keep analytics-only code in `withAnalyticsImplementation` dependencies / flavor-specific source sets.
- **Signing**: local builds sign with the checked-in `debug.keystore` (alias/password `android`). CI (`System.getenv("CI") == "true"`) uses `../keystore` and env vars `keystore_password` / `key_alias` / `key_password`.
- **View binding** is enabled (`buildFeatures { viewBinding true }`); `buildConfig` is on.
