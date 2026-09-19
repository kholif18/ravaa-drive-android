# ravaa-drive-android

Android client for **Ravaa Drive** — bagian dari ekosistem Ravaa HOME.

- **Package:** `com.ravaa.drive` / `id.my.ravaa.drive`
- **API Gateway:** `https://api.ravaa.my.id` (prod) / `http://localhost:2711/api/v1/mobile` (dev)
- **Auth:** `Bearer ravaa_token` (sama kayak web, via `POST /api/v1/auth/login`)
- **Stack:** Kotlin + Jetpack Compose + Retrofit + Room + WorkManager (offline sync) + DataStore

## Fitur (parity dengan Drive web 2713)
- Login via Ravaa Service (2711)
- List files/folders, breadcrumb, search
- Upload chunked 2MB resumable (pause/resume, 5 paralel) — `/mobile/drive/chunk`
- Download + offline cache
- Share LINK (password/expiry/maxViews) — `/mobile/drive/*` proxy

## Dev
```bash
# Gateway env
RAVAA_SERVICE_URL=http://10.0.2.2:2711 # emulator -> host
```

Terkoneksi ke `ravaa-service` mobile gateway:
- `GET /api/v1/mobile/drive/files`
- `POST /api/v1/mobile/drive/files/upload`
- `POST /api/v1/mobile/drive/chunk` + `GET /mobile/drive/chunk/status`
