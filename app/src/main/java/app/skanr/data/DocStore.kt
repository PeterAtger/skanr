package app.skanr.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class Doc(
    val id: String,
    val name: String,
    val created: Long,
    val pages: List<String>,
    val shared: Boolean = false,
    val text: String = "",
    val bytes: Long = 0,
)

object DocStore {
    private lateinit var root: File
    private lateinit var cache: File
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _docs = MutableStateFlow<List<Doc>>(emptyList())
    val docs: StateFlow<List<Doc>> = _docs

    fun init(context: Context) {
        root = File(context.filesDir, "docs").apply { mkdirs() }
        cache = context.cacheDir
        _docs.value = root.listFiles().orEmpty().mapNotNull(::read).sortedByDescending { it.created }
    }

    fun get(id: String): Doc? = _docs.value.find { it.id == id }

    fun pageFile(doc: Doc, page: String) = File(File(root, doc.id), page)

    fun pageFiles(doc: Doc) = doc.pages.map { pageFile(doc, it) }

    suspend fun importScan(context: Context, uris: List<Uri>): List<File> = withContext(Dispatchers.IO) {
        val dir = File(cache, "pending").apply { mkdirs() }
        uris.map { uri ->
            val out = File(dir, "p_${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)!!.use { input -> out.outputStream().use { input.copyTo(it) } }
            out
        }
    }

    suspend fun create(name: String, files: List<File>): Doc = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val dir = File(root, id).apply { mkdirs() }
        val pages = files.map { moveInto(it, dir) }
        val doc = withBytes(Doc(id = id, name = name, created = System.currentTimeMillis(), pages = pages))
        write(doc)
        _docs.update { (listOf(doc) + it).sortedByDescending { d -> d.created } }
        indexText(doc.id)
        doc
    }

    suspend fun addPages(id: String, files: List<File>) = change(id, reindex = true) { doc ->
        doc.copy(pages = doc.pages + files.map { moveInto(it, File(root, id)) })
    }

    suspend fun replacePage(id: String, index: Int, file: File) = change(id, reindex = true) { doc ->
        val old = doc.pages[index]
        val name = moveInto(file, File(root, id))
        pageFile(doc, old).delete()
        doc.copy(pages = doc.pages.toMutableList().also { it[index] = name })
    }

    suspend fun deletePage(id: String, index: Int) = change(id, reindex = true) { doc ->
        pageFile(doc, doc.pages[index]).delete()
        doc.copy(pages = doc.pages.filterIndexed { i, _ -> i != index })
    }

    suspend fun movePage(id: String, from: Int, to: Int) = change(id) { doc ->
        doc.copy(pages = doc.pages.toMutableList().apply { add(to, removeAt(from)) })
    }

    suspend fun rotatePage(id: String, index: Int) = change(id) { doc ->
        val src = pageFile(doc, doc.pages[index])
        val bitmap = BitmapFactory.decodeFile(src.path)
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(90f) }, true)
        val name = "p_${UUID.randomUUID()}.jpg"
        File(src.parentFile, name).outputStream().use { rotated.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        bitmap.recycle()
        rotated.recycle()
        src.delete()
        doc.copy(pages = doc.pages.toMutableList().also { it[index] = name })
    }

    suspend fun rename(id: String, name: String) = change(id) { it.copy(name = name) }

    suspend fun markShared(id: String) = change(id) { it.copy(shared = true) }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        File(root, id).deleteRecursively()
        File(cache, "export/$id").deleteRecursively()
        _docs.update { list -> list.filterNot { it.id == id } }
    }

    private suspend fun change(id: String, reindex: Boolean = false, block: (Doc) -> Doc): Doc? = withContext(Dispatchers.IO) {
        val doc = get(id) ?: return@withContext null
        val next = withBytes(block(doc))
        write(next)
        File(cache, "export/$id").deleteRecursively()
        _docs.update { list -> list.map { if (it.id == id) next else it } }
        if (reindex) indexText(id)
        next
    }

    private fun indexText(id: String) {
        io.launch {
            val doc = get(id) ?: return@launch
            val text = pageFiles(doc).mapNotNull { Ocr.read(it)?.text }.joinToString("\n")
            val latest = get(id) ?: return@launch
            val next = latest.copy(text = text)
            write(next)
            _docs.update { list -> list.map { if (it.id == id) next else it } }
        }
    }

    private fun moveInto(file: File, dir: File): String {
        val name = "p_${UUID.randomUUID()}.jpg"
        val target = File(dir, name)
        if (!file.renameTo(target)) {
            file.copyTo(target, overwrite = true)
            file.delete()
        }
        return name
    }

    private fun withBytes(doc: Doc) = doc.copy(bytes = pageFiles(doc).sumOf { it.length() })

    private fun write(doc: Doc) {
        val json = JSONObject()
            .put("name", doc.name)
            .put("created", doc.created)
            .put("pages", JSONArray(doc.pages))
            .put("shared", doc.shared)
            .put("text", doc.text)
        val dir = File(root, doc.id).apply { mkdirs() }
        val tmp = File(dir, "doc.json.tmp")
        tmp.writeText(json.toString())
        tmp.renameTo(File(dir, "doc.json"))
    }

    private fun read(dir: File): Doc? = runCatching {
        val json = JSONObject(File(dir, "doc.json").readText())
        val pages = json.getJSONArray("pages").let { arr -> List(arr.length()) { arr.getString(it) } }
        withBytes(
            Doc(
                id = dir.name,
                name = json.getString("name"),
                created = json.getLong("created"),
                pages = pages,
                shared = json.optBoolean("shared"),
                text = json.optString("text"),
            )
        )
    }.getOrNull()
}
