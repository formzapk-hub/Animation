package com.example.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object FrameDrawer {

    private var isOutlineEnabled: Boolean = false
    private var outlineColorHex: String = "#000000"
    private var outlineWidth: Float = 4.0f

    private fun Canvas.drawText(text: String, x: Float, y: Float, paint: Paint) {
        if (isOutlineEnabled) {
            val strokePaint = Paint(paint).apply {
                style = Paint.Style.STROKE
                strokeWidth = outlineWidth
                color = try { Color.parseColor(outlineColorHex) } catch(e: Exception) { Color.BLACK }
                clearShadowLayer()
            }
            this.drawText(text, x, y, strokePaint)
        }
        this.drawText(text, x, y, paint)
    }

    fun drawFrame(
        canvas: Canvas,
        width: Int,
        height: Int,
        frameIdx: Int,
        text: String,
        animType: String,
        speedMult: Float,
        textColorHex: String,
        bgColorHex: String,
        baseFontSize: Float,
        fontName: String = "Montserrat",
        context: android.content.Context? = null,
        isShadowEnabled: Boolean = false,
        shadowColorHex: String = "#000000",
        shadowRadius: Float = 8f,
        shadowDx: Float = 4f,
        shadowDy: Float = 4f,
        isOutlineEnabled: Boolean = false,
        outlineColorHex: String = "#000000",
        outlineWidth: Float = 4f
    ) {
        this.isOutlineEnabled = isOutlineEnabled
        this.outlineColorHex = outlineColorHex
        this.outlineWidth = outlineWidth

        // Parse colors safely
        val bgColor = try { Color.parseColor(bgColorHex) } catch (e: Exception) { Color.parseColor("#121212") }
        val textColor = try { Color.parseColor(textColorHex) } catch (e: Exception) { Color.parseColor("#00E5FF") }

        // Clear background
        canvas.drawColor(bgColor)

        val cx = width / 2f
        val cy = height / 2f

        val typeface = if (context != null) {
            val resId = when (fontName) {
                "Montserrat" -> com.example.R.font.montserrat
                "Playfair Display" -> com.example.R.font.playfair_display
                "Pacifico" -> com.example.R.font.pacifico
                "Roboto Mono" -> com.example.R.font.roboto_mono
                "Bebas Neue" -> com.example.R.font.bebas_neue
                else -> com.example.R.font.montserrat
            }
            try {
                androidx.core.content.res.ResourcesCompat.getFont(context, resId)
            } catch (e: Exception) {
                android.graphics.Typeface.DEFAULT
            }
        } else {
            android.graphics.Typeface.DEFAULT
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = baseFontSize
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
            if (typeface != null) {
                setTypeface(typeface)
            }
            if (isShadowEnabled) {
                val sc = try { Color.parseColor(shadowColorHex) } catch(e: Exception) { Color.BLACK }
                setShadowLayer(shadowRadius, shadowDx, shadowDy, sc)
            }
        }

        when (animType) {
            "Teks Melingkar" -> {
                val angleOffset = frameIdx * 4f * speedMult
                val radius = width * 0.25f // Responsive radius
                val numChars = text.length
                if (numChars > 0) {
                    val paintChar = Paint(paint).apply { textAlign = Paint.Align.LEFT }
                    for (i in 0 until numChars) {
                        val char = text[i].toString()
                        val charAngle = (i * (360f / numChars)) + angleOffset
                        val rad = Math.toRadians(charAngle.toDouble())
                        val px = (cx + radius * cos(rad)).toFloat()
                        val py = (cy + radius * sin(rad)).toFloat()

                        canvas.save()
                        // Rotate text relative to circle center
                        canvas.rotate(charAngle + 90f, px, py)
                        canvas.drawText(char, px - paintChar.measureText(char) / 2f, py, paintChar)
                        canvas.restore()
                    }
                }
            }

            "Teks Efek Ketik" -> {
                val charsToShow = (frameIdx * 0.3f * speedMult).toInt().coerceIn(0, text.length)
                var dispStr = text.substring(0, charsToShow)
                if ((frameIdx * speedMult / 5).toInt() % 2 == 0) {
                    dispStr += "_"
                }

                // Draw centered
                canvas.drawText(dispStr, cx, cy, paint)
            }

            "Teks Berjalan" -> {
                val textWidth = paint.measureText(text)
                var mx = width - (frameIdx * 8f * speedMult)
                while (mx < -textWidth) {
                    mx += width + textWidth
                }
                val paintLeft = Paint(paint).apply { textAlign = Paint.Align.LEFT }
                canvas.drawText(text, mx, cy, paintLeft)
            }

            "Teks Memudar" -> {
                val alpha = ((sin(frameIdx * 0.08f * speedMult) + 1f) / 2f).coerceIn(0f, 1f)
                val fadingPaint = Paint(paint).apply {
                    this.alpha = (alpha * 255).toInt()
                }
                canvas.drawText(text, cx, cy, fadingPaint)
            }

            "Teks Melompat" -> {
                val bounceY = cy + abs(sin(frameIdx * 0.1f * speedMult)) * -300f
                canvas.drawText(text, cx, bounceY, paint)
            }

            "Teks Zoom In" -> {
                val scale = 0.5f + (frameIdx * 0.02f * speedMult)
                val zoomPaint = Paint(paint).apply {
                    textSize = baseFontSize * scale
                }
                canvas.drawText(text, cx, cy, zoomPaint)
            }

            "Teks Bergetar" -> {
                val shakeX = sin(frameIdx * 1.5f * speedMult) * 20f
                val shakeY = cos(frameIdx * 2.0f * speedMult) * 15f
                canvas.drawText(text, cx + shakeX, cy + shakeY, paint)
            }

            "Teks Bergelombang" -> {
                val totalW = paint.measureText(text)
                var startX = cx - totalW / 2f
                val paintLeft = Paint(paint).apply { textAlign = Paint.Align.LEFT }

                for (i in text.indices) {
                    val char = text[i].toString()
                    val charW = paintLeft.measureText(char)
                    val waveY = cy + sin((frameIdx * 0.15f * speedMult) + (i * 0.5f)) * 80f
                    canvas.drawText(char, startX, waveY, paintLeft)
                    startX += charW
                }
            }

            "Teks Jatuh" -> {
                val targetY = cy
                val currentY = (-100f + (frameIdx * 25f * speedMult)).coerceAtMost(targetY)
                canvas.drawText(text, cx, currentY, paint)
            }

            "Teks Terbelah" -> {
                val halfLen = text.length / 2
                val part1 = text.substring(0, halfLen)
                val part2 = text.substring(halfLen)
                val offset = (500f - (frameIdx * 15f * speedMult)).coerceAtLeast(0f)

                val paintLeft = Paint(paint).apply { textAlign = Paint.Align.LEFT }
                val w1 = paintLeft.measureText(part1)

                val tx1 = cx - w1 - offset
                val tx2 = cx + offset

                canvas.drawText(part1, tx1, cy, paintLeft)
                canvas.drawText(part2, tx2, cy, paintLeft)
            }

            "Teks Neon Glitch" -> {
                val glitchActive = (frameIdx * speedMult).toInt() % 12 in listOf(3, 4, 9)
                val shakeX = if (glitchActive) ((frameIdx * 37) % 15 - 7f) else 0f
                val shakeY = if (glitchActive) ((frameIdx * 53) % 11 - 5f) else 0f

                val pCyan = Paint(paint).apply { color = Color.parseColor("#00FFFF"); alpha = 180 }
                val pPink = Paint(paint).apply { color = Color.parseColor("#FF007F"); alpha = 180 }
                val pMain = Paint(paint).apply { color = textColor }

                // Chromatic offset
                canvas.drawText(text, cx + shakeX - 10f, cy + shakeY, pCyan)
                canvas.drawText(text, cx + shakeX + 10f, cy + shakeY, pPink)
                canvas.drawText(text, cx + shakeX, cy + shakeY, pMain)
            }

            "Teks Rotasi 3D" -> {
                val camera = android.graphics.Camera()
                val matrix = android.graphics.Matrix()
                val angle = frameIdx * 3f * speedMult

                canvas.save()
                camera.save()
                camera.rotateY(angle)
                camera.rotateX(angle * 0.3f)
                camera.getMatrix(matrix)
                camera.restore()

                matrix.preTranslate(-cx, -cy)
                matrix.postTranslate(cx, cy)

                canvas.concat(matrix)
                canvas.drawText(text, cx, cy, paint)
                canvas.restore()
            }

            "Teks Putar 3D Silinder" -> {
                val angleOffset = frameIdx * 3f * speedMult
                val radius = width * 0.35f
                val numChars = text.length
                if (numChars > 0) {
                    val paintChar = Paint(paint).apply { textAlign = Paint.Align.CENTER }
                    for (i in 0 until numChars) {
                        val char = text[i].toString()
                        val charAngle = (i * (360f / numChars)) + angleOffset
                        val rad = Math.toRadians(charAngle.toDouble())

                        val cosVal = cos(rad).toFloat()
                        val sinVal = sin(rad).toFloat()

                        val scale = 0.5f + 0.5f * (cosVal + 1f) / 2f
                        val px = cx + radius * sinVal
                        val py = cy + sinVal * 40f

                        val charPaint = Paint(paintChar).apply {
                            textSize = baseFontSize * scale
                            alpha = ((0.2f + 0.8f * (cosVal + 1f) / 2f) * 255).toInt().coerceIn(0, 255)
                        }
                        canvas.drawText(char, px, py, charPaint)
                    }
                }
            }

            "Teks Ledakan Partikel" -> {
                val cycle = (frameIdx * 0.03f * speedMult) % 2f
                val progress = if (cycle < 1f) cycle else (2f - cycle)

                val paintLeft = Paint(paint).apply { textAlign = Paint.Align.LEFT }
                val totalW = paintLeft.measureText(text)
                var startX = cx - totalW / 2f

                for (i in text.indices) {
                    val char = text[i].toString()
                    val charW = paintLeft.measureText(char)

                    val angle = (i * 12345.67f) % 360f
                    val rad = Math.toRadians(angle.toDouble())
                    val dist = progress * 220f

                    val px = startX + (cos(rad) * dist).toFloat()
                    val py = cy + (sin(rad) * dist).toFloat()

                    val pChar = Paint(paintLeft).apply {
                        alpha = ((1f - progress * 0.7f) * 255).toInt().coerceIn(0, 255)
                    }
                    canvas.drawText(char, px, py, pChar)
                    startX += charW
                }
            }

            "Teks Tirai Bayangan" -> {
                val steps = 12
                val pulse = (sin(frameIdx * 0.1f * speedMult) + 1f) / 2f
                val maxOffset = 150f * pulse

                for (i in steps downTo 1) {
                    val fraction = i.toFloat() / steps
                    val offset = fraction * maxOffset
                    val stepPaint = Paint(paint).apply {
                        alpha = ((1f - fraction) * 120).toInt()
                    }
                    canvas.drawText(text, cx + offset, cy + offset, stepPaint)
                }
                canvas.drawText(text, cx, cy, paint)
            }

            "Teks Ketukan Bass" -> {
                val beats = sin(frameIdx * 0.2f * speedMult)
                val pump = if (beats > 0.8f) (1f + (beats - 0.8f) * 1.5f) else 1.0f

                if (beats > 0.6f) {
                    val ringPaint = Paint().apply {
                        color = textColor
                        style = Paint.Style.STROKE
                        strokeWidth = 6f
                        alpha = ((2.0f - pump).coerceIn(0f, 1f) * 100).toInt()
                    }
                    val maxRadius = width * 0.35f * pump
                    canvas.drawCircle(cx, cy, maxRadius, ringPaint)
                }

                val pumpPaint = Paint(paint).apply {
                    textSize = baseFontSize * pump
                }
                canvas.drawText(text, cx, cy, pumpPaint)
            }

            "Teks Penjelajah Angkasa" -> {
                val progress = (frameIdx * 1.2f * speedMult) % 150f
                val z = 1.0f - (progress / 150f)

                if (z > 0.05f) {
                    val scale = z * z
                    val pSpace = Paint(paint).apply {
                        textSize = baseFontSize * scale * 2.0f
                        alpha = (z * 255).toInt().coerceIn(0, 255)
                    }
                    val py = cy - (1.0f - z) * 350f
                    canvas.drawText(text, cx, py, pSpace)
                }
            }

            else -> {
                // Default fallback
                canvas.drawText(text, cx, cy, paint)
            }
        }
    }
}
