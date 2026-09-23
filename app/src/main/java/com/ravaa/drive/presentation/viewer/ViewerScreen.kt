package com.ravaa.drive.presentation.viewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.ravaa.drive.ui.theme.GlassBackground
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Viewer file ala Google Drive: gambar (geser untuk next/previous),
 * PDF, video internal, office via aplikasi luar. Gagal unduh (mis. 502)
 * bisa "Coba lagi" — file partial otomatis dibuang.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ViewerScreen(
    fileId: String,
    name: String,
    mime: String,
    galleryIds: List<String> = emptyList(),
    startIndex: Int = 0,
    onBack: () -> Unit,
    vm: ViewerViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val current by vm.current.collectAsState()
    val gallery by vm.gallery.collectAsState()

    LaunchedEffect(fileId) {
        vm.setGallery(galleryIds.ifEmpty { listOf(fileId) }, Triple(fileId, name, mime))
    }

    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, maxOf(galleryIds.size - 1, 0)),
        pageCount = { maxOf(gallery.size, 1) }
    )
    LaunchedEffect(pagerState.currentPage, gallery) {
        gallery.getOrNull(pagerState.currentPage)?.let { vm.openPage(it) }
        // Prefetch tetangga diam-diam (galeri tanpa jeda)
        gallery.getOrNull(pagerState.currentPage + 1)?.let { vm.prefetch(it) }
    }

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(current.second.ifBlank { name }, maxLines = 1, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Kembali", tint = Color.White) }
                    },
                    actions = {
                        if (state is ViewerState.Ready || state is ViewerState.External) {
                            IconButton(onClick = {
                                val f = (state as? ViewerState.Ready)?.file
                                    ?: (state as? ViewerState.External)?.file
                                if (f != null) vm.openExternal(f, ViewerViewModel.mimeFor(current.second, current.third))
                            }) { Icon(Icons.Filled.OpenInNew, "Buka di aplikasi lain", tint = Color.White) }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                if (gallery.size > 1) {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) {
                        PageContent(vm = vm)
                    }
                    if (gallery.size > 1) {
                        Text(
                            "${pagerState.currentPage + 1} / ${gallery.size}",
                            color = Color.White.copy(0.6f),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                        )
                    }
                } else {
                    PageContent(vm = vm)
                }
            }
        }
    }
}

@Composable
private fun PageContent(vm: ViewerViewModel) {
    val state by vm.state.collectAsState()
    val current by vm.current.collectAsState()
    val kind = remember(current) { ViewerViewModel.viewerKind(current.second, current.third) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val s = state) {
            is ViewerState.Loading -> CircularProgressIndicator(color = Color(0xFF3B82F6))
            is ViewerState.Downloading -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color(0xFF3B82F6))
                Spacer(Modifier.height(12.dp))
                Text("Mengunduh... ${s.progress}%", color = Color.White.copy(0.75f))
            }
            is ViewerState.Ready -> when (kind) {
                ViewerKind.IMAGE -> ImagePage(vm = vm, file = s.file, fileId = current.first)
                ViewerKind.VIDEO -> VideoPlayer(vm = vm, fileId = current.first)
                ViewerKind.PDF -> PdfPages(s.file)
                ViewerKind.EXTERNAL -> ExternalNote(current.second, current.third, auto = false, onOpen = { vm.openExternal(s.file, ViewerViewModel.mimeFor(current.second, current.third)) })
            }
            is ViewerState.External -> ExternalNote(current.second, current.third, auto = true, onOpen = { vm.openExternal(s.file, ViewerViewModel.mimeFor(current.second, current.third)) })
            is ViewerState.Error -> Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Gagal memuat", color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text(s.msg, color = Color(0xFFF87171), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { vm.retry() }) {
                    Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Coba lagi")
                }
            }
        }
    }
}

/**
 * Gambar thumb-dulu: thumb kecil (biasanya sudah di cache list) tampil
 * instan, full crossfade menyusul — tanpa layar "Mengunduh...".
 */
@Composable
private fun ImagePage(vm: ViewerViewModel, file: File, fileId: String) {
    val ctx = LocalContext.current
    var thumbGone by remember(file) { mutableStateOf(false) }
    var fullGone by remember(file) { mutableStateOf(false) }
    if (thumbGone && fullGone) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Gambar gagal dimuat", color = Color.White)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { thumbGone = false; fullGone = false; vm.retry() }) {
                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Coba lagi")
            }
        }
    } else {
        Box(Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
            if (!thumbGone) {
                AsyncImage(
                    model = vm.thumbUrl(fileId),
                    imageLoader = vm.imageLoader,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    onError = { thumbGone = true },
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (!fullGone) {
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(ctx)
                        .data(file)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    onError = { fullGone = true },
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (thumbGone && !fullGone) {
                CircularProgressIndicator(color = Color(0xFF3B82F6))
            }
        }
    }
}

@Composable
private fun ExternalNote(name: String, mime: String, auto: Boolean = false, onOpen: () -> Unit) {
    LaunchedEffect(name) { if (auto) onOpen() }
    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(name, color = Color.White, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Dibuka di aplikasi terinstall (${ViewerViewModel.mimeFor(name, mime)})",
            color = Color.White.copy(0.7f),
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onOpen) { Text("Buka lagi") }
    }
}

/**
 * Video streaming progresif ala Google Drive: langsung putar dari URL + auth,
 * tanpa menunggu unduhan penuh (server dukung Range/206).
 */
@Composable
private fun VideoPlayer(vm: ViewerViewModel, fileId: String) {
    val ctx = LocalContext.current
    var token by remember(fileId) { mutableStateOf<String?>(null) }
    LaunchedEffect(fileId) { token = vm.authToken() }
    val t = token
    val player = remember(fileId, t) {
        if (t == null) null else ExoPlayer.Builder(ctx).build().apply {
            val httpFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                .setDefaultRequestProperties(mapOf("Authorization" to "Bearer $t"))
            val source = androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(httpFactory)
                .createMediaSource(MediaItem.fromUri(vm.rawUrl(fileId)))
            setMediaSource(source)
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(fileId) { onDispose { player?.release() } }
    val p = player
    if (p == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF3B82F6))
        }
        return
    }
    var playing by remember { mutableStateOf(p.isPlaying) }
    var pos by remember { mutableStateOf(0L) }
    var dur by remember { mutableStateOf(0L) }
    DisposableEffect(p) {
        val l = object : Player.Listener {
            override fun onIsPlayingChanged(v: Boolean) { playing = v }
        }
        p.addListener(l)
        onDispose { p.removeListener(l) }
    }
    LaunchedEffect(p) {
        while (isActive) {
            pos = p.currentPosition.coerceAtLeast(0L)
            dur = p.duration.coerceAtLeast(0L).takeIf { it > 0 } ?: dur
            delay(500)
        }
    }
    Column(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { c ->
                PlayerView(c).apply {
                    setPlayer(p)
                    useController = false
                }
            },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (playing) p.pause() else p.play() }) {
                Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.White)
            }
            Slider(
                value = pos.toFloat(),
                onValueChange = { p.seekTo(it.toLong()) },
                valueRange = 0f..dur.coerceAtLeast(1L).toFloat(),
                modifier = Modifier.weight(1f)
            )
            Text(fmtTime(pos), color = Color.White.copy(0.75f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun fmtTime(ms: Long): String {
    val s = (ms / 1000).toInt()
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
private fun PdfPages(file: File) {
    var bitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(file) {
        bitmaps = emptyList()
        error = null
        try {
            val pfd: ParcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val out = mutableListOf<Bitmap>()
            val count = minOf(renderer.pageCount, 100)
            for (i in 0 until count) {
                renderer.openPage(i).use { page ->
                    val scale = 1080f / page.width
                    val bmp = Bitmap.createBitmap((page.width * scale).toInt(), (page.height * scale).toInt(), Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    out.add(bmp)
                }
            }
            renderer.close()
            pfd.close()
            bitmaps = out
            if (out.isEmpty()) error = "PDF kosong"
        } catch (e: Exception) {
            error = e.message ?: "Gagal render PDF"
        }
    }
    if (error != null) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(error!!, color = Color(0xFFF87171))
        }
    } else if (bitmaps.isEmpty()) {
        CircularProgressIndicator(color = Color(0xFF3B82F6))
    } else {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(bitmaps) { bmp ->
                Image(bmp.asImageBitmap(), null, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
            }
        }
    }
}
