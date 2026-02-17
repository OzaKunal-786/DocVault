package com.docvault.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream

/**
 * Handles on-device OCR using Google ML Kit.
 * Now supports both Images and PDFs.
 */
class OcrEngine(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Extracts text from an image.
     */
    suspend fun extractText(imageUri: Uri): String {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream) ?: return ""
            val image = InputImage.fromBitmap(bitmap, 0)
            
            val result = recognizer.process(image).await()
            bitmap.recycle()
            result.text
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Extracts text from a PDF (First page only for performance).
     */
    suspend fun extractTextFromPdf(pdfUri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r") ?: return@withContext ""
            val renderer = PdfRenderer(pfd)

            if (renderer.pageCount == 0) {
                pfd.close()
                renderer.close()
                return@withContext ""
            }

            val page = renderer.openPage(0)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()

            page.close()
            renderer.close()
            pfd.close()
            bitmap.recycle()

            result.text
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Extracts text from a bitmap.
     */
    suspend fun extractTextFromBitmap(bitmap: Bitmap): String {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            result.text
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
