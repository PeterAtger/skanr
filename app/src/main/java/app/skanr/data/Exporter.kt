package app.skanr.data

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.util.Locale

enum class Quality(val label: String, val note: String, val maxSide: Int, val jpeg: Int) {
    Small("Small", "Fine for email", 1400, 55),
    Standard("Standard", "Sharp when printed", 2200, 80),
    Original("Original", "Full camera quality", 0, 0),
}

object Exporter {
    private const val PDF_HEADER_AND_TRAILER = 1500L
    private const val PDF_PER_PAGE = 400L

    suspend fun pagesFor(context: Context, doc: Doc, quality: Quality): List<File> = withContext(Dispatchers.Default) {
        val sources = DocStore.pageFiles(doc)
        if (quality == Quality.Original) return@withContext sources
        val dir = File(context.cacheDir, "export/${doc.id}/${quality.name}").apply { mkdirs() }
        sources.map { src ->
            val out = File(dir, src.name)
            if (!out.exists()) {
                val bitmap = Images.decode(src, quality.maxSide * 2) ?: error("Can't read ${src.name}")
                val scaled = scaleDown(bitmap, quality.maxSide)
                val tmp = File(dir, src.name + ".tmp")
                tmp.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, quality.jpeg, it) }
                tmp.renameTo(out)
                if (scaled !== bitmap) scaled.recycle()
                bitmap.recycle()
            }
            out
        }
    }

    fun pdfBytes(imageBytes: Long, pageCount: Int) = imageBytes + PDF_HEADER_AND_TRAILER + pageCount * PDF_PER_PAGE

    suspend fun pdf(context: Context, doc: Doc, quality: Quality, searchable: Boolean): File = withContext(Dispatchers.Default) {
        val images = pagesFor(context, doc, quality)
        val originals = DocStore.pageFiles(doc)
        val text = if (searchable) originals.map { Ocr.read(it) } else originals.map { null }
        val out = File(shareDir(context), "${safeName(doc.name)}.pdf")
        out.outputStream().buffered().use { stream ->
            PdfWriter(stream).write(images.indices.map { i -> PdfPage(images[i], Images.size(originals[i]), text[i]) })
        }
        out
    }

    suspend fun images(context: Context, doc: Doc, quality: Quality): List<File> = withContext(Dispatchers.IO) {
        val pages = pagesFor(context, doc, quality)
        val dir = shareDir(context)
        val base = safeName(doc.name)
        pages.mapIndexed { i, page ->
            val name = if (pages.size == 1) "$base.jpg" else "$base ${i + 1}.jpg"
            page.copyTo(File(dir, name), overwrite = true)
        }
    }

    fun shareIntent(context: Context, files: List<File>, mime: String): Intent {
        val uris = files.map { FileProvider.getUriForFile(context, "${context.packageName}.files", it) }
        val send = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0])
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList<Uri>(uris))
        }
        send.type = mime
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(null, uris[0]).apply { uris.drop(1).forEach { addItem(ClipData.Item(it)) } }
        return Intent.createChooser(send, null)
    }

    suspend fun saveToDownloads(context: Context, files: List<File>, mime: String) = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        files.forEach { file ->
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("Downloads isn't available")
            resolver.openOutputStream(uri)!!.use { out -> file.inputStream().use { it.copyTo(out) } }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        }
    }

    private fun shareDir(context: Context) = File(context.cacheDir, "share").apply {
        deleteRecursively()
        mkdirs()
    }

    private fun safeName(name: String) = name.replace(Regex("[\\\\/:*?\"<>|]"), "-").trim().ifEmpty { "Scan" }

    private fun scaleDown(bitmap: Bitmap, maxSide: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxSide) return bitmap
        val k = maxSide.toFloat() / longest
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * k).toInt(), (bitmap.height * k).toInt(), true)
    }
}

class PdfPage(val jpeg: File, val originalSize: Pair<Int, Int>, val text: OcrPage?)

private class PdfWriter(private val out: OutputStream) {
    private var written = 0L
    private val offsets = sortedMapOf<Int, Long>()

    fun write(pages: List<PdfPage>) {
        raw("%PDF-1.4\n%âãÏÓ\n")
        val pageIds = pages.indices.map { 4 + it * 3 }
        obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
        obj(2, "<< /Type /Pages /Count ${pages.size} /Kids [${pageIds.joinToString(" ") { "$it 0 R" }}] >>")
        obj(3, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>")
        pages.forEachIndexed { i, page -> writePage(pageIds[i], page) }
        val xref = written
        val count = offsets.lastKey() + 1
        raw("xref\n0 $count\n0000000000 65535 f \n")
        for (n in 1 until count) raw(String.format(Locale.US, "%010d 00000 n \n", offsets.getValue(n)))
        raw("trailer\n<< /Size $count /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        out.flush()
    }

    private fun writePage(id: Int, page: PdfPage) {
        val (ow, oh) = page.originalSize
        val k = 842f / maxOf(ow, oh)
        val pw = ow * k
        val ph = oh * k
        val (iw, ih) = Images.size(page.jpeg)

        val content = StringBuilder()
        content.append("q ${f(pw)} 0 0 ${f(ph)} 0 0 cm /Im0 Do Q\n")
        page.text?.let { textLayer(content, it, pw, ph) }
        val contentBytes = content.toString().toByteArray(Charsets.ISO_8859_1)

        obj(id, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${f(pw)} ${f(ph)}] /Resources << /XObject << /Im0 ${id + 1} 0 R >> /Font << /F1 3 0 R >> >> /Contents ${id + 2} 0 R >>")
        val jpegLength = page.jpeg.length()
        start(id + 1)
        raw("<< /Type /XObject /Subtype /Image /Width $iw /Height $ih /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length $jpegLength >>\nstream\n")
        page.jpeg.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                bytes(buffer, n)
            }
        }
        raw("\nendstream\nendobj\n")
        start(id + 2)
        raw("<< /Length ${contentBytes.size} >>\nstream\n")
        bytes(contentBytes, contentBytes.size)
        raw("\nendstream\nendobj\n")
    }

    private fun textLayer(sb: StringBuilder, page: OcrPage, pw: Float, ph: Float) {
        val k = pw / page.width
        sb.append("BT 3 Tr\n")
        page.lines.forEach { line ->
            val text = line.text.trim()
            if (text.isEmpty()) return@forEach
            val h = line.box.height() * k
            val w = line.box.width() * k
            val size = (h * 0.8f).coerceAtLeast(1f)
            val scale = (100f * w / (0.5f * size * text.length)).coerceIn(10f, 500f)
            val x = line.box.left * k
            val y = ph - line.box.bottom * k + h * 0.15f
            sb.append("/F1 ${f(size)} Tf ${f(scale)} Tz 1 0 0 1 ${f(x)} ${f(y)} Tm (${escape(text)}) Tj\n")
        }
        sb.append("ET\n")
    }

    private fun escape(s: String) = buildString {
        s.forEach { c ->
            when {
                c == '\\' || c == '(' || c == ')' -> append('\\').append(c)
                c.code in 32..255 -> append(c)
                else -> append('?')
            }
        }
    }

    private fun f(v: Float) = String.format(Locale.US, "%.2f", v)

    private fun obj(n: Int, body: String) {
        start(n)
        raw("$body\nendobj\n")
    }

    private fun start(n: Int) {
        offsets[n] = written
        raw("$n 0 obj\n")
    }

    private fun raw(s: String) {
        val b = s.toByteArray(Charsets.ISO_8859_1)
        bytes(b, b.size)
    }

    private fun bytes(b: ByteArray, n: Int) {
        out.write(b, 0, n)
        written += n
    }
}
