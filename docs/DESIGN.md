# Ravaa Drive Android — Design (Google Drive Concept)

## 1. Arsitektur (MVVM + Clean, Offline-first)
```
com.ravaa.drive/
├── data/
│   ├── api/          # Retrofit: DriveApi, NotesApi, AuthApi (gateway 2711/mobile)
│   ├── db/           # Room: FileEntity, FolderEntity, SyncQueue
│   └── repository/   # DriveRepository, AuthRepository
├── domain/
│   ├── model/        # File, Folder, Share, StorageStats
│   └── usecase/      # GetFiles, UploadChunked, Search, ToggleStar
└── presentation/
    ├── auth/         # LoginScreen (Ravaa Service)
    ├── drive/        # DriveScreen (My Drive) — Google Drive Home
    ├── search/
    ├── starred/
    ├── shared/
    ├── recent/
    ├── trash/
    ├── details/      # File details + activity + share sheet
    └── upload/       # UploadWidget (5 paralel + pause/resume chunk 2MB)
```

**Stack:** Kotlin, Compose Material3, Hilt, Retrofit2 + OkHttp, Room, DataStore, WorkManager, Coil, Navigation Compose.

**Gateway:** `https://service.ravaa.my.id/api/v1/mobile/*` (prod) — satu baseURL, Bearer ravaa_token.

## 2. UI — Google Drive Clone (Material3)

### Bottom Navigation (4 tab, kayak Drive)
- **My Drive** (Home) — folder tree, breadcrumb, list/grid toggle
- **Shared** — Shared with me (FAMILY/LINK)
- **Starred** — starred files
- **Files** — Recent + Trash tab

### My Drive Screen
- **TopBar:** Search (global) + avatar (Ravaa Account) + storage bar (5GB HOME)
- **Breadcrumbs:** `My Drive / Ravaa Notes / {noteId}` — horizontal scroll, sticky
- **ViewToggle:** List (detail: name, owner, modified) ↔ Grid (thumbnail)
- **FAB:** `+` New → BottomSheet: Upload file, Upload folder, New folder, New doc (Notes shortcut)
- **List Item:** icon (KoraFileIcon), name, owner dot, modified 1 line, more `⋮` → BottomSheet: Share LINK, Move, Star, Rename, Delete → Trash
- **EmptyState:** illustration + "Drop files here"
- **Pull-to-refresh + offline banner**

### Search
- Global `?q=` + filter chips: Type (Docs, Images, Audio), Owner, Modified, Starred

### File Details BottomSheet (Google Drive style)
- Preview (image/video), Info (size, owner, modified), Activity, Share (LINK with password/expiry/maxViews + copy), Manage access

### UploadWidget (floating, sudah ada di web 5 paralel)
- Bottom sheet `Uploading 5 files` — progress per-file, Pause ⏸ / Resume ▶ per-file + Pause All/Resume All di header, Cancel all

### Trash
- Directory seperti File Manager — `Restore` / `Delete permanently`

## 3. Flow Penting
Login → My Drive → Breadcrumb → FAB Upload (chunk 2MB, 5 paralel, pause/resume meneruskan) → Share LINK → Recent/Starred.

## 4. Theming
`Material3` + `Dark #0A0A0A` (sama web glass), Dynamic Color, `KoraFileIcon` untuk mime.

## 5. Next
Offline Room sync via `GET /mobile/sync?since=` + WorkManager periodic.
