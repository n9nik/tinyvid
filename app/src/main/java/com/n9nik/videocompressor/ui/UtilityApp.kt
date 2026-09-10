package com.n9nik.videocompressor.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.n9nik.videocompressor.ads.BannerAd
import com.n9nik.videocompressor.domain.VideoCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtilityApp(
    adsReady: Boolean,
    privacyOptionsAvailable: Boolean,
    onPrivacyOptions: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var videoInfo by remember { mutableStateOf<VideoCompressor.VideoInfo?>(null) }
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var preset by remember { mutableStateOf(VideoCompressor.PRESETS[1]) } // Medium default
    var isCompressing by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var outputFile by remember { mutableStateOf<File?>(null) }
    var savedUri by remember { mutableStateOf<Uri?>(null) }

    fun resetOutputs() {
        outputFile = null
        savedUri = null
        progress = 0
    }

    // Modern video picker (Android 13+), fallback to GetContent
    val pickVisualMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            pickedUri = uri
            resetOutputs()
            scope.launch(Dispatchers.IO) {
                val info = VideoCompressor.getVideoInfo(context, uri)
                val thumb = VideoCompressor.getThumbnail(context, uri)
                withContext(Dispatchers.Main) {
                    videoInfo = info
                    thumbnail = thumb
                }
            }
        }
    }
    val pickFallback = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pickedUri = uri
            resetOutputs()
            scope.launch(Dispatchers.IO) {
                val info = VideoCompressor.getVideoInfo(context, uri)
                val thumb = VideoCompressor.getThumbnail(context, uri)
                withContext(Dispatchers.Main) {
                    videoInfo = info
                    thumbnail = thumb
                }
            }
        }
    }

    fun launchPicker() {
        try {
            pickVisualMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        } catch (_: Exception) {
            pickFallback.launch("video/*")
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("TinyVid - Video Compressor") }) },
        bottomBar = { if (adsReady) BannerAd() },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("One job. No account. Fast by default.", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Offline video compression, no watermark, no cloud. Privacy first.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

            Button(onClick = { launchPicker() }, modifier = Modifier.fillMaxWidth(), enabled = !isCompressing) {
                Text(if (pickedUri == null) "Pick Video" else "Pick Different Video")
            }

            val info = videoInfo
            if (pickedUri != null && info != null) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Original • ${VideoCompressor.formatBytes(info.sizeBytes)} • ${info.width}×${info.height} • ${VideoCompressor.formatDuration(info.durationMs)}",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium), contentAlignment = Alignment.Center) {
                            if (thumbnail != null) {
                                Image(bitmap = thumbnail!!.asImageBitmap(), contentDescription = "Video thumbnail", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Text("No preview", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }

                Text("Quality:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    VideoCompressor.PRESETS.forEach { p ->
                        FilterChip(
                            selected = preset.id == p.id,
                            onClick = { preset = p; resetOutputs() },
                            label = { Text(p.label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                val estBytes = VideoCompressor.estimateOutputBytes(preset, info.durationMs)
                Text(
                    if (estBytes > 0) "${preset.detail} • ≈ ${VideoCompressor.formatBytes(estBytes)} estimated"
                    else preset.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        val uri = pickedUri ?: return@Button
                        isCompressing = true
                        progress = 0
                        outputFile = null
                        savedUri = null
                        scope.launch {
                            try {
                                val out = withContext(Dispatchers.IO) {
                                    File(context.cacheDir, "tinyvid_${System.currentTimeMillis()}.mp4")
                                }
                                VideoCompressor.compress(context, uri, preset, out) { pct ->
                                    progress = pct
                                }
                                withContext(Dispatchers.Main) {
                                    outputFile = out
                                    isCompressing = false
                                    progress = 100
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isCompressing = false
                                    scope.launch { snackbarHostState.showSnackbar("Compress failed: ${e.message?.take(120)}") }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCompressing
                ) {
                    Text(if (isCompressing) "Compressing… $progress%" else "Compress Now — Offline")
                }

                if (isCompressing) {
                    LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                    Text("Hardware encoding on your phone — no upload, no waiting on servers.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }

            val out = outputFile
            if (out != null && out.exists()) {
                val outSize = out.length()
                val origSize = info?.sizeBytes ?: 0L
                val savedPct = if (origSize > 0) ((1 - outSize.toDouble() / origSize) * 100).toInt().coerceIn(0, 99) else 0
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Compressed • ${VideoCompressor.formatBytes(outSize)} • saved $savedPct% • no watermark",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = {
                                scope.launch {
                                    val uri = saveToGallery(context, out)
                                    savedUri = uri
                                    snackbarHostState.showSnackbar(if (uri != null) "Saved to Movies/TinyVid" else "Save failed")
                                }
                            }, modifier = Modifier.weight(1f)) { Text("Save") }
                            OutlinedButton(onClick = {
                                scope.launch {
                                    var uri = savedUri
                                    if (uri == null) {
                                        uri = saveToGallery(context, out)
                                        savedUri = uri
                                    }
                                    if (uri != null) shareVideo(context, uri)
                                    else snackbarHostState.showSnackbar("Share failed")
                                }
                            }, modifier = Modifier.weight(1f)) { Text("Share") }
                            OutlinedButton(onClick = { resetOutputs() }, modifier = Modifier.weight(1f)) { Text("Clear") }
                        }
                        Text("Offline • No watermark • Before: ${VideoCompressor.formatBytes(origSize)} → After: ${VideoCompressor.formatBytes(outSize)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Why TinyVid wins", style = MaterialTheme.typography.titleSmall)
                    Text("• True offline — works in airplane mode\n• Hardware encoding: minutes, not 20-min overheating encodes\n• Never upscales or inflates your video\n• No watermark, no signup, no cloud upload\n• Small app, fast by default", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (privacyOptionsAvailable) {
                OutlinedButton(onClick = onPrivacyOptions, modifier = Modifier.fillMaxWidth()) { Text("Privacy choices") }
            }
            Text("Banner test ad only in debug. Release needs your AdMob IDs.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

private suspend fun saveToGallery(context: Context, file: File): Uri? = withContext(Dispatchers.IO) {
    try {
        val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val name = "TinyVid_$time.mp4"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/TinyVid")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
            resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES + "/TinyVid").apply { mkdirs() }
            val dest = File(dir, name)
            file.copyTo(dest, overwrite = true)
            android.media.MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), arrayOf("video/mp4"), null)
            Uri.fromFile(dest)
        }
    } catch (_: Exception) { null }
}

private fun shareVideo(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share video"))
}
