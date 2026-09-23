package com.ravaa.drive.presentation.drive

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.ravaa.drive.data.api.DriveFile
import com.ravaa.drive.data.api.DriveFolder
import com.ravaa.drive.presentation.details.FileDetailsSheet
import com.ravaa.drive.presentation.details.ShareDialog
import com.ravaa.drive.presentation.details.VersionsSheet
import com.ravaa.drive.presentation.upload.UploadViewModel
import kotlinx.coroutines.launch
import com.ravaa.drive.presentation.upload.UploadWidget
import com.ravaa.drive.ui.theme.GlassBackground
import com.ravaa.drive.ui.theme.glassCard
import com.ravaa.drive.util.formatFileSize

/** My Drive ala Google Drive: search pill, breadcrumb folder, storage bar, list/grid, FAB+. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveScreen(
    nav: NavController? = null,
    onSessionExpired: () -> Unit = {},
    onMenu: () -> Unit = {},
    vm: DriveViewModel = hiltViewModel(),
    uploadVm: UploadViewModel = hiltViewModel()
) {
    val files by vm.files.collectAsState()
    val uploads by uploadVm.uploads.collectAsState()
    val stack by vm.stack.collectAsState()
    val selectedIds by vm.selectedIds.collectAsState()
    val selectionMode = selectedIds.isNotEmpty()
    val isOffline by vm.isOffline.collectAsState()
    val pendingCount by vm.pendingCount.collectAsState(0)
    val storage by vm.storage.collectAsState()
    val error by vm.error.collectAsState()
    val refreshing = vm.loading.collectAsState().value
    var isGrid by remember { mutableStateOf(false) }
    var showFabSheet by remember { mutableStateOf(false) }
    var showNewFolder by remember { mutableStateOf(false) }
    var folderName by remember { mutableStateOf("") }
    var detailsItem by remember { mutableStateOf<Any?>(null) }
    var moveItem by remember { mutableStateOf<Any?>(null) }
    var shareItem by remember { mutableStateOf<Any?>(null) }
    var versionsFileId by remember { mutableStateOf<String?>(null) }
    val moveFolders by vm.moveFolders.collectAsState()
    val scope = rememberCoroutineScope()
    var renameTarget by remember { mutableStateOf<Triple<String, String, Boolean>?>(null) }
    var renameText by remember { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { vm.load(null); vm.loadStorage() }
    LaunchedEffect(uploads.any { it.status == "success" }) {
        if (uploads.any { it.status == "success" }) vm.refresh()
    }
    LaunchedEffect(error) {
        error?.let {
            if (it == "SESSION_EXPIRED") onSessionExpired()
            else snackbar.showSnackbar(it)
            vm.clearError()
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { uploadVm.enqueue(it, vm.currentFolderId()) }
    }

    fun itemId(f: Any): String =
        (f as? DriveFile)?.id ?: (f as? DriveFolder)?.id ?: ""

    fun openViewer(file: DriveFile) {
        nav?.navigate(viewerRoute(file, files))
    }

    fun onItemClick(f: Any) {
        if (selectionMode) {
            vm.toggleSelect(itemId(f))
            return
        }
        when (f) {
            is DriveFolder -> vm.openFolder(f)
            is DriveFile -> openViewer(f)
            else -> detailsItem = f
        }
    }

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) },
            floatingActionButton = {
                FloatingActionButton(onClick = { showFabSheet = true }) { Icon(Icons.Filled.Add, null) }
            }
        ) { pad ->
            Column(Modifier.padding(pad).fillMaxSize().padding(horizontal = 12.dp)) {
                // Hamburger + search pill ala Google Drive
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onMenu, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Filled.Menu, "Menu", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    Box(
                        Modifier
                            .weight(1f)
                            .glassCard(28.dp)
                            .clickable { nav?.navigate("search") }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Search, null, tint = Color.White.copy(0.85f))
                            Spacer(Modifier.width(12.dp))
                            Text("Search in Drive", color = Color.White.copy(0.85f), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                // Mode seleksi (long-press): ganti breadcrumb jadi action bar
                if (selectionMode) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { vm.clearSelection() }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Filled.Close, "Batal", tint = Color.White)
                        }
                        Text(
                            "${selectedIds.size} dipilih",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { vm.deleteSelected() }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Filled.Delete, "Hapus", tint = Color(0xFFF87171))
                        }
                    }
                }
                // Banner offline + antrean menunggu sync
                if (isOffline) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .glassCard(12.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            if (pendingCount > 0) "Offline — $pendingCount perubahan menunggu sync"
                            else "Offline — menampilkan cache",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFBBF24)
                        )
                    }
                } else if (pendingCount > 0) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .glassCard(12.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "$pendingCount perubahan menunggu sync...",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(0.75f)
                        )
                    }
                }
                // Breadcrumb folder (sembunyi saat mode seleksi)
                if (!selectionMode) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "My Drive",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (stack.isEmpty()) Color.White else Color.White.copy(0.75f),
                        modifier = Modifier.clickable { vm.openCrumb(-1) }
                    )
                    stack.forEachIndexed { i, folder ->
                        Text("  /  ", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.55f))
                        Text(
                            folder.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (i == stack.lastIndex) Color.White else Color.White.copy(0.75f),
                            modifier = Modifier.clickable { vm.openCrumb(i) }
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { isGrid = !isGrid }, modifier = Modifier.size(36.dp)) {
                        Icon(if (isGrid) Icons.Filled.ViewList else Icons.Filled.GridView, null, modifier = Modifier.size(20.dp), tint = Color.White)
                    }
                    IconButton(onClick = { vm.refresh() }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(20.dp), tint = Color.White)
                    }
                }
                }
                // Storage bar mini
                storage?.let { st ->
                    val pct = if (st.limit > 0) (st.used.toDouble() / st.limit).coerceIn(0.0, 1.0).toFloat() else 0f
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { pct },
                            modifier = Modifier.weight(1f).height(4.dp),
                            trackColor = Color.White.copy(0.1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${formatFileSize(st.used)} of ${formatFileSize(st.limit)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(0.8f)
                        )
                    }
                }
                UploadWidget(uploadVm)
                SwipeRefresh(
                    state = rememberSwipeRefreshState(refreshing),
                    onRefresh = { vm.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isGrid && files.isNotEmpty()) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(files) { f ->
                            DriveGridItem(
                                f,
                                onClick = { onItemClick(f) },
                                onLongPress = { vm.toggleSelect(itemId(f)) },
                                selected = itemId(f) in selectedIds,
                                imageLoader = vm.imageLoader,
                                thumbUrl = vm::thumbUrl
                            )
                        }
                        }
                    } else {
                        DriveItemsList(
                            files,
                            "Empty — drop files here",
                            onItemClick = { onItemClick(it) },
                            onMoreClick = { detailsItem = it },
                            selectedIds = selectedIds,
                            onLongPress = { vm.toggleSelect(itemId(it)) },
                            imageLoader = vm.imageLoader,
                            thumbUrl = vm::thumbUrl
                        )
                    }
                }
            }
            if (showFabSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showFabSheet = false },
                    containerColor = Color(0xFF141414).copy(alpha = 0.92f)
                ) {
                ListItem(headlineContent = { Text("Upload file", color = Color.White) }, leadingContent = { Icon(Icons.Filled.Upload, null, tint = Color.White.copy(0.9f)) }, modifier = Modifier.clickable { showFabSheet = false; picker.launch("*/*") })
                ListItem(headlineContent = { Text("New folder", color = Color.White) }, leadingContent = { Icon(Icons.Filled.CreateNewFolder, null, tint = Color.White.copy(0.9f)) }, modifier = Modifier.clickable { showFabSheet = false; folderName = ""; showNewFolder = true })
                    Spacer(Modifier.height(24.dp))
                }
                if (showNewFolder) {
                    AlertDialog(
                        onDismissRequest = { showNewFolder = false },
                        title = { Text("New folder") },
                        text = { OutlinedTextField(value = folderName, onValueChange = { folderName = it }, label = { Text("Nama folder") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
                        confirmButton = {
                            TextButton(onClick = {
                                if (folderName.isNotBlank()) vm.createFolder(folderName.trim(), vm.currentFolderId())
                                showNewFolder = false
                            }) { Text("Buat") }
                        },
                        dismissButton = { TextButton(onClick = { showNewFolder = false }) { Text("Batal") } }
                    )
                }
            }
            detailsItem?.let { item ->
                val isFolder = item is DriveFolder
                val file = item as? DriveFile
                FileDetailsSheet(
                    item = item,
                    onDismiss = { detailsItem = null },
                    onToggleStar = { vm.toggleStar(it) },
                    onRename = { id, current -> renameTarget = Triple(id, current, isFolder); renameText = current },
                    onTrash = { id -> if (isFolder) vm.trashFolder(id) else vm.trashFile(id) },
                    onDownload = { id ->
                        scope.launch {
                            try {
                                val saved = vm.downloadToPublic(id, file?.name ?: "download", file?.mimeType)
                                snackbar.showSnackbar("Tersimpan di Download: $saved")
                            } catch (e: Exception) {
                                snackbar.showSnackbar(e.message ?: "Unduhan gagal")
                            }
                        }
                    },
                    onMove = { moveItem = item },
                    onShare = { shareItem = item },
                    onCopy = { vm.copyItem(itemId(item), isFolder) },
                    onVersions = { versionsFileId = itemId(item) }
                )
            }
            moveItem?.let { item ->
                val isFolder = item is DriveFolder
                MoveDialog(
                    folders = moveFolders,
                    onNavigate = { vm.loadMoveFolders(it) },
                    onDismiss = { moveItem = null },
                    onMove = { target ->
                        vm.moveItem(itemId(item), target)
                        moveItem = null
                    }
                )
            }
            shareItem?.let { item ->
                val file = item as? DriveFile
                val folder = item as? DriveFolder
                ShareDialog(
                    vm = vm,
                    itemId = itemId(item),
                    itemName = file?.name ?: folder?.name ?: "",
                    isFolder = item is DriveFolder,
                    onDismiss = { shareItem = null }
                )
            }
            versionsFileId?.let { fid ->
                VersionsSheet(vm = vm, fileId = fid, fileName = "", onDismiss = { versionsFileId = null })
            }
            renameTarget?.let { (id, _, isFolder) ->
                AlertDialog(
                    onDismissRequest = { renameTarget = null },
                    title = { Text("Rename") },
                    text = { OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
                    confirmButton = {
                        TextButton(onClick = {
                            if (renameText.isNotBlank()) {
                                if (isFolder) vm.renameFolder(id, renameText.trim()) else vm.renameFile(id, renameText.trim())
                            }
                            renameTarget = null
                        }) { Text("Simpan") }
                    },
                    dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Batal") } }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DriveGridItem(
    f: Any,
    onClick: () -> Unit = {},
    onLongPress: () -> Unit = {},
    selected: Boolean = false,
    imageLoader: coil.ImageLoader? = null,
    thumbUrl: (String) -> String = { "" }
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(16.dp)
            .border(
                2.dp,
                if (selected) Color(0xFF3B82F6) else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
    ) {
        Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                val file = f as? DriveFile
                val folder = f as? com.ravaa.drive.data.api.DriveFolder
                if (folder != null) {
                    androidx.compose.foundation.Image(
                        androidx.compose.ui.res.painterResource(com.ravaa.drive.R.drawable.kora_folder),
                        contentDescription = folder.name,
                        modifier = Modifier.size(48.dp)
                    )
                } else if (file != null) {
                    FileIcon(file, 48.dp, imageLoader, thumbUrl(file.id).takeIf { imageLoader != null })
                } else {
                    Icon(Icons.Filled.InsertDriveFile, null, modifier = Modifier.size(32.dp), tint = Color.White.copy(0.85f))
                }
                if (selected) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(20.dp).align(Alignment.TopEnd)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(f.let { (it as? DriveFile)?.name ?: (it as? com.ravaa.drive.data.api.DriveFolder)?.name ?: "Unknown" }, style = MaterialTheme.typography.bodySmall, maxLines = 2, color = Color.White)
        }
    }
}
