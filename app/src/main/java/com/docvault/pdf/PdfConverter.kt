package com.docvault.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Converts images to PDF documents.
 */
class PdfConverter(private val context: Context) {

    /**
     * Converts an image file to a PDF file in the app's internal storage.
     * @param imageUri The Uri of the source image.
     * @param outputFile The destination file for the PDF.
     */
    suspend fun convertImageToPdf(imageUri: Uri, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream) ?: return@withContext false
            
            val pdfDocument = PdfDocument()
            
            // Create a page with the same dimensions as the bitmap
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            
            // Draw the bitmap on the page
            val canvas: Canvas = page.canvas
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            
            pdfDocument.finishPage(page)
            
            // Save to the output file
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            
            pdfDocument.close()
            bitmap.recycle()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
