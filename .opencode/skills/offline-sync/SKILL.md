name: offline-sync
description: Reference for the offline-first sync system — Room cache, outbox queue, repository pattern, WorkManager sync, connectivity handling. Use when touching data/db/Cache.kt, data/repository/DriveRepository.kt, data/sync/, offline banner, pending ops, or adding any feature that must work offline. Triggers on "offline", "sync", "room", "cache", "outbox", "pending", "workmanager", "airplane".
---

# Offline-First Sync

## Architecture

```
UI (Flow) ← Room cached_items ← pull (API) ← server (wins conflicts)
                 ↑                        ↓
              optimistic              outbox pending_ops → replay FIFO → remove on success
```

- `data/db/Cache.kt`: `CachedItem` (id, name, kind, mime, size, folderId, parentId, starred, trashed, updatedAt) + `PendingOp` (seq, op STAR|RENAME|TRASH, itemId, payload JSON) + DAOs + `AppDatabase("ravaa-drive.db")`.
- `data/repository/DriveRepository.kt`: single entry for Drive data. Reads = Room Flows. Writes = online-direct else optimistic+queue. `sync()` = replay outbox in order (stop at first failure to preserve order) then pull root + trash.
- `data/sync/SyncWorker.kt` (`@HiltWorker`): calls `repo.sync()`, retry ×3. Scheduled periodic 15min from `RavaaApp`, on-demand via `SyncWorker.syncNow(ctx)` (used by pull-to-refresh).
- `data/sync/ConnectivityObserver.kt`: `isOnlineNow()` + `isOnline` Flow. DriveViewModel observes it: offline → banner + cache-only; regain → auto `repo.sync()`.

## Rules

- NEVER call `api.list*/search` directly for cached views (root/folder/starred/recent/trash) — go through `repo` Flows + `pull*`.
- New mutation? Add op type + replay branch + optimistic cache update, all in `DriveRepository`.
- `createFolder` / `restore` / `deletePermanent` / uploads require online (IDs/server truth needed) — throw clear Indonesian message, VM surfaces it.
- Outbox payloads are tiny JSON (`{"name":...}`, `{"starred":bool}`, `{"kind":...}`) — never store file bytes here.
- Server wins on conflict (pull overwrites). Document any exception.
- NOT offline (by design, phase 2): file bytes, uploads while offline, notes/todos. Say so explicitly if asked.

## Verification

- `assembleDebug` passes; install; login online (populate cache).
- `adb shell svc wifi disable; adb shell svc data disable` → banner "Offline — menampilkan cache", lists work.
- Star/rename/trash offline → UI updates instantly + pending badge.
- Re-enable → auto sync → verify on server API + outbox empty (`run-as ... databases/ravaa-drive.db*`, Room uses WAL — pull all 3 files).
- ALWAYS re-enable wifi/data after testing (`svc wifi enable; svc data enable`).
