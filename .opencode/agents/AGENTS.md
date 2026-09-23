# Ravaa-Drive-Android AI Agent Guide

## Project Overview

Ravaa-Drive-Android is the native Android client for Ravaa-Drive (single repo backend on port 2713 — no gateway, no `ravaa-service`). Kotlin + Jetpack Compose, Google-Drive-style UI (bottom nav 4 tabs + drawer, glassmorphism dark), offline-first (Room cache + outbox + WorkManager sync), file viewer (image gallery/PDF/video streaming/office via external app).

- **User language**: Indonesian — respond in Indonesian unless user writes English.
- **Git policy**: NEVER commit/push unless user says "push ke git"/"commit". Inspect `git status`, stage only intended, `feat:`/`fix:` prefix.
- **Backend**: Ravaa-Drive at `BuildConfig.DRIVE_BASE_URL` (debug `http://10.0.2.2:2713/` emulator or LAN IP for physical device, release `https://drive.ravaa.my.id/`), overridable at runtime via Settings → Server (HostRewriteInterceptor, no restart needed).
- **Build**: Gradle 8.7 (`~/gradle/gradle-8.7/bin/gradle`), JDK 21 (`/opt/android-studio/jbr`), SDK `~/Android/Sdk`, `assembleDebug` must succeed. No Android SDK wrapper in repo — use system gradle.

## Tech Stack

- **Language**: Kotlin, **UI**: Jetpack Compose Material3 (dark-only `#0A0A0A` glass)
- **DI**: Hilt (`@HiltAndroidApp RavaaApp`, `@HiltViewModel`, `SingletonComponent` modules)
- **Network**: Retrofit + Gson + OkHttp (Bearer interceptor + host-rewrite interceptor, 120s timeouts)
- **Storage**: DataStore preferences (token/prefix/email/server), Room (`ravaa-drive.db`: cached_items + pending_ops), Coil (authed ImageLoader)
- **Background**: WorkManager (periodic 15min + on-demand sync, HiltWorker)
- **Media**: Media3 ExoPlayer (streaming), PdfRenderer (built-in), FileProvider (open-with)
- **Icons**: Kora PNGs (`drawable-nodpi/kora_*.png`) for file types + Lucide-style Material icons for UI

## Project Structure

```
ravaa-drive-android/
├── .opencode/           # agents/skills
├── app/
│   ├── build.gradle.kts # AGP 8.5.2, Kotlin 1.9.22, compose 1.6.8
│   └── src/main/
│       ├── AndroidManifest.xml  # INTERNET, FileProvider, no WorkManager auto-init
│       ├── res/drawable-nodpi/  # logo.png + kora_*.png (192px)
│       ├── res/mipmap-*/        # launcher icons (from img/icon.png)
│       └── java/com/ravaa/drive/
│           ├── MainActivity.kt  # @AndroidEntryPoint, RavaaTheme + NavGraph
│           ├── RavaaApp.kt      # Hilt + WorkManager config + periodic sync
│           ├── di/              # NetworkModule (baseUrl/auth/host/cors/timeout/retrofit/apis/imageLoader), DataModule (Room)
│           ├── data/api/        # ApiModels (ApiResponse envelope), AuthApi, DriveApi, NotesApi, TodosApi
│           ├── data/db/         # Cache.kt (CachedItem, PendingOp, DAOs, AppDatabase)
│           ├── data/datastore/  # TokenDataStore (token/prefix/email/server; clear keeps server)
│           ├── data/repository/ # DriveRepository (cache-first + outbox + sync)
│           ├── data/sync/       # ConnectivityObserver, SyncWorker
│           ├── presentation/
│           │   ├── navigation/  # NavGraph (server→login→main, bottom 4 tabs + drawer + viewer/search/settings)
│           │   ├── server/      # ServerScreen (Nextcloud-style connect + /api/health check)
│           │   ├── auth/        # LoginScreen (glass, eye toggle) + LoginViewModel (mobile-login)
│           │   ├── drive/       # DriveScreen (search pill, breadcrumb, storage bar, FAB, details, selection), DriveViewModel, DriveListContent, MoveDialog
│           │   ├── details/     # FileDetailsSheet (full actions + trashMode), ShareDialog, VersionsSheet
│           │   ├── viewer/      # ViewerScreen (pager gallery, streaming video, PDF) + ViewerViewModel
│           │   ├── files/       # FilesScreen (Recent/Trash tabs)
│           │   ├── search|shared|starred|settings|upload|session|components/  # screens, UploadWidget, DriveDrawer
│           ├── ui/theme/        # Theme (dark), Glass (glassCard + GlassBackground + LocalContentColor white), KoraIcons
│           └── util/            # Format (file size, relative date id-ID)
├── img/                 # icon.png/icon.svg source
└── docs/                # DESIGN.md (Google Drive concept)
```

## Development Rules

- Kotlin everywhere, MVVM + Hilt, Compose only (no XML layouts except manifest/res).
- API envelope `{success, data|error}` — check `success`, map 401 → `SESSION_EXPIRED` → auto-logout to login.
- Auth: `POST /api/auth/mobile-login {email,password,deviceName}` → `ravaa_...` Bearer (auto via interceptor). Email lowercase. No refresh token — 401 means login again.
- Dark-only UI: explicit `Color.White` tints/texts (never rely on theme defaults inside custom containers — bare `Box` has BLACK content color; `GlassBackground` provides white globally).
- No `alert()`/native dialogs — Material3 AlertDialog/BottomSheet/snackbar.
- Long-press = multi-select, tap file = viewer, ⋮ = details sheet (Google Drive behavior).
- After changes: `assembleDebug` must pass; install + smoke test on device if plugged (`adb install -r`, `adb reverse tcp:2713` for localhost dev).

## Offline-First (authoritative pattern)

- **Read**: Room `cached_items` Flow (instant, offline OK). **Write online**: API then cache. **Write offline**: optimistic cache + `pending_ops` outbox (STAR/RENAME/TRASH only).
- **Sync**: `DriveRepository.sync()` (replay FIFO, stop on first failure) + pull root/trash. Triggers: connectivity regain (VM observer), manual refresh (expedited worker), periodic 15min.
- Banner "Offline — menampilkan cache" + pending count. Server wins conflicts.
- NOT offline (yet): file bytes, uploads while offline, notes/todos.

## Verification

```bash
~/gradle/gradle-8.7/bin/gradle assembleDebug # must BUILD SUCCESSFUL
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb reverse tcp:2713 tcp:2713 # USB testing vs laptop dev server
adb exec-out screencap -p > /tmp/check.png # visual verify (wake device first)
```

## Troubleshooting

- `Unresolved reference: hilt` → missing `hilt-navigation-compose` dep.
- Missing extended icons → `material-icons-extended` dep + explicit imports.
- `NetworkOnMainThreadException` → blocking I/O must run on `Dispatchers.IO` (viewModelScope is Main).
- `HttpUrl.get` error → use `toHttpUrl()` extension.
- Black screenshots → device screen off/dreaming; wake + swipe, or MENU key (82).
- `adb: unauthorized` → tap Allow USB debugging on phone.
- Login fills wrong field → use keyevent-per-char typing, TAB (61) between fields.
