# Roadmap — Ravaa Drive Android

**Konsep:** Google Drive clone untuk Ravaa HOME. Satu pintu `https://service.ravaa.my.id/api/v1/mobile` (proxy ke Drive 2713 & Notes 2714), auth `Bearer ravaa_token` sama kayak web.

## Phase 1 — Backend (Service + Drive) — 2 hari
**Tujuan:** Android bisa login & list/upload/download tanpa hit 2713/2714 langsung.

- [ ] **Service `src/modules/mobile/mobile.routes.ts` (gateway):**
  - `POST /auth/login` (reuse) → `ravaa_token` 15m
  - `GET /mobile/drive/files?folderId=&q=&page=&limit=&starred=&recent=` → proxy Drive `GET /api/files`
  - `GET /mobile/drive/storage` → proxy `GET /storage-stats` (bar 5GB di TopBar)
  - `POST /mobile/drive/files/upload` (multipart 10MB) → proxy `POST /api/files/upload`
  - `POST /mobile/drive/chunk` + `GET /mobile/drive/chunk/status?fileId=` → chunk 2MB resumable (5 paralel pause/resume meneruskan)
  - `GET /mobile/drive/files/:id/download` → proxy `GET /raw`
  - `POST /mobile/drive/folders` / `PATCH` / `DELETE` → rename/move/trash
  - `GET /mobile/notes|notebooks` → proxy Notes
  - `POST /mobile/devices` + `GET /mobile/sync?since=` → delta sync Room
- [ ] **Drive 2713:** tambah pagination `page/limit`, `starred` filter, `chunk` BigInt sanitize (sudah fix `e1eddce`)
- [ ] **Tes:** `curl` Bearer semua endpoint, `npm run build` Service + Drive PASS

## Phase 2 — Fondasi Android (2 hari)
- Hilt + Retrofit + Room + DataStore + Navigation Compose
- `LoginScreen`, simpan `ravaa_token`/`refreshToken`, auto-refresh, `Logout`
- `NavHost` bottom 4 tab: My Drive | Shared | Starred | Files(Recent/Trash)

## Phase 3 — Drive UI Core (3 hari)
- **My Drive:** TopBar Search + storage bar, Breadcrumb sticky, List/Grid toggle, FAB `+` BottomSheet (Upload file/folder, New folder)
- **File Item:** `KoraFileIcon`, owner dot, modified, `⋮` BottomSheet (Share LINK password/expiry/maxViews, Move, Star, Rename, Delete → Trash)
- **Search/Recent/Starred/Shared/Trash** (reuse DriveScreen filter)

## Phase 4 — Upload & Polish (2 hari)
- **UploadWidget:** port `Ravaa-Drive/components/drive/upload-widget.tsx` → Compose + WorkManager, pool 5 worker, pause ⏸ per-file & Pause All/Resume All header (sudah ada di web f586812), progress `overall`
- **Details BottomSheet:** preview (Coil/ExoPlayer), Info, Activity, Share
- **Theming:** Material3 Dark `#0A0A0A` + Dynamic Color, `TSC` build

**Estimasi total 9 hari.** Backend Phase 1 dulu — setelah `curl` semua mobile endpoint `200`, lanjut Phase 2 Compose.
