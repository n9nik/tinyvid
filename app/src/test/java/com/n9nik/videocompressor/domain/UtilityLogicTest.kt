package com.n9nik.videocompressor.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UtilityLogicTest {
    @Test
    fun formatBytes_works() {
        assertEquals("500 B", VideoCompressor.formatBytes(500))
        assertTrue(VideoCompressor.formatBytes(2048).contains("KB"))
        assertTrue(VideoCompressor.formatBytes(3 * 1024 * 1024).contains("MB"))
    }

    @Test
    fun presets_areSane() {
        assertEquals(3, VideoCompressor.PRESETS.size)
        VideoCompressor.PRESETS.forEach { p ->
            assertTrue(p.targetWidth > 0 && p.targetHeight > 0 && p.videoBitrateBps > 0)
        }
    }

    @Test
    fun estimateOutputBytes_scalesWithDuration() {
        val preset = VideoCompressor.PRESETS[1]
        val one = VideoCompressor.estimateOutputBytes(preset, 60_000)
        val two = VideoCompressor.estimateOutputBytes(preset, 120_000)
        assertTrue(one > 0)
        assertEquals(one * 2, two)
    }

    @Test
    fun formatDuration_works() {
        assertEquals("45s", VideoCompressor.formatDuration(45_000))
        assertEquals("2m 5s", VideoCompressor.formatDuration(125_000))
    }
}
