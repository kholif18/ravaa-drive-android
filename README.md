# ravaa-drive-android

Android client for **Ravaa Drive** — bagian dari ekosistem Ravaa HOME.

- **Package:** `com.ravaa.drive`
- **API:** langsung ke Ravaa-Drive, **tanpa gateway** (`ravaa-service` sudah dilebur ke Drive)
  - Debug (emulator): `http://10.0.2.2:2713/` (via `BuildConfig.DRIVE_BASE_URL`)
  - HP fisik: ganti `DRIVE_BASE_URL` debug ke IP LAN laptop (mis. `http://192.168.x.x:2713/`)
  - Release: `https://drive.ravaa.my.id/` (TODO: ganti ke domain produksi)
- **Auth:** `POST /api/auth/mobile-login` `{email, password, deviceName}` → token `ravaa_...`,
  dikirim sebagai `Authorization: Bearer ravaa_...` (otomatis oleh interceptor). 401 = sesi habis → login ulang.
- **Stack:** Kotlin + Jetpack Compose + Hilt + Retrofit + DataStore (+ Room & WorkManager menyusul untuk offline sync)

## Fitur (parity dengan Drive web 2713)
- Login perangkat + revoke per-device (`GET/DELETE /api/user/tokens`)
- List files/folders (`GET /api/files?folderId=&limit=&cursor=`), breadcrumb, search (`GET /api/search?q=`)
- Photos / Recent / Starred / Shared / Trash
- Upload multipart (`POST /api/files/upload`); file besar menyusul via `POST /api/files/chunk`
- Download streaming (`GET /api/files/{id}/download`), thumb (`GET /api/files/{id}/thumb`)
- Storage bar (`GET /api/files/storage-stats`)
- Notes & Tasks API client sudah tersedia (`NotesApi`, `TodosApi`), UI menyusul

## Dev
```bash
# Tidak ada Android SDK di mesin ini — build di Android Studio / CI.
# Base URL debug ada di app/build.gradle.kts -> buildConfigField DRIVE_BASE_URL
```
