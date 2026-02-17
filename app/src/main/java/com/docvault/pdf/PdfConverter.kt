package com.docvault.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Enhanced PdfConverter with color support and better page fitting.
 */
class PdfConverter(private val context: Context) {

    suspend fun convertImageToPdf(imageUri: Uri, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return@withContext false
            
            // Auto-Enhance but keep COLORS
            val enhancedBitmap = autoEnhanceBitmap(originalBitmap)
            
            val pdfDocument = PdfDocument()
            
            // Use standard A4 dimensions (at 72 DPI)
            val a4Width = 595
            val a4Height = 842
            
            // Calculate scale to fit image into A4 page while maintaining aspect ratio
            val scale = minOf(a4Width.toFloat() / enhancedBitmap.width, a4Height.toFloat() / enhancedBitmap.height)
            val finalWidth = (enhancedBitmap.width * scale).toInt()
            val finalHeight = (enhancedBitmap.height * scale).toInt()
            
            val pageInfo = PdfDocument.PageInfo.Builder(a4Width, a4Height, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            
            val canvas: Canvas = page.canvas
            
            // Center the image on the A4 page
            val left = (a4Width - finalWidth) / 2f
            val top = (a4Height - finalHeight) / 2f
            
            val rect = RectF(left, top, left + finalWidth, top + finalHeight)
            canvas.drawBitmap(enhancedBitmap, null, rect, Paint(Paint.FILTER_BITMAP_FLAG))
            
            pdfDocument.finishPage(page)
            
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            
            pdfDocument.close()
            originalBitmap.recycle()
            enhancedBitmap.recycle()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Enhances the bitmap while preserving color. 
     * Adjusts contrast and brightness to make it look "scanned".
     */
    private fun autoEnhanceBitmap(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val dest = Bitmap.createBitmap(width, height, src.config)
        
        val canvas = Canvas(dest)
        val paint = Paint()
        
        val cm = ColorMatrix()
        // Preserving saturation (color), but boosting contrast/brightness
        val contrast = 1.2f
        val brightness = 0f
        
        val scale = contrast
        val translate = brightness
        
        val matrix = floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
        
        cm.set(matrix)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        
        return dest
    }
}
