name: ravaa-android
description: Guide for developing Ravaa-Drive-Android, a Kotlin + Compose native client for Ravaa-Drive backend (files, upload, share, viewer, offline sync, tasks). Use when writing or modifying any Android code — screens, ViewModels, Retrofit APIs, Room, workers, navigation, styles. Covers MVVM+Hilt patterns, API envelope conventions, offline-first repository, icon system, glassmorphism, and verification steps. Triggers on "android", "compose screen", "viewmodel", "retrofit", "room", "worker", "drive screen", "upload", "viewer", "sync", "share dialog".
---

# Ravaa-Drive-Android Development Guide

## Before you start

- Read `.opencode/agents/AGENTS.md` — it is the authoritative project guide (architecture, offline-first pattern, conventions).
- Always finish with `assembleDebug` — must be BUILD SUCCESSFUL.
- User speaks Indonesian; Git push only when explicitly asked.

## MVVM + Hilt pattern

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val api: DriveApi,
    private val repo: DriveRepository
) : ViewModel() {
    private val _state = MutableStateFlow...
    val state = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            try { ... } catch (e: Exception) { _error.value = ... }
        }
    }
}
```

- Screens take `vm: XxxViewModel = hiltViewModel()` + `onMenu: () -> Unit = {}`.
- Navigation lives in `presentation/navigation/NavGraph.kt` (server→login→main, bottom tabs + `viewer?fileId&name&mime&gallery&index` + `files?tab=` + `settings`).
- Session expiry: VM sets `_error = "SESSION_EXPIRED"` → screen calls `onSessionExpired()` → root nav to login.

## API conventions (must match backend envelope)

- Every response: `ApiResponse<T>(success, data, error)`. Always check `success` first.
- `Authorization: Bearer ravaa_...` is automatic (interceptor). Never pass tokens manually, except ExoPlayer datasource (read via `TokenDataStore.tokenFlow.first()`).
- `POST /api/auth/mobile-login {email (lowercase!), password, deviceName}` → `data.token`.
- No refresh token. 401 → logout to login.
- Paging: `?limit&cursor` (files), `?limit&cursor` (notes). Map `fileSize` as Long (server sanitizes BigInt → number).
- Thumb: `GET api/files/{id}/thumb` (needs auth — use injected Coil `ImageLoader`). Raw bytes: `GET api/files/{id}/raw` (supports Range/206 — video streams progressively).
- Mutations: PATCH file `{name?, isStarred?, isTrashed?}`, bulk-move `{ids, folderId}`, copy `{ids:[{id,type}]}`, share `{fileId/folderId, permission, visibility:"LINK"}` → `data.share.shareToken` → public URL `{baseUrl}s/{token}`.

## Offline-first (do not bypass)

- UI reads Room Flows via `DriveRepository` (never bare `api.list*` for cached views).
- Writes: `repo.setStarred/rename/trash` (handles online-direct vs offline-queue internally).
- After any mutation: reload current view (`load(currentFolderId())`).
- Show offline banner via `vm.isOffline` + `vm.pendingCount`.
- New endpoints used for lists must ALSO update `pull*` + DAO, or the view won't work offline.

## Icons

- File/folder icons = Kora PNGs via `KoraIcons.forFile(name, mime)` (`ui/theme/KoraIcons.kt`, drawables `kora_*.png`). Never use grey generic icons for files.
- Folders: `R.drawable.kora_folder`. UI actions: Material icons with explicit white tints.
- To add a type: render SVG → `drawable-nodpi/kora_<name>.png` (lowercase, dots/dashes→underscore, `+`→`pp`, e.g. `c++`→`cpp`), extend the mapping function. See kora-android skill.

## Glassmorphism (dark-only)

- Wrap screens in `GlassBackground { }` (provides white `LocalContentColor` — bare `Box` defaults to BLACK content!).
- Cards: `Modifier.glassCard(corner)` (translucent gradient + hairline border).
- All text/icons inside custom containers need explicit `Color.White` (or `.copy(alpha)`) — never rely on theme defaults there.
- Login/server cards: `glassCard(32.dp)`, fields `RoundedCornerShape(20.dp)`, buttons `RoundedCornerShape(26.dp)`.

## Upload / download / share / viewer

- ≤10MB: single multipart. Larger: chunked 2MB via `chunkStatus` (resume offset) + `uploadChunk`.
- Upload/download I/O MUST run on `Dispatchers.IO` (viewModelScope is Main — `NetworkOnMainThreadException`/ANR otherwise).
- Viewer: images = thumb-first + full crossfade + gallery pager + prefetch neighbor; video = ExoPlayer streaming with Bearer header datasource; PDF = PdfRenderer (cap 100 pages); office/other = FileProvider `ACTION_VIEW` chooser (`file_paths.xml`, `cache-path preview/`).
- Partial downloads: delete on failure (else poisoned cache). Chunk resume: query offset first.
- Download to device: MediaStore `Downloads/Ravaa Drive` (API 29+).

## Verification checklist

- [ ] `assembleDebug` BUILD SUCCESSFUL
- [ ] New API usage has offline path or explicit online-require error
- [ ] Overlays/dialogs use explicit white colors
- [ ] No blocking I/O on Main thread
- [ ] `adb install -r` + smoke test if device is plugged (`adb devices`); wake device before screenshots
