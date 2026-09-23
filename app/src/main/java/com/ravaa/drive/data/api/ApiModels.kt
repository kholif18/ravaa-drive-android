package com.ravaa.drive.data.api

/**
 * Model respons Ravaa-Drive (langsung ke port 2713, tanpa gateway).
 * Semua endpoint JSON memakai envelope: { "success": bool, "data": T, "error": String? }
 */

// ---------- Envelope ----------

data class ApiResponse<T>(
    val success: Boolean = false,
    val data: T? = null,
    val error: String? = null
)

data class ApiUser(
    val id: String = "",
    val email: String = "",
    val fullName: String? = null,
    val role: String = "user"
)

// ---------- Auth (POST /api/auth/mobile-login) ----------

data class MobileLoginRequest(
    val email: String,
    val password: String,
    val deviceName: String
)

data class MobileLoginData(
    val token: String = "",
    val user: ApiUser? = null,
    val scopes: List<String> = emptyList(),
    val prefix: String = "",
    val message: String? = null
)

data class TokenInfo(
    val user: ApiUser? = null,
    val scopes: List<String> = emptyList(),
    val prefix: String = ""
)

// ---------- Drive ----------

data class DriveFile(
    val id: String = "",
    val name: String = "",
    val mimeType: String? = null,
    val fileSize: Long = 0L,
    val folderId: String? = null,
    val ownerId: String? = null,
    val isStarred: Boolean = false,
    val updatedAt: String? = null,
    val createdAt: String? = null
)

data class DriveFolder(
    val id: String = "",
    val name: String = "",
    val parentId: String? = null,
    val ownerId: String? = null,
    val updatedAt: String? = null
)

/** Dipakai list files, search, trash (field yang tidak ada diabaikan Gson). */
data class DriveData(
    val files: List<DriveFile>? = null,
    val folders: List<DriveFolder>? = null,
    val nextCursor: String? = null
)

data class DriveListResponse(
    val success: Boolean = false,
    val data: DriveData? = null,
    val error: String? = null
)

data class StorageCategory(
    val key: String = "",
    val label: String = "",
    val color: String = "",
    val size: Long = 0L,
    val count: Int = 0,
    val percent: Double = 0.0
)

data class StorageStats(
    val totalFiles: Int = 0,
    val totalSize: Long = 0L,
    val storageLimit: Long = 0L,
    val storageUsed: Long = 0L,
    val categories: List<StorageCategory> = emptyList(),
    val used: Long = 0L,
    val limit: Long = 0L,
    val count: Int = 0,
    val percent: Int = 0
)

data class UploadResult(
    val id: String? = null,
    val name: String? = null
)

data class FileDetail(val file: DriveFile? = null)

data class FileVersion(
    val id: String = "",
    val fileId: String? = null,
    val fileSize: Long = 0L,
    val mimeType: String? = null,
    val createdAt: String? = null
)

data class ShareInfo(val shareToken: String? = null)

data class ShareResult(val share: ShareInfo? = null)

data class VersionList(val versions: List<FileVersion> = emptyList())

/** Chunked upload — GET status: { offset }. POST per-chunk: { offset, received } / terakhir: { file }. */
data class ChunkStatus(val offset: Long = 0L)

data class ChunkReceipt(
    val offset: Long? = null,
    val received: Int? = null,
    val file: DriveFile? = null
)

// ---------- Notes ----------

data class Notebook(
    val id: String = "",
    val name: String = "",
    val color: String? = null,
    val parentId: String? = null
)

data class Note(
    val id: String = "",
    val title: String = "",
    val content: String? = null,
    val notebookId: String? = null,
    val tags: String? = null,
    val isPinned: Boolean = false,
    val isStarred: Boolean = false,
    val updatedAt: String? = null
)

data class NotesData(
    val notes: List<Note>? = null,
    val nextCursor: String? = null
)

// ---------- Tasks ----------

data class TodoList(
    val id: String = "",
    val name: String = "",
    val color: String? = null
)

data class Todo(
    val id: String = "",
    val title: String = "",
    val description: String? = null,
    val isCompleted: Boolean = false,
    val priority: String = "MEDIUM",
    val dueDate: String? = null,
    val listId: String? = null
)
