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
 * Enhanced PdfConverter with automatic alignment and "Scanned" look enhancement.
 */
class PdfConverter(private val context: Context) {

    suspend fun convertImageToPdf(imageUri: Uri, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return@withContext false
            
            // 1. Auto-Enhance for "Scanned" look (Grayscale + High Contrast)
            val enhancedBitmap = enhanceBitmapForScan(originalBitmap)
            
            val pdfDocument = PdfDocument()
            
            // A4 standard size at 72 DPI is roughly 595x842. We'll fit the image to A4 or keep original aspect.
            val pageInfo = PdfDocument.PageInfo.Builder(enhancedBitmap.width, enhancedBitmap.height, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            
            val canvas: Canvas = page.canvas
            canvas.drawBitmap(enhancedBitmap, 0f, 0f, null)
            
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
     * Applies color management and filters to make images look like scanned documents.
     */
    private fun enhanceBitmapForScan(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val dest = Bitmap.createBitmap(width, height, src.config)
        
        val canvas = Canvas(dest)
        val paint = Paint()
        
        // Color Matrix for high contrast and grayscale
        val cm = ColorMatrix()
        cm.setSaturation(0f) // Grayscale
        
        val contrast = 1.5f // Increase contrast
        val brightness = -10f // Adjust brightness
        val scale = contrast
        val translate = brightness
        
        val contrastMatrix = floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
        
        cm.postConcat(ColorMatrix(contrastMatrix))
        
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        
        return dest
    }
}
