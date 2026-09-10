package com.n9nik.videocompressor.domain

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Offline video compression via Media3 Transformer (hardware-accelerated).
 * Competitors use slow FFmpeg encodes that overheat phones; Transformer uses
 * the device's hardware encoder and is dramatically faster.
 */
object VideoCompressor {

    data class QualityPreset(
        val id: String,
        val label: String,
        val detail: String,
        val targetWidth: Int,
        val targetHeight: Int,
        val videoBitrateBps: Int
    )

    val PRESETS = listOf(
        QualityPreset("small", "Small", "360p • great for messaging", 640, 360, 800_000),
        QualityPreset("medium", "Medium", "720p • good balance", 1280, 720, 2_500_000),
        QualityPreset("high", "High", "1080p • near-original", 1920, 1080, 6_000_000)
    )

    data class VideoInfo(
        val width: Int,
        val height: Int,
        val durationMs: Long,
        val sizeBytes: Long,
        val rotation: Int,
        val bitrateBps: Int
    )

    fun getVideoInfo(context: Context, uri: Uri): VideoInfo {
        var width = 0; var height = 0; var durationMs = 0L; var rotation = 0; var bitrate = 0
        try {
            MediaMetadataRetriever().use { r ->
                r.setDataSource(context, uri)
                width = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                height = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                durationMs = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                rotation = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                bitrate = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
            }
        } catch (_: Exception) { }
        var size = 0L
        try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { size = it.length }
        } catch (_: Exception) { }
        return VideoInfo(width, height, durationMs, size, rotation, bitrate)
    }

    fun getThumbnail(context: Context, uri: Uri): Bitmap? {
        return try {
            MediaMetadataRetriever().use { r ->
                r.setDataSource(context, uri)
                r.getFrameAtTime(0)
            }
        } catch (_: Exception) { null }
    }

    /** Rough estimate: video bitrate * duration (+ ~128kbps audio). */
    fun estimateOutputBytes(preset: QualityPreset, durationMs: Long): Long {
        if (durationMs <= 0) return 0L
        val totalBps = preset.videoBitrateBps + 128_000L
        return totalBps * durationMs / 1000L / 8L
    }

    /**
     * Compress [uri] with [preset] into [outputFile]. Runs fully on-device.
     * Calls [onProgress] with 0..100 on the main thread.
     */
    suspend fun compress(
        context: Context,
        uri: Uri,
        preset: QualityPreset,
        outputFile: File,
        onProgress: (Int) -> Unit
    ): File = withContext(Dispatchers.Main) {
        // Never upscale, never exceed the source bitrate: output must be smaller.
        val info = getVideoInfo(context, uri)
        val effectiveBitrate = if (info.bitrateBps > 0)
            minOf(preset.videoBitrateBps, info.bitrateBps) else preset.videoBitrateBps
        val needsDownscale = info.width > 0 && info.height > 0 &&
            (info.width > preset.targetWidth || info.height > preset.targetHeight)

        suspendCancellableCoroutine { cont ->
            val encoderFactory = DefaultEncoderFactory.Builder(context)
                .setRequestedVideoEncoderSettings(
                    VideoEncoderSettings.Builder().setBitrate(effectiveBitrate).build()
                )
                .build()

            var transformer: Transformer? = null
            val listener = object : Transformer.Listener {
                override fun onCompleted(composition: Composition, result: ExportResult) {
                    transformer?.release()
                    if (cont.isActive) cont.resume(outputFile)
                }

                override fun onError(
                    composition: Composition,
                    result: ExportResult,
                    exception: ExportException
                ) {
                    transformer?.release()
                    if (cont.isActive) cont.resumeWithException(exception)
                }
            }

            transformer = Transformer.Builder(context)
                .setEncoderFactory(encoderFactory)
                .addListener(listener)
                .build()

            val effects = if (needsDownscale) {
                Effects(
                    emptyList(),
                    listOf(
                        Presentation.createForWidthAndHeight(
                            preset.targetWidth,
                            preset.targetHeight,
                            Presentation.LAYOUT_SCALE_TO_FIT
                        )
                    )
                )
            } else {
                Effects.EMPTY
            }
            val editedMediaItem = EditedMediaItem.Builder(MediaItem.fromUri(uri))
                .setEffects(effects)
                .build()

            if (outputFile.exists()) outputFile.delete()
            transformer.start(editedMediaItem, outputFile.absolutePath)

            cont.invokeOnCancellation {
                try { transformer?.cancel() } catch (_: Exception) { }
                try { transformer?.release() } catch (_: Exception) { }
            }

            // Progress polling on a background thread; listener resumes the coroutine.
            // onProgress is marshalled to the main thread for Compose state safety.
            val mainHandler = Handler(Looper.getMainLooper())
            Thread {
                val holder = Transformer.ProgressHolder()
                try {
                    while (cont.isActive) {
                        val state = transformer?.progress(holder)
                            ?: Transformer.PROGRESS_STATE_UNAVAILABLE
                        if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                            val pct = holder.progress.coerceIn(0, 100)
                            mainHandler.post { if (cont.isActive) onProgress(pct) }
                        }
                        if (state == Transformer.PROGRESS_STATE_AVAILABLE && holder.progress >= 100) break
                        Thread.sleep(400)
                    }
                } catch (_: InterruptedException) { }
            }.apply { isDaemon = true; start() }
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.0f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.1f MB", mb)
        return String.format("%.2f GB", mb / 1024.0)
    }

    fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val m = totalSec / 60
        val s = totalSec % 60
        return if (m > 0) "${m}m ${s}s" else "${s}s"
    }

    fun saveToCache(context: Context, source: File, name: String): File {
        val f = File(context.cacheDir, name)
        source.copyTo(f, overwrite = true)
        return f
    }
}
