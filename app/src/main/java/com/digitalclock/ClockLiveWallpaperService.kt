package com.digitalclock

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.core.content.res.ResourcesCompat
import java.sql.Date
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class ClockLiveWallpaperService : WallpaperService() {
    private val handler = Handler(Looper.getMainLooper())
    private var mcontext: Context? = null
    private var mBackground: Bitmap? = null
    override fun onCreateEngine(): Engine {
        mcontext = this
        mBackground = BitmapFactory.decodeResource(resources, R.drawable.back_wal)
        return ClockEngine()
    }

    private inner class ClockEngine : Engine() {
        private val matrix = Matrix()
        private val camera = Camera()
        private val paint = Paint()
        private val paint_stroke = Paint()
        private val _paintBlur = Paint()
        private val height: Int
        private val width: Int
        private var centerX = 0f
        private var centerY = 0f
        private var isVisible: Boolean?=false
        private val drawRunnable = Runnable { drawFrame() }
        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacks(drawRunnable)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            isVisible = visible
            if (visible) {
                drawFrame()
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            centerX = width / 2.0f
            centerY = height / 4.0f //2.0f;
            drawFrame()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            isVisible = false
            handler.removeCallbacks(drawRunnable)
        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int
        ) {
            super.onOffsetsChanged(xOffset,
                yOffset,
                xOffsetStep,
                yOffsetStep,
                xPixelOffset,
                yPixelOffset)
            drawFrame()
        }

        fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                canvas?.let { drawTime(it) }
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas)
            }

            // Reschedule the next redraw
            handler.removeCallbacks(drawRunnable)
            if (isVisible == true) {
                handler.postDelayed(drawRunnable, UPDATE_TIME_MILLIS.toLong())
            }
        }

        fun drawTime(canvas: Canvas) {
            canvas.save()
            val src = Rect(0, 0, mBackground!!.width - 1, mBackground!!.height - 1)
            val dest = Rect(0, 0, width - 1, height - 1)
            canvas.drawBitmap(mBackground!!, src, dest, null)
            camera.save()
            camera.getMatrix(matrix)
            matrix.postTranslate(centerX, centerY)
            canvas.concat(matrix)
            camera.restore()
            paint.textSize = resources.getDimension(R.dimen.text_size)
            paint.textAlign = Paint.Align.CENTER
            paint_stroke.textSize = resources.getDimension(R.dimen.text_size)
            paint_stroke.textAlign = Paint.Align.CENTER
            _paintBlur.textSize = resources.getDimension(R.dimen.text_size)
            _paintBlur.textAlign = Paint.Align.CENTER
            val text = getTimeIn12AmPm(System.currentTimeMillis()) //getTimeString();
            canvas.drawText(text, 0.0f, 0.0f, _paintBlur)
            canvas.drawText(text, 0.0f, 0.0f, paint_stroke)
            canvas.drawText(text, 0.0f, 0.0f, paint)
            canvas.restore()
        }

        /**
         * returns the time in 12 hrs format
         */
        private fun getTimeIn12AmPm(timestamp: Long): String {
            return try {
                val sdf: DateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val netDate = Date(timestamp)
                sdf.format(netDate)
            } catch (ex: Exception) {
                "xx"
            }
        }

        init {
            paint.isAntiAlias = true
            paint.isDither = true
            paint.color = resources.getColor(R.color.text_color)
            paint.style = Paint.Style.FILL
            paint.strokeJoin = Paint.Join.ROUND
            paint.strokeCap = Paint.Cap.ROUND
            paint.typeface = ResourcesCompat.getFont(mcontext!!, R.font.champagne_limousines)
            paint_stroke.isAntiAlias = true
            paint_stroke.isDither = true
            paint_stroke.color = resources.getColor(R.color.text_color_stroke)
            paint_stroke.style = Paint.Style.STROKE
            paint_stroke.strokeWidth = 5f
            paint_stroke.maskFilter = BlurMaskFilter(15F, BlurMaskFilter.Blur.OUTER)
            paint_stroke.strokeJoin = Paint.Join.ROUND
            paint_stroke.strokeCap = Paint.Cap.ROUND
            paint_stroke.typeface =
                ResourcesCompat.getFont(mcontext!!, R.font.champagne_limousines_bold)
            _paintBlur.set(paint)
            _paintBlur.strokeWidth = 30f
            _paintBlur.color = resources.getColor(R.color.text_color_trans)
            _paintBlur.maskFilter = BlurMaskFilter(45F, BlurMaskFilter.Blur.OUTER)
            _paintBlur.typeface =
                ResourcesCompat.getFont(mcontext!!, R.font.champagne_limousines_bold)
            val displayMetrics = mcontext!!.applicationContext.resources.displayMetrics
            height = displayMetrics.heightPixels
            width = displayMetrics.widthPixels
        }
    }

    companion object {
        private const val UPDATE_TIME_MILLIS = 40
    }
}