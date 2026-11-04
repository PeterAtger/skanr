package app.skanr.data

import android.graphics.BitmapFactory
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

data class OcrLine(val text: String, val box: Rect)

data class OcrPage(val width: Int, val height: Int, val lines: List<OcrLine>) {
    val text get() = lines.joinToString("\n") { it.text }
}

object Ocr {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    suspend fun read(file: File): OcrPage? = withContext(Dispatchers.Default) {
        runCatching {
            val bitmap = Images.decode(file, 2400) ?: return@runCatching null
            val result = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
            val lines = result.textBlocks.flatMap { block ->
                block.lines.mapNotNull { line -> line.boundingBox?.let { OcrLine(line.text, it) } }
            }
            OcrPage(bitmap.width, bitmap.height, lines).also { bitmap.recycle() }
        }.getOrNull()
    }

    fun suggestions(page: OcrPage): List<String> {
        val trim = charArrayOf(' ', ':', ',', '.', ';', '-', '|', '*')
        return page.lines
            .sortedByDescending { it.box.height() }
            .map { it.text.trim(*trim) }
            .filter { line -> line.length in 3..40 && line.count { it.isLetter() } >= 3 && '@' !in line }
            .distinctBy { it.lowercase() }
            .take(3)
    }
}

object Images {
    fun decode(file: File, maxSide: Int): android.graphics.Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    fun size(file: File): Pair<Int, Int> {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        return bounds.outWidth to bounds.outHeight
    }
}
