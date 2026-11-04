package app.skanr.scan

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

enum class ScannerStatus { Checking, Downloading, Ready, Unavailable }

object Scanner {
    var status by mutableStateOf(ScannerStatus.Checking)
        private set
    var progress by mutableFloatStateOf(0f)
        private set

    fun client(pageLimit: Int? = null) = GmsDocumentScanning.getClient(
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .apply { pageLimit?.let { setPageLimit(it) } }
            .build()
    )

    fun prepare(context: Context) {
        if (status == ScannerStatus.Ready || status == ScannerStatus.Downloading) return
        val installer = ModuleInstall.getClient(context)
        val scanner = client()
        installer.areModulesAvailable(scanner)
            .addOnSuccessListener { response ->
                if (response.areModulesAvailable()) {
                    status = ScannerStatus.Ready
                } else {
                    download(context, scanner)
                }
            }
            .addOnFailureListener { status = ScannerStatus.Unavailable }
    }

    private fun download(context: Context, scanner: com.google.mlkit.vision.documentscanner.GmsDocumentScanner) {
        status = ScannerStatus.Downloading
        val installer = ModuleInstall.getClient(context)
        lateinit var listener: InstallStatusListener
        listener = InstallStatusListener { update ->
            update.progressInfo?.let { info ->
                if (info.totalBytesToDownload > 0) progress = info.bytesDownloaded.toFloat() / info.totalBytesToDownload
            }
            when (update.installState) {
                InstallState.STATE_COMPLETED -> {
                    status = ScannerStatus.Ready
                    installer.unregisterListener(listener)
                }
                InstallState.STATE_CANCELED, InstallState.STATE_FAILED -> {
                    status = ScannerStatus.Unavailable
                    installer.unregisterListener(listener)
                }
            }
        }
        val request = ModuleInstallRequest.newBuilder().addApi(scanner).setListener(listener).build()
        installer.installModules(request)
            .addOnSuccessListener { if (it.areModulesAlreadyInstalled()) status = ScannerStatus.Ready }
            .addOnFailureListener { status = ScannerStatus.Unavailable }
    }
}

@Composable
fun rememberScanLauncher(onScanned: (List<Uri>) -> Unit, onError: () -> Unit): (pageLimit: Int?) -> Unit {
    val activity = LocalContext.current as Activity
    val latest by rememberUpdatedState(onScanned)
    val launcher = rememberLauncherForActivityResult(StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pages = GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.pages.orEmpty()
            if (pages.isNotEmpty()) latest(pages.map { it.imageUri })
        }
    }
    return { pageLimit ->
        Scanner.client(pageLimit).getStartScanIntent(activity)
            .addOnSuccessListener { launcher.launch(IntentSenderRequest.Builder(it).build()) }
            .addOnFailureListener { onError() }
    }
}
