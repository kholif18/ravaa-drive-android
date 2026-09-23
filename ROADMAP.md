# Roadmap — Ravaa Drive Android

**Konsep:** Google Drive clone untuk Ravaa HOME. Hit **langsung** ke Ravaa-Drive
(port 2713, single repo — tidak ada lagi `ravaa-service`/gateway 2711).
Auth `Authorization: Bearer ravaa_...` dari `POST /api/auth/mobile-login`.

## Phase 0 — Backend (Drive) — ✅ SELESAI
- `POST /api/auth/mobile-login` + `GET/DELETE /api/user/tokens` (login & revoke per-device)
- `getServerAuth()` terima Bearer; middleware terima token `ravaa_`, 401 JSON untuk `/api/*`, CORS+preflight
- Endpoint: files/folders/notes/notebooks/todos/share/search/activity/profile + `chunk` upload + `storage-stats`
- Tes: `curl` login → Bearer ke notes/todos/files = 200, tanpa token = 401, OPTIONS = 204

## Phase 1 — Fondasi Android — ✅ SELESAI (scaffold + API layer)
- Hilt + Retrofit + DataStore + Navigation Compose
- `NetworkModule`: base URL dari `BuildConfig.DRIVE_BASE_URL`, auth interceptor, timeout upload 120s
- `AuthApi` (mobile-login/validate/me), `DriveApi`, `NotesApi`, `TodosApi`, `ApiModels` sesuai respons 2713
- `LoginScreen` + `DriveViewModel` (load/search/starred/recent/shared/trash + state 401 `SESSION_EXPIRED`)

## Phase 2 — Drive UI Core (berikutnya)
- **My Drive:** TopBar Search + storage bar, Breadcrumb sticky, List/Grid toggle, FAB `+` BottomSheet
- **File Item:** icon per tipe, modified, `⋮` BottomSheet (Share LINK, Move, Star, Rename, Delete → Trash)
- Layar Search/Recent/Starred/Shared/Trash disambungkan ke loader yang sesuai (parsial: Search + Starred sudah)
- `FileDetailsSheet`: preview (Coil/ExoPlayer), Info, Share

## Phase 3 — Upload & Offline (menyusul)
- Upload multipart + `chunk` resumable via WorkManager (pause/resume, progress)
- Download + cache offline (Room), delta sync
- Material3 Dark `#0A0A0A` + Dynamic Color, release build + Play Internal
