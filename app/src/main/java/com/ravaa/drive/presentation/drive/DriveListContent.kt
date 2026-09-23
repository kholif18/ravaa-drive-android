package com.ravaa.drive.presentation.drive

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.ravaa.drive.R
import com.ravaa.drive.data.api.DriveFile
import com.ravaa.drive.data.api.DriveFolder
import com.ravaa.drive.ui.theme.KoraIcons
import com.ravaa.drive.ui.theme.glassCard
import com.ravaa.drive.util.formatDate
import com.ravaa.drive.util.formatFileSize

/** Icon file Kora full-color; gambar/video coba thumbnail server dulu. */
@Composable
fun FileIcon(
    file: DriveFile,
    size: Dp,
    imageLoader: ImageLoader? = null,
    thumbUrl: String? = null
) {
    val kora = KoraIcons.forFile(file.name, file.mimeType ?: "")
    if (imageLoader != null && thumbUrl != null && KoraIcons.isPreviewable(file.name, file.mimeType ?: "")) {
        AsyncImage(
            model = thumbUrl,
            contentDescription = file.name,
            imageLoader = imageLoader,
            placeholder = painterResource(kora),
            error = painterResource(kora),
            fallback = painterResource(kora),
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(RoundedCornerShape(8.dp))
        )
    } else {
        Image(
            painterResource(kora),
            contentDescription = file.name,
            modifier = Modifier.size(size)
        )
    }
}

/** Galeri swipe khusus gambar: (ids joined ",", index file saat ini). */
fun galleryArgs(files: List<Any>, currentId: String): Pair<String, Int> {
    val ids = files.mapNotNull { item ->
        (item as? DriveFile)?.takeIf { f ->
            com.ravaa.drive.presentation.viewer.ViewerViewModel.viewerKind(f.name, f.mimeType ?: "") ==
                com.ravaa.drive.presentation.viewer.ViewerKind.IMAGE
        }?.id
    }
    return ids.joinToString(",") to ids.indexOf(currentId).coerceAtLeast(0)
}

/** Route viewer lengkap dengan galeri (untuk swipe next/previous). */
fun viewerRoute(file: DriveFile, files: List<Any>): String {
    val (gallery, index) = galleryArgs(files, file.id)
    return "viewer?fileId=${file.id}" +
        "&name=${android.net.Uri.encode(file.name)}" +
        "&mime=${android.net.Uri.encode(file.mimeType ?: "")}" +
        "&gallery=$gallery&index=$index"
}

/** Daftar file/folder gaya Drive Mac-glass — dipakai Drive, Search, Starred, Shared, Recent, Trash, Photos. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DriveItemsList(
    files: List<Any>,
    emptyText: String = "Kosong",
    onItemClick: (Any) -> Unit = {},
    onMoreClick: (Any) -> Unit = onItemClick,
    selectedIds: Set<String> = emptySet(),
    onLongPress: (Any) -> Unit = {},
    imageLoader: ImageLoader? = null,
    thumbUrl: (String) -> String = { "" },
    trailing: (@Composable (Any) -> Unit)? = null
) {
    val selectionMode = selectedIds.isNotEmpty()
    if (files.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyText, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.7f))
        }
    } else {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(files) { f ->
                val file = f as? DriveFile
                val folder = f as? DriveFolder
                val id = file?.id ?: folder?.id ?: ""
                val name = file?.name ?: folder?.name ?: "Unknown"
                val isFolder = folder != null
                val selected = id in selectedIds
                val sub = if (isFolder) {
                    "Folder"
                } else {
                    val parts = mutableListOf<String>()
                    if (file?.isStarred == true) parts.add("★")
                    parts.add(formatFileSize(file?.fileSize ?: 0L))
                    formatDate(file?.updatedAt).takeIf { it.isNotBlank() }?.let { parts.add(it) }
                    parts.joinToString(" • ")
                }
                ListItem(
                    headlineContent = { Text(name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, color = Color.White) },
                    supportingContent = { Text(sub, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = Color.White.copy(0.7f)) },
                    leadingContent = {
                        if (selected) {
                            Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(36.dp), tint = Color(0xFF3B82F6))
                        } else if (isFolder) {
                            Image(
                                painterResource(R.drawable.kora_folder),
                                contentDescription = name,
                                modifier = Modifier.size(36.dp)
                            )
                        } else if (file != null) {
                            FileIcon(file, 36.dp, imageLoader, thumbUrl(file.id).takeIf { imageLoader != null })
                        } else {
                            Icon(Icons.Filled.InsertDriveFile, null, modifier = Modifier.size(32.dp), tint = Color.White.copy(0.85f))
                        }
                    },
                    trailingContent = { trailing?.invoke(f) ?: IconButton(onClick = { onMoreClick(f) }) { Icon(Icons.Filled.MoreVert, null, tint = Color.White.copy(0.85f)) } },
                    colors = ListItemDefaults.colors(containerColor = if (selected) Color(0xFF3B82F6).copy(0.18f) else Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(16.dp)
                        .combinedClickable(
                            onClick = { if (selectionMode) onLongPress(f) else onItemClick(f) },
                            onLongClick = { onLongPress(f) }
                        )
                )
            }
        }
    }
}
