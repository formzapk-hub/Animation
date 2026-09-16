package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Environment
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed class RenderState {
    object Idle : RenderState()
    data class GeneratingFrames(val current: Int, val total: Int) : RenderState()
    object CompilingVideo : RenderState()
    data class Success(val filePath: String) : RenderState()
    data class Error(val message: String) : RenderState()
}

class VideoRenderer(private val context: Context) {

    private val _renderState = MutableStateFlow<RenderState>(RenderState.Idle)
    val renderState: StateFlow<RenderState> = _renderState

    suspend fun renderVideo(
        text: String,
        animType: String,
        speedMult: Float,
        textColorHex: String,
        bgColorHex: String,
        baseFontSize: Float,
        width: Int,
        height: Int,
        fileName: String = "kinetic_text_${System.currentTimeMillis()}.mp4",
        isWatermarkMode: Boolean = false,
        bgVideoPath: String? = null,
        fontName: String = "Montserrat",
        isShadowEnabled: Boolean = false,
        shadowColorHex: String = "#000000",
        shadowRadius: Float = 8f,
        shadowDx: Float = 4f,
        shadowDy: Float = 4f,
        isOutlineEnabled: Boolean = false,
        outlineColorHex: String = "#000000",
        outlineWidth: Float = 4f
    ): File? = withContext(Dispatchers.IO) {
        val totalFrames = 150 // 5 seconds of 30 fps video
        val tempDir = File(context.cacheDir, "render_${System.currentTimeMillis()}")
        
        try {
            if (!tempDir.exists()) {
                tempDir.mkdirs()
            }

            // 1. Generate frames as PNG images
            for (i in 0 until totalFrames) {
                _renderState.value = RenderState.GeneratingFrames(i + 1, totalFrames)

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)

                FrameDrawer.drawFrame(
                    canvas = canvas,
                    width = width,
                    height = height,
                    frameIdx = i,
                    text = text,
                    animType = animType,
                    speedMult = speedMult,
                    textColorHex = textColorHex,
                    bgColorHex = if (isWatermarkMode) "#00000000" else bgColorHex,
                    baseFontSize = baseFontSize,
                    fontName = fontName,
                    context = context,
                    isShadowEnabled = isShadowEnabled,
                    shadowColorHex = shadowColorHex,
                    shadowRadius = shadowRadius,
                    shadowDx = shadowDx,
                    shadowDy = shadowDy,
                    isOutlineEnabled = isOutlineEnabled,
                    outlineColorHex = outlineColorHex,
                    outlineWidth = outlineWidth
                )

                val frameFile = File(tempDir, String.format("frame_%03d.png", i))
                FileOutputStream(frameFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                bitmap.recycle() // Protect against OutOfMemoryException
            }

            // 2. Prepare output file in app-specific Movies directory
            _renderState.value = RenderState.CompilingVideo
            val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                ?: context.filesDir
            
            if (!moviesDir.exists()) {
                moviesDir.mkdirs()
            }
            val outputFile = File(moviesDir, fileName)

            // Delete old file if exists
            if (outputFile.exists()) {
                outputFile.delete()
            }

            // 3. Compile using FFmpeg-Kit
            val actualBgPath = if (isWatermarkMode) {
                if (bgVideoPath != null && File(bgVideoPath).exists()) {
                    bgVideoPath
                } else {
                    // Generate a high-quality 5-second test-source pattern background video if none selected
                    val sampleBg = File(context.cacheDir, "sample_bg_pattern.mp4")
                    if (!sampleBg.exists()) {
                        val genCmd = "-y -f lavfi -i mandelbrot=size=${width}x${height}:rate=30 -t 5 -pix_fmt yuv420p \"${sampleBg.absolutePath}\""
                        FFmpegKit.execute(genCmd)
                    }
                    sampleBg.absolutePath
                }
            } else {
                null
            }

            val ffmpegCommand = if (isWatermarkMode && actualBgPath != null) {
                "-y -i \"$actualBgPath\" -framerate 30 -i \"${tempDir.absolutePath}/frame_%03d.png\" -filter_complex \"[0:v][1:v]overlay=0:0:shortest=1\" -pix_fmt yuv420p -c:a copy \"${outputFile.absolutePath}\""
            } else {
                "-y -framerate 30 -i \"${tempDir.absolutePath}/frame_%03d.png\" -c:v libx264 -pix_fmt yuv420p \"${outputFile.absolutePath}\""
            }

            Log.d("VideoRenderer", "Running FFmpeg: $ffmpegCommand")

            val session = FFmpegKit.execute(ffmpegCommand)
            val returnCode = session.returnCode

            if (ReturnCode.isSuccess(returnCode)) {
                Log.d("VideoRenderer", "FFmpeg completed successfully. Output saved to: ${outputFile.absolutePath}")
                _renderState.value = RenderState.Success(outputFile.absolutePath)
                return@withContext outputFile
            } else {
                val errorMsg = "FFmpeg failed with return code $returnCode: ${session.failStackTrace}"
                Log.e("VideoRenderer", errorMsg)
                _renderState.value = RenderState.Error("FFmpeg compile failed.")
                return@withContext null
            }

        } catch (e: Exception) {
            Log.e("VideoRenderer", "Error during video rendering process", e)
            _renderState.value = RenderState.Error(e.localizedMessage ?: "Unknown error occurred.")
            return@withContext null
        } finally {
            // Cleanup temporary frames safely
            try {
                tempDir.deleteRecursively()
            } catch (e: Exception) {
                Log.w("VideoRenderer", "Failed to clean up temp files", e)
            }
        }
    }

    fun resetState() {
        _renderState.value = RenderState.Idle
    }
}
